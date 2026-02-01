package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.model.Recurso;

@Repository
public interface RecursoRepository extends JpaRepository<Recurso, Long> {
}
