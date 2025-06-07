package com.evolv.app.authenticationandsignup;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonProperty;

@Document(collection = "videos-interest")
public class VideoInterest {

    @Id
    @JsonProperty("_id")
    private String id;

    private String username;
    private String interest;

    // Constructor
    public VideoInterest() {}

    public VideoInterest(String username, String interest) {
        this.username = username;
        this.interest = interest;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getInterest() {
        return interest;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setInterest(String interest) {
        this.interest = interest;
    }
}
