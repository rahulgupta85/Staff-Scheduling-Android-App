package com.example.edutech.admin.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.example.edutech.R;
import com.google.android.material.card.MaterialCardView;

public class ScheduleOptionsFragment extends Fragment {

    private static final String TAG = "ScheduleOptionsDebug"; // Tag for our logs

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Fragment is being created.");

        View view = inflater.inflate(R.layout.fragment_schedule_options, container, false);
        Log.d(TAG, "onCreateView: Layout has been inflated.");

        MaterialCardView createScheduleCard = view.findViewById(R.id.createScheduleCard);
        MaterialCardView viewScheduleCard = view.findViewById(R.id.viewScheduleCard);

        if (createScheduleCard == null || viewScheduleCard == null) {
            Log.e(TAG, "onCreateView: CRITICAL ERROR - Could not find one or more cards in the layout!");
        } else {
            Log.d(TAG, "onCreateView: Both cards were found successfully.");
        }

        createScheduleCard.setOnClickListener(v -> {
            Log.d(TAG, "Create Schedule card clicked.");
            loadFragment(new CreateScheduleFragment());
        });

        viewScheduleCard.setOnClickListener(v -> {
            Log.d(TAG, "View Schedule card clicked.");
            loadFragment(new ViewScheduleFragment());
        });

        return view;
    }

    private void loadFragment(Fragment fragment) {
        Log.d(TAG, "loadFragment: Attempting to load " + fragment.getClass().getSimpleName());
        FragmentManager fragmentManager = getParentFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}