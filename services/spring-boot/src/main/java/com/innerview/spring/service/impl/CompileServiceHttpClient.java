package com.innerview.spring.service.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.innerview.spring.dto.ExecutionOutcome;
import com.innerview.spring.dto.ExecutionResult;
import com.innerview.spring.service.CompileServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Locale;

@Service
public class CompileServiceHttpClient implements CompileServicePort {

    private static final Logger log = LoggerFactory.getLogger(CompileServiceHttpClient.class);

    private final RestClient restClient;
    private final String executeUrl;

    public CompileServiceHttpClient(
            @Value("${judge.execution.url:http://127.0.0.1:2000/api/v2/execute}") String executeUrl) {
        this.executeUrl = executeUrl;
        // Piston C++ compilation can take ~4-5 seconds;
        // use a generous 30s read timeout to avoid silent failures.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000); // 5 s to connect
        factory.setReadTimeout(30_000); // 30 s to read the response
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Override
    public ExecutionResult execute(
            String code,
            String language,
            String input,
            Integer timeLimitMs,
            Integer memoryLimitMb) {
        // 1. Standardize language for Piston (Piston expects "c++" not "cpp")
        String pistonLanguage = language.equalsIgnoreCase("cpp") ? "c++" : language.toLowerCase();

        // 2. Assign a proper filename so the compiler knows how to handle it
        String fileName = "main" + getFileExtension(pistonLanguage);

        // 3. Build the exact payload Piston expects
        ExecuteRequest request = new ExecuteRequest(
                pistonLanguage,
                "*", // "*" tells Piston to use the latest installed version
                List.of(new SourceFile(fileName, code)), // Inject filename and code
                input != null ? input : "", // Inject the test case here!
                List.of(), // Empty args array
                10000, // compile_timeout
                timeLimitMs != null ? timeLimitMs : 3000, // run_timeout
                10000, // compile_cpu_time
                timeLimitMs != null ? timeLimitMs : 3000, // run_cpu_time
                -1L, // compile_memory_limit
                memoryLimitMb == null ? -1L : memoryLimitMb.longValue() * 1024 * 1024 // run_memory_limit
        );

        log.debug("Calling Piston at {} with language='{}', version='{}'", executeUrl, pistonLanguage, "*");
        try {
            ExecuteResponse response = restClient.post()
                    .uri(executeUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ExecuteResponse.class);

            log.debug("Piston response: language={}, version={}, compile={}, run={}",
                    response == null ? null : response.language(),
                    response == null ? null : response.version(),
                    response == null ? null : response.compile(),
                    response == null ? null : response.run());

            return mapResponse(response);

        } catch (Exception e) {
            log.error("Piston API call failed: {}", e.getMessage(), e);
            return ExecutionResult.builder()
                    .outcome(ExecutionOutcome.RUNTIME_ERROR)
                    .errorOutput("Piston API Error: " + e.getMessage())
                    .durationMs(0L)
                    .build();
        }
    }

    private String getFileExtension(String language) {
        return switch (language) {
            case "c++" -> ".cpp";
            case "java" -> ".java";
            case "python", "python3" -> ".py";
            case "go" -> ".go";
            case "javascript", "js" -> ".js";
            default -> ".txt";
        };
    }

    private ExecutionResult mapResponse(ExecuteResponse response) {
        if (response == null) {
            return ExecutionResult.builder()
                    .outcome(ExecutionOutcome.RUNTIME_ERROR)
                    .errorOutput("Execution service returned no response.")
                    .durationMs(0L)
                    .build();
        }

        ProcessStage compile = response.compile();
        ProcessStage run = response.run();

        // Check for Compilation Errors first
        if (compile != null && isFailure(compile.code())) {
            return ExecutionResult.builder()
                    .outcome(ExecutionOutcome.COMPILE_ERROR)
                    .errorOutput(firstNonBlank(compile.stderr(), compile.output(), compile.message()))
                    .durationMs(nullSafe(compile.wallTime()))
                    .memoryBytes(compile.memory())
                    .build();
        }

        // Check if run stage is missing
        if (run == null) {
            return ExecutionResult.builder()
                    .outcome(ExecutionOutcome.RUNTIME_ERROR)
                    .errorOutput("Execution service did not return a run result.")
                    .durationMs(nullSafe(compile == null ? null : compile.wallTime()))
                    .memoryBytes(compile == null ? null : compile.memory())
                    .build();
        }

        ExecutionOutcome outcome = resolveRunOutcome(run);

        return ExecutionResult.builder()
                .outcome(outcome)
                .actualOutput(firstNonBlank(run.stdout(), run.output(), null))
                .errorOutput(firstNonBlank(run.stderr(), run.message()))
                .durationMs(nullSafe(compile == null ? null : compile.wallTime()) + nullSafe(run.wallTime()))
                .memoryBytes(run.memory())
                .build();
    }

    private ExecutionOutcome resolveRunOutcome(ProcessStage run) {
        // Did it crash from a signal? (e.g., SIGKILL usually means out of memory or
        // hard timeout)
        if (run.signal() != null) {
            if (run.signal().contains("KILL") || run.signal().contains("TERM")) {
                return ExecutionOutcome.TIME_LIMIT_EXCEEDED;
            }
        }

        String raw = firstNonBlank(run.status(), run.message(), run.stderr());
        String failureText = raw == null ? "" : raw.toLowerCase(Locale.ROOT);

        if (failureText.contains("memory") || failureText.contains("mle") || failureText.contains("out of memory")) {
            return ExecutionOutcome.MEMORY_LIMIT_EXCEEDED;
        }
        if (failureText.contains("time") || failureText.contains("timeout") || failureText.contains("tle")) {
            return ExecutionOutcome.TIME_LIMIT_EXCEEDED;
        }
        if (isFailure(run.code())) {
            return ExecutionOutcome.RUNTIME_ERROR;
        }
        return ExecutionOutcome.SUCCESS;
    }

    private boolean isFailure(Integer code) {
        return code != null && code != 0;
    }

    private long nullSafe(Long value) {
        return value == null ? 0L : value;
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }

    // --- EXACT PISTON JSON STRUCTURE MAPPINGS ---

    private record ExecuteRequest(
            String language,
            String version,
            List<SourceFile> files,
            String stdin,
            List<String> args,
            @JsonProperty("compile_timeout") Integer compileTimeout,
            @JsonProperty("run_timeout") Integer runTimeout,
            @JsonProperty("compile_cpu_time") Integer compileCpuTime,
            @JsonProperty("run_cpu_time") Integer runCpuTime,
            @JsonProperty("compile_memory_limit") Long compileMemoryLimit,
            @JsonProperty("run_memory_limit") Long runMemoryLimit) {
    }

    private record SourceFile(String name, String content) {
    }

    private record ExecuteResponse(
            ProcessStage compile,
            ProcessStage run,
            String language,
            String version) {
    }

    private record ProcessStage(
            String signal,
            String stdout,
            String stderr,
            Integer code,
            String output,
            Long memory,
            String message,
            String status,
            @JsonProperty("cpu_time") Long cpuTime,
            @JsonProperty("wall_time") Long wallTime) {
    }
}