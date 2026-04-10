package com.ansible.project.repository;

import com.ansible.project.entity.Project;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long> {

  @Query(
      "SELECT p FROM Project p WHERE p.id IN "
          + "(SELECT pm.projectId FROM ProjectMember pm WHERE pm.userId = :userId)")
  List<Project> findAllByMemberUserId(@Param("userId") Long userId);
}
