package com.example.bandlink.repository;

import com.example.bandlink.entity.Part;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PartRepository extends JpaRepository<Part, Long> { }
