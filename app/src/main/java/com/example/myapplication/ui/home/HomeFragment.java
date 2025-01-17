package com.example.myapplication.ui.home;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.R;
import com.example.myapplication.databinding.FragmentHomeBinding;
import com.example.myapplication.exercise.ExerciseRecommender;
import com.example.myapplication.services.HydrationService;
import com.example.myapplication.sleep.SleepRecommender;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

// Home fragment = home page
public class HomeFragment extends Fragment {

private FragmentHomeBinding binding;

    FirebaseAuth auth;
    TextView textViewHomeTitle, recommendedSleep, yesterdaysSleep, hydrationSatisfied, recommendedExercise, exerciseSatisfied;
    FirebaseUser user;
    String userID;
    FirebaseFirestore firestore;
    private HydrationService hydrationService;
    private boolean hydrationBound = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        textViewHomeTitle = binding.homeWelcome;
        recommendedSleep = binding.homePlaceholder1;
        yesterdaysSleep = binding.homePlaceholder2;
        hydrationSatisfied = binding.homePlaceholder4;
        recommendedExercise = binding.homePlaceholder5;
        exerciseSatisfied = binding.homePlaceholder6;

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        userID = user.getUid();
        firestore = FirebaseFirestore.getInstance();
        DocumentReference documentReference = firestore.collection("users").document(userID);

        // Update home page with information from user's account in Firestore database and display calculated recommendations
        documentReference.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (getActivity() != null && documentSnapshot != null) {
                    // Personalize title comment of home page with user's first name
                    textViewHomeTitle.setText(getString(R.string.home_intro, documentSnapshot.getString("firstName")));

                    // Recommended sleep and yesterday's sleep
                    int yesterdaysSleepHours = Math.toIntExact((((List<Long>)documentSnapshot.get("sleepHistory")).get(1)));
                    SleepRecommender sleepRecommender = new SleepRecommender(Math.toIntExact((Long)(documentSnapshot.get("age"))), yesterdaysSleepHours);
                    recommendedSleep.setText(getString(R.string.home_placeholder_rec1, Integer.toString(sleepRecommender.sleepAmountRecommender())));
                    yesterdaysSleep.setText(getString(R.string.home_placeholder_rec1, Integer.toString(yesterdaysSleepHours)));

                    // Hydration status
                    boolean hydratedToday = documentSnapshot.getBoolean("hydratedToday");
                    if (hydratedToday)
                        hydrationSatisfied.setText("Complete");

                    // Recommended exercise amount
                    ExerciseRecommender exerciseRecommender = new ExerciseRecommender(Math.toIntExact((Long)(documentSnapshot.get("age"))));
                    recommendedExercise.setText(getString(R.string.home_placeholder_rec3, exerciseRecommender.exerciseAmountRecommender()));

                    // Exercise status
                    boolean exercisedToday = documentSnapshot.getBoolean("exercisedToday");
                    if (exercisedToday)
                        exerciseSatisfied.setText("Complete");


                }
            }
        });

        final TextView textView = binding.textHome;
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        Intent serviceIntent = new Intent(getActivity(), HydrationService.class);
        getActivity().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (hydrationBound) {
            getActivity().unbindService(serviceConnection);
            hydrationBound = false;
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.d("MainActivity", "Hydration Service connected.");
            HydrationService.HydrationBinder hydrationBinder = (HydrationService.HydrationBinder) service;
            hydrationService = hydrationBinder.getService();
            hydrationBound = true;

            CompletableFuture<Double> hydrationRecommendation = hydrationService.getHydrationRecommendation();
            hydrationRecommendation.thenAccept(totalIntake -> {
                requireActivity().runOnUiThread(() -> {
                    String formattedText = String.format(Locale.getDefault(), "%.0f Cups", totalIntake);
                    binding.hydrationRecommendationValue.setText(formattedText);
                });
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            hydrationBound = false;
        }
    };
}