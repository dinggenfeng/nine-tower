package com.ansible.tag.repository;

import com.ansible.tag.entity.Tag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {

  List<Tag> findByProjectIdOrderByIdAsc(Long projectId);

  boolean existsByProjectIdAndName(Long projectId, String name);

  boolean existsByProjectIdAndNameAndIdNot(Long projectId, String name, Long id);
}
