package com.wenhao.cmpe277project;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputLayout;

public class ChatTextInputFragment extends Fragment {

    private DataTransfer dataTransfer;
    private Context context;

    public ChatTextInputFragment(Context context, DataTransfer dataTransfer) {
        this.dataTransfer = dataTransfer;
        this.context = context;
    }

    public static ChatTextInputFragment newInstance(Context context, DataTransfer dataTransfer) {
        Bundle args = new Bundle();
        ChatTextInputFragment fragment = new ChatTextInputFragment(context, dataTransfer);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_input_text,container,false);
        TextInputLayout input = view.findViewById(R.id.inputText);
        input.getEditText().setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                //pass message to parent and clear input
                dataTransfer.sendTextMessage(v.getText().toString());
                v.setText("");
                return true;
            }
            return false;
        });
        return view;
    }
}
