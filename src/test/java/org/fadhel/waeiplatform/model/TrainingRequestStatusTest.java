package org.fadhel.waeiplatform.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainingRequestStatusTest {

    @Test
    void paymentPendingCanBeConfirmed() {
        assertTrue(TrainingRequestStatus.PAYMENT_PENDING.canTransitionTo(TrainingRequestStatus.CONFIRMED));
    }

    @Test
    void confirmedCanStartOrCancel() {
        assertTrue(TrainingRequestStatus.CONFIRMED.canTransitionTo(TrainingRequestStatus.IN_PROGRESS));
        assertTrue(TrainingRequestStatus.CONFIRMED.canTransitionTo(TrainingRequestStatus.CANCELLED));
    }

    @Test
    void paidIsNotARequestStatus() {
        assertThrows(IllegalArgumentException.class, () -> TrainingRequestStatus.valueOf("PAID"));
    }
}
