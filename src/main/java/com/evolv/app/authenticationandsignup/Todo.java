package com.evolv.app.authenticationandsignup;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.List;

@Document(collection = "todo")
public class Todo {
    @Id
    @JsonProperty("_id")
    private String id;

    private String username;
    private String title;
    private String period;           // "onetime" or "permanent"
    private List<String> tags;       // new field: tags array
    private Date deadline;           // new field: deadline date/time
    private Boolean completed;       // new field: true/false
    private Date completedAt;        // new field: when it was marked completed

    // Constructors
    public Todo() {
        this.completed = false;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public Date getDeadline() { return deadline; }
    public void setDeadline(Date deadline) { this.deadline = deadline; }

    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }

    public Date getCompletedAt() { return completedAt; }
    public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }
}
