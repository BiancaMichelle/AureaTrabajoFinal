package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.ExamenFeedbackPendiente;

public interface ExamenFeedbackPendienteRepository extends JpaRepository<ExamenFeedbackPendiente, Long> {
    List<ExamenFeedbackPendiente> findByEnviadoFalseAndFechaProgramadaLessThanEqual(LocalDateTime fecha);
}
