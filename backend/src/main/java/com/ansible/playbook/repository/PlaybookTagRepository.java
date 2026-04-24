package com.ansible.playbook.repository;

import com.ansible.playbook.entity.PlaybookTag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaybookTagRepository extends JpaRepository<PlaybookTag, Long> {
  List<PlaybookTag> findByPlaybookId(Long playbookId);

  void deleteByPlaybookIdAndTagId(Long playbookId, Long tagId);

  boolean existsByPlaybookIdAndTagId(Long playbookId, Long tagId);

  void deleteByPlaybookId(Long playbookId);

  void deleteByTagId(Long tagId);

  @Query("SELECT pt FROM PlaybookTag pt WHERE pt.playbookId IN :playbookIds")
  List<PlaybookTag> findByPlaybookIdIn(@Param("playbookIds") List<Long> playbookIds);
}
