package com.http200ok.finbuddy.product.domain;

import lombok.Getter;

@Getter
public enum SubscriptionRestriction {
    UNLIMITED(1, "제한없음"),
    CIVILIAN_ONLY(2, "서민전용"),
    PARTIALLY_RESTRICTED(3, "일부제한");

    private final int code;
    private final String description;

    SubscriptionRestriction(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static String getDescriptionByCode(int code) {
        for (SubscriptionRestriction restriction : values()) {
            if (restriction.code == code) {
                return restriction.description;
            }
        }
        throw new IllegalArgumentException("Invalid SubscriptionRestriction code: " + code);
    }
}