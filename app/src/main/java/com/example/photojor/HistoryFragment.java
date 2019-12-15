package com.example.photojor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import com.example.photojor.Utils.Consts;
import com.example.photojor.model.Ingredient;
import com.example.photojor.model.ServIngrResponse;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class HistoryFragment extends Fragment {
    View v;
    IngAdapter adapter;
    ListView ingList;
    ArrayList<String> ingrArr;

    ServIngrResponse response;
    Bitmap newImg;
    Ingredient ing;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        v= inflater.inflate(R.layout.fragment_history, container, false);

            ingrArr = new ArrayList<>();
            adapter = new IngAdapter(getActivity(), ingrArr);
            ingList = v.findViewById(R.id.ingrList);
            ingList.setAdapter(adapter);
            if(!Consts.isHistoryLaunched)
            loadFridge();
            Log.wtf("Create", " history");

        RefreshFridge();
        //adapter.addAll(Consts.fridge);

        Consts.isHistoryLaunched=true;
        return  v;
    }


    @Override
    public void onResume() {
        //RefreshFridge();
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        saveFridge();
    }

    public void RefreshFridge()
    {
        adapter.clear();
        Log.w("Fridge size: ",""+Consts.fridge.size());
        adapter.addAll(Consts.fridge);
        adapter.notifyDataSetChanged();
    }

    void loadFridge() {
        Log.wtf("go to ","fridge");
        if (Consts.sp.contains(Consts.APP_PREFERENCES_INGREDIENTS)) {
            Log.wtf("Load ","fridge");
            Set<String> set = Consts.sp.getStringSet(Consts.APP_PREFERENCES_INGREDIENTS, null);
            //ArrayList<Chat> tAr = new ArrayList<Chat>();
            for (String s : set) {
                Ingredient temp = Consts.gson.fromJson(s, Ingredient.class);
                String h=new String();
                byte[] imageAsBytes = Base64.decode(temp.getSaveBit().getBytes(),Base64.DEFAULT);
                temp.setImage(BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length));
                Log.w("ingName: ",temp.getName());
                Consts.fridge.add(temp);
                //tAr.add(temp);
                //chatsAr = tAr;

            }
            RefreshFridge();
        }
    }
    void saveFridge() {
        Set<String> set = new HashSet<>();
        for (int i = 0; i < adapter.getCount(); i++) {
            Ingredient ing=adapter.getItem(i);
            if(ing.getImage()!=null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ing.getImage().compress(Bitmap.CompressFormat.PNG, 100, baos); //bm is the bitmap object
                byte[] b = baos.toByteArray();
                ing.setSaveBit(Base64.encodeToString(b, Base64.DEFAULT));
            }
            //ing.setImage(null);
            String temp = Consts.gson.toJson(ing);
            Log.w("ingName: ",temp);
            set.add(temp);
        }

        Consts.editor.putStringSet(Consts.APP_PREFERENCES_INGREDIENTS, set);
        Consts.editor.apply();
    }
}
