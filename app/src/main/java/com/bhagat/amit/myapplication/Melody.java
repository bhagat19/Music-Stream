package com.bhagat.amit.myapplication;

import com.google.gson.annotations.SerializedName;

/**
 * Created by amit on 16/12/17.
 * base class representing a song
 */


public class Melody {

    public Melody(){}

    @SerializedName("song")
    private String title;

    @SerializedName("artists")
    private String artists;

    @SerializedName("url")
    private String melodyUrl;

    @SerializedName("cover_image")
    private String imageUrl;

    public String getTitle() {
        return title;
    }

    public void setTitle(String name) {
        this.title = name;
    }

    public String getArtists() {
        return artists;
    }

    public void setArtists(String artists) {
        this.artists = artists;
    }

    public String getMelodyUrl() {
        return melodyUrl;
    }

    public void setMelodyUrl(String melodyUrl) {
        this.melodyUrl = melodyUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

}
