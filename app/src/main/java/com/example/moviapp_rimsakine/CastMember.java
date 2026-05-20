package com.example.moviapp_rimsakine;

public class CastMember {
    private final String name;
    private final String character;
    private final String profilePath;

    public CastMember(String name, String character, String profilePath) {
        this.name        = name;
        this.character   = character;
        this.profilePath = profilePath;
    }

    public String getName()        { return name; }
    public String getCharacter()   { return character; }
    public String getProfilePath() { return profilePath; }
}