package com.abhinavmehta.confx.repository;

import com.abhinavmehta.confx.entity.Rule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RuleRepository extends JpaRepository<Rule, Long> {
    List<Rule> findByConfigVersionIdOrderByPriorityAsc(Long configVersionId);
    Optional<Rule> findByConfigVersionIdAndPriority(Long configVersionId, Integer priority);
    void deleteByConfigVersionId(Long configVersionId); // For bulk deletion when a version is superseded or rules are fully replaced
} 