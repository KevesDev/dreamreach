package com.keves.dreamreach.util;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
/**
 * Utility component responsible for generating randomized,
 * "Reddit-style" display names for new accounts.
 */
@Component // Tells Spring to build this tool and put it in the Application Context toolbox
public class DisplayNameGenerator {

    private static final List<String> ADJECTIVES = List.of(
            "Mighty", "Shadow", "Golden", "Swift", "Crimson", "Silent", "Iron", "Crystal"
    );
    private static final List<String> NOUNS = List.of(
            "Goblin", "Knight", "Dragon", "Rogue", "Paladin", "Slime", "Wizard", "Ranger"
    );

    /**
     * Assembles a string: Adjective + Noun + 4-digit number.
     * Uses ThreadLocalRandom for thread-safe, high-performance concurrency.
     */

    public String generateRandomName() {
        String adj = ADJECTIVES.get(ThreadLocalRandom.current().nextInt(ADJECTIVES.size()));
        String noun = NOUNS.get(ThreadLocalRandom.current().nextInt(NOUNS.size()));
        int suffix = ThreadLocalRandom.current().nextInt(1000, 10000);

        return adj + noun + suffix;
    }
}
