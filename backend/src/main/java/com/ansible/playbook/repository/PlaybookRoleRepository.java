package com.ansible.playbook.repository;

import com.ansible.playbook.entity.PlaybookRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaybookRoleRepository extends JpaRepository<PlaybookRole, Long> {
  List<PlaybookRole> findByPlaybookIdOrderByOrderIndexAsc(Long playbookId);

  void deleteByPlaybookIdAndRoleId(Long playbookId, Long roleId);

  boolean existsByPlaybookIdAndRoleId(Long playbookId, Long roleId);

  void deleteByPlaybookId(Long playbookId);
}
