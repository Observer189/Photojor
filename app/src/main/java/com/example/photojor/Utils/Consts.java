package com.example.photojor.Utils;

import android.content.SharedPreferences;

import com.example.photojor.model.Ingredient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;

import retrofit2.Retrofit;

public class Consts {
    public static GsonBuilder gsonBuilder;
    public static Gson gson;

    public static Retrofit retrofit;
    public static ApiService service;
    public static ArrayList<Ingredient> fridge;


    public static SharedPreferences sp;
    public static SharedPreferences.Editor editor;

    public static final String APP_PREFERENCES = "settings";
    public static final String APP_PREFERENCES_INGREDIENTS="ingredients";


    public static final String baseUrl="http://192.168.88.245:5000";
    public static final String stepaUrl="http://192.168.88.244:8000";

    public static boolean isHistoryLaunched;
}
