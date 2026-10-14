package com.finex.auth.agent.workflow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Conversation-level coordination. Individual workflow runtime remains the executor. */
@Service
@RequiredArgsConstructor
public class FinWorkOrchestrationService {
    private final FinWorkQueryPlanner planner;
    private final FinWorkParameterService parameters;
    private final WorkflowSelectorService selector;
    private final AgentWorkflowRuntimeService runtime;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public Outcome respond(String sessionId, Long userId, String message,
                           Long employeeId, Long bindingId, List<String> selectedOptions) {
        return respond(sessionId, userId, message, employeeId, bindingId, selectedOptions, Map.of());
    }

    public Outcome respond(String sessionId, Long userId, String message,
                           Long employeeId, Long bindingId, List<String> selectedOptions,
                           Map<String, String> selectedAnswers) {
        Pending pending = loadPending(sessionId, userId);
        if (pending != null && !pendingStillAuthorized(userId, pending)) {
            clearPending(sessionId, userId);
            throw new IllegalArgumentException("澄清状态已失效，请重新提问");
        }
        String newQuestion = explicitNewQuestion(message);
        if (pending != null && newQuestion != null) {
            clearPending(sessionId, userId);
            return respond(sessionId, userId, newQuestion, employeeId, bindingId,
                    List.of(), Map.of());
        }
        if (pending != null && !pending.groups().isEmpty() && pending.steps().isEmpty()) {
            clearPending(sessionId, userId);
            throw new IllegalArgumentException("旧澄清状态已失效，请重新提问");
        }
        Map<String, String> confirmed = new java.util.LinkedHashMap<>(checkedAnswers(pending, selectedAnswers));
        String answerText = clarificationText(message, pending, selectedAnswers);
        String conversationQuestion = pending == null ? answerText
                : pending.originalQuestion() + "\n补充说明：" + answerText;
        if (conversationQuestion == null || conversationQuestion.length() > 3000)
            throw new IllegalArgumentException("澄清内容过长，请重新提问");
        String fullQuestion = conversationQuestion;
        if (!confirmed.isEmpty()) fullQuestion += "\n用户已确认参数：" + confirmed;
        Set<Long> chosen = selectedBindings(selectedOptions, pending);
        if (!chosen.isEmpty()) fullQuestion += "\n用户明确选择的工作流绑定 ID：" + chosen;
        Long selectedBinding = pending != null && pending.bindingIds().size() == 1
                ? pending.bindingIds().get(0) : bindingId;
        FinWorkQueryPlanner.Decision decision;
        if (pending != null && !pending.steps().isEmpty()) {
            List<FinWorkQueryPlanner.PlannedStep> current = restoreSteps(userId, pending);
            FinWorkQueryPlanner.Revision revised = planner.revise(userId, pending.originalQuestion(),
                    answerText, current, pending.groups(), sessionId);
            if (revised.newTask()) {
                clearPending(sessionId, userId);
                return respond(sessionId, userId, message, employeeId, bindingId,
                        List.of(), Map.of());
            }
            if (revised.changedFields().isEmpty()
                    && message.matches("(?s).*(?:改成|换成|改为|不是这个|重新查|不对).*"))
                return new Outcome("CLARIFY", "请再说明要修改的查询条件。",
                        List.of(), pending.groups(), List.of());
            if (conflictingSelection(pending, selectedAnswers, revised, message))
                return new Outcome("CLARIFY", "本轮文字与所选项不一致，请确认查询条件。",
                        List.of(), pending.groups(), List.of());
            clearOverriddenAnswers(confirmed, selectedAnswers, revised.changedFields());
            decision = new FinWorkQueryPlanner.Decision(FinWorkQueryPlanner.Mode.EXECUTE,
                    revised.steps(), "", pending.analysisGoal(),
                    revised.steps().stream().map(FinWorkQueryPlanner.PlannedStep::workflow).toList());
        } else {
            decision = planner.plan(userId, fullQuestion,
                    employeeId, selectedBinding, sessionId);
        }
        return switch (decision.mode()) {
            case CHAT -> {
                clearPending(sessionId, userId);
                yield new Outcome("CHAT", "", List.of(), List.of(), List.of());
            }
            case NO_CAPABILITY -> {
                clearPending(sessionId, userId);
                yield new Outcome("NO_CAPABILITY", "当前分配的数字员工没有能够完成这项任务的工作流。",
                        List.of(), List.of(), List.of());
            }
            case CLARIFY -> {
                List<Option> options = decision.candidates().stream()
                        .map(item -> new Option("binding:" + item.getWorkflowBindingId(),
                                item.getDigitalEmployeeName() + " · " + item.getAgentName()))
                        .toList();
                List<Option> displayed = new ArrayList<>(options);
                if (options.size() > 1 && options.size() <= 4) {
                    displayed.add(new Option("all", "同时执行以上工作流"));
                }
                savePending(sessionId, userId, new Pending(
                        pending == null ? message : pending.originalQuestion(),
                        decision.candidates().stream().map(SelectedWorkflow::getWorkflowBindingId).toList(),
                        confirmed, versionIds(decision.candidates()),
                        parameters.authorizationSignatures(userId, decision.candidates()), List.of(),
                        List.of(), decision.analysisGoal()));
                yield new Outcome("CLARIFY", decision.question(), displayed, List.of(), List.of());
            }
            case EXECUTE -> {
                if (!chosen.isEmpty() && !decision.steps().stream()
                        .map(step -> step.workflow().getWorkflowBindingId()).collect(java.util.stream.Collectors.toSet())
                        .equals(chosen)) {
                    throw new IllegalStateException("澄清选择与任务计划不一致，请重新说明需求");
                }
                if (pending != null && !pending.versionIds().isEmpty()) {
                    for (FinWorkQueryPlanner.PlannedStep step : decision.steps()) {
                        Long expected = pending.versionIds().get(step.workflow().getWorkflowBindingId());
                        if (expected == null || !expected.equals(step.workflow().getAgentVersionId()))
                            throw new IllegalStateException("工作流版本或授权已变化，请重新提问");
                    }
                }
                FinWorkParameterService.Preparation prepared = parameters.prepare(
                        userId, fullQuestion, decision.steps(), confirmed);
                if (!prepared.groups().isEmpty()) {
                    List<SelectedWorkflow> workflows = decision.steps().stream()
                            .map(FinWorkQueryPlanner.PlannedStep::workflow).toList();
                    savePending(sessionId, userId, new Pending(
                            pending == null ? message : pending.originalQuestion(),
                            workflows.stream().map(SelectedWorkflow::getWorkflowBindingId).toList(),
                            confirmed, versionIds(workflows),
                            parameters.authorizationSignatures(userId, workflows), prepared.groups(),
                            states(decision.steps()), decision.analysisGoal()));
                    yield new Outcome("CLARIFY", "请补充查询条件后继续。", List.of(),
                            prepared.groups(), List.of());
                }
                clearPending(sessionId, userId);
                List<Execution> executions = new ArrayList<>();
                StringBuilder answer = new StringBuilder();
                BigDecimal expenseTotal = BigDecimal.ZERO;
                int expenseWorkflowCount = 0;
                boolean expenseTotalComplete = true;
                for (FinWorkQueryPlanner.PlannedStep step : prepared.steps()) {
                    WorkflowExecutionResult result = runtime.executePlannedWorkflow(
                            sessionId, userId, step.workflow(), step.queryText(), step.parameters());
                    if (!result.isSuccess()) {
                        throw new SecurityException(result.getContent() == null
                                ? "系统查询到您没有相关权限，请换一个问题" : result.getContent());
                    }
                    executions.add(new Execution(result, step.queryText(), step.workflow().getWorkflowBindingId()));
                    if (!answer.isEmpty()) answer.append("\n\n");
                    answer.append(result.getContent());
                    if ((step.workflow().getToolCodes() != null
                            && step.workflow().getToolCodes().contains(ExpenseDocumentControlledQueryTool.TOOL_CODE)
                            || ExpenseDocumentControlledQueryTool.TOOL_CODE.equals(step.workflow().getToolCode()))) {
                        expenseWorkflowCount++;
                        if (result.getTotalAmount() == null) expenseTotalComplete = false;
                        else expenseTotal = expenseTotal.add(result.getTotalAmount());
                    }
                }
                if (expenseWorkflowCount == 1 && expenseTotalComplete
                        && asksForTotal(fullQuestion, decision.analysisGoal())) {
                    answer.append("\n\n上述授权报销单的总金额：")
                            .append(expenseTotal.stripTrailingZeros().toPlainString())
                            .append("。该金额基于全部符合条件的单据，不限于展示条数。");
                } else if (expenseWorkflowCount > 1 && asksForTotal(fullQuestion, decision.analysisGoal())) {
                    answer.append("\n\n多个工作流的结果可能包含同一单据，未将各结果金额直接相加。请指定需要合计的工作流或查询范围。");
                } else if (expenseWorkflowCount == 1 && !expenseTotalComplete
                        && asksForTotal(fullQuestion, decision.analysisGoal())) {
                    answer.append("\n\n该工作流未提供可复核的完整金额合计，请明确报销单查询范围后重试。");
                }
                yield new Outcome("ANSWER", answer.toString(), List.of(), List.of(), List.copyOf(executions));
            }
        };
    }

    public List<Option> pendingOptions(String sessionId, Long userId) {
        Pending pending;
        try { pending = loadPending(sessionId, userId); }
        catch (IllegalArgumentException invalid) { return List.of(); }
        if (pending == null) return List.of();
        if (!pendingStillAuthorized(userId, pending)) return List.of();
        if (!pending.groups().isEmpty()) return List.of();
        List<Option> options = new ArrayList<>();
        for (Long id : pending.bindingIds()) options.add(new Option("binding:" + id, "工作流 #" + id));
        if (options.size() > 1 && options.size() <= 4) options.add(new Option("all", "同时执行以上工作流"));
        return options;
    }

    public List<FinWorkParameterService.Group> pendingGroups(String sessionId, Long userId) {
        Pending pending;
        try { pending = loadPending(sessionId, userId); }
        catch (IllegalArgumentException invalid) { return List.of(); }
        if (pending == null || pending.groups().isEmpty()) return List.of();
        if (!pendingStillAuthorized(userId, pending)) {
            clearPending(sessionId, userId);
            return List.of();
        }
        return pending.groups();
    }

    private Map<String, String> checkedAnswers(Pending pending, Map<String, String> selected) {
        Map<String, String> confirmed = new java.util.LinkedHashMap<>(pending == null ? Map.of() : pending.answers());
        if (selected == null || selected.isEmpty()) return Map.copyOf(confirmed);
        if (pending == null) throw new IllegalArgumentException("澄清选项已失效，请重新提问");
        for (Map.Entry<String, String> answer : selected.entrySet()) {
            FinWorkParameterService.Group group = pending.groups().stream()
                    .filter(item -> item.key().equals(answer.getKey())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("澄清选项无效"));
            String value = answer.getValue() == null ? "" : answer.getValue().trim();
            if (value.isBlank() || value.length() > 200 || (!group.options().isEmpty()
                    && !group.options().stream().anyMatch(item -> item.id().equals(value))))
                throw new IllegalArgumentException("澄清选项无效");
            if (!group.options().isEmpty()) confirmed.put(group.key(), value);
        }
        return Map.copyOf(confirmed);
    }

    private String clarificationText(String message, Pending pending, Map<String, String> selected) {
        if (pending == null || selected == null || selected.isEmpty()) return message;
        String visible = message == null ? "" : message.trim();
        for (FinWorkParameterService.Group group : pending.groups()) {
            String value = selected.get(group.key());
            if (value == null || value.isBlank()) continue;
            String label = group.options().stream().filter(item -> item.id().equals(value))
                    .map(FinWorkParameterService.Choice::label).findFirst().orElse(value);
            visible = visible.replace(group.question() + "：" + label, "");
        }
        StringBuilder result = new StringBuilder(visible.replaceAll("^[；;\\s]+|[；;\\s]+$", ""));
        for (FinWorkParameterService.Group group : pending.groups()) {
            String value = selected.get(group.key());
            if (value == null || value.isBlank()) continue;
            String label = group.options().stream().filter(item -> item.id().equals(value))
                    .map(FinWorkParameterService.Choice::label).findFirst().orElse(value);
            result.append("\n").append(group.key()).append("：").append(label);
        }
        return result.toString();
    }

    private String explicitNewQuestion(String message) {
        if (message == null) return null;
        String trimmed = message.trim();
        for (String prefix : List.of("换一个任务：", "换一个任务:", "新问题：", "新问题:",
                "重新提问：", "重新提问:", "改查：", "改查:")) {
            if (trimmed.startsWith(prefix) && trimmed.length() > prefix.length())
                return trimmed.substring(prefix.length()).trim();
        }
        return null;
    }

    private boolean conflictingSelection(Pending pending, Map<String, String> selected,
                                         FinWorkQueryPlanner.Revision revised, String message) {
        if (selected == null || selected.isEmpty()) return false;
        for (FinWorkParameterService.Group group : pending.groups()) {
            String value = selected.get(group.key());
            if (value == null || group.options().isEmpty()) continue;
            if (group.key().endsWith("voucherStatus")) {
                String stated = parameters.detectVoucherStatus(message);
                if (stated != null && !stated.equals(value)) return true;
            } else {
                String selectedName = group.options().stream().filter(item -> item.id().equals(value))
                        .map(item -> item.label().split("（", 2)[0]).findFirst().orElse("");
                for (FinWorkParameterService.Choice choice : group.options()) {
                    if (choice.id().equals(value)) continue;
                    String name = choice.label().split("（", 2)[0];
                    if (name.length() >= 2 && message.contains(name)
                            && (!message.contains(selectedName) || !selectedName.contains(name))) return true;
                }
            }
        }
        for (String path : revised.changedFields()) {
            String[] parts = path.split(":", 3);
            if (parts.length != 3) continue;
            String optionField = optionField(parts[2]);
            String selectedValue = selected.getOrDefault(pathKey(parts, optionField),
                    selected.get(optionField));
            if (selectedValue == null) continue;
            FinWorkParameterService.Group group = pending.groups().stream().filter(item ->
                    item.key().equals(pathKey(parts, optionField)) || item.key().equals(optionField))
                    .findFirst().orElse(null);
            if (group == null || group.options().isEmpty()) continue;
            FinWorkParameterService.Choice choice = group.options().stream()
                    .filter(item -> item.id().equals(selectedValue)).findFirst().orElse(null);
            if (choice == null) continue;
            FinWorkQueryPlanner.PlannedStep step = revised.steps().stream().filter(item ->
                    String.valueOf(item.workflow().getWorkflowBindingId()).equals(parts[0]))
                    .findFirst().orElse(null);
            if (step == null) continue;
            Object rawNode = step.parameters().get(parts[1]);
            if (!(rawNode instanceof Map<?, ?> node)) continue;
            String proposed = String.valueOf(node.get(parts[2]));
            if (parts[2].endsWith("Name")) {
                if (!choice.label().contains(proposed)) return true;
            } else if (!choice.id().equals(proposed)) return true;
        }
        return false;
    }

    private void clearOverriddenAnswers(Map<String, String> confirmed, Map<String, String> selected,
                                        Set<String> changed) {
        for (String path : changed) {
            String[] parts = path.split(":", 3);
            if (parts.length != 3) continue;
            String field = optionField(parts[2]);
            String scoped = pathKey(parts, field);
            if (selected != null && (selected.containsKey(scoped) || selected.containsKey(field))) continue;
            confirmed.remove(scoped);
            confirmed.remove(field);
            if ("companyId".equals(field)) {
                confirmed.remove(pathKey(parts, "supplierCode"));
                confirmed.remove("supplierCode");
            }
        }
    }

    private String optionField(String field) {
        return switch (field) {
            case "companyName" -> "companyId";
            case "supplierName" -> "supplierCode";
            case "yearFrom", "periodFrom", "yearTo", "periodTo" -> "periodRange";
            default -> field;
        };
    }

    private String pathKey(String[] parts, String field) {
        return parts[0] + ":" + parts[1] + ":" + field;
    }

    private Map<Long, Long> versionIds(List<SelectedWorkflow> workflows) {
        Map<Long, Long> result = new java.util.LinkedHashMap<>();
        workflows.forEach(item -> result.put(item.getWorkflowBindingId(), item.getAgentVersionId()));
        return Map.copyOf(result);
    }

    private List<StepState> states(List<FinWorkQueryPlanner.PlannedStep> steps) {
        return steps.stream().map(step -> new StepState(step.workflow().getWorkflowBindingId(),
                step.queryText(), step.parameters(), step.evidence())).toList();
    }

    private List<FinWorkQueryPlanner.PlannedStep> restoreSteps(Long userId, Pending pending) {
        Map<Long, SelectedWorkflow> active = new java.util.LinkedHashMap<>();
        selector.authorizedCandidates(userId, null, null).forEach(item ->
                active.put(item.getWorkflowBindingId(), item));
        return pending.steps().stream().map(state -> {
            SelectedWorkflow workflow = active.get(state.bindingId());
            if (workflow == null || !workflow.getAgentVersionId().equals(pending.versionIds().get(state.bindingId())))
                throw new SecurityException("工作流版本或授权已变化，请重新提问");
            return new FinWorkQueryPlanner.PlannedStep(workflow, state.queryText(),
                    state.parameters(), state.evidence());
        }).toList();
    }

    private boolean pendingStillAuthorized(Long userId, Pending pending) {
        if (pending.versionIds().isEmpty()) return true;
        List<SelectedWorkflow> active = selector.authorizedCandidates(userId, null, null).stream()
                .filter(item -> pending.versionIds().containsKey(item.getWorkflowBindingId())).toList();
        Map<Long, Long> versions = versionIds(active);
        if (!pending.versionIds().equals(versions)) return false;
        return pending.signatures().isEmpty() || pending.signatures().equals(
                parameters.authorizationSignatures(userId, active));
    }

    private boolean asksForTotal(String question, String goal) {
        String combined = (question == null ? "" : question) + (goal == null ? "" : goal);
        return combined.contains("总金额") || combined.contains("金额合计")
                || combined.contains("合计金额") || combined.contains("总额");
    }

    private Set<Long> selectedBindings(List<String> selections, Pending pending) {
        if (selections == null || selections.isEmpty()) return Set.of();
        if (pending == null) throw new IllegalArgumentException("澄清选项已失效，请重新提问");
        if (selections.size() == 1 && "all".equals(selections.get(0))) {
            return Set.copyOf(pending.bindingIds());
        }
        Set<Long> result = new LinkedHashSet<>();
        for (String item : selections) {
            if (item == null || !item.startsWith("binding:")) {
                throw new IllegalArgumentException("澄清选项无效");
            }
            long bindingId;
            try { bindingId = Long.parseLong(item.substring(8)); }
            catch (NumberFormatException exception) { throw new IllegalArgumentException("澄清选项无效"); }
            if (!pending.bindingIds().contains(bindingId)) throw new IllegalArgumentException("澄清选项已失效");
            result.add(bindingId);
        }
        if (result.size() > 4) throw new IllegalArgumentException("最多选择四个工作流");
        return Set.copyOf(result);
    }

    private Pending loadPending(String sessionId, Long userId) {
        List<String> rows = jdbc.queryForList("""
                SELECT pending_json FROM ea_agent_chat_pending_plan
                WHERE session_id=? AND user_id=? AND expires_at>NOW()
                """, String.class, sessionId, userId);
        if (rows.isEmpty()) return null;
        try {
            Map<String, Object> source = json.readValue(rows.get(0), new TypeReference<>() { });
            String original = String.valueOf(source.get("originalQuestion"));
            List<Long> ids = ((List<?>) source.get("bindingIds")).stream()
                    .map(item -> ((Number) item).longValue()).toList();
            Map<String, String> answers = new java.util.LinkedHashMap<>();
            if (source.get("answers") instanceof Map<?, ?> map)
                map.forEach((key, value) -> answers.put(String.valueOf(key), String.valueOf(value)));
            Map<Long, Long> versions = new java.util.LinkedHashMap<>();
            if (source.get("versionIds") instanceof Map<?, ?> map)
                map.forEach((key, value) -> versions.put(Long.parseLong(String.valueOf(key)),
                        ((Number) value).longValue()));
            Map<Long, String> signatures = new java.util.LinkedHashMap<>();
            if (source.get("signatures") instanceof Map<?, ?> map)
                map.forEach((key, value) -> signatures.put(Long.parseLong(String.valueOf(key)),
                        String.valueOf(value)));
            List<FinWorkParameterService.Group> groups = source.get("groups") == null ? List.of()
                    : json.convertValue(source.get("groups"), new TypeReference<>() { });
            List<StepState> steps = source.get("steps") == null ? List.of()
                    : json.convertValue(source.get("steps"), new TypeReference<>() { });
            return new Pending(original, ids, Map.copyOf(answers), Map.copyOf(versions),
                    Map.copyOf(signatures), groups, steps,
                    String.valueOf(source.getOrDefault("analysisGoal", "")));
        } catch (Exception exception) {
            clearPending(sessionId, userId);
            throw new IllegalArgumentException("澄清状态已失效，请重新提问", exception);
        }
    }

    private void savePending(String sessionId, Long userId, Pending pending) {
        try {
            String payload = json.writeValueAsString(Map.of(
                    "originalQuestion", pending.originalQuestion(), "bindingIds", pending.bindingIds(),
                    "answers", pending.answers(), "versionIds", pending.versionIds(),
                    "signatures", pending.signatures(), "groups", pending.groups(),
                    "steps", pending.steps(), "analysisGoal", pending.analysisGoal()));
            if (payload.length() > 12000) throw new IllegalArgumentException("澄清状态过大，请缩小查询范围");
            jdbc.update("""
                    INSERT INTO ea_agent_chat_pending_plan (session_id,user_id,pending_json,expires_at)
                    VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE user_id=VALUES(user_id),
                    pending_json=VALUES(pending_json),expires_at=VALUES(expires_at)
                    """, sessionId, userId, payload, LocalDateTime.now().plusMinutes(15));
        } catch (Exception exception) {
            throw new IllegalStateException("无法保存澄清状态", exception);
        }
    }

    private void clearPending(String sessionId, Long userId) {
        jdbc.update("DELETE FROM ea_agent_chat_pending_plan WHERE session_id=? AND user_id=?", sessionId, userId);
    }

    private record Pending(String originalQuestion, List<Long> bindingIds, Map<String, String> answers,
                           Map<Long, Long> versionIds, Map<Long, String> signatures,
                           List<FinWorkParameterService.Group> groups, List<StepState> steps,
                           String analysisGoal) { }
    private record StepState(Long bindingId, String queryText, Map<String, Object> parameters,
                             Map<String, Map<String, String>> evidence) { }
    public record Option(String id, String label) { }
    public record Execution(WorkflowExecutionResult result, String queryText, Long bindingId) { }
    public record Outcome(String kind, String content, List<Option> options,
                          List<FinWorkParameterService.Group> clarifications,
                          List<Execution> executions) { }
}
