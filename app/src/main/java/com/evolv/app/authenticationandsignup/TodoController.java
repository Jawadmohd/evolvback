package com.evolv.app.authenticationandsignup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/tasks")
public class TodoController {

    @Autowired
    private ToDoRepository todoRepository;

    @Autowired
    private CountRepository countRepository;

    @GetMapping
    public ResponseEntity<?> getTodos(@RequestParam String username) {
        try {
            List<Todo> tasks = todoRepository.findByUsername(username);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error fetching tasks");
        }
    }

    @PostMapping
    public ResponseEntity<?> addTodo(@RequestBody Todo todoRequest,
                                     @RequestParam String username) {
        try {
            if (todoRequest.getTitle() == null || todoRequest.getTitle().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Task description cannot be empty");
            }

            Todo todo = new Todo();
            todo.setTitle(todoRequest.getTitle());
            todo.setPeriod(todoRequest.getPeriod());
            todo.setUsername(username);

            Todo saved = todoRepository.save(todo);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error adding task");
        }
    }

    /**
     * DELETE /api/tasks/{id}?username=...&date=2025-06-03T10:18:35.147Z
     *
     * - `id` is the task ID (path variable).
     * - `username` is required as a query parameter.
     * - `date` is passed as an ISO-8601 string; we parse it into a Date internally.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTodo(
            @PathVariable String id,
            @RequestParam String username,
            @RequestParam String date   // now: receive ISO-8601 string, not a Date directly
    ) {
        try {
            // Parse the incoming ISO-8601 string into a java.util.Date
            Date deletionDate;
            try {
                Instant inst = Instant.parse(date);
                deletionDate = Date.from(inst);
            } catch (Exception pe) {
                return ResponseEntity.badRequest().body("Invalid date format. Please send ISO-8601 string.");
            }

            Optional<Todo> opt = todoRepository.findByIdAndUsername(id, username);
            if (opt.isEmpty()) {
                return ResponseEntity.status(404).body("Task not found");
            }
            Todo task = opt.get();
            if (!"onetime".equalsIgnoreCase(task.getPeriod())) {
                return ResponseEntity.badRequest().body("Cannot delete permanent tasks");
            }

            // 1) Remove the task
            todoRepository.deleteById(id);

            // 2) Update or create the Count document, now adding the new deletion date
            Count updatedCount = countRepository
                    .findByUsername(username)
                    .map(c -> {
                        c.addDeletionDate(deletionDate);
                        return countRepository.save(c);
                    })
                    .orElseGet(() -> {
                        Count c = new Count(username, deletionDate);
                        return countRepository.save(c);
                    });

            return ResponseEntity.ok(
                String.format("Task deleted. %s total deletions: %d.",
                              username,
                              updatedCount.getTotalCount())
            );
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error deleting task");
        }
    }

    // Fetch deletion metrics for a user: total, last day, last week
    @GetMapping("/count")
    public ResponseEntity<?> getDeletionMetrics(@RequestParam String username) {
        try {
            Optional<Count> optCount = countRepository.findByUsername(username);

            int total = 0, lastDay = 0, lastWeek = 0;
            if (optCount.isPresent()) {
                List<Date> dates = optCount.get().getDeletionDates();
                total = dates.size();

                Date now = new Date();
                Calendar cal = Calendar.getInstance();
                cal.setTime(now);

                // boundary for last 24 hours
                cal.add(Calendar.DAY_OF_MONTH, -1);
                Date oneDayAgo = cal.getTime();

                // boundary for last 7 days
                cal.setTime(now);
                cal.add(Calendar.DAY_OF_MONTH, -7);
                Date oneWeekAgo = cal.getTime();

                lastDay = (int) dates.stream()
                        .filter(d -> d.after(oneDayAgo))
                        .count();

                lastWeek = (int) dates.stream()
                        .filter(d -> d.after(oneWeekAgo))
                        .count();
            }

            Map<String, Object> body = Map.of(
                "username", username,
                "totalDeletions", total,
                "deletionsLastDay", lastDay,
                "deletionsLastWeek", lastWeek
            );
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                   .status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .body("Error fetching deletion metrics");
        }
    }
}
