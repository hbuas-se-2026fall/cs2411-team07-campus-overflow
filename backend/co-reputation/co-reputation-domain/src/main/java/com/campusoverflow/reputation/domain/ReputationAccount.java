package com.campusoverflow.reputation.domain;

import java.math.BigInteger;
import java.util.Objects;

public class ReputationAccount {
    private final Long userId;
    private BigInteger reputation;

    private ReputationAccount(Long userId, BigInteger initialReputation) {
        this.userId = userId;
        this.reputation = initialReputation;
    }

    public static ReputationAccount create(Long userId, BigInteger initialReputation) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(initialReputation, "initialReputation must not be null");
        return new ReputationAccount(userId, initialReputation);
    }

    public ReputationAccount applyReputation(BigInteger delta) {
        this.reputation = this.reputation.add(delta);
        return this;
    }

    public Long getUserId() {
        return userId;
    }

    public BigInteger getReputation() {
        return reputation;
    }
}
