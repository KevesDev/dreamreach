package com.keves.dreamreach.service;

import com.keves.dreamreach.entity.PlayerAccount;
import com.keves.dreamreach.entity.PlayerProfile;
import com.keves.dreamreach.entity.VerificationToken;
import com.keves.dreamreach.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountCleanupService {

    private final VerificationTokenRepository tokenRepository;
    private final PlayerAccountRepository accountRepository;

    // Repositories for orphaned relational data
    private final PlayerCharacterRepository characterRepository;
    private final ConstructionTaskRepository constructionRepository;
    private final TrainingTaskRepository trainingRepository;
    private final TavernListingRepository tavernRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

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
            deleteAccountAndAllRelationalData(token.getAccount());
        }

        log.info("Daily Sweep complete. Purged {} unverified accounts from the server.", expiredTokens.size());
    }

    /**
     * Helper function to completely eradicate an account and all associated entities.
     * Prevents Foreign Key Constraint violations from loose tables.
     */
    @Transactional
    public void deleteAccountAndAllRelationalData(PlayerAccount account) {
        PlayerProfile profile = account.getProfile();

        if (profile != null) {
            UUID profileId = profile.getId();

            // 1. Manually wipe out tables that do not have CascadeType.ALL mapped in PlayerProfile
            tavernRepository.deleteAll(tavernRepository.findByProfileId(profileId).stream().toList());
            trainingRepository.deleteAll(trainingRepository.findByProfileIdOrderByStartTimeAsc(profileId));
            constructionRepository.deleteAll(constructionRepository.findByProfileId(profileId));
            characterRepository.deleteAll(characterRepository.findByOwnerId(profileId));

            // Delete kingdom history logs
            ledgerEntryRepository.deleteByProfileId(profileId);
        }

        // 2. Delete the root account.
        // This triggers the JPA CascadeType.ALL to wipe the Profile, Buildings, Resources, and Population.
        accountRepository.delete(account);
        log.info("Successfully wiped account: {}", account.getEmail());
    }
}