package com.ansible.role.repository;

import com.ansible.role.entity.Task;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

  List<Task> findAllByRoleIdOrderByTaskOrderAsc(Long roleId);
}
