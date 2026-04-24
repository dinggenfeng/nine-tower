package com.ansible.role.repository;

import com.ansible.role.entity.Task;
import com.ansible.tag.entity.TaskTag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long> {

  List<Task> findAllByRoleIdOrderByTaskOrderAsc(Long roleId);

  List<Task> findAllByParentTaskIdOrderByTaskOrderAsc(Long parentTaskId);

  List<Task> findAllByRoleIdAndParentTaskIdIsNullOrderByTaskOrderAsc(Long roleId);

  void deleteByRoleId(Long roleId);

  @Modifying
  @Query("DELETE FROM TaskTag tt WHERE tt.taskId IN (SELECT t.id FROM Task t WHERE t.roleId = :roleId)")
  void deleteTaskTagsByRoleId(@Param("roleId") Long roleId);
}
