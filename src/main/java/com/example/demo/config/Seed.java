package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component 
@RequiredArgsConstructor 
public class Seed implements CommandLineRunner{
    private final JdbcClient db;
    private final PasswordEncoder bcrypt;

    // Ambil username & password dari env
    @Value("${ROOT_USERNAME:}")
    private String username;
    @Value("${ROOT_PASSWORD:}")
    private String password;

    @Override
    public void run(String... args) throws Exception {
        // Cek apakah sudah ada di database
        boolean exist = sudahAda();

        if (exist) return; // Tidak perlu validasi env, atau mencoba untuk insert

        // Validasi isi env
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Username & Password dalam .env perlu diisi");
        }

        // Hash password
        String hashedPassword = bcrypt.encode(password);

        // Insert ke database
        int rows = db.sql("INSERT INTO Users (username, password_hash, full_name, role) VALUES (:username, :password_hash, :full_name, 'ADMIN')")
            .param("username", username)
            .param("password_hash", hashedPassword)
            .param("full_name", username)
            .update();
        
        // Cek apakah insert berhasil
        if (rows != 1) {
            throw new IllegalStateException("Gagal melakukan seeding ke database");
        }
    }

    /**
     * Method untuk mengecek apakah sudah pernah dilakukan seeding sebelumnya.
     * Asumsi sekarang adalah hanya ada 1 admin dalam server
     * @return {@code true} jika sudah ada admin dalam database, {@code false} jika belum.
     */
    private boolean sudahAda() {
        return db.sql("SELECT EXISTS(SELECT * FROM Users WHERE role = 'ADMIN')")
            .query(Boolean.class)
            .single();
    }
}
