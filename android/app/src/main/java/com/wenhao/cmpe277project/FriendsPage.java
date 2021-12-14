package com.wenhao.cmpe277project;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class FriendsPage extends Fragment {
    private RecyclerView recyclerView;
    private ArrayList<String> friendsList;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public FriendsPage() {
        // Required empty public constructor
    }


    public static FriendsPage newInstance(String param1, String param2) {
        FriendsPage fragment = new FriendsPage();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        friendsList = new ArrayList<>();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DocumentReference docRef = db.collection("users").document(currentUser.getUid());
            //listens for update
            docRef.addSnapshotListener((snapshot, e) -> {
                if (e != null) {
                    Log.w("myapp", "Listen failed.", e);
                    return;
                }
                //fetch data
                if (snapshot != null && snapshot.exists()) {
                    Log.d("myapp", "updating friends list");
                    updateFriendList(snapshot);

                } else {
                    Log.d("myapp", "Current data: null");
                }
            });
        }
    }

    private void updateFriendList(DocumentSnapshot document) {
        friendsList = new ArrayList<>((ArrayList<String>) document.get("friends"));
//        Log.d("myapp","fetching friend list " + friendsList.toString());
        recyclerView.setAdapter(new UserRowAdapter(friendsList,getContext()));
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_friends_page, container, false);

        //set up floating button
        FloatingActionButton fab = view.findViewById(R.id.floatingActionButton);
        fab.setOnClickListener(v -> {
            new AddFriendDialogFragment().show(getFragmentManager(),null);
        });

        recyclerView = view.findViewById(R.id.friendListRv);
        return view;
    }

    public static class AddFriendDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.fragment_input_dialog,null);
            TextInputLayout input = view.findViewById(R.id.inputTextLayout);
            input.setHint("search by email");
            view.findViewById(R.id.inputNegBtn).setOnClickListener(v -> {
                getDialog().dismiss();
            });
            view.findViewById(R.id.inputPosBtn).setOnClickListener(v -> {
//                Toast.makeText(getContext(), input.getEditText().getText().toString(), Toast.LENGTH_SHORT).show();
                RequestQueue queue = Volley.newRequestQueue(getContext());
                String url = BackendAPI.getUrl() + "/friend/" + FirebaseAuth.getInstance().getUid() + "/" + input.getEditText().getText();
                StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                        response -> {
                            getDialog().dismiss();
                        }, error -> {
                            input.setError("User not found");
                        });
                queue.add(stringRequest);
            });
            return builder.setView(view).setTitle("Add Friend").create();
        }

    }
}