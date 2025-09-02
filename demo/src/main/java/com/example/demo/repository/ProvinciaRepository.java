package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Provincia;

@Repository
public interface ProvinciaRepository extends JpaRepository<Provincia,Long> {
    List<Provincia> findByPaisCodigo(String paisCodigo);
}
