package com.keves.dreamreach.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class DailyReward {
    private int day;
    private int food;
    private int wood;
    private int stone;
    private int gold;
    private boolean summon;
}