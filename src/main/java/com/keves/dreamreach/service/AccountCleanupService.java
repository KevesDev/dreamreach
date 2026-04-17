package com.keves.dreamreach.service;

import com.keves.dreamreach.entity.VerificationToken;
import com.keves.dreamreach.repository.PlayerAccountRepository;
import com.keves.dreamreach.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountCleanupService {

    private final VerificationTokenRepository tokenRepository;
    private final PlayerAccountRepository accountRepository;

    // Execute at exactly midnight server time every day
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void reapUnverifiedAccounts() {
        log.info("Initiating Daily Sweep: Reaping expired unverified accounts...");

        List<VerificationToken> expiredTokens = tokenRepository.findAllByExpiryDateBefore(LocalDateTime.now());

        if (expiredTokens.isEmpty()) {
            log.info("Daily Sweep complete. No expired accounts found.");
            return;
        }

        for (VerificationToken token : expiredTokens) {
            log.debug("Reaping unverified account ID: {}", token.getAccount().getId());
            // Deleting the account triggers the PostgreSQL ON DELETE CASCADE
            // which automatically deletes the token and profile.
            accountRepository.delete(token.getAccount());
        }

        log.info("Daily Sweep complete. Purged {} unverified accounts from the server.", expiredTokens.size());
    }
}