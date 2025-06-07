package com.evolv.app.authenticationandsignup;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final UserRepository userRepository;
    private final VideoInterestRepository videoInterestRepository;
    private final ToDoRepository todoRepository;
    private final CountRepository countRepository;
    private final ChallengeRepository challengeRepository;  // Added ChallengeRepository

    public SettingsController(
            UserRepository userRepository,
            VideoInterestRepository videoInterestRepository,
            ToDoRepository todoRepository,
            CountRepository countRepository,
            ChallengeRepository challengeRepository  // Inject ChallengeRepository
    ) {
        this.userRepository = userRepository;
        this.videoInterestRepository = videoInterestRepository;
        this.todoRepository = todoRepository;
        this.countRepository = countRepository;
        this.challengeRepository = challengeRepository;  // Assign to field
    }

    // Get all interests for a user
    @GetMapping("/interests/{username}")
    public ResponseEntity<List<VideoInterest>> getInterests(@PathVariable String username) {
        return ResponseEntity.ok(videoInterestRepository.findByUsername(username));
    }

    // Add new interest
    @PostMapping("/interests")
    public ResponseEntity<VideoInterest> addInterest(@RequestBody VideoInterest newInterest) {
        VideoInterest saved = videoInterestRepository.save(newInterest);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // Delete an interest
    @DeleteMapping("/interests/{id}")
    public ResponseEntity<Void> deleteInterest(@PathVariable String id) {
        videoInterestRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Update username across User, VideoInterest, Todo, Count, and Challenge collections
    @PutMapping("/username")
    public ResponseEntity<?> updateUsername(@RequestBody UsernameUpdateRequest req) {
        // Verify current credentials
        User user = userRepository.findByUsernameAndPassword(
                req.getCurrentUsername(),
                req.getCurrentPassword()
        );
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Incorrect credentials");
        }

        // New username availability
        if (userRepository.findByUsername(req.getNewUsername()) != null) {
            return ResponseEntity.badRequest().body("Username already taken");
        }

        String oldUsername = user.getUsername();
        String newUsername = req.getNewUsername();

        // 1) Update User
        user.setUsername(newUsername);
        userRepository.save(user);

        // 2) Update VideoInterest documents
        List<VideoInterest> interests = videoInterestRepository.findByUsername(oldUsername);
        interests.forEach(i -> i.setUsername(newUsername));
        videoInterestRepository.saveAll(interests);

        // 3) Update Todo documents
        List<Todo> todos = todoRepository.findByUsername(oldUsername);
        todos.forEach(t -> t.setUsername(newUsername));
        todoRepository.saveAll(todos);

        // 4) Update Count document
        countRepository.findByUsername(oldUsername).ifPresent(count -> {
            count.setUsername(newUsername);
            countRepository.save(count);
        });

        // 5) Update Challenge documents (new addition)
        List<Challenge> challenges = challengeRepository.findByUsername(oldUsername);
        challenges.forEach(c -> c.setUsername(newUsername));
        challengeRepository.saveAll(challenges);

        return ResponseEntity.ok("Username updated successfully");
    }

    // Update password for User only
    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(@RequestBody PasswordUpdateRequest req) {
        User user = userRepository.findByUsernameAndPassword(
                req.getUsername(),
                req.getCurrentPassword()
        );
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Incorrect current password");
        }
        user.setPassword(req.getNewPassword());
        userRepository.save(user);
        return ResponseEntity.ok("Password updated successfully");
    }
}
