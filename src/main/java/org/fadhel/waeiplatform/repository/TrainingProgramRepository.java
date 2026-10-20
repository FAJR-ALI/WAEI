package org.fadhel.waeiplatform.repository;

import org.fadhel.waeiplatform.model.TrainingProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainingProgramRepository extends JpaRepository<TrainingProgram, Long> {

    TrainingProgram findTrainingProgramByTitleAndOrganization_Id(String title, Long organizationId);

    List<TrainingProgram> findTrainingProgramsByIsActive(Boolean isActive);

    List<TrainingProgram> findTrainingProgramsByCategory(String category);

    List<TrainingProgram> findTrainingProgramsByOrganization_Id(Long organizationId);
}
