package com.orm.example;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "users", 
    indexes = {
        @Index(name = "idx_email", columnList = "email", unique = true),
        @Index(name = "idx_name", columnList = "first_name,last_name")
    }
)
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", length = 50, nullable = false)
    private String firstName;

    @Column(name = "last_name", length = 50, nullable = false)
    private String lastName;

    @Column(length = 100, unique = true, nullable = false)
    private String email;

    @Column(length = 60)  // For storing bcrypt hashed passwords
    private String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "active", columnDefinition = "BOOLEAN DEFAULT true")
    private boolean active = true;

    @Version
    private Long version;
} 