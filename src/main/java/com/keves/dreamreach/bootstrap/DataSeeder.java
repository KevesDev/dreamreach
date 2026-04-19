package com.keves.dreamreach.bootstrap;

import com.keves.dreamreach.entity.CharacterTemplate;
import com.keves.dreamreach.entity.QuestTemplate;
import com.keves.dreamreach.entity.RecruitmentPool;
import com.keves.dreamreach.enums.DndClass;
import com.keves.dreamreach.enums.QuestType;
import com.keves.dreamreach.enums.Rarity;
import com.keves.dreamreach.repository.CharacterTemplateRepository;
import com.keves.dreamreach.repository.QuestTemplateRepository;
import com.keves.dreamreach.repository.RecruitmentPoolRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final CharacterTemplateRepository characterTemplateRepository;
    private final RecruitmentPoolRepository recruitmentPoolRepository;
    private final QuestTemplateRepository questTemplateRepository;

    public DataSeeder(CharacterTemplateRepository characterTemplateRepository,
                      RecruitmentPoolRepository recruitmentPoolRepository,
                      QuestTemplateRepository questTemplateRepository) {
        this.characterTemplateRepository = characterTemplateRepository;
        this.recruitmentPoolRepository = recruitmentPoolRepository;
        this.questTemplateRepository = questTemplateRepository;
    }

    @Override
    public void run(String... args) throws Exception {

        if (recruitmentPoolRepository.count() == 0) {
            System.out.println("Bootstrapping Tavern Recruitment Pool...");
            try {
                List<CharacterTemplate> existingTemplates = characterTemplateRepository.findAll();

                CharacterTemplate goblinShaman = existingTemplates.stream().filter(t -> t.getName().equals("Goblin Shaman")).findFirst().orElseGet(() -> {
                    CharacterTemplate t = new CharacterTemplate();
                    t.setName("Goblin Shaman"); t.setRarity(Rarity.UNCOMMON); t.setDndClass(DndClass.CLERIC);
                    t.setBaseStr(8); t.setBaseDex(14); t.setBaseCon(10); t.setBaseInt(12); t.setBaseWis(16); t.setBaseCha(10);
                    t.setHitDieType(8); t.setPrimaryStat("WIS"); t.setClassTags("[\"Magical\", \"Healer\"]");
                    t.setFlavorQuips("{\"IDLE\": \"The spirits are restless.\", \"MISSION\": \"The ancestors guide our path!\"}"); t.setPortraitUrl("/assets/hero.png");
                    t.setBaseGoldCost(300); t.setBaseGemCost(30);
                    t.setDescription("A mystic from the deep caverns who communes with ancestral spirits to mend the wounded and curse the wicked.");
                    return characterTemplateRepository.save(t);
                });

                CharacterTemplate humanFighter = existingTemplates.stream().filter(t -> t.getName().equals("Human Vanguard")).findFirst().orElseGet(() -> {
                    CharacterTemplate t = new CharacterTemplate();
                    t.setName("Human Vanguard"); t.setRarity(Rarity.COMMON); t.setDndClass(DndClass.FIGHTER);
                    t.setBaseStr(16); t.setBaseDex(12); t.setBaseCon(14); t.setBaseInt(10); t.setBaseWis(10); t.setBaseCha(10);
                    t.setHitDieType(10); t.setPrimaryStat("STR"); t.setClassTags("[\"Brute\", \"Frontline\"]");
                    t.setFlavorQuips("{\"IDLE\": \"Ready for battle.\", \"MISSION\": \"Shield wall! Advance!\"}"); t.setPortraitUrl("/assets/hero.png");
                    t.setBaseGoldCost(150); t.setBaseGemCost(15);
                    t.setDescription("A steadfast warrior wielding iron and grit. Relies on heavy armor and martial discipline to hold the line.");
                    return characterTemplateRepository.save(t);
                });

                CharacterTemplate elfWizard = existingTemplates.stream().filter(t -> t.getName().equals("Elven Evoker")).findFirst().orElseGet(() -> {
                    CharacterTemplate t = new CharacterTemplate();
                    t.setName("Elven Evoker"); t.setRarity(Rarity.EPIC); t.setDndClass(DndClass.WIZARD);
                    t.setBaseStr(8); t.setBaseDex(14); t.setBaseCon(12); t.setBaseInt(18); t.setBaseWis(12); t.setBaseCha(10);
                    t.setHitDieType(6); t.setPrimaryStat("INT"); t.setClassTags("[\"Magical\", \"Scholar\"]");
                    t.setFlavorQuips("{\"IDLE\": \"So many spells, so little time.\", \"MISSION\": \"Fireball solves everything.\"}"); t.setPortraitUrl("/assets/hero.png");
                    t.setBaseGoldCost(1000); t.setBaseGemCost(100);
                    t.setDescription("An ancient scholar who has mastered the destructive forces of the arcane. Their spells can decimate entire battlefields.");
                    return characterTemplateRepository.save(t);
                });

                RecruitmentPool rp1 = new RecruitmentPool(); rp1.setCharacterTemplate(goblinShaman); rp1.setWeight(10);
                RecruitmentPool rp2 = new RecruitmentPool(); rp2.setCharacterTemplate(humanFighter); rp2.setWeight(30);
                RecruitmentPool rp3 = new RecruitmentPool(); rp3.setCharacterTemplate(elfWizard); rp3.setWeight(2);
                recruitmentPoolRepository.saveAll(List.of(rp1, rp2, rp3));
                System.out.println("Tavern Recruitment Pool seeded successfully!");
            } catch (Exception exception) {
                System.out.println("Failed to seed Tavern data: " + exception.getMessage());
            }
        }

        if (questTemplateRepository.count() == 0) {
            System.out.println("Bootstrapping Quest Templates...");
            try {
                QuestTemplate q1 = new QuestTemplate();
                q1.setType(QuestType.HUNT);
                q1.setTitle("The Goblin Menace");
                q1.setDescription("A pack of vicious goblins has been raiding the eastern farms. Track them down and eliminate the threat.");
                q1.setTargetStatsJson("{\"STR\": 30, \"CON\": 20}");
                q1.setAdvantageClassesJson("[\"FIGHTER\"]");
                q1.setDisadvantageClassesJson("[\"BARD\", \"CLERIC\"]");
                q1.setBaseExp(1500); q1.setRewardGold(500); q1.setRewardFood(200);
                questTemplateRepository.save(q1);

                QuestTemplate q2 = new QuestTemplate();
                q2.setType(QuestType.SCOUTING);
                q2.setTitle("Ruins of Aethelgard");
                q2.setDescription("Ancient ruins have been discovered in the dense forest. We need a perceptive team to map the area and report back safely.");
                q2.setTargetStatsJson("{\"DEX\": 25, \"WIS\": 20}");
                q2.setAdvantageClassesJson("[\"ROGUE\", \"RANGER\"]");
                q2.setDisadvantageClassesJson("[\"FIGHTER\", \"PALADIN\"]");
                q2.setBaseExp(800); q2.setRewardGold(150); q2.setRewardStone(100); q2.setRewardWood(100);
                questTemplateRepository.save(q2);

                System.out.println("Quest Templates seeded successfully!");
            } catch (Exception exception) {
                System.out.println("Failed to seed Quest data: " + exception.getMessage());
            }
        }
    }
}