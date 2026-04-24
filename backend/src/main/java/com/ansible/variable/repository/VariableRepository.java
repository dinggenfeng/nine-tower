package com.ansible.variable.repository;

import com.ansible.variable.entity.Variable;
import com.ansible.variable.entity.VariableScope;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VariableRepository extends JpaRepository<Variable, Long> {

  List<Variable> findByScopeAndScopeIdOrderByIdAsc(VariableScope scope, Long scopeId);

  List<Variable> findByScopeOrderByIdAsc(VariableScope scope);

  boolean existsByScopeAndScopeIdAndKey(VariableScope scope, Long scopeId, String key);

  boolean existsByScopeAndScopeIdAndKeyAndIdNot(
      VariableScope scope, Long scopeId, String key, Long id);

  void deleteByScopeAndScopeId(VariableScope scope, Long scopeId);
}
