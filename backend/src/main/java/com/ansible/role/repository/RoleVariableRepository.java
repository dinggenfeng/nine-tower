package com.ansible.role.repository;

import com.ansible.role.entity.RoleVariable;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleVariableRepository extends JpaRepository<RoleVariable, Long> {

  List<RoleVariable> findAllByRoleIdOrderByKeyAsc(Long roleId);

  boolean existsByRoleIdAndKey(Long roleId, String key);
}
