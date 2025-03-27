package com.demo.todo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "todos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "task", nullable = false)
    private String task;
    
    // Explicit getter and setter for id
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
} 