package com.keves.dreamreach.service;

import com.keves.dreamreach.dto.DailyReward;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class RewardService {

    private final Random random = new Random();

    /**
     * Generates the 7-day visual track.
     * Future/Past days show the base expected values.
     * The current streak day gets a slight RNG variance applied.
     */
    public List<DailyReward> getWeeklyTrack(int currentStreak) {
        List<DailyReward> track = new ArrayList<>();
        int visualStreak = currentStreak > 7 ? (currentStreak % 7 == 0 ? 7 : currentStreak % 7) : currentStreak;

        for (int i = 1; i <= 7; i++) {
            if (i == 7) {
                // Day 7 is always the Epic Summon, no resources
                track.add(new DailyReward(i, 0, 0, 0, 0, true));
                continue;
            }

            // Base scaling math
            int baseAmount = i * 5;
            int goldAmount = i >= 3 ? i * 25 : 0;

            if (i == visualStreak) {
                // Apply +/- 10% RNG ONLY to today's actual reward
                track.add(new DailyReward(
                        i,
                        applyRng(baseAmount),
                        applyRng(baseAmount),
                        applyRng(baseAmount),
                        goldAmount > 0 ? applyRng(goldAmount) : 0,
                        false
                ));
            } else {
                // Show clean base amounts for the rest of the visual calendar
                track.add(new DailyReward(i, baseAmount, baseAmount, baseAmount, goldAmount, false));
            }
        }
        return track;
    }

    private int applyRng(int base) {
        int variance = (int) (base * 0.10); // 10% variance
        return base - variance + random.nextInt((variance * 2) + 1);
    }
}