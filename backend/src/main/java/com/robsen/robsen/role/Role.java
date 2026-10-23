
package com.robsen.robsen.role;

import jakarta.persistence.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "roles")
@Data 
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private boolean active;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_navigation_items", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "navigation_item", nullable = false)
    private List<String> navigationItems = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "permission", nullable = false)
    private List<String> permissions = new ArrayList<>();
}
