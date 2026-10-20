package org.fadhel.waeiplatform.repository;

import org.fadhel.waeiplatform.model.TrainingRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainingRequestRepository extends JpaRepository<TrainingRequest, Long> {

    List<TrainingRequest> findTrainingRequestsByOrganization_Id(Long organizationId);

    List<TrainingRequest> findTrainingRequestsByAccount_Id(Long accountId);

    List<TrainingRequest> findTrainingRequestsByTrainingProgram_Id(Long trainingProgramId);

    List<TrainingRequest> findTrainingRequestsByStatus(String status);

    boolean existsByTrainingProgram_Id(Long trainingProgramId);
}
