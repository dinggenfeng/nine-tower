package com.ansible.playbook.repository;

import com.ansible.playbook.entity.PlaybookEnvironment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaybookEnvironmentRepository extends JpaRepository<PlaybookEnvironment, Long> {

  List<PlaybookEnvironment> findByPlaybookId(Long playbookId);

  boolean existsByPlaybookIdAndEnvironmentId(Long playbookId, Long environmentId);

  void deleteByPlaybookIdAndEnvironmentId(Long playbookId, Long environmentId);

  void deleteByPlaybookId(Long playbookId);

  void deleteByEnvironmentId(Long environmentId);

  @Query("SELECT pe FROM PlaybookEnvironment pe WHERE pe.playbookId IN :playbookIds")
  List<PlaybookEnvironment> findByPlaybookIdIn(@Param("playbookIds") List<Long> playbookIds);
}
