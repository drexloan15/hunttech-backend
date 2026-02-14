package com.comutel.backend.workflow.repository;

import com.comutel.backend.workflow.model.WorkflowAssignmentRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowAssignmentRuleRepository extends JpaRepository<WorkflowAssignmentRule, Long> {
    Optional<WorkflowAssignmentRule> findByIdAndWorkflowDefinitionId(Long id, Long workflowDefinitionId);
    long countByWorkflowDefinitionIdAndStateKeyAndActiveTrue(Long workflowDefinitionId, String stateKey);
    List<WorkflowAssignmentRule> findByWorkflowDefinitionIdAndStateKeyAndActiveTrueOrderByPriorityOrderAsc(Long workflowDefinitionId, String stateKey);
    List<WorkflowAssignmentRule> findByWorkflowDefinitionIdAndStateKeyOrderByPriorityOrderAsc(Long workflowDefinitionId, String stateKey);
    List<WorkflowAssignmentRule> findByWorkflowDefinitionIdOrderByPriorityOrderAsc(Long workflowDefinitionId);
}
