package com.comutel.backend.controller;

import com.comutel.backend.workflow.model.*;
import com.comutel.backend.workflow.service.WorkflowAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workflows/admin")
public class WorkflowAdminController {

    @Autowired
    private WorkflowAdminService workflowAdminService;

    @GetMapping("/definitions")
    public List<Map<String, Object>> listarDefiniciones(@RequestParam(required = false) String processType) {
        return workflowAdminService.listarDefinicionesView(processType);
    }

    @GetMapping("/definitions/{id}")
    public Map<String, Object> obtenerDefinicion(@PathVariable Long id) {
        return workflowAdminService.obtenerDefinicionView(id);
    }

    @PostMapping("/definitions")
    public Map<String, Object> crearDefinicion(@RequestBody Map<String, Object> payload) {
        WorkflowDefinition definition = workflowAdminService.crearDefinicion(payload);
        return Map.of(
                "id", definition.getId(),
                "key", definition.getKey(),
                "name", definition.getName(),
                "processType", definition.getProcessType(),
                "version", definition.getVersion(),
                "status", definition.getStatus().name(),
                "active", definition.isActive()
        );
    }

    @PutMapping("/definitions/{id}/activate")
    public Map<String, Object> activarDefinicion(@PathVariable Long id) {
        WorkflowDefinition definition = workflowAdminService.activarDefinicion(id);
        return Map.of(
                "id", definition.getId(),
                "status", definition.getStatus().name(),
                "active", definition.isActive(),
                "publishedAt", definition.getPublishedAt()
        );
    }

    @PutMapping("/definitions/{id}/deactivate")
    public Map<String, Object> desactivarDefinicion(@PathVariable Long id) {
        WorkflowDefinition definition = workflowAdminService.desactivarDefinicion(id);
        return Map.of(
                "id", definition.getId(),
                "status", definition.getStatus().name(),
                "active", definition.isActive()
        );
    }

    @PostMapping("/definitions/{id}/states")
    public Map<String, Object> agregarEstado(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        WorkflowStateDefinition state = workflowAdminService.agregarEstado(id, payload);
        return Map.of(
                "id", state.getId(),
                "stateKey", state.getStateKey(),
                "name", state.getName(),
                "stateType", state.getStateType().name(),
                "externalStatus", state.getExternalStatus() == null ? "" : state.getExternalStatus()
        );
    }

    @PutMapping("/definitions/{definitionId}/states/{stateId}")
    public Map<String, Object> actualizarEstado(
            @PathVariable Long definitionId,
            @PathVariable Long stateId,
            @RequestBody Map<String, Object> payload
    ) {
        WorkflowStateDefinition state = workflowAdminService.actualizarEstado(definitionId, stateId, payload);
        return Map.of(
                "id", state.getId(),
                "stateKey", state.getStateKey(),
                "name", state.getName(),
                "stateType", state.getStateType().name(),
                "externalStatus", state.getExternalStatus() == null ? "" : state.getExternalStatus(),
                "uiColor", state.getUiColor() == null ? "" : state.getUiColor(),
                "slaPolicyId", state.getSlaPolicy() != null ? state.getSlaPolicy().getId() : 0
        );
    }

    @DeleteMapping("/definitions/{definitionId}/states/{stateId}")
    public Map<String, Object> eliminarEstado(@PathVariable Long definitionId, @PathVariable Long stateId) {
        workflowAdminService.eliminarEstado(definitionId, stateId);
        return Map.of("deleted", true, "stateId", stateId);
    }

    @PostMapping("/definitions/{id}/transitions")
    public Map<String, Object> agregarTransicion(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        WorkflowTransitionDefinition transition = workflowAdminService.agregarTransicion(id, payload);
        return Map.of(
                "id", transition.getId(),
                "fromStateKey", transition.getFromStateKey(),
                "toStateKey", transition.getToStateKey(),
                "eventKey", transition.getEventKey(),
                "name", transition.getName(),
                "priority", transition.getPriority(),
                "active", transition.isActive()
        );
    }

    @PutMapping("/definitions/{definitionId}/transitions/{transitionId}")
    public Map<String, Object> actualizarTransicion(
            @PathVariable Long definitionId,
            @PathVariable Long transitionId,
            @RequestBody Map<String, Object> payload
    ) {
        WorkflowTransitionDefinition transition = workflowAdminService.actualizarTransicion(definitionId, transitionId, payload);
        return Map.of(
                "id", transition.getId(),
                "fromStateKey", transition.getFromStateKey(),
                "toStateKey", transition.getToStateKey(),
                "eventKey", transition.getEventKey(),
                "name", transition.getName(),
                "conditionExpression", transition.getConditionExpression() == null ? "" : transition.getConditionExpression(),
                "priority", transition.getPriority(),
                "active", transition.isActive(),
                "actionsCount", transition.getActions().size()
        );
    }

    @DeleteMapping("/definitions/{definitionId}/transitions/{transitionId}")
    public Map<String, Object> eliminarTransicion(@PathVariable Long definitionId, @PathVariable Long transitionId) {
        workflowAdminService.eliminarTransicion(definitionId, transitionId);
        return Map.of("deleted", true, "transitionId", transitionId);
    }

    @PostMapping("/definitions/{id}/assignment-rules")
    public Map<String, Object> agregarReglaAsignacion(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        WorkflowAssignmentRule rule = workflowAdminService.agregarReglaAsignacion(id, payload);
        return Map.of(
                "id", rule.getId(),
                "stateKey", rule.getStateKey(),
                "strategy", rule.getStrategy().name(),
                "priorityOrder", rule.getPriorityOrder(),
                "active", rule.isActive()
        );
    }

    @PutMapping("/definitions/{definitionId}/assignment-rules/{ruleId}")
    public Map<String, Object> actualizarReglaAsignacion(
            @PathVariable Long definitionId,
            @PathVariable Long ruleId,
            @RequestBody Map<String, Object> payload
    ) {
        WorkflowAssignmentRule rule = workflowAdminService.actualizarReglaAsignacion(definitionId, ruleId, payload);
        return Map.of(
                "id", rule.getId(),
                "stateKey", rule.getStateKey(),
                "strategy", rule.getStrategy().name(),
                "expression", rule.getExpression() == null ? "" : rule.getExpression(),
                "targetGroupId", rule.getTargetGroup() != null ? rule.getTargetGroup().getId() : 0,
                "priorityOrder", rule.getPriorityOrder(),
                "active", rule.isActive()
        );
    }

    @DeleteMapping("/definitions/{definitionId}/assignment-rules/{ruleId}")
    public Map<String, Object> eliminarReglaAsignacion(@PathVariable Long definitionId, @PathVariable Long ruleId) {
        workflowAdminService.eliminarReglaAsignacion(definitionId, ruleId);
        return Map.of("deleted", true, "ruleId", ruleId);
    }

    @GetMapping("/sla-policies")
    public List<Map<String, Object>> listarSlaPolicies() {
        return workflowAdminService.listarSlaPolicies().stream().map(policy -> Map.<String, Object>of(
                "id", policy.getId(),
                "name", policy.getName(),
                "responseMinutes", policy.getResponseMinutes() == null ? 0 : policy.getResponseMinutes(),
                "resolutionMinutes", policy.getResolutionMinutes() == null ? 0 : policy.getResolutionMinutes(),
                "warningMinutes", policy.getWarningMinutes() == null ? 0 : policy.getWarningMinutes(),
                "escalationEventKey", policy.getEscalationEventKey() == null ? "" : policy.getEscalationEventKey(),
                "escalationGroupId", policy.getEscalationGroup() != null ? policy.getEscalationGroup().getId() : 0
        )).toList();
    }

    @PostMapping("/sla-policies")
    public Map<String, Object> crearSlaPolicy(@RequestBody Map<String, Object> payload) {
        WorkflowSlaPolicy policy = workflowAdminService.crearSlaPolicy(payload);
        return Map.of(
                "id", policy.getId(),
                "name", policy.getName(),
                "responseMinutes", policy.getResponseMinutes() == null ? 0 : policy.getResponseMinutes(),
                "resolutionMinutes", policy.getResolutionMinutes() == null ? 0 : policy.getResolutionMinutes(),
                "warningMinutes", policy.getWarningMinutes() == null ? 0 : policy.getWarningMinutes()
        );
    }
}
