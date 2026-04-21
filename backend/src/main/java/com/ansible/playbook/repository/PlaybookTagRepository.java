package com.ansible.playbook.repository;

import com.ansible.playbook.entity.PlaybookTag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaybookTagRepository extends JpaRepository<PlaybookTag, Long> {
  List<PlaybookTag> findByPlaybookId(Long playbookId);

  void deleteByPlaybookIdAndTagId(Long playbookId, Long tagId);

  boolean existsByPlaybookIdAndTagId(Long playbookId, Long tagId);

  void deleteByPlaybookId(Long playbookId);
}
