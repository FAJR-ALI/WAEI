package org.fadhel.waeiplatform.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum TrainingRequestStatus {

    PENDING,
    ACCEPTED,
    REJECTED,
    PAYMENT_PENDING,
    PAID,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    private static final Map<TrainingRequestStatus, Set<TrainingRequestStatus>> TRANSITIONS;

    static {
        Map<TrainingRequestStatus, Set<TrainingRequestStatus>> transitions = new EnumMap<>(TrainingRequestStatus.class);
        transitions.put(PENDING, EnumSet.of(ACCEPTED, REJECTED, CANCELLED));
        transitions.put(ACCEPTED, EnumSet.of(PAYMENT_PENDING, CANCELLED));
        transitions.put(PAYMENT_PENDING, EnumSet.of(PAID, ACCEPTED, CANCELLED));
        transitions.put(PAID, EnumSet.of(IN_PROGRESS, CANCELLED));
        transitions.put(IN_PROGRESS, EnumSet.of(COMPLETED));
        transitions.put(REJECTED, Collections.emptySet());
        transitions.put(COMPLETED, Collections.emptySet());
        transitions.put(CANCELLED, Collections.emptySet());
        TRANSITIONS = Collections.unmodifiableMap(transitions);
    }

    public boolean canTransitionTo(TrainingRequestStatus next) {
        return TRANSITIONS.get(this).contains(next);
    }

    public boolean isTerminal() {
        return TRANSITIONS.get(this).isEmpty();
    }
}
