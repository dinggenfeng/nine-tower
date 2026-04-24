package com.ansible.host.repository;

import com.ansible.host.entity.HostGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HostGroupRepository extends JpaRepository<HostGroup, Long> {

  List<HostGroup> findAllByProjectId(Long projectId);

  boolean existsByProjectIdAndName(Long projectId, String name);

  void deleteByProjectId(Long projectId);
}
