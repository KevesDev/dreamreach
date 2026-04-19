package com.keves.dreamreach.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "game.quest")
@Getter
@Setter
public class GameQuestConfig {
    // These default values implement the math bounds defined in our design
    private double classAdvantageBonus = 0.05;
    private double classDisadvantagePenalty = 0.05;
    private double maximumSuccessChance = 0.95;
    private double minimumSuccessChance = 0.05;
}