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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;

public class UserRowAdapter extends RecyclerView.Adapter<UserRowAdapter.ViewHolder> {
    private ArrayList<String> friends;
    private Context context;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public UserRowAdapter(ArrayList<String> friends, Context context) {
        this.friends = friends;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.fragment_user_row,parent,false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String uid = friends.get(position);
        String userUid = FirebaseAuth.getInstance().getUid();
        String friendUid = friends.get(position);

        //fetching profile
        FirebaseFunctions.updateProfile(uid,holder.profile,context);

        //fetching display name
        FirebaseFunctions.updateDisplayName(uid,holder.name,context);

        holder.deleteBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(context).setTitle("Delete").setMessage("Are you sure to remove this friend from your list")
                    .setNegativeButton("Cancel", (dialog, which) -> {

                    })
                    .setPositiveButton("Delete", (dialog, which) -> {
                        DocumentReference docRef = db.collection("users").document(userUid);
                        docRef.update("friends", FieldValue.arrayRemove(friendUid));

                        DocumentReference friendRef = db.collection("users").document(uid);
                        friendRef.update("friends", FieldValue.arrayRemove(userUid));
                    }).show();

        });

        holder.card.setOnClickListener(v -> {
            db.collection("chats")
                    .whereEqualTo("participants." + userUid, true)
                    .whereEqualTo("participants." + friendUid, true)
                    .whereEqualTo("groupSize",2)
                    .get().addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            //if chat exists, open chat with old id
                            Log.d("myapp","loading old chat");
                            String chatId = queryDocumentSnapshots.getDocuments().get(0).getId();
                            Intent intent = new Intent(context,ChatDetail.class);
                            intent.putExtra("chatID",chatId);
                            ((Activity)context).startActivityForResult(intent,1);
                        }
                        else{
                            //new chat
                            Log.d("myapp", "starting new chat");
                            HashMap<String, Object> participants = new HashMap<>();
                            participants.put(userUid,true);
                            participants.put(friendUid,true);
                            ArrayList<Object> messages = new ArrayList<>();
                            HashMap<String,Object> unread = new HashMap<>();
                            unread.put(userUid,0);
                            unread.put(friendUid,0);
                            HashMap<String, Object> docData = new HashMap<>();
                            docData.put("participants",participants);
                            docData.put("messages",messages);
                            docData.put("unread",unread);
                            docData.put("groupSize",2);
                            db.collection("chats").add(docData).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                @Override
                                public void onSuccess(DocumentReference documentReference) {
                                    String chatId = documentReference.getId();
                                    Intent intent = new Intent(context,ChatDetail.class);
                                    intent.putExtra("chatID",chatId);
                                    ((Activity)context).startActivityForResult(intent,1);
                                }
                            });

                        }
                    });
        });

    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        public TextView name;
        public ImageView profile;
        public ImageButton deleteBtn;
        public MaterialCardView card;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.friendRowName);
            profile = itemView.findViewById(R.id.friendRowProfile);
            deleteBtn = itemView.findViewById(R.id.friendRowDeleteBtn);
            card = itemView.findViewById(R.id.friendRow);
        }
    }
}
