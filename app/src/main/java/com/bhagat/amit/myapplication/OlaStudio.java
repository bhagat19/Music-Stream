package com.bhagat.amit.myapplication;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by amit on 16/12/17.
 * Implementing a Singleton behavior for one time initialisation of Retrofit
 */

public class OlaStudio {

    private static final Retrofit RETROFIT = new Retrofit.Builder()
            .baseUrl(Config.API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
    private static final OlaStudioService SERVICE = RETROFIT.create(OlaStudioService.class);

    public static OlaStudioService getService() {
        return SERVICE;
    }
}
