package com.ansible.role.repository;

import com.ansible.role.entity.Role;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

  List<Role> findAllByProjectId(Long projectId);
}
