package com.ansible.role.repository;

import com.ansible.role.entity.Template;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateRepository extends JpaRepository<Template, Long> {

  List<Template> findAllByRoleIdOrderByParentDirAscNameAsc(Long roleId);

  boolean existsByRoleIdAndParentDirAndName(Long roleId, String parentDir, String name);

  List<Template> findByRoleIdAndParentDirStartingWith(Long roleId, String parentDirPrefix);

  void deleteByRoleId(Long roleId);
}
