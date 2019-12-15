package com.example.photojor.model;

import android.graphics.Bitmap;
import android.media.Image;

import java.net.URI;

public class Ingredient {
    String name;
    int id;
    Bitmap image;

    String saveBit;
    public Ingredient(String name,Bitmap im)

    {
        this.name=name;
        image=im;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public Bitmap getImage() {
        return image;
    }

    public void setSaveBit(String saveBit) {
        this.saveBit = saveBit;
    }

    public String getSaveBit() {
        return saveBit;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
