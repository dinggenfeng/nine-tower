package com.ansible.playbook.repository;

import com.ansible.playbook.entity.PlaybookHostGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaybookHostGroupRepository
    extends JpaRepository<PlaybookHostGroup, Long> {
  List<PlaybookHostGroup> findByPlaybookId(Long playbookId);

  void deleteByPlaybookIdAndHostGroupId(Long playbookId, Long hostGroupId);

  boolean existsByPlaybookIdAndHostGroupId(Long playbookId, Long hostGroupId);

  void deleteByPlaybookId(Long playbookId);

  void deleteByHostGroupId(Long hostGroupId);
}
