package com.keves.dreamreach.bootstrap;

import com.keves.dreamreach.entity.CharacterTemplate;
import com.keves.dreamreach.entity.RecruitmentPool;
import com.keves.dreamreach.enums.DndClass;
import com.keves.dreamreach.enums.Rarity;
import com.keves.dreamreach.repository.CharacterTemplateRepository;
import com.keves.dreamreach.repository.RecruitmentPoolRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final CharacterTemplateRepository characterTemplateRepository;
    private final RecruitmentPoolRepository recruitmentPoolRepository;

    public DataSeeder(CharacterTemplateRepository characterTemplateRepository,
                      RecruitmentPoolRepository recruitmentPoolRepository) {
        this.characterTemplateRepository = characterTemplateRepository;
        this.recruitmentPoolRepository = recruitmentPoolRepository;
    }

    @Override
    public void run(String... args) throws Exception {

        // Only run this if the recruitment pool is empty
        if (recruitmentPoolRepository.count() == 0) {
            System.out.println("Bootstrapping Tavern Recruitment Pool...");

            try {
                // Fetch existing templates to prevent unique constraint violations
                List<CharacterTemplate> existingTemplates = characterTemplateRepository.findAll();

                // --- 1. Uncommon Healer ---
                CharacterTemplate goblinShaman = existingTemplates.stream()
                        .filter(t -> t.getName().equals("Goblin Shaman"))
                        .findFirst()
                        .orElseGet(() -> {
                            CharacterTemplate t = new CharacterTemplate();
                            t.setName("Goblin Shaman");
                            t.setRarity(Rarity.UNCOMMON);
                            t.setDndClass(DndClass.CLERIC);
                            t.setBaseStr(8); t.setBaseDex(14); t.setBaseCon(10);
                            t.setBaseInt(12); t.setBaseWis(16); t.setBaseCha(10);
                            t.setHitDieType(8);
                            t.setPrimaryStat("WIS");
                            t.setClassTags("[\"Magical\", \"Healer\"]");
                            t.setFlavorQuips("{\"IDLE\": \"The spirits are restless.\"}");
                            t.setPortraitUrl("/assets/hero.png");
                            t.setBaseGoldCost(300);
                            t.setBaseGemCost(30);
                            return characterTemplateRepository.save(t);
                        });

                // --- 2. Common Frontline ---
                CharacterTemplate humanFighter = existingTemplates.stream()
                        .filter(t -> t.getName().equals("Human Vanguard"))
                        .findFirst()
                        .orElseGet(() -> {
                            CharacterTemplate t = new CharacterTemplate();
                            t.setName("Human Vanguard");
                            t.setRarity(Rarity.COMMON);
                            t.setDndClass(DndClass.FIGHTER);
                            t.setBaseStr(16); t.setBaseDex(12); t.setBaseCon(14);
                            t.setBaseInt(10); t.setBaseWis(10); t.setBaseCha(10);
                            t.setHitDieType(10);
                            t.setPrimaryStat("STR");
                            t.setClassTags("[\"Brute\", \"Frontline\"]");
                            t.setFlavorQuips("{\"IDLE\": \"Ready for battle.\"}");
                            t.setPortraitUrl("/assets/hero.png");
                            t.setBaseGoldCost(150);
                            t.setBaseGemCost(15);
                            return characterTemplateRepository.save(t);
                        });

                // --- 3. Epic Spellcaster ---
                CharacterTemplate elfWizard = existingTemplates.stream()
                        .filter(t -> t.getName().equals("Elven Evoker"))
                        .findFirst()
                        .orElseGet(() -> {
                            CharacterTemplate t = new CharacterTemplate();
                            t.setName("Elven Evoker");
                            t.setRarity(Rarity.EPIC);
                            t.setDndClass(DndClass.WIZARD);
                            t.setBaseStr(8); t.setBaseDex(14); t.setBaseCon(12);
                            t.setBaseInt(18); t.setBaseWis(12); t.setBaseCha(10);
                            t.setHitDieType(6);
                            t.setPrimaryStat("INT");
                            t.setClassTags("[\"Magical\", \"Scholar\"]");
                            t.setFlavorQuips("{\"IDLE\": \"So many spells, so little time.\"}");
                            t.setPortraitUrl("/assets/hero.png");
                            t.setBaseGoldCost(1000);
                            t.setBaseGemCost(100);
                            return characterTemplateRepository.save(t);
                        });

                // --- Add Templates to the Tavern Recruitment Pool ---
                RecruitmentPool rp1 = new RecruitmentPool();
                rp1.setCharacterTemplate(goblinShaman);
                rp1.setWeight(10); // Standard chance

                RecruitmentPool rp2 = new RecruitmentPool();
                rp2.setCharacterTemplate(humanFighter);
                rp2.setWeight(30); // Very common appearance

                RecruitmentPool rp3 = new RecruitmentPool();
                rp3.setCharacterTemplate(elfWizard);
                rp3.setWeight(2);  // Rare appearance

                recruitmentPoolRepository.saveAll(List.of(rp1, rp2, rp3));

                System.out.println("Tavern Recruitment Pool seeded successfully!");
            }
            catch (Exception exception) {
                System.out.println("Failed to seed Tavern data: " + exception.getMessage());
            }
        }
    }
}