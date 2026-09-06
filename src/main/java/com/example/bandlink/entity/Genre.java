package com.example.bandlink.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "genres", uniqueConstraints = @UniqueConstraint(name = "uk_genres_name", columnNames = "name"))
public class Genre {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 80)
    private String name;
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected Genre() {}
    public Genre(String name, int displayOrder) { this.name = name; this.displayOrder = displayOrder; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
