package com.aura.auraid.model;

import jakarta.persistence.*;
import lombok.*;
import com.aura.auraid.enums.ERole;

@Entity
@Table(name = "roles",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "name")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ERole name;
} 