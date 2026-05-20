package com.example.moviapp_rimsakine;



public class MyMovieData {
    private String movieName;
    private String movieDate;
    private String movieImage;
    private String movieDescription;
    private int movieId;

    public MyMovieData(int movieId, String movieName, String movieDate, String movieImage) {
        this.movieId = movieId;
        this.movieName = movieName;
        this.movieDate = movieDate;
        this.movieImage = movieImage;
    }

    public int getMovieId() { return movieId; }
    public String getMovieName() { return movieName; }
    public String getMovieDate() { return movieDate; }
    public String getMovieImage() { return movieImage; }
    public String getMovieDescription() { return movieDescription; }

    public void setMovieId(int movieId) { this.movieId = movieId; }
    public void setMovieName(String movieName) { this.movieName = movieName; }
    public void setMovieDate(String movieDate) { this.movieDate = movieDate; }
    public void setMovieImage(String movieImage) { this.movieImage = movieImage; }
    public void setMovieDescription(String movieDescription) { this.movieDescription = movieDescription; }
}