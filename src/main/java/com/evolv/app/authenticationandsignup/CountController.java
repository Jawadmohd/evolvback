package com.evolv.app.authenticationandsignup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class CountController {

    @Autowired
    private CountRepository countRepository;

    /**
     * GET /api/tasks/count?username={username}
     *
     * Returns:
     *  {
     *    "totalDeletions": <int>,
     *    "deletionsLastDay": <int>,
     *    "deletionsLastWeek": <int>
     *  }
     *
     * If the user has no Count document yet, returns zeros for all metrics.
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Integer>> getDeletionCounts(
            @RequestParam(name = "username", required = true) String username) {

        if (username == null || username.isBlank()) {
            // Bad request if no username provided
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "totalDeletions", 0,
                            "deletionsLastDay", 0,
                            "deletionsLastWeek", 0
                    ));
        }

        Optional<Count> optCount = countRepository.findByUsername(username);
        if (optCount.isEmpty()) {
            // No deletions recorded yet
            return ResponseEntity.ok(Map.of(
                    "totalDeletions", 0,
                    "deletionsLastDay", 0,
                    "deletionsLastWeek", 0
            ));
        }

        Count countDoc = optCount.get();
        List<Date> deletionDates = countDoc.getDeletionDates();

        // Calculate "now" once
        Instant nowInstant = Instant.now();

        int total = deletionDates.size();
        int lastDay = 0;
        int lastWeek = 0;

        for (Date d : deletionDates) {
            Instant deletionInstant = d.toInstant();
            Duration elapsed = Duration.between(deletionInstant, nowInstant);
            long hoursAgo = elapsed.toHours();
            long daysAgo = elapsed.toDays();

            if (hoursAgo < 24) {
                lastDay++;
            }
            if (daysAgo < 7) {
                lastWeek++;
            }
        }

        Map<String, Integer> response = new HashMap<>();
        response.put("totalDeletions", total);
        response.put("deletionsLastDay", lastDay);
        response.put("deletionsLastWeek", lastWeek);

        return ResponseEntity.ok(response);
    }
}
