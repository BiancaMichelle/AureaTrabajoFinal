package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Examen;

@Repository
public interface ExamenRepository extends JpaRepository<Examen, Long> {
}
