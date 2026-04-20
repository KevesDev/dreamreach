package com.keves.dreamreach.service;

import com.keves.dreamreach.config.GameLedgerConfig;
import com.keves.dreamreach.entity.LedgerEntry;
import com.keves.dreamreach.entity.PlayerProfile;
import com.keves.dreamreach.repository.LedgerEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class LedgerService {

    private final LedgerEntryRepository ledgerRepository;
    private final GameLedgerConfig ledgerConfig;

    public LedgerService(LedgerEntryRepository ledgerRepository, GameLedgerConfig ledgerConfig) {
        this.ledgerRepository = ledgerRepository;
        this.ledgerConfig = ledgerConfig;
    }

    /**
     * Appends a new roleplay event to the kingdom's history log.
     * Automatically prunes the oldest events if the limit is exceeded.
     */
    @Transactional
    public void appendLog(PlayerProfile profile, String category, String message) {
        LedgerEntry entry = new LedgerEntry();
        entry.setProfile(profile);
        entry.setTimestamp(Instant.now());
        entry.setCategory(category.toUpperCase());
        entry.setMessage(message);
        ledgerRepository.save(entry);

        List<LedgerEntry> allLogs = ledgerRepository.findByProfileIdOrderByTimestampDesc(profile.getId());
        if (allLogs.size() > ledgerConfig.getMaxLedgerEntries()) {
            List<LedgerEntry> logsToDelete = allLogs.subList(ledgerConfig.getMaxLedgerEntries(), allLogs.size());
            ledgerRepository.deleteAll(logsToDelete);
        }
    }
}