package com.example.demo.repository;

import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.example.demo.model.User;

import lombok.RequiredArgsConstructor;

@Repository 
@RequiredArgsConstructor 
public class UserRepository {
    private final JdbcClient db;

    /**
     * Method untuk mencari user berdasarkan username
     * @param username
     * @return {@code Optional<User>} yang berisi user jika ditemukan, {@code Optional.empty()} jika tidak ditemukan.
     */
    public Optional<User> findByUsername(String username) {
        return db.sql("SELECT * FROM Users WHERE username = :username")
            .param("username", username)
            .query(User.class)
            .optional();
    }

    /**
     * Method untuk menyimpan user baru ke database
     * @param user 
     * @return {@code Long} yang berisi id user yang baru disimpan
     */
    public Long save(User user) {
        Long userId = db.sql(
                "INSERT INTO Users (username, password_hash, full_name, role) VALUES (:username, :password_hash, :full_name, CAST(:role AS user_role)) RETURNING id")
                .param("username", user.getUsername())
                .param("password_hash", user.getPasswordHash())
                .param("full_name", user.getFullName())
                .param("role", user.getRole().name())
                .query(Long.class)
                .single();

        return userId;
    }
}
