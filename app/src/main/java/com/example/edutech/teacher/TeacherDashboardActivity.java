package com.example.edutech.teacher;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.edutech.Notification;
import com.example.edutech.R;
import com.example.edutech.fragments.NotificationsFragment;
import com.example.edutech.fragments.ProfileFragment;
import com.example.edutech.teacher.fragments.TeacherHomeFragment;
import com.example.edutech.teacher.fragments.ViewAnnouncementsFragment;
import com.example.edutech.teacher.fragments.ViewMyLeaveRequestsFragment;
import com.example.edutech.teacher.fragments.ViewScheduleFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.LinkedList;
import java.util.Queue;

public class TeacherDashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Toolbar toolbar;
    private TextView notificationBadge;
    private Queue<Pair<String, Notification>> unreadNotificationsQueue = new LinkedList<>();

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this,
                            "You will not receive important push notifications.",
                            Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Disable edge-to-edge (IMPORTANT)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);

        setContentView(R.layout.activity_teacher_dashboard);

        // Status bar background = WHITE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(
                    ContextCompat.getColor(this, android.R.color.white)
            );
        }

        // Status bar icons = DARK (so they are visible)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            );
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(navListener);

        if (savedInstanceState == null) {
            loadFragment(new TeacherHomeFragment(), false);
        }

        loadTeacherProfile();
        askNotificationPermission();
    }

    private void loadTeacherProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String firstName = doc.getString("firstName");
                            if (firstName != null) {
                                toolbar.setTitle("Hello, " + firstName + "!");
                            }
                        }
                    });
        }
    }

    private final BottomNavigationView.OnItemSelectedListener navListener =
            item -> {
                Fragment fragment = null;

                if (item.getItemId() == R.id.nav_home) {
                    fragment = new TeacherHomeFragment();
                    loadTeacherProfile();
                } else if (item.getItemId() == R.id.nav_schedule) {
                    fragment = new ViewScheduleFragment();
                    setTitle("My Schedule");
                } else if (item.getItemId() == R.id.nav_leave) {
                    fragment = new ViewMyLeaveRequestsFragment();
                    setTitle("Leave Requests");
                } else if (item.getItemId() == R.id.nav_announcements) {
                    fragment = new ViewAnnouncementsFragment();
                    setTitle("Announcements");
                } else if (item.getItemId() == R.id.nav_profile) {
                    fragment = new ProfileFragment();
                    setTitle("My Profile");
                }

                if (fragment != null) {
                    loadFragment(fragment, false);
                }
                return true;
            };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_toolbar_menu_teacher, menu);
        MenuItem notificationItem = menu.findItem(R.id.action_notifications);
        View actionView = notificationItem.getActionView();
        notificationBadge = actionView.findViewById(R.id.notification_badge);
        actionView.setOnClickListener(v -> onOptionsItemSelected(notificationItem));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_notifications) {
            loadFragment(new NotificationsFragment(), true);
            setTitle("Notifications");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void loadFragment(Fragment fragment, boolean addToBackStack) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.fragment_container, fragment);
        if (addToBackStack) {
            ft.addToBackStack(null);
        }
        ft.commit();
    }
}
