package com.example.demo.dto;

import java.io.Serializable;
import com.example.demo.model.Role;

public record SessionUser(Long id, String username, String fullName, Role role) implements Serializable {}
