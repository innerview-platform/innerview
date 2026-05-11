package com.innerview.spring.service;

import com.innerview.spring.entity.Submission;

public interface SubmissionJudgingAsyncService {

    void judgeSubmission(Submission submission);
}
