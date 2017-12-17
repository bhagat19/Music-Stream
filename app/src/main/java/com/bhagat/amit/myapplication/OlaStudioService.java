package com.bhagat.amit.myapplication;

import java.util.ArrayList;


import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by amit on 16/12/17.
 */

public interface OlaStudioService {

    @GET("/studio")
    Call<ArrayList<Melody>> getRecentTracks();
}
