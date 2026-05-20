package com.example.moviapp_rimsakine;

public class ChatMessage {
    private final String text;
    private final boolean isUser;
    private final MovieFilm film;
    private final long timestamp;

    public ChatMessage(String text, boolean isUser, MovieFilm film, long timestamp) {
        this.text = text;
        this.isUser = isUser;
        this.film = film;
        this.timestamp = timestamp;
    }

    public String getText() { return text; }
    public boolean isUser() { return isUser; }
    public MovieFilm getFilm() { return film; }
    public long getTimestamp() { return timestamp; }
}
