package com.keves.dreamreach.bootstrap;

import com.keves.dreamreach.entity.Alliance;
import com.keves.dreamreach.entity.PlayerProfile;
import com.keves.dreamreach.repository.AllianceRepository;
import com.keves.dreamreach.repository.PlayerProfileRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * The seeder executes automatically when the application is started up.
 * It populates the database with baseline data for testing.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final AllianceRepository allianceRepository;
    private final PlayerProfileRepository playerProfileRepository;

    // Constructor injection for injecting dependencies in Spring.
    // Preferred over the @Autowired annotation since it makes the class easier to unit test.
    public DataSeeder(AllianceRepository allianceRepository, PlayerProfileRepository playerProfileRepository) {
        this.allianceRepository = allianceRepository;
        this.playerProfileRepository = playerProfileRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Only seed if the db is empty in order to prevent duplicate key crashes on restart
        if (allianceRepository.count() == 0) {
            System.out.println("Bootstrapping test data...");

            // Create and save a new Alliance first
            Alliance horde = new Alliance();
            horde.setName("The Horde");
            horde.setTag("H");
            horde.setLevel(0);
            horde.setDescription("For the Horde!");
            horde.setAlliancePvpEnabled(true);

            // then have the repo writes the INSERT SQL to the db
            // this is handled by Hibernate
            allianceRepository.save(horde);

            // Create and save a player profile
            PlayerProfile testPlayer = new PlayerProfile();
            testPlayer.setDisplayName("Bob Derp");
            testPlayer.setPersonalPvpEnabled(false);

            // and link the new player to the horde alliance
            testPlayer.setAlliance(horde);

            // have the repo write the INSERT SQL to the player db
            playerProfileRepository.save(testPlayer);

            System.out.println("Added the test data!");
        }
    }
}
