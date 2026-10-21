package org.fadhel.waeiplatform.repository;

import jakarta.persistence.LockModeType;
import org.fadhel.waeiplatform.model.TrainingProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrainingProgramRepository extends JpaRepository<TrainingProgram, Long> {

    TrainingProgram findTrainingProgramByTitleAndOrganization_Id(String title, Long organizationId);

    List<TrainingProgram> findTrainingProgramsByIsActive(Boolean isActive);

    List<TrainingProgram> findTrainingProgramsByCategory(String category);

    List<TrainingProgram> findTrainingProgramsByOrganization_Id(Long organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from TrainingProgram p where p.id = :id")
    Optional<TrainingProgram> findByIdForUpdate(@Param("id") Long id);
}
