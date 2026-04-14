package com.ansible.role.repository;

import com.ansible.role.entity.Handler;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HandlerRepository extends JpaRepository<Handler, Long> {

  List<Handler> findAllByRoleId(Long roleId);
}
