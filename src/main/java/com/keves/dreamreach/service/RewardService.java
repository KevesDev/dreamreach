package com.keves.dreamreach.service;

import com.keves.dreamreach.dto.DailyReward;
import com.keves.dreamreach.entity.PendingReward;
import com.keves.dreamreach.entity.PlayerAccount;
import com.keves.dreamreach.entity.PlayerResources;
import com.keves.dreamreach.repository.PlayerAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class RewardService {

    private final PlayerAccountRepository accountRepository;
    private final Random random = new Random();

    public RewardService(PlayerAccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Replaces getWeeklyTrack.
     * Checks the database holding space. If empty or expired, rolls TRUE RNG and saves it.
     */
    public List<DailyReward> getOrGenerateTrack(PlayerAccount account) {
        List<DailyReward> track = new ArrayList<>();
        int currentStreak = account.getConsecutiveLogins();
        int visualStreak = currentStreak > 7 ? (currentStreak % 7 == 0 ? 7 : currentStreak % 7) : currentStreak;
        LocalDate serverToday = LocalDate.now(ZoneOffset.UTC);

        PendingReward pending = account.getPendingReward();
        boolean needsNewRoll = (pending == null || !serverToday.equals(pending.getPendingDate()));

        boolean alreadyClaimed = account.getLastClaimDate() != null &&
                LocalDate.ofInstant(account.getLastClaimDate(), ZoneOffset.UTC).equals(serverToday);

        for (int i = 1; i <= 7; i++) {
            if (i == 7) {
                track.add(new DailyReward(i, 0, 0, 0, 0, true));
                continue;
            }

            // Preserving your exact math rules
            int baseAmount = i * 5;
            int goldAmount = i >= 3 ? i * 25 : 0;

            if (i == visualStreak && !alreadyClaimed) {
                if (needsNewRoll) {
                    // 1. ROLL TRUE RNG
                    DailyReward newReward = new DailyReward(
                            i,
                            applyRng(baseAmount),
                            applyRng(baseAmount),
                            applyRng(baseAmount),
                            goldAmount > 0 ? applyRng(goldAmount) : 0,
                            false
                    );
                    track.add(newReward);

                    // 2. SAVE TO HOLDING SPACE
                    PendingReward newPending = new PendingReward();
                    newPending.setPendingFood(newReward.getFood());
                    newPending.setPendingWood(newReward.getWood());
                    newPending.setPendingStone(newReward.getStone());
                    newPending.setPendingGold(newReward.getGold());
                    newPending.setPendingSummon(false);
                    newPending.setPendingDate(serverToday);
                    account.setPendingReward(newPending);
                } else {
                    // 3. RECONSTRUCT FROM DATABASE
                    track.add(new DailyReward(
                            i,
                            pending.getPendingFood(),
                            pending.getPendingWood(),
                            pending.getPendingStone(),
                            pending.getPendingGold(),
                            pending.getPendingSummon()
                    ));
                }
            } else {
                track.add(new DailyReward(i, baseAmount, baseAmount, baseAmount, goldAmount, false));
            }
        }
        return track;
    }

    /**
     * Securely moves the pending rewards into the actual inventory.
     */
    @Transactional
    public void claimReward(PlayerAccount account) {
        PendingReward pending = account.getPendingReward();
        LocalDate serverToday = LocalDate.now(ZoneOffset.UTC);

        if (pending == null || pending.getPendingDate() == null) {
            throw new IllegalStateException("No pending reward found.");
        }

        if (!pending.getPendingDate().equals(serverToday)) {
            account.setPendingReward(null);
            accountRepository.save(account);
            throw new IllegalStateException("Reward expired. Please refresh the page.");
        }

        PlayerResources resources = account.getProfile().getResources();
        resources.setFood(resources.getFood() + pending.getPendingFood());
        resources.setWood(resources.getWood() + pending.getPendingWood());
        resources.setStone(resources.getStone() + pending.getPendingStone());
        resources.setGold(resources.getGold() + pending.getPendingGold());

        // Wipe holding space and update spam tracker
        account.setLastClaimDate(Instant.now());
        account.setPendingReward(null);
        accountRepository.save(account);
    }

    private int applyRng(int base) {
        int variance = (int) (base * 0.10);

        // Prevent random.nextInt(0) if variance ends up being 0 due to very small base numbers
        if (variance == 0) return base;

        return base - variance + random.nextInt((variance * 2) + 1);
    }
}