package com.comutel.backend.workflow.service;

import com.comutel.backend.model.GrupoResolutor;
import com.comutel.backend.repository.GrupoResolutorRepository;
import com.comutel.backend.workflow.model.*;
import com.comutel.backend.workflow.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class WorkflowAdminService {

    @Autowired
    private WorkflowDefinitionRepository definitionRepository;

    @Autowired
    private WorkflowStateDefinitionRepository stateRepository;

    @Autowired
    private WorkflowTransitionDefinitionRepository transitionRepository;

    @Autowired
    private WorkflowAssignmentRuleRepository assignmentRuleRepository;

    @Autowired
    private WorkflowSlaPolicyRepository slaPolicyRepository;

    @Autowired
    private GrupoResolutorRepository grupoResolutorRepository;

    public List<WorkflowDefinition> listarDefiniciones(String processType) {
        if (processType == null || processType.isBlank()) {
            return definitionRepository.findAll();
        }
        return definitionRepository.findByProcessTypeOrderByKeyAscVersionDesc(processType);
    }

    public List<Map<String, Object>> listarDefinicionesView(String processType) {
        return listarDefiniciones(processType).stream()
                .map(this::toDefinitionSummary)
                .toList();
    }

    public WorkflowDefinition obtenerDefinicion(Long id) {
        return definitionRepository.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "WorkflowDefinition no encontrada"));
    }

    public Map<String, Object> obtenerDefinicionView(Long id) {
        WorkflowDefinition definition = obtenerDefinicion(id);

        Map<String, Object> detail = toDefinitionSummary(definition);
        List<WorkflowStateDefinition> states = stateRepository.findByWorkflowDefinitionIdOrderByIdAsc(id);
        List<WorkflowTransitionDefinition> transitions = transitionRepository.findByWorkflowDefinitionIdOrderByPriorityAsc(id);
        List<WorkflowAssignmentRule> rules = assignmentRuleRepository.findByWorkflowDefinitionIdOrderByPriorityOrderAsc(id);
        List<Map<String, Object>> statesView = states.stream().map(this::toStateView).toList();
        List<Map<String, Object>> transitionsView = transitions.stream()
                .map(this::toTransitionView)
                .toList();
        List<Map<String, Object>> rulesView = rules.stream()
                .map(this::toRuleView)
                .toList();

        detail.put("states", statesView);
        detail.put("transitions", transitionsView);
        detail.put("assignmentRules", rulesView);
        detail.put("transitionsCount", transitionsView.size());
        detail.put("statesCount", statesView.size());
        detail.put("rulesCount", rulesView.size());
        return detail;
    }

    @Transactional
    public WorkflowDefinition crearDefinicion(Map<String, Object> payload) {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setKey(requiredString(payload, "key"));
        definition.setName(requiredString(payload, "name"));
        definition.setProcessType(requiredString(payload, "processType"));
        definition.setVersion(parseInt(payload.get("version"), 1));
        definition.setStatus(WorkflowDefinitionStatus.DRAFT);
        definition.setActive(false);
        return definitionRepository.save(definition);
    }

    @Transactional
    public WorkflowDefinition activarDefinicion(Long id) {
        WorkflowDefinition definition = obtenerDefinicion(id);

        List<WorkflowDefinition> sameKey = definitionRepository.findByProcessTypeOrderByKeyAscVersionDesc(definition.getProcessType());
        for (WorkflowDefinition item : sameKey) {
            if (item.getKey().equalsIgnoreCase(definition.getKey()) && item.isActive() && !item.getId().equals(definition.getId())) {
                item.setActive(false);
                if (item.getStatus() == WorkflowDefinitionStatus.PUBLISHED) {
                    item.setStatus(WorkflowDefinitionStatus.ARCHIVED);
                }
                definitionRepository.save(item);
            }
        }

        definition.setActive(true);
        definition.setStatus(WorkflowDefinitionStatus.PUBLISHED);
        definition.setPublishedAt(LocalDateTime.now());
        return definitionRepository.save(definition);
    }

    @Transactional
    public WorkflowDefinition desactivarDefinicion(Long id) {
        WorkflowDefinition definition = obtenerDefinicion(id);
        definition.setActive(false);
        if (definition.getStatus() == WorkflowDefinitionStatus.PUBLISHED) {
            definition.setStatus(WorkflowDefinitionStatus.ARCHIVED);
        }
        return definitionRepository.save(definition);
    }

    @Transactional
    public WorkflowStateDefinition agregarEstado(Long definitionId, Map<String, Object> payload) {
        WorkflowDefinition definition = obtenerDefinicion(definitionId);
        String stateKey = requiredString(payload, "stateKey");

        if (stateRepository.existsByWorkflowDefinitionIdAndStateKey(definitionId, stateKey)) {
            throw badRequest("Ya existe un estado con stateKey=" + stateKey + " en esta definicion");
        }

        WorkflowStateDefinition state = new WorkflowStateDefinition();
        state.setWorkflowDefinition(definition);
        state.setStateKey(stateKey);
        state.setName(requiredString(payload, "name"));
        state.setStateType(parseEnum(payload.get("stateType"), WorkflowStateType.class, WorkflowStateType.NORMAL));
        state.setExternalStatus(optionalString(payload, "externalStatus"));
        state.setUiColor(optionalString(payload, "uiColor"));
        state.setSlaPolicy(resolveSlaPolicy(payload));

        return stateRepository.save(state);
    }

    @Transactional
    public WorkflowTransitionDefinition agregarTransicion(Long definitionId, Map<String, Object> payload) {
        WorkflowDefinition definition = obtenerDefinicion(definitionId);
        String fromStateKey = requiredString(payload, "fromStateKey");
        String toStateKey = requiredString(payload, "toStateKey");
        validateStateKeyExists(definitionId, fromStateKey, "fromStateKey");
        validateStateKeyExists(definitionId, toStateKey, "toStateKey");

        WorkflowTransitionDefinition transition = new WorkflowTransitionDefinition();
        transition.setWorkflowDefinition(definition);
        transition.setFromStateKey(fromStateKey);
        transition.setToStateKey(toStateKey);
        transition.setEventKey(requiredString(payload, "eventKey"));
        transition.setName(requiredString(payload, "name"));
        transition.setConditionExpression(optionalString(payload, "conditionExpression"));
        transition.setPriority(parseInt(payload.get("priority"), 100));
        transition.setActive(parseBoolean(payload.get("active"), true));
        transition = transitionRepository.save(transition);

        replaceActionsIfProvided(transition, payload, false);
        transition = transitionRepository.save(transition);

        return transition;
    }

    @Transactional
    public WorkflowAssignmentRule agregarReglaAsignacion(Long definitionId, Map<String, Object> payload) {
        WorkflowDefinition definition = obtenerDefinicion(definitionId);
        String stateKey = requiredString(payload, "stateKey");
        validateStateKeyExists(definitionId, stateKey, "stateKey");

        WorkflowAssignmentRule rule = new WorkflowAssignmentRule();
        rule.setWorkflowDefinition(definition);
        rule.setStateKey(stateKey);
        rule.setStrategy(parseEnum(payload.get("strategy"), WorkflowAssignmentStrategy.class, WorkflowAssignmentStrategy.NONE));
        rule.setExpression(optionalString(payload, "expression"));
        rule.setPriorityOrder(parseInt(payload.get("priorityOrder"), 100));
        rule.setActive(parseBoolean(payload.get("active"), true));
        rule.setTargetGroup(resolveTargetGroup(payload, false, rule.getTargetGroup()));

        return assignmentRuleRepository.save(rule);
    }

    @Transactional
    public WorkflowStateDefinition actualizarEstado(Long definitionId, Long stateId, Map<String, Object> payload) {
        WorkflowStateDefinition state = obtenerEstadoEnDefinicion(definitionId, stateId);
        String oldStateKey = state.getStateKey();
        String newStateKey = requiredString(payload, "stateKey");

        stateRepository.findByWorkflowDefinitionIdAndStateKey(definitionId, newStateKey)
                .filter(existing -> !existing.getId().equals(stateId))
                .ifPresent(existing -> {
                    throw badRequest("Ya existe un estado con stateKey=" + newStateKey + " en esta definicion");
                });

        state.setStateKey(newStateKey);
        state.setName(requiredString(payload, "name"));
        state.setStateType(parseEnum(payload.get("stateType"), WorkflowStateType.class, WorkflowStateType.NORMAL));
        state.setExternalStatus(optionalString(payload, "externalStatus"));
        state.setUiColor(optionalString(payload, "uiColor"));
        state.setSlaPolicy(resolveSlaPolicy(payload));
        state = stateRepository.save(state);

        if (!oldStateKey.equals(newStateKey)) {
            remapStateKeyReferences(definitionId, oldStateKey, newStateKey);
        }

        return state;
    }

    @Transactional
    public void eliminarEstado(Long definitionId, Long stateId) {
        WorkflowStateDefinition state = obtenerEstadoEnDefinicion(definitionId, stateId);
        String stateKey = state.getStateKey();

        boolean referencedByActiveTransition =
                transitionRepository.existsByWorkflowDefinitionIdAndFromStateKeyAndActiveTrue(definitionId, stateKey) ||
                        transitionRepository.existsByWorkflowDefinitionIdAndToStateKeyAndActiveTrue(definitionId, stateKey);
        if (referencedByActiveTransition) {
            throw badRequest("No se puede eliminar el estado porque tiene transiciones activas asociadas");
        }

        if (assignmentRuleRepository.countByWorkflowDefinitionIdAndStateKeyAndActiveTrue(definitionId, stateKey) > 0) {
            throw badRequest("No se puede eliminar el estado porque tiene reglas de asignacion activas");
        }

        List<WorkflowTransitionDefinition> relatedTransitions = transitionRepository.findByWorkflowDefinitionIdOrderByPriorityAsc(definitionId)
                .stream()
                .filter(t -> stateKey.equals(t.getFromStateKey()) || stateKey.equals(t.getToStateKey()))
                .toList();
        if (!relatedTransitions.isEmpty()) {
            transitionRepository.deleteAll(relatedTransitions);
        }

        List<WorkflowAssignmentRule> relatedRules = assignmentRuleRepository.findByWorkflowDefinitionIdAndStateKeyOrderByPriorityOrderAsc(definitionId, stateKey);
        if (!relatedRules.isEmpty()) {
            assignmentRuleRepository.deleteAll(relatedRules);
        }

        stateRepository.delete(state);
    }

    @Transactional
    public WorkflowTransitionDefinition actualizarTransicion(Long definitionId, Long transitionId, Map<String, Object> payload) {
        WorkflowTransitionDefinition transition = obtenerTransicionEnDefinicion(definitionId, transitionId);
        String fromStateKey = requiredString(payload, "fromStateKey");
        String toStateKey = requiredString(payload, "toStateKey");
        validateStateKeyExists(definitionId, fromStateKey, "fromStateKey");
        validateStateKeyExists(definitionId, toStateKey, "toStateKey");

        transition.setFromStateKey(fromStateKey);
        transition.setToStateKey(toStateKey);
        transition.setEventKey(requiredString(payload, "eventKey"));
        transition.setName(requiredString(payload, "name"));
        transition.setConditionExpression(optionalString(payload, "conditionExpression"));
        transition.setPriority(parseInt(payload.get("priority"), 100));
        transition.setActive(parseBoolean(payload.get("active"), true));

        replaceActionsIfProvided(transition, payload, true);
        return transitionRepository.save(transition);
    }

    @Transactional
    public void eliminarTransicion(Long definitionId, Long transitionId) {
        WorkflowTransitionDefinition transition = obtenerTransicionEnDefinicion(definitionId, transitionId);
        transitionRepository.delete(transition);
    }

    @Transactional
    public WorkflowAssignmentRule actualizarReglaAsignacion(Long definitionId, Long ruleId, Map<String, Object> payload) {
        WorkflowAssignmentRule rule = obtenerReglaEnDefinicion(definitionId, ruleId);
        String stateKey = requiredString(payload, "stateKey");
        validateStateKeyExists(definitionId, stateKey, "stateKey");

        rule.setStateKey(stateKey);
        rule.setStrategy(parseEnum(payload.get("strategy"), WorkflowAssignmentStrategy.class, WorkflowAssignmentStrategy.NONE));
        rule.setExpression(optionalString(payload, "expression"));
        rule.setPriorityOrder(parseInt(payload.get("priorityOrder"), rule.getPriorityOrder()));
        rule.setActive(parseBoolean(payload.get("active"), rule.isActive()));
        rule.setTargetGroup(resolveTargetGroup(payload, true, rule.getTargetGroup()));
        return assignmentRuleRepository.save(rule);
    }

    @Transactional
    public void eliminarReglaAsignacion(Long definitionId, Long ruleId) {
        WorkflowAssignmentRule rule = obtenerReglaEnDefinicion(definitionId, ruleId);
        assignmentRuleRepository.delete(rule);
    }

    @Transactional
    public WorkflowSlaPolicy crearSlaPolicy(Map<String, Object> payload) {
        WorkflowSlaPolicy policy = new WorkflowSlaPolicy();
        policy.setName(requiredString(payload, "name"));
        policy.setResponseMinutes(parseInt(payload.get("responseMinutes"), null));
        policy.setResolutionMinutes(parseInt(payload.get("resolutionMinutes"), null));
        policy.setWarningMinutes(parseInt(payload.get("warningMinutes"), null));
        policy.setEscalationEventKey(optionalString(payload, "escalationEventKey"));

        Long groupId = parseLong(payload.get("escalationGroupId"), null);
        if (groupId != null) {
            policy.setEscalationGroup(grupoResolutorRepository.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Grupo de escalamiento no encontrado")));
        }

        return slaPolicyRepository.save(policy);
    }

    public List<WorkflowSlaPolicy> listarSlaPolicies() {
        return slaPolicyRepository.findAll();
    }

    private WorkflowStateDefinition obtenerEstadoEnDefinicion(Long definitionId, Long stateId) {
        return stateRepository.findByIdAndWorkflowDefinitionId(stateId, definitionId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Estado no encontrado en la definicion"));
    }

    private WorkflowTransitionDefinition obtenerTransicionEnDefinicion(Long definitionId, Long transitionId) {
        return transitionRepository.findByIdAndWorkflowDefinitionId(transitionId, definitionId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Transicion no encontrada en la definicion"));
    }

    private WorkflowAssignmentRule obtenerReglaEnDefinicion(Long definitionId, Long ruleId) {
        return assignmentRuleRepository.findByIdAndWorkflowDefinitionId(ruleId, definitionId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Regla de asignacion no encontrada en la definicion"));
    }

    private void validateStateKeyExists(Long definitionId, String stateKey, String fieldName) {
        if (stateRepository.findByWorkflowDefinitionIdAndStateKey(definitionId, stateKey).isEmpty()) {
            throw badRequest(fieldName + " no existe en esta definicion: " + stateKey);
        }
    }

    private void remapStateKeyReferences(Long definitionId, String oldStateKey, String newStateKey) {
        List<WorkflowTransitionDefinition> transitions = new ArrayList<>(transitionRepository.findByWorkflowDefinitionIdOrderByPriorityAsc(definitionId));
        boolean transitionsChanged = false;
        for (WorkflowTransitionDefinition transition : transitions) {
            if (oldStateKey.equals(transition.getFromStateKey())) {
                transition.setFromStateKey(newStateKey);
                transitionsChanged = true;
            }
            if (oldStateKey.equals(transition.getToStateKey())) {
                transition.setToStateKey(newStateKey);
                transitionsChanged = true;
            }
        }
        if (transitionsChanged) {
            transitionRepository.saveAll(transitions);
        }

        List<WorkflowAssignmentRule> rules = new ArrayList<>(assignmentRuleRepository.findByWorkflowDefinitionIdOrderByPriorityOrderAsc(definitionId));
        boolean rulesChanged = false;
        for (WorkflowAssignmentRule rule : rules) {
            if (oldStateKey.equals(rule.getStateKey())) {
                rule.setStateKey(newStateKey);
                rulesChanged = true;
            }
        }
        if (rulesChanged) {
            assignmentRuleRepository.saveAll(rules);
        }
    }

    private WorkflowSlaPolicy resolveSlaPolicy(Map<String, Object> payload) {
        if (!payload.containsKey("slaPolicyId")) {
            return null;
        }

        Long slaId = parseLong(payload.get("slaPolicyId"), null);
        if (slaId == null) {
            return null;
        }

        return slaPolicyRepository.findById(slaId)
                .orElseThrow(() -> badRequest("SLA policy no encontrada"));
    }

    private GrupoResolutor resolveTargetGroup(Map<String, Object> payload, boolean allowUnset, GrupoResolutor currentGroup) {
        if (!payload.containsKey("targetGroupId")) {
            return currentGroup;
        }

        Long groupId = parseLong(payload.get("targetGroupId"), null);
        if (groupId == null) {
            return allowUnset ? null : currentGroup;
        }

        return grupoResolutorRepository.findById(groupId)
                .orElseThrow(() -> badRequest("Grupo no encontrado"));
    }

    private void replaceActionsIfProvided(WorkflowTransitionDefinition transition, Map<String, Object> payload, boolean replaceExisting) {
        if (!payload.containsKey("actions")) {
            return;
        }

        Object actionsObj = payload.get("actions");
        if (!(actionsObj instanceof List<?> actionsList)) {
            throw badRequest("actions debe ser una lista");
        }

        if (replaceExisting) {
            transition.getActions().clear();
        }

        int order = 1;
        for (Object actionObj : actionsList) {
            if (!(actionObj instanceof Map<?, ?> actionMap)) {
                continue;
            }

            Object actionKeyRaw = actionMap.get("actionKey");
            String actionKey = actionKeyRaw == null ? "" : String.valueOf(actionKeyRaw).trim();
            if (actionKey.isBlank()) {
                throw badRequest("Cada action debe tener actionKey");
            }

            WorkflowTransitionAction action = new WorkflowTransitionAction();
            action.setTransition(transition);
            action.setActionKey(actionKey);

            Object actionParamsRaw = actionMap.get("actionParams");
            action.setActionParams(actionParamsRaw == null ? "" : String.valueOf(actionParamsRaw));
            action.setRunMode(parseEnum(actionMap.get("runMode"), WorkflowActionRunMode.class, WorkflowActionRunMode.SYNC));
            action.setOrderNo(parseInt(actionMap.get("orderNo"), order++));
            transition.getActions().add(action);
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(BAD_REQUEST, message);
    }

    private String requiredString(Map<String, Object> payload, String key) {
        String value = optionalString(payload, key);
        if (value == null || value.isBlank()) {
            throw badRequest(key + " es obligatorio");
        }
        return value;
    }

    private String optionalString(Map<String, Object> payload, String key) {
        Object raw = payload.get(key);
        return raw == null ? null : String.valueOf(raw).trim();
    }

    private Integer parseInt(Object value, Integer fallback) {
        try {
            return value == null ? fallback : Integer.valueOf(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private Long parseLong(Object value, Long fallback) {
        try {
            return value == null ? fallback : Long.valueOf(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private boolean parseBoolean(Object value, boolean fallback) {
        if (value == null) return fallback;
        if (value instanceof Boolean b) return b;
        String raw = String.valueOf(value);
        if ("true".equalsIgnoreCase(raw)) return true;
        if ("false".equalsIgnoreCase(raw)) return false;
        return fallback;
    }

    private <T extends Enum<T>> T parseEnum(Object value, Class<T> type, T fallback) {
        if (value == null) return fallback;
        try {
            return Enum.valueOf(type, String.valueOf(value).trim().toUpperCase());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private Map<String, Object> toDefinitionSummary(WorkflowDefinition definition) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", definition.getId());
        view.put("key", definition.getKey());
        view.put("name", definition.getName());
        view.put("processType", definition.getProcessType());
        view.put("version", definition.getVersion());
        view.put("status", definition.getStatus().name());
        view.put("active", definition.isActive());
        view.put("publishedAt", definition.getPublishedAt());
        view.put("createdAt", definition.getCreatedAt());
        view.put("updatedAt", definition.getUpdatedAt());
        return view;
    }

    private Map<String, Object> toStateView(WorkflowStateDefinition state) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", state.getId());
        view.put("stateKey", state.getStateKey());
        view.put("name", state.getName());
        view.put("stateType", state.getStateType().name());
        view.put("externalStatus", state.getExternalStatus());
        view.put("uiColor", state.getUiColor());
        view.put("slaPolicyId", state.getSlaPolicy() != null ? state.getSlaPolicy().getId() : null);
        view.put("slaPolicyName", state.getSlaPolicy() != null ? state.getSlaPolicy().getName() : null);
        return view;
    }

    private Map<String, Object> toTransitionView(WorkflowTransitionDefinition transition) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", transition.getId());
        view.put("fromStateKey", transition.getFromStateKey());
        view.put("toStateKey", transition.getToStateKey());
        view.put("eventKey", transition.getEventKey());
        view.put("name", transition.getName());
        view.put("conditionExpression", transition.getConditionExpression());
        view.put("priority", transition.getPriority());
        view.put("active", transition.isActive());
        view.put("actions", transition.getActions().stream().map(action -> {
            Map<String, Object> actionView = new HashMap<>();
            actionView.put("id", action.getId());
            actionView.put("actionKey", action.getActionKey());
            actionView.put("actionParams", action.getActionParams());
            actionView.put("runMode", action.getRunMode().name());
            actionView.put("orderNo", action.getOrderNo());
            return actionView;
        }).toList());
        return view;
    }

    private Map<String, Object> toRuleView(WorkflowAssignmentRule rule) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", rule.getId());
        view.put("stateKey", rule.getStateKey());
        view.put("strategy", rule.getStrategy().name());
        view.put("expression", rule.getExpression());
        view.put("targetGroupId", rule.getTargetGroup() != null ? rule.getTargetGroup().getId() : null);
        view.put("priorityOrder", rule.getPriorityOrder());
        view.put("active", rule.isActive());
        return view;
    }
}
