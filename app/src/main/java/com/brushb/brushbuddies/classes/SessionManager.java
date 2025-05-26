package com.brushb.brushbuddies.classes;

import com.brushb.brushbuddies.models.User;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {}

    public static synchronized SessionManager get() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }
}
