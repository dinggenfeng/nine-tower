package com.ansible.environment.repository;

import com.ansible.environment.entity.Environment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnvironmentRepository extends JpaRepository<Environment, Long> {

  List<Environment> findByProjectIdOrderByIdAsc(Long projectId);

  boolean existsByProjectIdAndName(Long projectId, String name);

  boolean existsByProjectIdAndNameAndIdNot(Long projectId, String name, Long id);

  void deleteByProjectId(Long projectId);
}
