package com.ansible.role.repository;

import com.ansible.role.entity.RoleDefaultVariable;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleDefaultVariableRepository extends JpaRepository<RoleDefaultVariable, Long> {

  List<RoleDefaultVariable> findAllByRoleIdOrderByKeyAsc(Long roleId);

  boolean existsByRoleIdAndKey(Long roleId, String key);
}
