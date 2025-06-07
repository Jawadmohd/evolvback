package com.evolv.app.authenticationandsignup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    @Autowired
    private QuizRepository quizRepository;

    // 1) POST /api/quizzes
    //    Body: { "profession": "doctor" }
    @PostMapping
    public ResponseEntity<Quiz> getQuizByProfession(@RequestBody Map<String, String> body) {
        String profession = body.get("profession");
        if (profession == null || profession.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String normalizedProf = profession.toLowerCase().trim();
        Optional<Quiz> optQuiz = quizRepository.findByProfession(normalizedProf);
        if (optQuiz.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(optQuiz.get());
    }

    // 4) POST /api/quizzes/score
    @PostMapping("/score")
    public ResponseEntity<Map<String, Integer>> submitScore(
            @RequestBody SubmitScoreRequest request
    ) {
        String username = request.getUsername().trim();
        String prof = request.getProfession().toLowerCase().trim();
        int score = request.getScore();

        if (username.isEmpty() || prof.isEmpty() || score < 0) {
            return ResponseEntity.badRequest().build();
        }

        Optional<Quiz> optQuiz = quizRepository.findByProfession(prof);
        if (optQuiz.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Quiz quiz = optQuiz.get();
        quiz.updateLeaderboard(username, score);
        quizRepository.save(quiz);

        return ResponseEntity.ok(quiz.getLeaderboard());
    }

    // Helper class to receive score submissions
    public static class SubmitScoreRequest {
        private String username;
        private String profession;
        private int score;

        public SubmitScoreRequest() {}

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getProfession() { return profession; }
        public void setProfession(String profession) { this.profession = profession; }

        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
    }

    // GET /api/quizzes/leaderboard?profession=developer
@GetMapping("/leaderboard")
public ResponseEntity<Map<String, Integer>> getLeaderboard(@RequestParam("profession") String profession) {
    if (profession == null || profession.trim().isEmpty()) {
        return ResponseEntity.badRequest().build();
    }

    String normalizedProf = profession.toLowerCase().trim();
    Optional<Quiz> optQuiz = quizRepository.findByProfession(normalizedProf);
    if (optQuiz.isEmpty()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    Quiz quiz = optQuiz.get();
    Map<String, Integer> leaderboard = quiz.getLeaderboard();

    if (leaderboard == null) {
        leaderboard = new HashMap<>();
    }

    return ResponseEntity.ok(leaderboard);
}

}
