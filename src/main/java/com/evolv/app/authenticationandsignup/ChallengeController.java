package com.evolv.app.authenticationandsignup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@RestController
@RequestMapping("/api/challenges")
@CrossOrigin(origins = "*")
public class ChallengeController {

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    // ───────────────────────────────────────────────────────────────────────────
    // 1) POST /api/challenges
    //    Front end now does a POST with body { "username": "bob" }.
    //    Returns all challenges (setting hasApplauded if username ∈ applaudedBy).
    @PostMapping
    public ResponseEntity<List<Challenge>> getAllChallenges(@RequestBody Map<String,String> body) {
        String currentUser = body.get("username");
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

    // ───────────────────────────────────────────────────────────────────────────
    // 2) POST /api/challenges/add
    //    Creates a new challenge. (Unchanged: still POST with full JSON Challenge.)
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
        challenge.setApplaudedBy(List.of());
        challenge.setProofImageUrls(List.of());
        challenge.setProgressEntries(List.of());

        Challenge saved = challengeRepository.save(challenge);
        return ResponseEntity.ok(saved);
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 3) PUT /api/challenges/applause
    //    Body: { "username": "bob", "usernameOwner": "alice", "challenge": "Build a portfolio site" }
    @PutMapping("/applause")
    public ResponseEntity<String> updateApplause(@RequestBody ApplauseRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()
                || request.getUsernameOwner() == null || request.getUsernameOwner().isBlank()
                || request.getChallenge() == null || request.getChallenge().isBlank()) {
            return ResponseEntity.badRequest().body("Invalid request body");
        }

        if (request.getUsername().equals(request.getUsernameOwner())) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Cannot applaud your own challenge");
        }

        Optional<Challenge> opt = challengeRepository.findByUsernameAndChallenge(
                request.getUsernameOwner(), request.getChallenge()
        );
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Challenge not found");
        }
        Challenge existing = opt.get();

        if (existing.getApplaudedBy().contains(request.getUsername())) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("You have already applauded this challenge");
        }

        Query query = new Query(Criteria.where("id").is(existing.getId()));
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

    // ───────────────────────────────────────────────────────────────────────────
    // 4) POST /api/challenges/active
    //    Body: { "username": "bob" }
    //    Returns { "active": true/false }
    @PostMapping("/active")
    public ResponseEntity<Map<String, Boolean>> isChallengeActive(@RequestBody Map<String,String> body) {
        String username = body.get("username");
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

    // Helper to parse "30 days", "2 weeks", "1 month", etc → integer days
    private int parseDurationToDays(String duration) {
        if (duration == null || duration.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(duration);
        } catch (NumberFormatException e) {
            duration = duration.toLowerCase().trim();
            String[] parts = duration.split("\\D+");
            int value = 0;
            if (parts.length > 0 && !parts[0].isEmpty()) {
                value = Integer.parseInt(parts[0]);
            }
            if (duration.contains("year")) {
                return value * 365;
            } else if (duration.contains("month")) {
                return value * 30;   // approximation
            } else if (duration.contains("week")) {
                return value * 7;
            } else if (duration.contains("day")) {
                return value;
            }
            return 0;
        }
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 5) POST /api/challenges/{id}/uploadProof
    //    Body: { "imageUrl": "https://..." }
    @PostMapping("/{id}/uploadProof")
    public ResponseEntity<String> uploadProof(
            @PathVariable("id") String challengeId,
            @RequestBody ImageProofRequest request) {

        if (request.getImageUrl() == null || request.getImageUrl().isBlank()) {
            return ResponseEntity.badRequest().body("Invalid imageUrl");
        }
        Optional<Challenge> opt = challengeRepository.findById(challengeId);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Challenge not found");
        }
        Challenge ch = opt.get();
        ch.addProofImageUrl(request.getImageUrl());
        challengeRepository.save(ch);
        return ResponseEntity.ok("Proof image URL added");
    }

    public static class ImageProofRequest {
        private String imageUrl;
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 6) POST /api/challenges/{id}/progress
    //    Body: { "date": "2025-06-05", "report": "Did 10 push-ups" }
    @PostMapping("/{id}/progress")
    public ResponseEntity<String> addProgress(
            @PathVariable("id") String challengeId,
            @RequestBody ProgressRequest request) {

        if (request.getDate() == null || request.getDate().isBlank()
                || request.getReport() == null || request.getReport().isBlank()) {
            return ResponseEntity.badRequest().body("Invalid request body");
        }
        Optional<Challenge> opt = challengeRepository.findById(challengeId);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Challenge not found");
        }
        Challenge ch = opt.get();
        try {
            LocalDate parsed = LocalDate.parse(request.getDate());
            Challenge.ProgressEntry entry = new Challenge.ProgressEntry(parsed, request.getReport());
            ch.addProgressEntry(entry);
            challengeRepository.save(ch);
            return ResponseEntity.ok("Progress entry added");
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest()
                    .body("Invalid date format, expected YYYY-MM-DD");
        }
    }

    public static class ProgressRequest {
        private String date;    // "YYYY-MM-DD"
        private String report;  // short report text

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public String getReport() { return report; }
        public void setReport(String report) { this.report = report; }
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 7) POST /api/challenges/{id}/streak
    //    (Front end now uses POST instead of GET.)
@PostMapping("/{id}/streak")
public ResponseEntity<Map<String, Integer>> getCurrentStreak(
        @PathVariable("id") String challengeId) {

    Optional<Challenge> opt = challengeRepository.findById(challengeId);
    if (opt.isEmpty()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    Challenge ch = opt.get();
    List<Challenge.ProgressEntry> entries = ch.getProgressEntries();

    if (entries == null || entries.isEmpty()) {
        return ResponseEntity.ok(Map.of(
            "streak", 0,
            "entries", 0,
            "points", 0
        ));
    }

    // 1. Create a set of unique dates to calculate streak
    Set<LocalDate> uniqueDates = new TreeSet<>(Comparator.reverseOrder());
    for (Challenge.ProgressEntry e : entries) {
        uniqueDates.add(e.getDate());
    }

    // 2. Calculate streak from today backward
    int streak = 0;
    LocalDate expected = LocalDate.now();
    for (LocalDate date : uniqueDates) {
        if (date.isEqual(expected)) {
            streak++;
            expected = expected.minusDays(1);
        } else if (date.isBefore(expected)) {
            break;
        }
    }

    // 3. Total points = number of entries (even multiple on same day)
    int totalEntries = entries.size();
    int points = totalEntries;

    return ResponseEntity.ok(Map.of(
        "streak", streak,
        "entries", totalEntries,
        "points", points
    ));
}


    @PutMapping("/{id}/markCompleted")
public ResponseEntity<String> markChallengeCompleted(@PathVariable("id") String challengeId) {
    Optional<Challenge> opt = challengeRepository.findById(challengeId);
    if (opt.isEmpty()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Challenge not found");
    }

    Challenge ch = opt.get();
    if (ch.isCompleted()) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Challenge already completed");
    }

    ch.setCompleted(true);
    challengeRepository.save(ch);
    return ResponseEntity.ok("Challenge marked completed");
}

}

// ───────────────────────────────────────────────────────────────────────────
// Request body for applause endpoint
class ApplauseRequest {
    private String username;        // who is applauding
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
