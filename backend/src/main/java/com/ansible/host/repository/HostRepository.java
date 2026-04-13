package com.ansible.host.repository;

import com.ansible.host.entity.Host;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HostRepository extends JpaRepository<Host, Long> {

  List<Host> findAllByHostGroupId(Long hostGroupId);

  void deleteAllByHostGroupId(Long hostGroupId);
}