package com.innerview.spring.service.impl;

import com.innerview.spring.entity.Submission;
import com.innerview.spring.enums.SubmissionStatus;
import com.innerview.spring.repository.SubmissionRepository;
import com.innerview.spring.service.JudgeService;
import com.innerview.spring.service.SubmissionJudgingAsyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubmissionJudgingAsyncServiceImpl implements SubmissionJudgingAsyncService {

    private final JudgeService judgeService;
    private final SubmissionRepository submissionRepository;

    @Override
    @Async("submissionJudgeExecutor")
    @Transactional
    public void judgeSubmission(Submission submission) {
        try {
            judgeService.judge(submission);
        } catch (Exception exception) {
            submission.setStatus(SubmissionStatus.RUNTIME_ERROR);
            submission.setCompilationError(exception.getMessage());
            submissionRepository.save(submission);
        }
    }
}
