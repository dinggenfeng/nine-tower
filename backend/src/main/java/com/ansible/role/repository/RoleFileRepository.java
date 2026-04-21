package com.ansible.role.repository;

import com.ansible.role.entity.RoleFile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleFileRepository extends JpaRepository<RoleFile, Long> {

  List<RoleFile> findByRoleIdOrderByParentDirAscNameAsc(Long roleId);

  List<RoleFile> findByRoleIdAndParentDirOrderByIsDirectoryDescNameAsc(
      Long roleId, String parentDir);

  Optional<RoleFile> findByRoleIdAndParentDirAndName(Long roleId, String parentDir, String name);

  boolean existsByRoleIdAndParentDirAndName(Long roleId, String parentDir, String name);

  boolean existsByRoleIdAndParentDirAndNameAndIdNot(
      Long roleId, String parentDir, String name, Long id);

  void deleteByRoleId(Long roleId);

  List<RoleFile> findByRoleIdAndParentDirStartingWith(Long roleId, String parentDirPrefix);

  long countByRoleIdAndParentDirStartingWith(Long roleId, String parentDirPrefix);
}
