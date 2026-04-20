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

    private String missionDispatchMessage = "A party of {count} heroes has been dispatched to {questTitle}.";
    private String missionSuccessMessage = "A party has returned from {questTitle}, triumphant!";
    private String missionFailureMessage = "A party has fled from {questTitle}, returning in defeat.";

}