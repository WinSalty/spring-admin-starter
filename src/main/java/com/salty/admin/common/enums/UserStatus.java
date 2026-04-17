package com.salty.admin.common.enums;

public enum UserStatus {

    DISABLED(0),
    ENABLED(1);

    private final int value;

    UserStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
