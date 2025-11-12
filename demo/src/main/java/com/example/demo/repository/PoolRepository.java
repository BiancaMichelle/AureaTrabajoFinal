package com.example.demo.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Pool;

@Repository
public interface PoolRepository extends JpaRepository<Pool, UUID> {
}
