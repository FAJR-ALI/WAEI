package org.fadhel.waeiplatform.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum PaymentStatus {

    PENDING,
    PAID,
    FAILED,
    REFUNDED,
    CANCELLED;

    private static final Map<PaymentStatus, Set<PaymentStatus>> TRANSITIONS;

    static {
        Map<PaymentStatus, Set<PaymentStatus>> transitions = new EnumMap<>(PaymentStatus.class);
        transitions.put(PENDING, EnumSet.of(PAID, FAILED, CANCELLED));
        transitions.put(PAID, EnumSet.of(REFUNDED, CANCELLED));
        transitions.put(FAILED, EnumSet.of(PAID, PENDING));
        transitions.put(REFUNDED, Collections.emptySet());
        transitions.put(CANCELLED, Collections.emptySet());
        TRANSITIONS = Collections.unmodifiableMap(transitions);
    }

    public boolean canTransitionTo(PaymentStatus next) {
        return this == next || TRANSITIONS.get(this).contains(next);
    }

    public boolean isSuccessful() {
        return this == PAID || this == REFUNDED;
    }

    public boolean isActiveAttempt() {
        return this == PENDING;
    }
}
