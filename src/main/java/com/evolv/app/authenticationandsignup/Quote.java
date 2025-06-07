package com.evolv.app.authenticationandsignup;

// Quote.java (Model)

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "quotes")
public class Quote {
    @Id
    private String id;
    private String quote;

    // Constructors, getters, and setters
    public Quote() {}

    public Quote(String quote) {
        this.quote = quote;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getQuote() { return quote; }
    public void setQuote(String quote) { this.quote = quote; }
}