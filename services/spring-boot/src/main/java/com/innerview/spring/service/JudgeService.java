package com.innerview.spring.service;

import com.innerview.spring.entity.Submission;

public interface JudgeService {

    Submission judge(Submission submission);
}
