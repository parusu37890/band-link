package com.example.bandlink.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "stances", uniqueConstraints = @UniqueConstraint(name = "uk_stances_name", columnNames = "name"))
public class Stance {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 80)
    private String name;
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected Stance() {}
    public Stance(String name, int displayOrder) { this.name = name; this.displayOrder = displayOrder; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
