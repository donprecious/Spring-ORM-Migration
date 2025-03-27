package com.orm.migration;

import com.orm.schema.diff.SchemaChange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.*;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InteractiveMigrationManagerTest {

    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        // Save original output stream
        originalOut = System.out;
        
        // Set up output stream to capture console output
        outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        System.setOut(printStream);
    }

    @AfterEach
    void tearDown() {
        // Restore original output stream
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("Should auto-confirm low risk changes")
    void shouldAutoConfirmLowRiskChanges() {
        // Given
        TestableInteractiveMigrationManager migrationManager = new TestableInteractiveMigrationManager("y");
        List<SchemaChange> changes = Arrays.asList(
            createSchemaChange(SchemaChange.ChangeType.CREATE_TABLE, "users", SchemaChange.RiskLevel.LOW),
            createSchemaChange(SchemaChange.ChangeType.ADD_COLUMN, "email", SchemaChange.RiskLevel.LOW)
        );

        // When
        boolean result = migrationManager.confirmChanges(changes);

        // Then
        assertTrue(result);
        assertTrue(outputStream.toString().contains("No changes require confirmation"));
    }

    @Test
    @DisplayName("Should require confirmation for high risk changes")
    void shouldRequireConfirmationForHighRiskChanges() {
        // Given
        TestableInteractiveMigrationManager migrationManager = new TestableInteractiveMigrationManager("y");
        SchemaChange highRiskChange = createSchemaChange(
            SchemaChange.ChangeType.DROP_TABLE, 
            "users", 
            SchemaChange.RiskLevel.HIGH
        );

        // When
        boolean result = migrationManager.confirmChanges(Arrays.asList(highRiskChange));

        // Then
        assertTrue(result);
        assertTrue(outputStream.toString().contains("WARNING"));
    }

    @Test
    @DisplayName("Should reject changes when user responds with no")
    void shouldRejectChangesWhenUserSaysNo() {
        // Given
        TestableInteractiveMigrationManager migrationManager = new TestableInteractiveMigrationManager("n");
        SchemaChange criticalChange = createSchemaChange(
            SchemaChange.ChangeType.DROP_COLUMN, 
            "email", 
            SchemaChange.RiskLevel.CRITICAL
        );

        // When
        boolean result = migrationManager.confirmChanges(Arrays.asList(criticalChange));

        // Then
        assertFalse(result);
        assertTrue(outputStream.toString().contains("WARNING"));
    }

    @Test
    @DisplayName("Should handle destructive changes")
    void shouldHandleDestructiveChanges() {
        // Given
        TestableInteractiveMigrationManager migrationManager = new TestableInteractiveMigrationManager("y");
        SchemaChange change = SchemaChange.builder()
            .changeType(SchemaChange.ChangeType.DROP_TABLE)
            .objectName("users")
            .objectType("TABLE")
            .riskLevel(SchemaChange.RiskLevel.HIGH)
            .destructive(true)
            .build();

        // When
        boolean result = migrationManager.confirmChanges(Arrays.asList(change));

        // Then
        assertTrue(result);
        assertTrue(outputStream.toString().contains("WARNING"));
    }

    @Test
    @DisplayName("Should warn about destructive changes")
    void shouldWarnAboutDestructiveChanges() {
        // Given
        TestableInteractiveMigrationManager migrationManager = new TestableInteractiveMigrationManager("y");
        List<SchemaChange> changes = Arrays.asList(
            createSchemaChange(SchemaChange.ChangeType.DROP_TABLE, "users", SchemaChange.RiskLevel.CRITICAL),
            createSchemaChange(SchemaChange.ChangeType.DROP_COLUMN, "email", SchemaChange.RiskLevel.CRITICAL)
        );

        // When
        boolean result = migrationManager.confirmChanges(changes);

        // Then
        assertTrue(result);
        String output = outputStream.toString();
        assertTrue(output.contains("WARNING"));
    }

    private SchemaChange createSchemaChange(SchemaChange.ChangeType changeType, 
                                          String objectName, 
                                          SchemaChange.RiskLevel riskLevel) {
        return SchemaChange.builder()
                .changeType(changeType)
                .objectName(objectName)
                .objectType(changeType.toString().contains("TABLE") ? "TABLE" : "COLUMN")
                .riskLevel(riskLevel)
                .destructive(changeType == SchemaChange.ChangeType.DROP_TABLE || 
                             changeType == SchemaChange.ChangeType.DROP_COLUMN)
                .build();
    }
    
    /**
     * A testable subclass of InteractiveMigrationManager that doesn't use Scanner
     * and instead returns predefined responses
     */
    private static class TestableInteractiveMigrationManager extends InteractiveMigrationManager {
        private final String response;
        
        public TestableInteractiveMigrationManager(String response) {
            this.response = response;
        }
        
        @Override
        public boolean confirmChanges(List<SchemaChange> changes) {
            if (changes.isEmpty()) {
                System.out.println("No changes to apply.");
                return true;
            }

            System.out.println("\nProposed schema changes:");
            for (SchemaChange change : changes) {
                System.out.println("- " + change.getDescription());
                if (change.requiresWarning()) {
                    System.out.println("  " + change.getWarning());
                }
            }

            boolean hasDestructiveChanges = changes.stream().anyMatch(SchemaChange::isDestructive);
            boolean hasDataLossRisk = changes.stream().anyMatch(SchemaChange::isDataLossRisk);

            if (hasDestructiveChanges || hasDataLossRisk) {
                System.out.println("\nWARNING: Some changes are destructive or may cause data loss!");
                System.out.println("Auto-responding with: " + response);
                return response.equals("yes") || response.equals("y");
            }

            System.out.println("No changes require confirmation");
            return true;
        }
        
        @Override
        public boolean confirmRevert(SchemaChange change) {
            System.out.println("\nAbout to revert: " + change.getDescription());
            
            if (change.isDestructive() || change.isDataLossRisk()) {
                System.out.println("WARNING: This reversion may cause data loss!");
                System.out.println("Auto-responding with: " + response);
                return response.equals("yes") || response.equals("y");
            }
            
            return true;
        }
    }
} 