package com.http200ok.finbuddy.transaction.domain;

public enum TransactionType {
    DEPOSIT(1),
    WITHDRAWAL(2);

    private final int value;

    TransactionType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static TransactionType fromValue(int value) {
        for (TransactionType type : TransactionType.values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid transaction type: " + value);
    }
}
