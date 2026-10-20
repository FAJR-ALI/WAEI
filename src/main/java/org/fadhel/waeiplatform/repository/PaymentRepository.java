package org.fadhel.waeiplatform.repository;

import org.fadhel.waeiplatform.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findPaymentsByTrainingRequest_Id(Long trainingRequestId);

    List<Payment> findPaymentsByStatus(String status);

    Payment findPaymentByMoyasarPaymentId(String moyasarPaymentId);
}
