package com.example.photojor;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.example.photojor.Utils.ApiService;
import com.example.photojor.Utils.Consts;
import com.example.photojor.model.Recipe;
import com.example.photojor.model.ServIngrResponse;
import com.example.photojor.model.ServRecipeResp;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RecipeFragment extends Fragment {
    View v;
    ArrayList<Recipe> recArrray;
    RecipeAdapter recipeAdapter;
    ListView recipeList;
    ArrayList<Integer> recipeIds;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        v=inflater.inflate(R.layout.fragment_recipes, container, false);
        Consts.retrofit = new Retrofit.Builder()
                .baseUrl(Consts.stepaUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        Consts.service = Consts.retrofit.create(ApiService.class);
        recipeIds=new ArrayList<Integer>();
       /*for(int i=0;i<Consts.fridge.size();i++)
       {
           recipeIds.add(Consts.fridge.get(i).getId());
       }*/
       recipeIds.add(780);
       recipeIds.add(1854);
       recipeIds.add(777);
       String pack=Consts.gson.toJson(recipeIds);
        Call<ServRecipeResp> call = Consts.service.getRecipes(pack);
        call.enqueue(new Callback<ServRecipeResp>() {
            @Override
            public void onResponse(Call<ServRecipeResp> call,
                                   Response<ServRecipeResp> response) {
                Log.wtf("Upload", "success:");
                if(response.body()!=null) {
                    Log.wtf("Upload", "success:" + response.body().getRecipes());
                    /*recArrray = response.body();
                    recipeAdapter = new RecipeAdapter(getActivity(), recArrray);
                    recipeList.setAdapter(recipeAdapter);
                    recipeAdapter.notifyDataSetChanged();*/
                }
            }

            @Override
            public void onFailure(Call<ServRecipeResp> call, Throwable t) {
                Log.wtf("Upload ", t);
            }


        });
        return v;
    }


}
