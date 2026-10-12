package com.finex.auth.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class DependencyBoundaryTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests()).importPackages("com.finex.auth");

    @Test
    void controllersDoNotAccessMappers() {
        noClasses().that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().resideInAPackage("..mapper..")
                .check(CLASSES);
    }

    @Test
    void controllersDoNotAddImplementationDependencies() {
        // Bytecode dependency baseline: these two PDF-print imports are the only legacy exemptions.
        Set<String> allowed = Set.of(
                "com.finex.auth.controller.ExpenseDocumentQueryController -> com.finex.auth.service.impl.expense.ExpenseDocumentPrintService",
                "com.finex.auth.controller.ExpenseDocumentQueryController -> com.finex.auth.service.impl.expense.ExpenseDocumentPrintOrientation",
                "com.finex.auth.controller.ExpenseDocumentQueryController -> com.finex.auth.service.impl.expense.ExpenseDocumentPrintService$ExpensePrintPdfResult");
        Set<String> actual = CLASSES.stream()
                .filter(source -> source.getPackageName().contains(".controller"))
                .flatMap(source -> source.getDirectDependenciesFromSelf().stream())
                .filter(dependency -> dependency.getTargetClass().getPackageName().contains(".service.impl."))
                .map(dependency -> dependency.getOriginClass().getName() + " -> " + dependency.getTargetClass().getName())
                .collect(Collectors.toSet());
        // Equality also forces a stale exception to be removed after migration.
        assertThat(actual).containsExactlyInAnyOrderElementsOf(allowed);
    }

    @Test
    void agentToolsDoNotAccessBusinessMappers() {
        noClasses().that().resideInAnyPackage("..agent.tool..", "..agent.workflow..")
                .and().haveSimpleNameEndingWith("Tool")
                .should().dependOnClassesThat().resideInAPackage("..mapper..")
                .check(CLASSES);
    }

    @Test
    void businessPortsDoNotDependOnAdapters() {
        noClasses().that().resideInAPackage("..service.port..")
                .should().dependOnClassesThat().resideInAnyPackage("..controller..", "..mapper..", "..service.impl..")
                .check(CLASSES);
    }
}
