package com.ansible.playbook.repository;

import com.ansible.playbook.entity.PlaybookRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaybookRoleRepository extends JpaRepository<PlaybookRole, Long> {
  List<PlaybookRole> findByPlaybookIdOrderByOrderIndexAsc(Long playbookId);

  void deleteByPlaybookIdAndRoleId(Long playbookId, Long roleId);

  boolean existsByPlaybookIdAndRoleId(Long playbookId, Long roleId);

  void deleteByPlaybookId(Long playbookId);

  void deleteByRoleId(Long roleId);

  @Query(
      "SELECT pr FROM PlaybookRole pr "
          + "WHERE pr.playbookId IN :playbookIds ORDER BY pr.orderIndex ASC")
  List<PlaybookRole> findByPlaybookIdIn(@Param("playbookIds") List<Long> playbookIds);
}
