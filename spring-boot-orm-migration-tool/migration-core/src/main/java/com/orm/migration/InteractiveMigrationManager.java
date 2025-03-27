package com.orm.migration;

import com.orm.schema.diff.SchemaChange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Scanner;

@Component
@RequiredArgsConstructor
public class InteractiveMigrationManager {
    private final Scanner scanner = new Scanner(System.in);

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
            System.out.print("Are you sure you want to proceed? (yes/no): ");
            String response = scanner.nextLine().trim().toLowerCase();
            return response.equals("yes") || response.equals("y");
        }

        return true;
    }

    public boolean confirmRevert(SchemaChange change) {
        System.out.println("\nAbout to revert: " + change.getDescription());
        
        if (change.isDestructive() || change.isDataLossRisk()) {
            System.out.println("WARNING: This reversion may cause data loss!");
            System.out.print("Are you sure you want to proceed? (yes/no): ");
            String response = scanner.nextLine().trim().toLowerCase();
            return response.equals("yes") || response.equals("y");
        }

        return true;
    }
} 