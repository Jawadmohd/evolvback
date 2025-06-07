package com.evolv.app.authenticationandsignup;

  // adjust this to match your project structure

public class CompletionStat {
    private String date;
    private int count;

    public CompletionStat() { }

    public CompletionStat(String date, int count) {
        this.date = date;
        this.count = count;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
