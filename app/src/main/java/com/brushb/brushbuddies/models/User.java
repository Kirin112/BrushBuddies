package com.brushb.brushbuddies.models;

public class    User {
    private String uid;
    private String username;
    private boolean isGuest;

    private String email;
    private int totalGames;
    private int wins;
    private int totalScore;
    private float averageScore;


    public User() {}

    //для гостя
    public User(String uid) {
        this.uid = uid;
        this.username = "Guest_" + uid.substring(0, 4);
        this.isGuest = true;
        this.totalGames = 0;
        this.wins = 0;
        this.totalScore = 0;
    }

    //для рег
    public User(String uid, String username, String email) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.isGuest = false;
        this.totalGames = 0;
        this.wins = 0;
        this.totalScore = 0;
    }

    public String getUid() {
        return uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public int getTotalGames() {
        return totalGames;
    }

    public int getWins() {
        return wins;
    }

    public void setTotalGames(int i) {
        this.totalGames = i;
    }

    public void setTotalScore(int i) {
        this.totalScore = i;
    }

    public void setAverageScore(float v) {
        this.averageScore = v;
    }

    public void setWins(int i) {
        this.wins = i;
    }
}

