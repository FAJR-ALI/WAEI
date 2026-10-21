package org.fadhel.waeiplatform.repository;

import org.fadhel.waeiplatform.model.Payment;
import org.fadhel.waeiplatform.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findPaymentsByTrainingRequest_Id(Long trainingRequestId);

    List<Payment> findPaymentsByStatus(PaymentStatus status);

    Payment findPaymentByMoyasarPaymentId(String moyasarPaymentId);

    Payment findPaymentByMoyasarInvoiceId(String moyasarInvoiceId);

    Optional<Payment> findFirstByTrainingRequest_IdAndStatusOrderByCreatedAtDesc(Long trainingRequestId, PaymentStatus status);

    boolean existsByTrainingRequest_IdAndStatusIn(Long trainingRequestId, Collection<PaymentStatus> statuses);

    @Query("""
            select p from Payment p
            where p.trainingRequest.organization.id = :organizationId
               or p.trainingRequest.trainingProgram.organization.id = :organizationId
            order by p.createdAt desc
            """)
    List<Payment> findAccessibleByOrganization(@Param("organizationId") Long organizationId);
}
