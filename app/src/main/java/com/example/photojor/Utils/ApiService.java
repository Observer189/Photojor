package com.example.photojor.Utils;

import com.example.photojor.model.Ingredient;
import com.example.photojor.model.ServIngrResponse;
import com.example.photojor.model.ServRecipeResp;

import java.util.ArrayList;
import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface ApiService {
    @Multipart
    @POST("/")
    Call<ServIngrResponse> uploadImage(@Part MultipartBody.Part filePart);

    @POST("/api/giveAdvice")
    Call<ServRecipeResp> getRecipes(@Query("ingridients_ids") String list);

}
