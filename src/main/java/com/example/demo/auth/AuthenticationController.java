package com.example.demo.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user/auth")
@CrossOrigin(origins = "http://localhost:4200/")
@RequiredArgsConstructor
public class AuthenticationController {

    @Autowired
    private AuthenticationService service;

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody CustomerRegistrationRequest request
            ) {
        return service.registerCustomer(request);
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticate(
            @RequestBody AccountLoginRequest request
            ) {
        return ResponseEntity.ok(service.loginToAccount(request, "userIp", "userAgent"));
    }


    @GetMapping("/logout")
    public ResponseEntity<?> logout() {
        return service.logout();
    }
}