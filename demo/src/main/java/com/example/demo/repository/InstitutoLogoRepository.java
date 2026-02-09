package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Instituto;
import com.example.demo.model.InstitutoLogo;

public interface InstitutoLogoRepository extends JpaRepository<InstitutoLogo, Long> {
    Optional<InstitutoLogo> findByInstituto(Instituto instituto);
}
