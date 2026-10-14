package com.finex.auth.agent.workflow;

import com.finex.auth.service.port.FinanceAgentQueryPort;
import com.finex.auth.service.queryauth.QueryAuthorization;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resolves a user-facing supplier name within one authorized account set. */
final class SupplierLedgerSelection {
    private static final Pattern SUPPLIER_PHRASE = Pattern.compile("供应商[：: ]?(.+?)(?:在|的|所有|往来|明细|[，,。；;\\s]|$)");

    private SupplierLedgerSelection() { }

    record Match(String code, List<FinanceAgentQueryPort.Supplier> options) {
        boolean resolved() { return code != null; }
    }

    static Match resolve(FinanceAgentQueryPort finance, QueryAuthorization grant, String companyId,
                         String question, String proposedName, String selectedCode) {
        String words = question == null ? "" : question;
        int supplement = words.lastIndexOf("\n补充说明：");
        if (supplement >= 0 && proposedName != null && !proposedName.isBlank()
                && words.substring(supplement).contains(proposedName.trim())) {
            String latestName = proposedName.trim();
            return choose(finance, grant, companyId, latestName,
                    finance.suppliers(grant, companyId, latestName), selectedCode);
        }
        String name = "";
        Matcher phrase = SUPPLIER_PHRASE.matcher(words);
        String matchedPhrase = null;
        while (phrase.find()) {
            name = phrase.group(1).trim();
            matchedPhrase = phrase.group(0);
        }
        if (matchedPhrase != null) {
            // “供应商6” may be the actual archive name, rather than “supplier: 6”.
            if (matchedPhrase.startsWith("供应商" + name)) {
                String fullName = "供应商" + name;
                List<FinanceAgentQueryPort.Supplier> exactFullName = finance.suppliers(grant, companyId, fullName)
                        .stream().filter(item -> fullName.equals(item.name())).toList();
                if (!exactFullName.isEmpty())
                    return choose(finance, grant, companyId, fullName, exactFullName, selectedCode);
            }
        } else if (proposedName != null && !proposedName.isBlank() && words.contains(proposedName.trim()))
            name = proposedName.trim();

        List<FinanceAgentQueryPort.Supplier> matches = finance.suppliers(grant, companyId, name);
        if (matches.isEmpty() && proposedName != null && !proposedName.isBlank()
                && !proposedName.trim().equals(name) && words.contains(proposedName.trim())) {
            name = proposedName.trim();
            matches = finance.suppliers(grant, companyId, name);
        }
        return choose(finance, grant, companyId, name, matches, selectedCode);
    }

    private static Match choose(FinanceAgentQueryPort finance, QueryAuthorization grant, String companyId,
                                String name, List<FinanceAgentQueryPort.Supplier> matches, String selectedCode) {
        if (selectedCode != null && !selectedCode.isBlank()) {
            String code = selectedCode.trim();
            if (!name.isBlank() && matches.stream().noneMatch(item -> code.equals(item.code())))
                throw new IllegalArgumentException("所选供应商与查询名称不一致");
            List<FinanceAgentQueryPort.Supplier> selected = finance.suppliers(grant, companyId, code);
            if (selected.stream().noneMatch(item -> code.equals(item.code())))
                throw new IllegalArgumentException("所选供应商不在当前账套");
            return new Match(code, List.of());
        }
        return matches.size() == 1
                ? new Match(matches.get(0).code(), List.of())
                : new Match(null, List.copyOf(matches));
    }
}
