package com.ansible.tag.repository;

import com.ansible.tag.entity.TaskTag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskTagRepository extends JpaRepository<TaskTag, Long> {

  List<TaskTag> findByTaskId(Long taskId);

  List<TaskTag> findByTagId(Long tagId);

  void deleteByTaskIdAndTagId(Long taskId, Long tagId);

  boolean existsByTaskIdAndTagId(Long taskId, Long tagId);

  void deleteByTaskId(Long taskId);

  void deleteByTagId(Long tagId);
}
