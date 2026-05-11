package com.innerview.spring.repository;

import com.innerview.spring.entity.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TestCaseRepository extends JpaRepository<TestCase, UUID> {

    List<TestCase> findAllByProblemIdOrderByOrderIndexAsc(UUID problemId);

    List<TestCase> findAllByProblemIdAndIsSampleTrueOrderByOrderIndexAsc(UUID problemId);
}
