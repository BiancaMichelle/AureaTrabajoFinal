package com.example.demo.service;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

@Service
public class JaaSTokenService {

    @Value("${jaas.appId}")
    private String appId;

    @Value("${jaas.apiKey}")
    private String apiKey;

    @Value("${jaas.privateKeyPath}")
    private String privateKeyPath;

    private final ResourceLoader resourceLoader;

    public JaaSTokenService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String generateToken(String userName, String userEmail, String userAvatar, boolean isModerator) {
        try {
            RSAPrivateKey privateKey = loadPrivateKey();

            Map<String, Object> context = new HashMap<>();
            Map<String, Object> user = new HashMap<>();
            user.put("id", userEmail);
            user.put("name", userName);
            user.put("email", userEmail);
            user.put("avatar", userAvatar);
            user.put("moderator", isModerator); // Pass as boolean, not string

            Map<String, Object> features = new HashMap<>();
            features.put("livestreaming", true);
            features.put("recording", true);
            features.put("transcription", true);
            features.put("outbound-call", true);

            context.put("user", user);
            context.put("features", features);

            Algorithm algorithm = Algorithm.RSA256(null, privateKey);

            // Calculate expiration (e.g., 2 hours)
            Instant now = Instant.now();
            Instant nbf = now.minusSeconds(30); // Allow 30s clock skew
            Instant exp = now.plusSeconds(7200);

            return JWT.create()
                    .withIssuer("chat")
                    .withSubject(appId)
                    .withAudience("jitsi")
                    .withKeyId(apiKey) // The API Key ID (kid)
                    .withClaim("context", context)
                    .withClaim("room", "*") // Allow access to any room
                    .withClaim("sub", appId)
                    .withClaim("iss", "chat")
                    .withExpiresAt(Date.from(exp))
                    .withNotBefore(Date.from(nbf))
                    .sign(algorithm);

        } catch (Exception e) {
            throw new RuntimeException("Error generating JaaS token", e);
        }
    }

    private RSAPrivateKey loadPrivateKey() throws Exception {
        Resource resource = resourceLoader.getResource(privateKeyPath);
        String keyContent = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

        // Remove headers and newlines
        String privateKeyPEM = keyContent
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }
}
