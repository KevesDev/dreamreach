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
                // --- 1. Uncommon Healer ---
                CharacterTemplate goblinShaman = new CharacterTemplate();
                goblinShaman.setName("Goblin Shaman");
                goblinShaman.setRarity(Rarity.UNCOMMON);
                goblinShaman.setDndClass(DndClass.CLERIC);
                goblinShaman.setBaseStr(8); goblinShaman.setBaseDex(14); goblinShaman.setBaseCon(10);
                goblinShaman.setBaseInt(12); goblinShaman.setBaseWis(16); goblinShaman.setBaseCha(10);
                goblinShaman.setHitDieType(8);
                goblinShaman.setPrimaryStat("WIS");
                goblinShaman.setClassTags("[\"Magical\", \"Healer\"]");
                goblinShaman.setFlavorQuips("{\"IDLE\": \"The spirits are restless.\"}");
                goblinShaman.setPortraitUrl("/assets/hero.png");
                goblinShaman.setBaseGoldCost(300);
                goblinShaman.setBaseGemCost(30);

                // --- 2. Common Frontline ---
                CharacterTemplate humanFighter = new CharacterTemplate();
                humanFighter.setName("Human Vanguard");
                humanFighter.setRarity(Rarity.COMMON);
                humanFighter.setDndClass(DndClass.FIGHTER);
                humanFighter.setBaseStr(16); humanFighter.setBaseDex(12); humanFighter.setBaseCon(14);
                humanFighter.setBaseInt(10); humanFighter.setBaseWis(10); humanFighter.setBaseCha(10);
                humanFighter.setHitDieType(10);
                humanFighter.setPrimaryStat("STR");
                humanFighter.setClassTags("[\"Brute\", \"Frontline\"]");
                humanFighter.setFlavorQuips("{\"IDLE\": \"Ready for battle.\"}");
                humanFighter.setPortraitUrl("/assets/hero.png");
                humanFighter.setBaseGoldCost(150);
                humanFighter.setBaseGemCost(15);

                // --- 3. Epic Spellcaster ---
                CharacterTemplate elfWizard = new CharacterTemplate();
                elfWizard.setName("Elven Evoker");
                elfWizard.setRarity(Rarity.EPIC);
                elfWizard.setDndClass(DndClass.WIZARD);
                elfWizard.setBaseStr(8); elfWizard.setBaseDex(14); elfWizard.setBaseCon(12);
                elfWizard.setBaseInt(18); elfWizard.setBaseWis(12); elfWizard.setBaseCha(10);
                elfWizard.setHitDieType(6);
                elfWizard.setPrimaryStat("INT");
                elfWizard.setClassTags("[\"Magical\", \"Scholar\"]");
                elfWizard.setFlavorQuips("{\"IDLE\": \"So many spells, so little time.\"}");
                elfWizard.setPortraitUrl("/assets/hero.png");
                elfWizard.setBaseGoldCost(1000);
                elfWizard.setBaseGemCost(100);

                characterTemplateRepository.saveAll(List.of(goblinShaman, humanFighter, elfWizard));

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