package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Carpeta;

@Repository
public interface CarpetaRepository extends JpaRepository<Carpeta, Long> {
}
