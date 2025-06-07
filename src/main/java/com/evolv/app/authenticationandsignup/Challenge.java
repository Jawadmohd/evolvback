package com.evolv.app.authenticationandsignup;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "challenge")
public class Challenge {

    @Id
    private String id;

    private String username;           // Owner of this challenge
    private String challenge;          // Challenge text
    private String duration;           // e.g. "30 days", "2 weeks"
    private int applause;              // Total applause count
    private boolean completed;         // Whether owner marked it completed
    private LocalDateTime createdAt;

    // List of usernames who have applauded
    private List<String> applaudedBy = new ArrayList<>();

    // === NEW FIELD: Photo proof URLs (e.g., S3 or wherever you host images) ===
    private List<String> proofImageUrls = new ArrayList<>();

    // === NEW FIELD: Daily progress check‐ins ===
    private List<ProgressEntry> progressEntries = new ArrayList<>();

    // Transient field: computed on the fly to let front‐end know if current user already applauded
    @Transient
    private boolean hasApplauded;

    // ───────────────────────────────────────────────────────────────────────────
    // Nested static class to represent a single daily progress check‐in
    public static class ProgressEntry {
        private LocalDate date;   // e.g. 2025-06-05
        private String report;    // short text from the user

        public ProgressEntry() {}

        public ProgressEntry(LocalDate date, String report) {
            this.date = date;
            this.report = report;
        }

        public LocalDate getDate() {
            return date;
        }
        public void setDate(LocalDate date) {
            this.date = date;
        }

        public String getReport() {
            return report;
        }
        public void setReport(String report) {
            this.report = report;
        }
    }

    // ───────────────────────────────────────────────────────────────────────────
    // Constructors
    public Challenge() {}

    // ───────────────────────────────────────────────────────────────────────────
    // Getters & Setters

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public String getChallenge() {
        return challenge;
    }
    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    public String getDuration() {
        return duration;
    }
    public void setDuration(String duration) {
        this.duration = duration;
    }

    public int getApplause() {
        return applause;
    }
    public void setApplause(int applause) {
        this.applause = applause;
    }

    public boolean isCompleted() {
        return completed;
    }
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<String> getApplaudedBy() {
        return applaudedBy;
    }
    public void setApplaudedBy(List<String> applaudedBy) {
        this.applaudedBy = applaudedBy;
    }

    public boolean isHasApplauded() {
        return hasApplauded;
    }
    public void setHasApplauded(boolean hasApplauded) {
        this.hasApplauded = hasApplauded;
    }

    // ───────────────────────────────────────────────────────────────────────────
    // PROOF IMAGE URL METHODS

    public List<String> getProofImageUrls() {
        return proofImageUrls;
    }
    public void setProofImageUrls(List<String> proofImageUrls) {
        this.proofImageUrls = proofImageUrls;
    }

    // Add one URL
    public void addProofImageUrl(String url) {
        if (this.proofImageUrls == null) {
            this.proofImageUrls = new ArrayList<>();
        }
        this.proofImageUrls.add(url);
    }

    // ───────────────────────────────────────────────────────────────────────────
    // PROGRESS ENTRIES METHODS

    public List<ProgressEntry> getProgressEntries() {
        return progressEntries;
    }
    public void setProgressEntries(List<ProgressEntry> progressEntries) {
        this.progressEntries = progressEntries;
    }

    // Add a single progress entry
    public void addProgressEntry(ProgressEntry entry) {
        if (this.progressEntries == null) {
            this.progressEntries = new ArrayList<>();
        }
        this.progressEntries.add(entry);
    }
}
