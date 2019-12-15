package com.example.photojor;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.photojor.model.Ingredient;
import com.example.photojor.model.Recipe;

import java.util.List;

public class RecipeAdapter extends android.widget.ArrayAdapter<Recipe> {
    public RecipeAdapter(@NonNull Context context, List list) {
        super(context, R.layout.ingredient_item, list);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Recipe recipe = getItem(position);

        if(convertView==null)
        {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.recipe_item,null);
        }
        TextView recipeName = convertView.findViewById(R.id.recipe_name);
        recipeName.setText(recipe.getName());

        ImageView recipeImage = convertView.findViewById(R.id.recipe_image);
        recipeImage.setImageBitmap(recipe.getImage());
        return convertView;
    }
}
