package org.fadhel.waeiplatform.repository;

import org.fadhel.waeiplatform.model.AwarenessService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AwarenessServiceRepository extends JpaRepository<AwarenessService, Long> {


    AwarenessService findAwarenessServiceById(Long id);

    AwarenessService findAwarenessServiceByTitle(String title);

    List<AwarenessService> findAwarenessServicesByCategory(String category);

    List<AwarenessService> findAwarenessServicesByIsActive(Boolean isActive);
}
