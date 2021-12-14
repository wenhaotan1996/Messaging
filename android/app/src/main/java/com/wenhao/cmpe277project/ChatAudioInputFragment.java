package com.wenhao.cmpe277project;

import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;

import java.io.IOException;
import java.util.UUID;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class ChatAudioInputFragment extends Fragment {
    private DataTransfer dataTransfer;
    private MediaRecorder recorder;
    private String fileName;
    private boolean recording = false;
    private String id;

    public ChatAudioInputFragment(DataTransfer dataTransfer) {
        this.dataTransfer = dataTransfer;
    }

    public static ChatAudioInputFragment newInstance(DataTransfer dataTransfer) {
        Bundle args = new Bundle();
        ChatAudioInputFragment fragment = new ChatAudioInputFragment(dataTransfer);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_input_audio,container,false);
        MaterialCardView cardView = view.findViewById(R.id.inputAudio);
        cardView.setOnLongClickListener(v -> {
            Log.d("myapp", "should start audio record");
            startRecord();
            return true;
        });
        cardView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP && recording){
                    Log.d("myapp","should stop recording");
                    stopRecord();
                    dataTransfer.sendAudioMessage(fileName);
                }

                return false;
            }
        });

        return view;
    }

    private void startRecord() {
        Log.d("myapp","trying to start recording");
        if (!checkPermissions()) {
            requestPermissions();
        }
        else{
            recording = true;
            id = UUID.randomUUID().toString();
            fileName = getActivity().getExternalCacheDir().getAbsolutePath();
            fileName += "/" + id + ".mp3";
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(fileName);

            try {
                recorder.prepare();
            } catch (IOException e) {
                Log.e("myapp", "prepare() failed: " + e.getMessage());
            }
            recorder.start();
        }

    }

    private void stopRecord() {
        if (recording) {
            recording = false;
            recorder.stop();
            recorder.release();
            recorder = null;

        }
    }

    private boolean checkPermissions() {
        int result = ContextCompat.checkSelfPermission(getContext(), WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getContext(), RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }
    private void requestPermissions() {
        ActivityCompat.requestPermissions(getActivity(), new String[]{RECORD_AUDIO, WRITE_EXTERNAL_STORAGE}, 1);
    }

    private void startPlaying(String filePath) {
        MediaPlayer player = new MediaPlayer();
        try {
            Log.d("myapp","try to start playing");
            player.setDataSource(filePath);
            player.prepare();
            player.start();
        } catch (IOException e) {
            Log.e("myapp", "prepare() failed: " + e.getMessage());
        }
    }
}
