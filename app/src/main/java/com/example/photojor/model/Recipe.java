package com.example.photojor.model;

import android.graphics.Bitmap;

import java.util.ArrayList;

public class Recipe {
    String name;
    Bitmap image;
    ArrayList<Step> steps;
    public Recipe() {

    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSteps(ArrayList<Step> steps) {
        this.steps = steps;
    }

    public Bitmap getImage() {
        return image;
    }

    public String getName() {
        return name;
    }

    public ArrayList<Step> getSteps() {
        return steps;
    }
}

