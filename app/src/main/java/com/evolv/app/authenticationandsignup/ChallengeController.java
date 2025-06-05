package com.evolv.app.authenticationandsignup;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/challenges")
@CrossOrigin(origins = "*")
public class ChallengeController {

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * GET /api/challenges
     * Optionally accepts ?username={currentUser} so we can compute hasApplauded per challenge.
     */
    @GetMapping
    public ResponseEntity<List<Challenge>> getAllChallenges(
            @RequestParam(name = "username", required = false) String currentUser) {

        List<Challenge> all = challengeRepository.findAll();

        if (currentUser != null && !currentUser.isBlank()) {
            for (Challenge ch : all) {
                boolean hasApplauded = ch.getApplaudedBy().contains(currentUser);
                ch.setHasApplauded(hasApplauded);
            }
        } else {
            // If no username supplied, default all.hasApplauded = false
            for (Challenge ch : all) {
                ch.setHasApplauded(false);
            }
        }
        return ResponseEntity.ok(all);
    }

    /**
     * POST /api/challenges/add
     * Creates a new challenge. Initializes applause=0, completed = false, applaudedBy = [].
     */
    @PostMapping("/add")
    public ResponseEntity<Challenge> addChallenge(@RequestBody Challenge challenge) {
        if (challenge.getUsername() == null || challenge.getUsername().isBlank()
                || challenge.getChallenge() == null || challenge.getChallenge().isBlank()
                || challenge.getDuration() == null || challenge.getDuration().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        challenge.setCreatedAt(LocalDateTime.now());
        challenge.setApplause(0);
        challenge.setCompleted(false);
        challenge.setApplaudedBy(List.of()); // empty list
        Challenge saved = challengeRepository.save(challenge);
        return ResponseEntity.ok(saved);
    }

    /**
     * PUT /api/challenges/applause
     * Body: { "username": "bob", "challenge": "Build a portfolio site" }
     *
     * Logic:
     *  - We locate the challenge by (ownerUsername, challengeText).
     *  - If not found → 404
     *  - If request.username equals the owner → 400 (cannot applaud own)
     *  - If request.username already in applaudedBy → 400 (already applauded)
     *  - Otherwise, inc applause, push username into applaudedBy.
     */
    @PutMapping("/applause")
    public ResponseEntity<String> updateApplause(@RequestBody ApplauseRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()
                || request.getChallenge() == null || request.getChallenge().isBlank()) {
            return ResponseEntity.badRequest().body("Invalid request body");
        }

        // 1) Find the challenge document
        Query query = new Query(
                Criteria.where("username").is(request.getUsernameOwner())
                        .and("challenge").is(request.getChallenge())
        );

        // But first check: if the logged-in user is the same as the owner, reject
        if (request.getUsername().equals(request.getUsernameOwner())) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Cannot applaud your own challenge");
        }

        // 2) Check if they have already applauded
        //    We can do a findOne first to inspect applaudedBy
        Challenge existing = mongoTemplate.findOne(query, Challenge.class);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Challenge not found");
        }
        if (existing.getApplaudedBy().contains(request.getUsername())) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("You have already applauded this challenge");
        }

        // 3) Perform update: increment applause by 1, push username into applaudedBy
        Update update = new Update()
                .inc("applause", 1)
                .push("applaudedBy", request.getUsername());
        Challenge updated = mongoTemplate.findAndModify(query, update, Challenge.class);

        if (updated != null) {
            return ResponseEntity.ok("Applause updated");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Challenge not found");
        }
    }

    @GetMapping("/active")
public ResponseEntity<Map<String, Boolean>> isChallengeActive(
        @RequestParam(name = "username", required = true) String username) {

    if (username == null || username.isBlank()) {
        return ResponseEntity.badRequest().body(Map.of("active", false));
    }

    List<Challenge> userChallenges = challengeRepository.findByUsername(username);
    LocalDateTime now = LocalDateTime.now();
    boolean anyActive = false;

    for (Challenge ch : userChallenges) {
        if (ch.isCompleted()) continue;

        int days = parseDurationToDays(ch.getDuration());
        LocalDateTime endTime = ch.getCreatedAt().plusDays(days);

        if (!endTime.isBefore(now)) {
            anyActive = true;
            break;
        }
    }

    return ResponseEntity.ok(Map.of("active", anyActive));
}

// Enhanced duration parser
private int parseDurationToDays(String duration) {
    if (duration == null || duration.isBlank()) {
        return 0;
    }
    
    try {
        // Handle numeric formats directly
        return Integer.parseInt(duration);
    } catch (NumberFormatException e) {
        duration = duration.toLowerCase().trim();
        
        // Extract the numeric value
        String[] parts = duration.split("\\D+");
        int value = 0;
        if (parts.length > 0 && !parts[0].isEmpty()) {
            value = Integer.parseInt(parts[0]);
        }
        
        // Handle different duration units
        if (duration.contains("year")) {
            return value * 365;  // 365 days/year
        } 
        else if (duration.contains("month")) {
            return value * 30;   // 30 days/month approximation
        }
        else if (duration.contains("week")) {
            return value * 7;    // 7 days/week
        }
        else if (duration.contains("day")) {
            return value;        // Days directly
        }
        
        return 0;  // Unrecognized format
    }
}

}

/**
 * We need both “who is making the applause” (request.username)
 * and “which challenge’s owner” (request.usernameOwner), plus challenge text.
 *
 * Frontend must send:
 *   {
 *     "username": "bob",             // the applauding user
 *     "usernameOwner": "alice",      // the challenge’s owner
 *     "challenge": "Build a portfolio site"
 *   }
 */
class ApplauseRequest {
    private String username;        // applauding user
    private String usernameOwner;   // owner of the challenge
    private String challenge;       // challenge text

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsernameOwner() {
        return usernameOwner;
    }
    public void setUsernameOwner(String usernameOwner) {
        this.usernameOwner = usernameOwner;
    }

    public String getChallenge() {
        return challenge;
    }
    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }
}
