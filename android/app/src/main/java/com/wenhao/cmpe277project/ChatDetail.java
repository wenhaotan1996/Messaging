package com.wenhao.cmpe277project;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.PopupMenu;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

public class ChatDetail extends AppCompatActivity implements DataTransfer {

    private String chatID;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String TAG = "chatDetail";
    private ArrayList<Message> messages;
    private RecyclerView recyclerView;
    private boolean textInput = false;
    private int PICK_IMAGE = 456;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail);

        //bind ui views
        recyclerView = findViewById(R.id.chatDetailRv);
        ImageButton add = findViewById(R.id.chatDetailAddBtn);

        //display up button
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        //set chat id
        chatID = getIntent().getStringExtra("chatID");

        //set chat id to block notification for current chat
        MyFirebaseInstanceIDService.currentChat = chatID;

        //default text input
        switchInputType();

        // bind popup menu to add button
        add.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(getApplicationContext(),v);
            MenuInflater inflater = popupMenu.getMenuInflater();
            inflater.inflate(R.menu.chat_popup_menu,popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(item -> {
                //switch to audio input
                if (item.getItemId() == R.id.chatPopupAudio) switchInputType();
                //open select photo intent
                else if (item.getItemId() == R.id.chatPopupPhoto) {
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
                }
                return true;
            });
            popupMenu.show();
        });

        //initialize recyclerview with no data
        messages = new ArrayList<>();
        recyclerView.setAdapter(new MessageRowAdapter(messages,this,chatID));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //fetch chat data from firebase nad add listener
        DocumentReference docRef = db.collection("chats").document(chatID);
        docRef.addSnapshotListener((snapshot,e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed.", e);
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                //display names on app bar except current user's
                actionBar.setTitle("");
                HashMap<String,Object> participants = (HashMap<String, Object>) snapshot.get("participants");
                String selfUID = FirebaseAuth.getInstance().getUid();
                for (String id : participants.keySet()) {
                    if (!id.equals(selfUID)) {
                        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
                        String url = BackendAPI.getUrl() + "/name/" + id;
                        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, response -> {
                            if (actionBar.getTitle().toString().isEmpty()) {
                                actionBar.setTitle(response);
                            }
                            else{
                                actionBar.setTitle(actionBar.getTitle() + "," + response);
                            }
                        }, error -> {
                            Log.d("myapp", "failed fetching user name for " + id);
                        });
                        queue.add(stringRequest);
                    }
                }

                //fetch message list
                ArrayList<Object> messageObjList = (ArrayList<Object>) snapshot.getData().get("messages");
                ArrayList<Message> newMessages = new ArrayList<>();
                for (Object m : messageObjList) {
                    HashMap<String,Object> messageObj = (HashMap<String,Object>) m;
                    String data = null;
                    try {
                        data = URLDecoder.decode(messageObj.get("data").toString(), StandardCharsets.UTF_8.name());
                    } catch (UnsupportedEncodingException unsupportedEncodingException) {
                        unsupportedEncodingException.printStackTrace();
                    }
                    String from = messageObj.get("from").toString();
                    String id = messageObj.get("id").toString();
                    String type = messageObj.get("type").toString();
                    newMessages.add(new Message(id,type,data,from));
                }

                //look for new message and notify adapter
                for (Message m : newMessages) {
                    if (!messages.contains(m)) {
                        messages.add(m);
                        recyclerView.getAdapter().notifyItemInserted(messages.size()-1);
                    }
                }

                //scroll to bottom if more than 0 message
                if (recyclerView.getAdapter().getItemCount() > 0)
                    recyclerView.smoothScrollToPosition(recyclerView.getAdapter().getItemCount()-1);

            } else {
                //chat is deleted, return to main activity
                Intent intent = new Intent();
                intent.putExtra("chatID","");
                setResult(RESULT_OK,intent);
                finish();
            }
        });

        //detect if keyboard is open and scroll to bottom
        recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            private int mPreviousHeight;
            @Override
            public void onGlobalLayout() {
                int newHeight = recyclerView.getHeight();
                if (mPreviousHeight != 0 && mPreviousHeight > newHeight && recyclerView.getAdapter().getItemCount() > 0) {
                    // Height decreased: keyboard was shown
                    recyclerView.smoothScrollToPosition(recyclerView.getAdapter().getItemCount() - 1);
                }
                mPreviousHeight = newHeight;
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            //return to main activity and pass chatID
            Intent intent = new Intent();
            intent.putExtra("chatID",chatID);
            setResult(RESULT_OK,intent);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        //return to main activity and pass chatID
        Intent intent = new Intent();
        intent.putExtra("chatID",chatID);
        setResult(RESULT_OK,intent);
        finish();
    }

    private void switchInputType() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (textInput) {
            ft.replace(R.id.chatDetailInput, ChatAudioInputFragment.newInstance(this)).commit();
        }
        else {
            ft.replace(R.id.chatDetailInput, ChatTextInputFragment.newInstance(getApplicationContext(),this)).commit();

        }
        textInput = !textInput;
    }


    @Override
    public void sendTextMessage(String string) {
        String uid = FirebaseAuth.getInstance().getUid();
        FirebaseFunctions.sendMessage(chatID,uid,"text",string,getApplicationContext());
    }

    @Override
    public void sendAudioMessage(String filePath) {
        FirebaseFunctions.sendAudioMessage(filePath,chatID,getApplicationContext());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            FirebaseFunctions.uploadPhotoMessage(chatID,data,getApplicationContext());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MyFirebaseInstanceIDService.currentChat = "";
    }
}