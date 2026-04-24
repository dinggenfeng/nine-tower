package com.ansible.project.repository;

import com.ansible.common.enums.ProjectRole;
import com.ansible.project.entity.ProjectMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

  List<ProjectMember> findAllByProjectId(Long projectId);

  Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

  boolean existsByProjectIdAndUserId(Long projectId, Long userId);

  void deleteByProjectId(Long projectId);

  long countByProjectIdAndRole(Long projectId, ProjectRole role);

  List<ProjectMember> findByUserId(Long userId);
}
