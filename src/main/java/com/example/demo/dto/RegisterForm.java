package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data 
public class RegisterForm {
    @NotBlank(message = "Username tidak boleh kosong")
    @Size(min = 5, max = 50, message = "Username harus berukuran 5-50 karakter")
    private String username;
    
    @NotBlank(message = "Full name tidak boleh kosong") 
    @Size (min = 5, max = 100, message = "Full name harus berukuran 5-100 karakter")
    private String fullName;
    
    @NotBlank(message = "Password tidak boleh kosong")
    @Size(min = 8, max = 72, message = "Password harus berukuran 8-72 karakter")
    private String password;
    
    @NotBlank(message = "Konfirmasi password tidak boleh kosong")
    private String confirmPassword; 
}
