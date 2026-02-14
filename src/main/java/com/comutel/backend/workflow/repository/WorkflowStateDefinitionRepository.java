package com.comutel.backend.workflow.repository;

import com.comutel.backend.workflow.model.WorkflowStateDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowStateDefinitionRepository extends JpaRepository<WorkflowStateDefinition, Long> {
    boolean existsByWorkflowDefinitionIdAndStateKey(Long workflowDefinitionId, String stateKey);
    java.util.Optional<WorkflowStateDefinition> findByIdAndWorkflowDefinitionId(Long id, Long workflowDefinitionId);
    Optional<WorkflowStateDefinition> findByWorkflowDefinitionIdAndStateKey(Long workflowDefinitionId, String stateKey);
    Optional<WorkflowStateDefinition> findByWorkflowDefinitionIdAndStateType(Long workflowDefinitionId, com.comutel.backend.workflow.model.WorkflowStateType stateType);
    List<WorkflowStateDefinition> findByWorkflowDefinitionIdOrderByIdAsc(Long workflowDefinitionId);
}
