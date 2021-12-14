package com.wenhao.cmpe277project;

import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.JsonArray;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class FirebaseFunctions {

    public static void updateProfile(String uid, ImageView imageView, Context context) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference imageRef = storageRef.child("profiles/" + uid);
        imageRef.getDownloadUrl().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                GlideApp.with(context).load(imageRef).into(imageView);
            }
            else {

            }
        });
    }

    public static void updateDisplayName(String uid, TextView textView, Context context) {

        RequestQueue queue = Volley.newRequestQueue(context);
        String url = BackendAPI.getUrl() + "/name/" + uid;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, response -> {
            textView.setText(response);
        }, error -> {
            Log.d("myapp", "failed fetching user name for " + uid);
        });
        queue.add(stringRequest);
    }

    public static void sendMessage(String chatID, String uid, String type, String data, Context context) {
        RequestQueue queue = Volley.newRequestQueue(context);
        String url = BackendAPI.getUrl() + "/message/" + chatID + "/" + uid + "/" + type + "/" + URLEncoder.encode(data);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, response -> {

        }, error -> {
            Log.d("myapp", "failed sending new message for " + chatID);
        });
        queue.add(stringRequest);
    }

    public static void uploadPhotoMessage(String chatID, Intent data, Context context) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        String uid = FirebaseAuth.getInstance().getUid();
        UUID id = UUID.randomUUID();
        StorageReference imageRef = storageRef.child("chats/" + chatID + "/" + id.toString());
        UploadTask uploadTask = imageRef.putFile(data.getData());
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            //upload success
            sendMessage(chatID,uid,"photo",id.toString(),context);
        }).addOnFailureListener(e -> {
            Snackbar.make(context,null,"Failed to upload profile picture",Snackbar.LENGTH_LONG).show();
        });
    }

    public static void updatePhotoView(String chatID, String photoID, ImageView imageView, Context context) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference imageRef = storageRef.child("chats/" + chatID + "/" + photoID);
        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            GlideApp.with(context).load(imageRef).into(imageView);
        });

    }

    public static void setChatAsRead(String chatId, Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("chats").document(chatId);
        String uid = FirebaseAuth.getInstance().getUid();
        docRef.update("unread." + uid, 0);
    }

    public static void deleteChat(String chatID) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("chats").document(chatID);
        docRef.delete();
    }

    public static void sendDeviceToken(String uid, String token) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("tokens").document(uid);
        HashMap<String,Object> docData = new HashMap<>();
        docData.put("token",token);
        docRef.set(docData).addOnFailureListener(e -> {
           Log.d("myapp","failed to send device token");
        });
    }


    public static void sendAudioMessage(String filePath, String chatID, Context context){
        String uid = FirebaseAuth.getInstance().getUid();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        String audioId = UUID.randomUUID().toString();
        StorageReference audioRef = storageRef.child("chats/" + chatID + "/" + audioId);
        Uri data = Uri.fromFile(new File(filePath));
        UploadTask uploadTask = audioRef.putFile(data);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            //upload success
            sendMessage(chatID,uid,"voice",audioId,context);
        }).addOnFailureListener(e -> {
            Snackbar.make(context,null,"Failed to upload profile picture",Snackbar.LENGTH_LONG).show();
        });
    }

    public static void playAudioMessage(String chatID, String audioId, Context context) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference audioRef = storageRef.child("chats/" + chatID + "/" + audioId);
        audioRef.getDownloadUrl().addOnSuccessListener(uri -> {
            MediaPlayer player = new MediaPlayer();
            try {
                Log.d("myapp","try to start playing");
                player.setDataSource(context,uri);
                player.prepare();
                player.start();
            } catch (IOException e) {
                Log.e("myapp", "prepare() failed: " + e.getMessage());
            }
        });
    }

    public static void openImageDetail(String chatId, String photoId, Context context) {
        Intent intent = new Intent(context,ImageDetail.class);
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference imageRef = storageRef.child("chats/" + chatId + "/" + photoId);
        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            intent.putExtra("uri",uri.toString());
            context.startActivity(intent);
        });
    }
}

