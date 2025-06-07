package com.evolv.app.authenticationandsignup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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

    @Autowired
    private MongoTemplate mongoTemplate;

    // ─── 1) POST /api/tasks
    //    Body: { "username": "bob", "tag": "work" }  OR  { "username": "bob" }
   @PostMapping
public ResponseEntity<?> getTodos(@RequestBody Map<String, Object> body) {
    try {
        String username = (String) body.get("username");
        String tag = (String) body.get("tag");
        Boolean completed = null;

        if (body.containsKey("completed")) {
            Object completedValue = body.get("completed");
            if (completedValue instanceof Boolean) {
                completed = (Boolean) completedValue;
            } else if (completedValue instanceof String) {
                completed = Boolean.parseBoolean((String) completedValue);
            }
        }

        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body("username is required");
        }

        List<Todo> tasks;

        if (tag != null && !tag.isEmpty() && completed != null) {
            tasks = todoRepository.findByUsernameAndTagsContainingAndCompleted(username, tag, completed);
        } else if (tag != null && !tag.isEmpty()) {
            tasks = todoRepository.findByUsernameAndTagsContaining(username, tag);
        } else if (completed != null) {
            tasks = todoRepository.findByUsernameAndCompleted(username, completed);
        } else {
            tasks = todoRepository.findByUsername(username);
        }

        return ResponseEntity.ok(tasks);
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.internalServerError().body("Error fetching tasks");
    }
}

    // ─── 2) POST /api/tasks/add
    //    Body JSON: { "username":"bob", "title":"Buy milk", "period":"onetime", "tags":["errand"], "deadline":"2025-06-10T14:30:00.000Z" }
    @PostMapping("/add")
    public ResponseEntity<?> addTodo(@RequestBody Todo todoRequest) {
        try {
            String username = todoRequest.getUsername();
            if (username == null || username.isBlank()) {
                return ResponseEntity.badRequest().body("username is required");
            }
            if (todoRequest.getTitle() == null || todoRequest.getTitle().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Task description cannot be empty");
            }
            String period = todoRequest.getPeriod();
            if (period == null || 
               (!period.equalsIgnoreCase("onetime") && !period.equalsIgnoreCase("permanent"))) {
                return ResponseEntity.badRequest().body("Invalid period value");
            }

            Todo todo = new Todo();
            todo.setUsername(username);
            todo.setTitle(todoRequest.getTitle().trim());
            todo.setPeriod(period.toLowerCase());

            if (todoRequest.getTags() != null) {
                todo.setTags(todoRequest.getTags());
            } else {
                todo.setTags(Collections.emptyList());
            }

            if (todoRequest.getDeadline() != null) {
                todo.setDeadline(todoRequest.getDeadline());
            }

            todo.setCompleted(false);
            todo.setCompletedAt(null);

            Todo saved = todoRepository.save(todo);
            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error adding task");
        }
    }

    // In your Controller class:

// Complete or delete a task (only for one-time tasks)
@PatchMapping("/{id}/complete")
public ResponseEntity<?> completeTask(
        @PathVariable String id,
        @RequestParam String username
) {
    try {
        Optional<Todo> opt = todoRepository.findByIdAndUsername(id, username);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body("Task not found");
        }
        Todo task = opt.get();

        if (Boolean.TRUE.equals(task.getCompleted())) {
            return ResponseEntity.badRequest().body("Task already completed");
        }

        // Mark as completed regardless of period
        task.setCompleted(true);
        task.setCompletedAt(new Date());
        todoRepository.save(task);

        return ResponseEntity.ok("Task marked as completed");
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.internalServerError().body("Error completing task");
    }
}

// ─── 5) POST /api/tasks/completion-stats
    //     Body: { "username": "bob" }
      @PostMapping("/completion-stats")
    public ResponseEntity<?> getCompletionStats(@RequestBody Map<String, String> body) {
        try {
            String username = body.get("username");
            if (username == null || username.isBlank()) {
                return ResponseEntity.badRequest().body("username is required");
            }

            // 1) MATCH stage: only those documents where username matches & completed == true
            MatchOperation matchStage = Aggregation.match(
                Criteria.where("username").is(username)
                        .and("completed").is(true)
            );

            // 2) PROJECT stage: create a new field "date" by formatting completedAt → "YYYY-MM-DD"
            ProjectionOperation projectStage = Aggregation.project()
                .and(
                    DateOperators.DateToString
                        .dateOf("completedAt")
                        .toString("%Y-%m-%d")
                )
                .as("date");

            // 3) GROUP stage: group by that "date" string, and count how many in each
            GroupOperation groupStage = Aggregation.group("date")
                .count()
                .as("count");

            // 4) SORT stage: sort ascending by the grouping key (“_id” holds the date string)
            SortOperation sortStage = Aggregation.sort(Sort.by(Sort.Direction.ASC, "_id"));

            // Build the aggregation pipeline
            Aggregation aggregation = Aggregation.newAggregation(
                matchStage,
                projectStage,
                groupStage,
                sortStage
            );

            // Execute against the "todo" collection, mapping results into our helper POJO
            AggregationResults<CompletionStat> results = mongoTemplate.aggregate(
                aggregation,
                "todo",
                CompletionStat.class
            );

            List<CompletionStat> stats = results.getMappedResults();
            return ResponseEntity.ok(stats);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching completion stats");
        }
    }

    // ─── 6) POST /api/tasks/dates
    //     Body: { "username": "bob" }
    @PostMapping("/dates")
    public ResponseEntity<?> getTaskDates(@RequestBody Map<String,String> body) {
        try {
            String username = body.get("username");
            if (username == null || username.isBlank()) {
                return ResponseEntity.badRequest().body("username is required");
            }

            List<Todo> tasks = todoRepository.findByUsername(username);
            Set<String> dates = tasks.stream()
                    .filter(t -> t.getDeadline() != null)
                    .map(t -> {
                        Instant i = t.getDeadline().toInstant();
                        return DateTimeFormatter.ISO_LOCAL_DATE
                                .withZone(ZoneId.of("UTC"))
                                .format(i);
                    })
                    .collect(Collectors.toSet());
            return ResponseEntity.ok(dates);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching task dates");
        }
    }


     public static class CompletionStat {
        private String _id;   // holds the "YYYY-MM-DD" date string
        private int    count; // number of tasks completed on that date

        public String get_id() { return _id; }
        public void set_id(String _id) { this._id = _id; }

        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
    }
}
