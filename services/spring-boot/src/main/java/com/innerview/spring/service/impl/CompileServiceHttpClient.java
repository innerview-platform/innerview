package com.innerview.spring.service.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.innerview.spring.dto.ExecutionOutcome;
import com.innerview.spring.dto.ExecutionResult;
import com.innerview.spring.service.CompileServicePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Locale;

@Service
public class CompileServiceHttpClient implements CompileServicePort {

    private final RestClient restClient;
    private final String executeUrl;

    public CompileServiceHttpClient(
            RestClient.Builder restClientBuilder,
            @Value("${judge.execution.url:http://127.0.0.1:2000/api/v2/execute}") String executeUrl
    ) {
        this.restClient = restClientBuilder.build();
        this.executeUrl = executeUrl;
    }

    @Override
    public ExecutionResult execute(
            String code,
            String language,
            String input,
            Integer timeLimitMs,
            Integer memoryLimitMb
    ) {
        ExecuteRequest request = new ExecuteRequest(
                language,
                "*",
                List.of(new SourceFile(code)),
                input,
                timeLimitMs,
                timeLimitMs,
                memoryLimitMb == null ? null : memoryLimitMb.longValue() * 1024 * 1024,
                memoryLimitMb == null ? null : memoryLimitMb.longValue() * 1024 * 1024
        );

        ExecuteResponse response = restClient.post()
                .uri(executeUrl)
                .body(request)
                .retrieve()
                .body(ExecuteResponse.class);

        return mapResponse(response);
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

        if (compile != null && isFailure(compile.code())) {
            return ExecutionResult.builder()
                    .outcome(ExecutionOutcome.COMPILE_ERROR)
                    .errorOutput(firstNonBlank(compile.stderr(), compile.output(), compile.message()))
                    .durationMs(nullSafe(compile.wallTime()))
                    .memoryBytes(compile.memory())
                    .build();
        }

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
                .actualOutput(firstNonBlank(run.output(), run.stdout(), ""))
                .errorOutput(firstNonBlank(run.stderr(), run.message()))
                .durationMs(nullSafe(compile == null ? null : compile.wallTime()) + nullSafe(run.wallTime()))
                .memoryBytes(run.memory())
                .build();
    }

    private ExecutionOutcome resolveRunOutcome(ProcessStage run) {
        String failureText = (firstNonBlank(run.status(), run.message(), run.stderr(), run.signal(), ""))
                .toLowerCase(Locale.ROOT);

        if (failureText.contains("memory") || failureText.contains("mle") || failureText.contains("out of memory")) {
            return ExecutionOutcome.MEMORY_LIMIT_EXCEEDED;
        }
        if (failureText.contains("time") || failureText.contains("timeout") || failureText.contains("tle")) {
            return ExecutionOutcome.TIME_LIMIT_EXCEEDED;
        }
        if (isFailure(run.code()) || run.signal() != null) {
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

    private record ExecuteRequest(
            String language,
            String version,
            List<SourceFile> files,
            String stdin,
            @JsonProperty("compile_timeout") Integer compileTimeout,
            @JsonProperty("run_timeout") Integer runTimeout,
            @JsonProperty("compile_memory_limit") Long compileMemoryLimit,
            @JsonProperty("run_memory_limit") Long runMemoryLimit
    ) {
    }

    private record SourceFile(String content) {
    }

    private record ExecuteResponse(
            ProcessStage compile,
            ProcessStage run,
            String language,
            String version
    ) {
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
            @JsonProperty("wall_time") Long wallTime
    ) {
    }
}
