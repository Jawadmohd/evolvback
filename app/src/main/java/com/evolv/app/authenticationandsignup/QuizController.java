package com.evolv.app.authenticationandsignup;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    private final QuizRepository quizRepository;

    public QuizController(QuizRepository quizRepository) {
        this.quizRepository = quizRepository;
    }

            @GetMapping
        public ResponseEntity<List<Quiz>> getByProfession(
            @RequestParam("profession") String profession
        ) {
            String normalizedProfession = profession.toLowerCase();
            List<Quiz> quizzes = quizRepository.findByProfession(normalizedProfession);
            return quizzes.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(quizzes);
        }
}
