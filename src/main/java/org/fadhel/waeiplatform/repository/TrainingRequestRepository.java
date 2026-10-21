package org.fadhel.waeiplatform.repository;

import org.fadhel.waeiplatform.model.TrainingRequest;
import org.fadhel.waeiplatform.model.TrainingRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface TrainingRequestRepository extends JpaRepository<TrainingRequest, Long> {

    List<TrainingRequest> findTrainingRequestsByOrganization_Id(Long organizationId);

    List<TrainingRequest> findTrainingRequestsByAccount_Id(Long accountId);

    List<TrainingRequest> findTrainingRequestsByTrainingProgram_Id(Long trainingProgramId);

    List<TrainingRequest> findTrainingRequestsByStatus(TrainingRequestStatus status);

    List<TrainingRequest> findTrainingRequestsByTrainingProgram_Organization_Id(Long organizationId);

    boolean existsByTrainingProgram_Id(Long trainingProgramId);

    @Query("""
            select r from TrainingRequest r
            where r.organization.id = :organizationId
               or r.trainingProgram.organization.id = :organizationId
            order by r.requestedAt desc
            """)
    List<TrainingRequest> findAccessibleByOrganization(@Param("organizationId") Long organizationId);

    @Query("""
            select count(r) from TrainingRequest r
            where r.trainingProgram.id = :programId
              and r.status not in :releasedStatuses
              and r.id <> :excludeId
              and r.scheduledStart < :end
              and r.scheduledEnd > :start
            """)
    long countOverlapping(@Param("programId") Long programId,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end,
                          @Param("excludeId") Long excludeId,
                          @Param("releasedStatuses") Collection<TrainingRequestStatus> releasedStatuses);
}
