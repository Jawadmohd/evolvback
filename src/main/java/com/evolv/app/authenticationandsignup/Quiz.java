package com.evolv.app.authenticationandsignup;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;

/**
 * Each Quiz document represents a single profession’s quiz set.
 *   • `profession`: e.g. "doctor", "engineer", etc.
 *   • `questions`: a list of nested Question objects (with options, answer, explanation).
 *   • `leaderboard`: a map from username → their highest score (for this profession).
 */
@Document(collection = "quizzes")
public class Quiz {
    @Id
    private String id;

    private String profession; 
    private List<Question> questions = new ArrayList<>();

    /**
     * Map of “username” → “highest score achieved (0…100)” for this profession.
     * If a user never took it, they simply do not appear in this map.
     */
    private Map<String, Integer> leaderboard = new HashMap<>();

    // Default constructor
    public Quiz() {}

    // Convenience constructor
    public Quiz(String profession, List<Question> questions) {
        this.profession = profession.toLowerCase().trim();
        this.questions = questions;
        this.leaderboard = new HashMap<>();
    }

    // ───────────────────────────────────────────────────────────────────────────
    // Getters & Setters
    // ───────────────────────────────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProfession() { return profession; }
    public void setProfession(String profession) {
        this.profession = profession.toLowerCase().trim();
    }

    public List<Question> getQuestions() { return questions; }
    public void setQuestions(List<Question> questions) { this.questions = questions; }

    public Map<String, Integer> getLeaderboard() { return leaderboard; }
    public void setLeaderboard(Map<String, Integer> leaderboard) {
        this.leaderboard = leaderboard;
    }

    // ───────────────────────────────────────────────────────────────────────────
    // “Upsert” helper for leaderboard: stores the user’s score if it’s higher
    // than their existing one (or adds a new entry if none existed yet).
    public void updateLeaderboard(String username, int newScore) {
        if (username == null || username.trim().isEmpty() || newScore < 0) {
            return;
        }
        String key = username.trim();
        Integer existing = this.leaderboard.get(key);
        if (existing == null || newScore > existing) {
            this.leaderboard.put(key, newScore);
        }
    }

    // ───────────────────────────────────────────────────────────────────────────
    // Nested “Question” class to store each individual question + options/answer/explanation
    public static class Question {
        private String question;        // question text
        private List<String> options;   // exactly 4 (for example) answer choices
        private String answer;          // the correct answer (must match one in options)
        private String explanation;     // why that answer is correct

        public Question() {}

        public Question(String question, List<String> options, String answer, String explanation) {
            this.question = question;
            this.options = options;
            this.answer = answer;
            this.explanation = explanation;
        }

        // Getters & Setters
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }

        public List<String> getOptions() { return options; }
        public void setOptions(List<String> options) { this.options = options; }

        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }

        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
    }
}
