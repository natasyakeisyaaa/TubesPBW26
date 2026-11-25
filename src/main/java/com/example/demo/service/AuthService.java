package com.example.demo.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class AuthService {
    private final PasswordEncoder bcrypt;
    private final UserRepository db;

    public User register(String username, String fullname, String password, String confirmPassword) {
        // Validasi & Normalisasi String
        username = username.trim();
        fullname = fullname.trim();
        boolean passwordCheck = password.equals(confirmPassword);

        if (!passwordCheck) throw new IllegalArgumentException("Konfirmasi password tidak cocok");

        // Cek apakah user exist
        if (db.findByUsername(username).isPresent()) throw new IllegalArgumentException("Username sudah digunakan");

        // Build objek user baru, hash password, dan masukkan user baru ke dalam db
        User newUser = User.builder()
            .username(username)
            .fullName(fullname)
            .role(Role.USER)
            .build();
        
        String passwordHash = bcrypt.encode(password);
        newUser.setPasswordHash(passwordHash);

        try {
            Long userId = db.save(newUser);
            newUser.setId(userId);
        } catch (DuplicateKeyException e) { // Jika registrasi atas username yang sama dilakukan oleh lebih dari 1 device bersamaan
            throw new IllegalArgumentException("Username sudah digunakan");
        }

        return newUser;
    }

    public User login(String username, String password) {
        // Validasi & Normalisasi String
        username = username.trim();

        // Cari user dari db, throw error jika tidak ditemukan
        User user = db.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("Username atau password salah"));

        // Cocokan password
        boolean passwordCheck = bcrypt.matches(password, user.getPasswordHash());
        if (!passwordCheck) throw new IllegalArgumentException("Username atau password salah");

        // Return user jika benar
        return user;
    }
}
