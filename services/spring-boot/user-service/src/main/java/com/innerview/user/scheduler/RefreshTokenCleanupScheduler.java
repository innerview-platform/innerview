package com.innerview.user.scheduler;


import com.innerview.user.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenService refreshTokenService;

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupExpiredTokens() {
        try {
            int deleted = refreshTokenService.cleanupExpiredAndRevokedTokens();
            log.info("Cleanup Job: Deleted {} expired/revoked tokens", deleted);
        } catch (Exception e) {
            log.error("Cleanup Job failed", e);
        }
    }
}