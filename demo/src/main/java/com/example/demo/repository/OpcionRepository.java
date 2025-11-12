package com.example.demo.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Opcion;

@Repository
public interface OpcionRepository extends JpaRepository<Opcion, UUID> {
}
