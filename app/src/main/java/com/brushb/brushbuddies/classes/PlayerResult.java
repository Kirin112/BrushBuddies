package com.brushb.brushbuddies.classes;

import java.io.Serializable;

public class PlayerResult implements Serializable {
    private final String playerUid;
    private final String playerName;
    private final String base64Image;
    private final float averageScore;

    public PlayerResult(String playerUid, String playerName, String base64Image, float averageScore) {
        this.playerUid    = playerUid;
        this.playerName   = playerName;
        this.base64Image  = base64Image;
        this.averageScore = averageScore;
    }

    public String getPlayerUid()    { return playerUid; }
    public String getPlayerName()   { return playerName; }
    public String getBase64Image()  { return base64Image; }
    public float  getAverageScore() { return averageScore; }
}
