package com.brushb.brushbuddies.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class Lobby {
    private String lobbyId;
    private String host;
    private String state;
    private Map<String, String> players;
    private List<String> playersOrder;
    private Map<String, Object> timers;
    private Map<String, Object> game;
    private long createdAt;
    private int maxPlayers;
    private String code;

    public static final String STATE_WAITING = "waiting";
    public static final String STATE_DRAWING = "drawing";

    public Lobby() {}

    public Lobby(String hostUid, String hostName) {
        this.lobbyId = "lobby_" + UUID.randomUUID().toString();
        this.host = hostUid;
        this.state = STATE_WAITING;
        this.players = new HashMap<>();
        this.playersOrder = new ArrayList<>();
        this.game = new HashMap<>();
        this.maxPlayers = 6;
        this.code = generateRandomCode();

        this.players.put(hostUid, hostName);
        this.playersOrder.add(hostUid);

        this.game.put("current_word", null);
        this.game.put("drawings", new HashMap<>());
        this.game.put("votes", new HashMap<>());
    }

    private String generateRandomCode() {
        String letters = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        Random random = new Random();

        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            code.append(letters.charAt(random.nextInt(letters.length())));
        }
        return code.toString();
    }

    public boolean isHost(String userId) {
        return host != null && host.equals(userId);
    }

    public String getCurrentWord() {
        if (game == null) return null;
        Object word = game.get("current_word");
        if (word instanceof String) {
            return (String) word;
        }
        return null;
    }

    public void setCurrentWord(String word) {
        if (game == null) game = new HashMap<>();
        game.put("current_word", word);
    }

    public String getLobbyId() { return lobbyId; }
    public String getCode() { return code; }
    public String getState() { return state; }
    public Map<String, String> getPlayers() { return players; }
    public long getCreatedAt() { return createdAt; }
    public String getHost() { return host; }
    public int getMaxPlayers() { return maxPlayers; }

    public void setStatus(String status) { this.state = status; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }
    public void setCode(String code) {
        this.code = code;
    }

    @Deprecated
    public void setLobbyId(String lobbyId) { this.lobbyId = lobbyId; }
    @Deprecated
    public void setPlayers(Map<String, String> players) { this.players = players; }
    @Deprecated
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    @Deprecated
    public void setHostUid(String creatorUid) { this.host = creatorUid; }

}
