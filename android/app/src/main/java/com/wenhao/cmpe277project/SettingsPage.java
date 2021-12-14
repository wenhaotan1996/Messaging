package com.wenhao.cmpe277project;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;


public class SettingsPage extends Fragment {
    private ImageView profilePic;
    private TextView displayName;
    private RecyclerView recyclerView;
    private int PICK_IMAGE = 456;

    public SettingsPage() {
        // Required empty public constructor
    }


    // TODO: Rename and change types and number of parameters
    public static SettingsPage newInstance(String param1, String param2) {
        SettingsPage fragment = new SettingsPage();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings_page, container, false);

        view.findViewById(R.id.settingLogout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getActivity(),SignInActivity.class));
            getActivity().finish();
        });

        view.findViewById(R.id.settingEditProfile).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);

        });

        view.findViewById(R.id.settingEditName).setOnClickListener(v -> {
            new EditNameDialogFragment(this).show(getChildFragmentManager(),null);
        });
        profilePic = view.findViewById(R.id.settingProfilePic);
        displayName = view.findViewById(R.id.settingDisplayName);
        updateUserProfile();
        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();
            StorageReference imageRef = storageRef.child("profiles/" + currentUser.getUid());
            UploadTask uploadTask = imageRef.putFile(data.getData());
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                updateUserProfile();
            }).addOnFailureListener(e -> {
                Snackbar.make(getActivity(),null,"Failed to upload profile picture",Snackbar.LENGTH_LONG).show();
            });

        }
    }

    private void updateUserProfile() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFunctions.updateProfile(FirebaseAuth.getInstance().getUid(),profilePic,getContext());
        if (currentUser.getDisplayName() != null) displayName.setText(currentUser.getDisplayName());
    }

    public static class EditNameDialogFragment extends DialogFragment {

        private SettingsPage parentView;

        public EditNameDialogFragment(SettingsPage parentView) {
            this.parentView = parentView;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            View dialogFragment = getActivity().getLayoutInflater().inflate(R.layout.fragment_input_dialog,null);
            dialogFragment.findViewById(R.id.inputNegBtn).setOnClickListener(v -> {
                getDialog().dismiss();
            });
            Button posBtn = dialogFragment.findViewById(R.id.inputPosBtn);
            TextInputLayout input = dialogFragment.findViewById(R.id.inputTextLayout);
            input.setHint("");
            posBtn.setText("set");
            posBtn.setOnClickListener(v -> {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                currentUser.updateProfile(new UserProfileChangeRequest.Builder()
                        .setDisplayName(input.getEditText().getText().toString()).build())
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) parentView.updateUserProfile();
                            else Snackbar.make(getContext(),null,"Failed to update display name",Snackbar.LENGTH_LONG).show();
                            getDialog().dismiss();
                        });
            });
            return builder.setView(dialogFragment)
                    .setTitle("Edit Display Name")
                    .create();
        }
    }
}