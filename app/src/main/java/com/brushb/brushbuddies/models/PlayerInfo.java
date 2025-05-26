package com.brushb.brushbuddies.models;

public class PlayerInfo {
    public final String userId;
    public final String username;
    public final int totalGames;
    public final int wins;
    public final int totalScore;
    public final boolean isHost;

    public PlayerInfo(String userId, String username,
                      int totalGames, int wins, int totalScore,
                      boolean isHost) {
        this.userId = userId;
        this.username = username;
        this.totalGames = totalGames;
        this.wins = wins;
        this.totalScore = totalScore;
        this.isHost = isHost;
    }
}
