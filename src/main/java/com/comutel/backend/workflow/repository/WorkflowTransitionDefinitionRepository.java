package com.comutel.backend.workflow.repository;

import com.comutel.backend.workflow.model.WorkflowTransitionDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowTransitionDefinitionRepository extends JpaRepository<WorkflowTransitionDefinition, Long> {
    Optional<WorkflowTransitionDefinition> findByIdAndWorkflowDefinitionId(Long id, Long workflowDefinitionId);
    boolean existsByWorkflowDefinitionIdAndFromStateKeyAndActiveTrue(Long workflowDefinitionId, String fromStateKey);
    boolean existsByWorkflowDefinitionIdAndToStateKeyAndActiveTrue(Long workflowDefinitionId, String toStateKey);
    List<WorkflowTransitionDefinition> findByWorkflowDefinitionIdAndFromStateKeyAndEventKeyAndActiveTrueOrderByPriorityAsc(
            Long workflowDefinitionId,
            String fromStateKey,
            String eventKey
    );

    List<WorkflowTransitionDefinition> findByWorkflowDefinitionIdAndFromStateKeyAndActiveTrueOrderByPriorityAsc(
            Long workflowDefinitionId,
            String fromStateKey
    );

    List<WorkflowTransitionDefinition> findByWorkflowDefinitionIdOrderByPriorityAsc(Long workflowDefinitionId);
}
