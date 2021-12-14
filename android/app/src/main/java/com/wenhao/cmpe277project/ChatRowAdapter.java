package com.wenhao.cmpe277project;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

import ru.nikartm.support.ImageBadgeView;

public class ChatRowAdapter extends RecyclerView.Adapter<ChatRowAdapter.ViewHolder>{
    private ArrayList<String> chats;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private Context context;


    public ChatRowAdapter(ArrayList<String> chats,Context context) {
        this.chats = chats;
        this.context = context;

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.fragment_chat_row,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String chatId = chats.get(position);
        DocumentReference docRef = db.collection("chats").document(chatId);
        docRef.addSnapshotListener((snapshot,e)->{
            if (snapshot.exists()){
                String selfUID = FirebaseAuth.getInstance().getUid();
                HashMap<String,Object> unreadMsg = (HashMap<String, Object>) snapshot.get("unread");
//           Log.d("myapp", " unread: " + unreadMsg.toString());

                //set badge
                int unreadValue = Integer.parseInt(unreadMsg.get(selfUID).toString());
                holder.profile.setBadgeValue(unreadValue);

                ArrayList<String> participants = new ArrayList<>(((HashMap<String, Object>) snapshot.get("participants")).keySet());
                String uid = participants.get(0).equals(selfUID) ? participants.get(1) : participants.get(0);

                //fetch profile
                FirebaseFunctions.updateProfile(uid,holder.profile,context);

                //fetch name
                FirebaseFunctions.updateDisplayName(uid,holder.name,context);

                //display last message
                ArrayList<Object> messages = (ArrayList<Object>) snapshot.get("messages");
                if (messages.size() > 0) {
                    HashMap<String,Object> lastMsg = (HashMap<String,Object>) messages.get(messages.size()-1);

                    if (lastMsg.get("type").toString().equals("photo")) {
                        holder.lastMsg.setText("[photo]");
                    }
                    else if (lastMsg.get("type").toString().equals("voice")){
                        holder.lastMsg.setText("[voice]");
                    }
                    else{
                        try {
                            holder.lastMsg.setText(URLDecoder.decode(lastMsg.get("data").toString(), StandardCharsets.UTF_8.name()));
                        } catch (UnsupportedEncodingException unsupportedEncodingException) {
                            unsupportedEncodingException.printStackTrace();
                        }
                    }
                }
                else holder.lastMsg.setText("");
            }

        });

        holder.cardView.setOnClickListener(v -> {
//            Toast.makeText(context, chatId, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(context,ChatDetail.class);
            intent.putExtra("chatID",chatId);
            ((Activity)context).startActivityForResult(intent,1);
        });

        holder.deleteBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(context).setTitle("Delete")
                    .setMessage("Are you sure to delete this chat?")
                    .setNegativeButton("Cancel", (dialog, which) -> {
                    })
                    .setPositiveButton("Delete", (dialog, which) -> {
                        FirebaseFunctions.deleteChat(chatId);
                    }).show();

        });
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageBadgeView profile;
        public TextView name;
        public TextView lastMsg;
        public ImageButton deleteBtn;
        public boolean initialSetup = true;
        public MaterialCardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            profile = itemView.findViewById(R.id.chatRowProfile);
            name = itemView.findViewById(R.id.chatRowName);
            lastMsg = itemView.findViewById(R.id.chatRowLastMsg);
            deleteBtn = itemView.findViewById(R.id.chatRowDeleteBtn);
            cardView = itemView.findViewById(R.id.chatRowCV);
        }
    }
}
