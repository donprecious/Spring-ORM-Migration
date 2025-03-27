package com.orm.model;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class MigrationScript {
    private String version;
    private String description;
    private String upSql;
    private String downSql;
    private LocalDateTime createdAt;
    private LocalDateTime appliedAt;
    private LocalDateTime revertedAt;
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    public boolean isValid() {
        return upSql != null && !upSql.trim().isEmpty() &&
               downSql != null && !downSql.trim().isEmpty();
    }

    public void addWarning(String warning) {
        if (warnings == null) {
            warnings = new ArrayList<>();
        }
        warnings.add(warning);
    }

    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }

    public List<String> getWarnings() {
        return warnings != null ? warnings : new ArrayList<>();
    }
} 