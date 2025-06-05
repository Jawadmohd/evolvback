package com.evolv.app.authenticationandsignup;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "challenge")
public class Challenge {

    @Id
    private String id;

    private String username;
    private String challenge;
    private String duration;
    private int applause;                // how many total applause
    private boolean completed;           // whether the owner has marked it completed
    private LocalDateTime createdAt;

    // List of usernames who have already applauded this challenge
    private List<String> applaudedBy = new ArrayList<>();

    // Transient field: computed on the fly to let front-end know if current user already applauded
    @Transient
    private boolean hasApplauded;

    // ——— Getters & Setters ———

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
}
