package com.keves.dreamreach.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "game.ledger")
@Getter
@Setter
public class GameLedgerConfig {

    private int maxLedgerEntries = 50;

    private String missionDispatchMessage = "A party of {count} heroes has been dispatched to {questTitle}.";
    private String missionSuccessMessage = "A party has returned from {questTitle}, triumphant!";
    private String missionFailureMessage = "A party has fled from {questTitle}, returning in defeat.";

    private String economyStarvationMessage = "The lack of food is causing deep unrest among the populace.";
    private String economyUtopiaMessage = "Citizens rejoice in the prosperity and freedom of your reign.";
    private String taxChangeMessage = "A new tax decree has been issued: {bracket}.";

    private String trainingCompleteMessage = "{count} peasant(s) have finished their training as {profession}.";
    private String constructionCompleteMessage = "Construction of a new {buildingType} has been completed.";

    private String upgradeCompleteMessage = "The {buildingType} has been upgraded to Level {level}.";
    private String keepUpgradeCompleteMessage = "A grand celebration marks the expansion of the Keep to Level {level}.";
}