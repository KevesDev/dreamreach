package com.keves.dreamreach.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Delivers the "Base Card" data to the frontend when a hero is waiting in the Tavern.
 */
@Getter
@Setter
@Builder
public class TavernListingResponse {
    private UUID listingId;
    private String name;
    private String dndClass;
    private String portraitUrl;
    private int goldCost;
    private int gemCost;
    private long expiryTimeEpoch;
}