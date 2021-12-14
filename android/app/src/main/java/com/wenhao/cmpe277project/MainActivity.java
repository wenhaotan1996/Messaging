package com.wenhao.cmpe277project;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

//    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ViewPager2 viewPager2;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().hide();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser==null) {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
        }
        else{
            //register device token
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
               if (task.isSuccessful()) {
                   FirebaseFunctions.sendDeviceToken(currentUser.getUid(),task.getResult());
                   Log.d("token",task.getResult());
                   return;
               }
               else{
                   Log.d("myapp", "failed fetching device token");
               }
            });



            viewPager2 = findViewById(R.id.viewPager2);
            viewPager2.setAdapter(new ViewPager2Adapter(this));
            viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    switch (position) {
                        case 0:
                            bottomNavigationView.setSelectedItemId(R.id.menuChat);
                            break;
                        case 1:
                            bottomNavigationView.setSelectedItemId(R.id.menuFriends);
                            break;
                        case 2:
                            bottomNavigationView.setSelectedItemId(R.id.menuSettings);
                            break;
                    }
                }
            });

            bottomNavigationView = findViewById(R.id.bottomNav);
            bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
                switch (item.getItemId()) {
                    case R.id.menuChat:
                        viewPager2.setCurrentItem(0);
                        break;
                    case R.id.menuFriends:
                        viewPager2.setCurrentItem(1);
                        break;
                    case R.id.menuSettings:
                        viewPager2.setCurrentItem(2);
                        break;
                }
                return true;
            });
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode== RESULT_OK) {

            String chatID = data.getStringExtra("chatID");
            if (!chatID.isEmpty()) FirebaseFunctions.setChatAsRead(chatID,this);
            viewPager2.setCurrentItem(0);
            bottomNavigationView.setSelectedItemId(R.id.menuChat);
        }
    }

    private static class ViewPager2Adapter extends FragmentStateAdapter {

        public ViewPager2Adapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 1: return new FriendsPage();
                case 2: return new SettingsPage();
                default: return new ChatPage();
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }

}