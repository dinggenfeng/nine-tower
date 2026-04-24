package com.ansible.environment.repository;

import com.ansible.environment.entity.EnvConfig;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnvConfigRepository extends JpaRepository<EnvConfig, Long> {

  List<EnvConfig> findByEnvironmentIdOrderByConfigKeyAsc(Long environmentId);

  boolean existsByEnvironmentIdAndConfigKey(Long environmentId, String configKey);

  boolean existsByEnvironmentIdAndConfigKeyAndIdNot(
      Long environmentId, String configKey, Long id);

  void deleteByEnvironmentId(Long environmentId);

  List<EnvConfig> findByEnvironmentIdInOrderByEnvironmentIdAscConfigKeyAsc(List<Long> environmentIds);
}
