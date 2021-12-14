package com.wenhao.cmpe277project;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;


public class ChatPage extends Fragment {
    private RecyclerView recyclerView;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ArrayList<String> chatIDs;
    private boolean initialSetup = true;
    private HashMap<String, Integer> unreadMap;


    public ChatPage() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        chatIDs = new ArrayList<>();
        unreadMap = new HashMap<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chat_page, container, false);
        recyclerView = view.findViewById(R.id.chatListRv);
        recyclerView.setAdapter(new ChatRowAdapter(chatIDs,getContext()));
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        //fetch chat list and set update listener
        db.collection("chats").whereEqualTo("participants." + FirebaseAuth.getInstance().getUid(),true)
                .addSnapshotListener((value, e) -> {
                    if (e != null) {
                        Log.w("myapp", "Listen failed.", e);
                        return;
                    }

                    ArrayList<String> newList = new ArrayList<>();
                    HashMap<String,Integer> newUnread = new HashMap<>();
                    String selfUid = FirebaseAuth.getInstance().getUid();
                    for (QueryDocumentSnapshot doc : value) {
                        newList.add(doc.getId());
                        HashMap<String,Object> chatUnread = (HashMap<String, Object>) doc.get("unread");
                        newUnread.put(doc.getId(),Integer.parseInt(chatUnread.get(selfUid).toString()));
                    }

                    //look for deleted chat
                    for (String id : chatIDs) {
                        if (!newList.contains(id)) {
                            int position = chatIDs.indexOf(id);
                            chatIDs.remove(position);
                            recyclerView.getAdapter().notifyItemRemoved(position);
                        }

                    }
                    


                    for (String id : newList) {
                        if (!chatIDs.contains(id)) {
                            //look for new chat
                            chatIDs.add(id);
                            unreadMap.put(id,newUnread.get(id));
                            recyclerView.getAdapter().notifyItemInserted(chatIDs.size()-1);
                        }
                        else{
                            //notify badge update if unread value changes
                            if (!unreadMap.get(id).equals(newUnread.get(id))) {
                                int position = chatIDs.indexOf(id);
                                unreadMap.put(id,newUnread.get(id));
                                recyclerView.getAdapter().notifyItemChanged(position);
                            }
                        }
                    }

                });
        return view;
    }
}