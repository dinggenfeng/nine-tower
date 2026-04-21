package com.ansible.playbook.repository;

import com.ansible.playbook.entity.Playbook;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaybookRepository extends JpaRepository<Playbook, Long> {
  List<Playbook> findByProjectIdOrderByIdAsc(Long projectId);
}
