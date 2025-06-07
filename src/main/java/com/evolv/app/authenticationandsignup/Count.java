package com.evolv.app.authenticationandsignup;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document(collection = "count")
public class Count {
    @Id
    private String id;
    private String username;
    private List<Date> deletionDates = new ArrayList<>();

    public Count() {}

    public Count(String username, Date firstDeletionDate) {
        this.username = username;
        this.deletionDates.add(firstDeletionDate);
    }

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

    public List<Date> getDeletionDates() {
        return deletionDates;
    }
    public void setDeletionDates(List<Date> deletionDates) {
        this.deletionDates = deletionDates;
    }

    public void addDeletionDate(Date date) {
        this.deletionDates.add(date);
    }

    public int getTotalCount() {
        return this.deletionDates.size();
    }
}
