package com.keves.dreamreach.bootstrap;

import com.keves.dreamreach.entity.Alliance;
import com.keves.dreamreach.entity.CharacterTemplate;
import com.keves.dreamreach.entity.PlayerCharacter;
import com.keves.dreamreach.entity.PlayerProfile;
import com.keves.dreamreach.enums.DndClass;
import com.keves.dreamreach.enums.Rarity;
import com.keves.dreamreach.repository.AllianceRepository;
import com.keves.dreamreach.repository.CharacterTemplateRepository;
import com.keves.dreamreach.repository.PlayerCharacterRepository;
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
    private final CharacterTemplateRepository characterTemplateRepository;
    private final PlayerCharacterRepository playerCharacterRepository;

    // Constructor injection for injecting dependencies in Spring.
    // Preferred over the @Autowired annotation since it makes the class easier to unit test.
    public DataSeeder(AllianceRepository allianceRepository, PlayerProfileRepository playerProfileRepository,
                      CharacterTemplateRepository characterTemplateRepository, PlayerCharacterRepository playerCharacterRepository) {
        this.allianceRepository = allianceRepository;
        this.playerProfileRepository = playerProfileRepository;
        this.characterTemplateRepository = characterTemplateRepository;
        this.playerCharacterRepository = playerCharacterRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Only seed if the db is empty in order to prevent duplicate key crashes on restart
        if (allianceRepository.count() == 0) {
            System.out.println("Bootstrapping test data...");
            try {
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
                testPlayer.setAlliance(horde);

                // have the repo write the INSERT SQL to the player db
                playerProfileRepository.save(testPlayer);

                // Create and save a character template
                CharacterTemplate goblinShaman = new CharacterTemplate();
                goblinShaman.setName("Goblin Shaman");
                goblinShaman.setRarity(Rarity.UNCOMMON);
                goblinShaman.setDndClass(DndClass.CLERIC);
                goblinShaman.setBaseStr(8);
                goblinShaman.setBaseDex(14);
                goblinShaman.setBaseCon(10);
                goblinShaman.setBaseInt(12);
                goblinShaman.setBaseWis(16);
                goblinShaman.setBaseCha(10);

                // have the repo write the INSERT SQL to the character template db
                characterTemplateRepository.save(goblinShaman);

                // add an instance of the templated character to a player's 'deck'.
                PlayerCharacter playerGoblin = new PlayerCharacter();
                playerGoblin.setOwner(testPlayer);
                playerGoblin.setTemplate(goblinShaman);
                playerGoblin.setBonusWis(2);
                playerCharacterRepository.save(playerGoblin);

                System.out.println("Added the test data!");
            }
            catch (Exception exception) {
                System.out.print("Failed to seed new database.");
            }
        }
    }
}
