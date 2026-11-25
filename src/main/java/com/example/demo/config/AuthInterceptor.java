package com.example.demo.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.demo.dto.SessionUser;
import com.example.demo.model.Role;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component 
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        HttpSession session = request.getSession(false);
        Object user = (session != null) ? session.getAttribute("user") : null;
        SessionUser sessionUser = (user instanceof SessionUser) ? (SessionUser) user : null;

        // Sesi kosong = harus login/signup
        if ((path.startsWith("/user/") || path.startsWith("/admin/")) && sessionUser == null) {
            response.sendRedirect("/login");
            return false;
        }

        // Jika sesi ada, routing semua login/signup ke dashboard masing"
        if (sessionUser != null && (path.equals("/login") || path.equals("/signup"))) {
            // Cek apakah admin atau user
            if (sessionUser.role() == Role.ADMIN) {
                response.sendRedirect("/admin/dashboard");
            } else {
                response.sendRedirect("/user/dashboard");
            }

            return false;  
        }  

        // User tidak diperbolehkan mengakses dashboard admin
        if (path.startsWith("/admin/") && sessionUser != null && sessionUser.role() != Role.ADMIN) {
            response.sendRedirect("/user/dashboard");
            return false;
        }

        return true;
    }
}
