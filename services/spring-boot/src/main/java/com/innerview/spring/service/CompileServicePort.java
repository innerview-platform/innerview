package com.innerview.spring.service;

import com.innerview.spring.dto.ExecutionResult;

public interface CompileServicePort {

    ExecutionResult execute(String code, String language, String input, Integer timeLimitMs, Integer memoryLimitMb);
}
