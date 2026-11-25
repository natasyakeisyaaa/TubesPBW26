package com.example.demo.controller;

import com.example.demo.service.AuthService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.demo.dto.LoginForm;
import com.example.demo.dto.RegisterForm;
import com.example.demo.dto.SessionUser;
import com.example.demo.model.Role;
import com.example.demo.model.User;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;





@Controller 
public class AuthController {
    private final AuthService authService;

    AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/signup")
    public String signup(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        return "test/signup";
    }
    
    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute("registerForm") RegisterForm form, BindingResult res, HttpSession session, Model model) {
        if (res.hasErrors()) return "test/signup";

        try {
            User newUser = authService.register(form.getUsername(), form.getFullName(), form.getPassword(), form.getConfirmPassword());
            saveSession(session, newUser);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "test/signup";
        }

        return "redirect:/user/dashboard";
    }
    
    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("loginForm", new LoginForm());
        return "test/login";
    }
    
    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("loginForm") LoginForm form, BindingResult res, HttpSession session, Model model) {
        if (res.hasErrors()) return "test/login";

        try {
            User user = authService.login(form.getUsername(), form.getPassword());
            saveSession(session, user);

            if (user.getRole() == Role.ADMIN) {
                return "redirect:/admin/dashboard";
            } else {
                return "redirect:/user/dashboard";
            }
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "test/login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();

        return "redirect:/";
    }
    
    
    private void saveSession(HttpSession session, User user) {
        SessionUser sessionUser = new SessionUser(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole());
        session.setAttribute("user", sessionUser);
    }
}
