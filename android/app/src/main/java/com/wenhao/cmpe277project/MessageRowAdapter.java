package com.wenhao.cmpe277project;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;
import java.util.ArrayList;

public class MessageRowAdapter extends RecyclerView.Adapter<MessageRowAdapter.ViewHolder> {

    private ArrayList<Message> messages;
    private Context context;
    private String chatId;

    public MessageRowAdapter(ArrayList<Message> messages, Context context, String chatId) {
        this.messages = messages;
        this.context = context;
        this.chatId = chatId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        switch (viewType){
            case 1: return new ViewHolder(inflater.inflate(R.layout.fragment_message_self_photo,parent,false));
            case 2: return new ViewHolder(inflater.inflate(R.layout.fragment_message_self_audio,parent,false));
            case 4: return new ViewHolder(inflater.inflate(R.layout.fragment_message_other_text,parent,false));
            case 5: return new ViewHolder(inflater.inflate(R.layout.fragment_message_other_photo,parent,false));
            case 6: return new ViewHolder(inflater.inflate(R.layout.fragment_message_other_audio,parent,false));
            default: return new ViewHolder(inflater.inflate(R.layout.fragment_message_self_text,parent,false));
        }
    }

    @Override
    public int getItemViewType(int position) {
        String selfUid = FirebaseAuth.getInstance().getUid();
        String uid = messages.get(position).getFrom();
        String type = messages.get(position).getType();
        if (uid.equals(selfUid)){
            if (type.equals("text")) return 0;
            else if (type.equals("photo")) return 1;
            else if (type.equals("voice")) return 2;
            return 0;
        }
        else{
            if (type.equals("text")) return 4;
            else if (type.equals("photo")) return 5;
            else if (type.equals("voice")) return 6;
            return 4;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
//        Log.d("myapp", "binding view for position " + position);
        Message message = messages.get(position);
        FirebaseFunctions.updateProfile(message.getFrom(),holder.profile,context);
        if (message.getType().equals("voice")) {
            holder.voiceBox.setOnClickListener(v -> {
                FirebaseFunctions.playAudioMessage(chatId,message.getData(),context);
            });
        }
        else if (message.getType().equals("photo")) {
            FirebaseFunctions.updatePhotoView(chatId, message.getData(), holder.photo, context);
            holder.photo.setOnClickListener(v -> {
                Intent intent = new Intent(context,ImageDetail.class);
                intent.putExtra("chatId",chatId);
                intent.putExtra("photoId",message.getData());
                context.startActivity(intent);
            });
        }
        else holder.textView.setText(message.getData());
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView profile;
        public TextView textView;
        public ImageView photo;
        public MaterialCardView voiceBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            profile = itemView.findViewById(R.id.messageProfile);
            textView = itemView.findViewById(R.id.messageText);
            photo = itemView.findViewById(R.id.messagePhoto);
            voiceBox = itemView.findViewById(R.id.messageAudio);
        }
    }

}
