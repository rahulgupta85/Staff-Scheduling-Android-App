package com.example.edutech.admin;

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
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.edutech.R;
import com.example.edutech.fragments.NotificationsFragment;
import com.example.edutech.fragments.ProfileFragment;
import com.example.edutech.admin.fragments.AdminAnnouncementsFragment;
import com.example.edutech.admin.fragments.AdminHomeFragment;
import com.example.edutech.admin.fragments.AdminStaffFragment;
import com.example.edutech.admin.fragments.ManageLeaveRequestsFragment;
import com.example.edutech.admin.fragments.ScheduleOptionsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminDashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Toolbar toolbar;
    private TextView notificationBadge;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (!isGranted) {
                            Toast.makeText(
                                    this,
                                    "You will not receive important updates.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ SAME AS TEACHER (THIS IS THE KEY)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);

        setContentView(R.layout.activity_admin_dashboard);

        // ✅ SAME AS TEACHER: WHITE STATUS BAR
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(
                    ContextCompat.getColor(this, android.R.color.white)
            );
        }

        // ✅ SAME AS TEACHER: DARK ICONS
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
            loadFragment(new AdminHomeFragment(), false);
        }

        loadAdminProfile();
        askNotificationPermission();
    }

    private void loadAdminProfile() {
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
                    fragment = new AdminHomeFragment();
                    loadAdminProfile();
                } else if (item.getItemId() == R.id.nav_staff) {
                    fragment = new AdminStaffFragment();
                    setTitle("Staff");
                } else if (item.getItemId() == R.id.nav_schedule) {
                    fragment = new ScheduleOptionsFragment();
                    setTitle("Schedule");
                } else if (item.getItemId() == R.id.nav_leave_requests) {
                    fragment = new ManageLeaveRequestsFragment();
                    setTitle("Leave Requests");
                } else if (item.getItemId() == R.id.nav_announcements) {
                    fragment = new AdminAnnouncementsFragment();
                    setTitle("Announcements");
                }

                if (fragment != null) {
                    loadFragment(fragment, false);
                }
                return true;
            };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_toolbar_menu, menu);
        MenuItem notificationItem = menu.findItem(R.id.action_notifications);
        View actionView = notificationItem.getActionView();
        notificationBadge = actionView.findViewById(R.id.notification_badge);
        actionView.setOnClickListener(v -> onOptionsItemSelected(notificationItem));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_profile) {
            loadFragment(new ProfileFragment(), true);
            setTitle("My Profile");
            return true;
        } else if (item.getItemId() == R.id.action_notifications) {
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
