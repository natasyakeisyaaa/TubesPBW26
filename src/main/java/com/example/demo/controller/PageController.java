package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;



@Controller 
public class PageController {
    @GetMapping("/")
    public String home() {
        return "test/home";
    }

    @GetMapping("/user/dashboard")
    public String userDash() {
        return "test/user-dashboard";
    }
    
    @GetMapping("/admin/dashboard")
    public String adminDash() {
        return "test/admin-dashboard";
    }
}
