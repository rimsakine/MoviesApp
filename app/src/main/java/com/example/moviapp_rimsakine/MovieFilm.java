package com.example.moviapp_rimsakine;

public class MovieFilm {
    private final int tmdbId;
    private final String titre;
    private final String annee;
    private final String genre;
    private final String synopsis;
    private String posterPath;

    public MovieFilm(int tmdbId, String titre, String annee, String genre, String synopsis) {
        this.tmdbId = tmdbId;
        this.titre = titre;
        this.annee = annee;
        this.genre = genre;
        this.synopsis = synopsis;
    }

    public int getTmdbId() { return tmdbId; }
    public String getTitre() { return titre; }
    public String getAnnee() { return annee; }
    public String getGenre() { return genre; }
    public String getSynopsis() { return synopsis; }
    public String getPosterPath() { return posterPath; }
    public void setPosterPath(String posterPath) { this.posterPath = posterPath; }
}
