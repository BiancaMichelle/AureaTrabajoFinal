package com.example.demo.ia.repository;

import com.example.demo.ia.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);
    
    List<ChatMessage> findByUserDniOrderByCreatedAtDesc(String userDni);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.userDni = :userDni AND cm.createdAt >= :since ORDER BY cm.createdAt DESC")
    List<ChatMessage> findRecentMessagesByUser(@Param("userDni") String userDni, @Param("since") LocalDateTime since);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.sessionId = :sessionId AND cm.createdAt >= :since ORDER BY cm.createdAt ASC")
    List<ChatMessage> findSessionMessagesSince(@Param("sessionId") String sessionId, @Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.userDni = :userDni AND cm.createdAt >= :since")
    long countMessagesByUserSince(@Param("userDni") String userDni, @Param("since") LocalDateTime since);
    
    void deleteBySessionId(String sessionId);
}
