package com.example.bandlink.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "prefectures", uniqueConstraints = @UniqueConstraint(name = "uk_prefectures_name", columnNames = "name"))
public class Prefecture {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 40)
    private String name;
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected Prefecture() {}
    public Prefecture(String name, int displayOrder) { this.name = name; this.displayOrder = displayOrder; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
