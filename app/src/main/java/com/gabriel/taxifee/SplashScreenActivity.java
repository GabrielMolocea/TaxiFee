package com.gabriel.taxifee;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthMethodPickerLayout;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.gabriel.taxifee.Utils.UserUtils;
import com.gabriel.taxifee.model.DriverInfoModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class SplashScreenActivity extends AppCompatActivity {

    private static final int LOGIN_REQUEST_CODE = 7171;
    private List<AuthUI.IdpConfig> providers;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener listener;

    // initialization of firebase database
    FirebaseDatabase database;
    DatabaseReference driverInfoRef;

    // Initialization of progressbar
    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        displaySplashScreen();
    }

    @Override
    protected void onStop() {
        if (firebaseAuth != null && listener != null) {
            firebaseAuth.removeAuthStateListener(listener);
        }
        super.onStop();
    }

    private void init() {

        ButterKnife.bind(this);

        database = FirebaseDatabase.getInstance();
        driverInfoRef = database.getReference(_common.DRIVER_INFO_REFERENCE);

        providers = Arrays.asList(
                new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());

        firebaseAuth = FirebaseAuth.getInstance();

        listener = myFirebaseAuth -> {
            FirebaseUser user = myFirebaseAuth.getCurrentUser();
            if (user != null) {
                // Updating token
                FirebaseInstanceId.getInstance()
                        .getInstanceId()
                        .addOnFailureListener(e -> Toast.makeText(SplashScreenActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show())
                        .addOnSuccessListener(instanceIdResult -> {
                            Log.d("TOKEN", instanceIdResult.getToken());
                            UserUtils.updateToken(SplashScreenActivity.this, instanceIdResult.getToken());
                        });

                checkUserFromFirebase();
                Toast.makeText(this, "Welcome: " + user.getUid(), Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.VISIBLE);
            } else {
                showLoginLayout();
            }
        };
    }

    private void checkUserFromFirebase() {
        try {
            driverInfoRef.child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                //Toast.makeText(SplashScreenActivity.this, "User already register", Toast.LENGTH_SHORT).show();
                                DriverInfoModel driverInfoModel = snapshot.getValue(DriverInfoModel.class);
                                goToHomeActivity(driverInfoModel);
                            } else {
                                showRegisterLayout();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(SplashScreenActivity.this, "" + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (NullPointerException e) {
            showRegisterLayout();
        }

    }

    private void goToHomeActivity(DriverInfoModel driverInfoModel) {
        _common.currentUser = driverInfoModel; // Init Value
        startActivity(new Intent(SplashScreenActivity.this, DriverHomeActivity.class));
        finish();
    }

    private void showRegisterLayout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogTheme);
        View itemView = LayoutInflater.from(this).inflate(R.layout.registry_layout, null);

        TextInputEditText edit_first_name = itemView.findViewById(R.id.edit_first_name);
        TextInputEditText edit_last_name = itemView.findViewById(R.id.edit_last_name);
        TextInputEditText edit_phone_number = itemView.findViewById(R.id.edit_phone_number);

        Button button_continue = itemView.findViewById(R.id.button_continue);

        // Set data
        if (Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getPhoneNumber() != null &&
                !TextUtils.isEmpty(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber())) {
            edit_phone_number.setText(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber());
        }

        // Set view
        builder.setView(itemView);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();


        button_continue.setOnClickListener(view -> {
            if (TextUtils.isEmpty(Objects.requireNonNull(edit_first_name.getText()).toString())) {
                Toast.makeText(this, "Please enter first name", Toast.LENGTH_SHORT).show();
            } else  if (TextUtils.isEmpty(Objects.requireNonNull(edit_last_name.getText()).toString())) {
                Toast.makeText(this, "Please enter last name", Toast.LENGTH_SHORT).show();
            } else  if (TextUtils.isEmpty(Objects.requireNonNull(edit_phone_number.getText()).toString())) {
                Toast.makeText(this, "Please enter phone number", Toast.LENGTH_SHORT).show();
            } else {
                DriverInfoModel model = new DriverInfoModel();
                model.setFirstName(edit_first_name.getText().toString());
                model.setLastName(edit_last_name.getText().toString());
                model.setPhoneNumber(edit_phone_number.getText().toString());
                model.setRating(0.0);

                driverInfoRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .setValue(model)
                        .addOnFailureListener(e -> {
                            Toast.makeText(SplashScreenActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            alertDialog.dismiss();
                        })
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(SplashScreenActivity.this, "Register Successful", Toast.LENGTH_SHORT).show();
                            alertDialog.dismiss();
                            goToHomeActivity(model);
                        });

            }


        });
    }
    private void showLoginLayout() {
        AuthMethodPickerLayout authMethodPickerLayout = new AuthMethodPickerLayout
                .Builder(R.layout.layout_sign_in)
                .setPhoneButtonId(R.id.button_phone_sign_in)
                .setGoogleButtonId(R.id.button_google_sign_in)
                .build();

        startActivityForResult(AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAuthMethodPickerLayout(authMethodPickerLayout)
                .setIsSmartLockEnabled(false)
                .setTheme(R.style.LoginTheme)
                .setAvailableProviders(providers)
                .build(), LOGIN_REQUEST_CODE);
    }

    @SuppressLint("CheckResult")
    private void displaySplashScreen() {

        progressBar.setVisibility(View.VISIBLE);

        Completable.timer(3, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread())
               .subscribe(() ->  firebaseAuth.addAuthStateListener(listener));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOGIN_REQUEST_CODE) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (requestCode == RESULT_OK) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            } else {
                Toast.makeText(this, "[ERROR]: " + response.getError().getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
}