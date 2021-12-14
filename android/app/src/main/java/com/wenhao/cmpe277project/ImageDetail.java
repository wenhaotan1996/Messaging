package com.wenhao.cmpe277project;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

import com.github.chrisbanes.photoview.PhotoView;

public class ImageDetail extends AppCompatActivity {
    private String photoId, chatId;
    private PhotoView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_detail);
        //bind ui view
        imageView = findViewById(R.id.imageDetail);

        //fetch intent data
        chatId = getIntent().getStringExtra("chatId");
        photoId = getIntent().getStringExtra("photoId");
        //get image
        if (!chatId.isEmpty() && !photoId.isEmpty()) FirebaseFunctions.updatePhotoView(chatId,photoId,imageView,this);
        //setup action bar
        ActionBar actionBar = getSupportActionBar();
       actionBar.setDisplayShowTitleEnabled(false);
       actionBar.setDisplayHomeAsUpEnabled(true);

    }



    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //return if home pressed
        if (item.getItemId() == android.R.id.home) finish();
        return super.onOptionsItemSelected(item);
    }

}