package com.evolv.app.authenticationandsignup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/auth")
public class AuthController {

 
    @Autowired
    private UserRepository userRepository;

 @PostMapping("/login")
public ResponseEntity<?> login(@RequestBody User loginData) {
    try {
        User found = userRepository.findByUsernameAndPassword(
            loginData.getUsername(),
            loginData.getPassword()
        );

        if (found != null) {
            return ResponseEntity.ok().body(
                java.util.Map.of(
                    "status", "verified",
                    "profession", found.getProfession()
                )
            );
        } else {
            return ResponseEntity.status(401).body(
                java.util.Map.of(
                    "status", "invalid"
                )
            );
        }
    } catch (Exception e) {
        // Log the real error to Railway logs / console
        e.printStackTrace();

        return ResponseEntity.status(500).body(
            java.util.Map.of(
                "status", "error",
                "message", "Login failed: " + e.getMessage()
            )
        );
    }
}


    @PostMapping("/signup")
public String signup(@RequestBody User newUser) {
    // Check if username already exists
    if (userRepository.findByUsername(newUser.getUsername()) != null) {
        return "username already exists";
    }

    userRepository.save(newUser);
    return "signup successful";
}

}
