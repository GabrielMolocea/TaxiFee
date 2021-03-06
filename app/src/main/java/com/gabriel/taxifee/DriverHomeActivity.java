package com.gabriel.taxifee;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.gabriel.taxifee.Utils.UserUtils;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class DriverHomeActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 7172;
    private AppBarConfiguration mAppBarConfiguration;
    private DrawerLayout drawer;
    private  NavigationView navigationView;
    private NavController navController;

    private AlertDialog waitingDialog;
    private StorageReference storageReference;

    private Uri imageUri;
    private ImageView imageAvatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_home);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        drawer = findViewById(R.id.drawer_layout);
       navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home)
                .setDrawerLayout(drawer)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        init();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                imageUri = data.getData();
                imageAvatar.setImageURI(imageUri);

                showDialogUpdate();
            }
        }
    }

    // This method shows a dialog for confirmation of avatar changing
    private void showDialogUpdate() {
        AlertDialog.Builder builder = new AlertDialog.Builder(DriverHomeActivity.this);
        builder.setTitle("Change avatar")
                .setMessage("Do you really want to change avatar ?")
                .setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("UPLOAD", (dialog, which) -> {
                    if (imageUri != null) {
                        waitingDialog.setMessage("Uploading...");
                        waitingDialog.show();

                        String uniqueName = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        StorageReference avatarFolder = storageReference.child("avatar/" + uniqueName);

                        avatarFolder.putFile(imageUri)
                                .addOnFailureListener(e -> {
                                    waitingDialog.dismiss();
                                    Snackbar.make(drawer, e.getMessage(), Snackbar.LENGTH_SHORT).show();
                                })
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {

                                        avatarFolder.getDownloadUrl().addOnSuccessListener(uri -> {
                                            Map<String, Object> updateData = new HashMap<>();
                                            updateData.put("avatar", uri.toString());

                                            UserUtils.updateUser(drawer, updateData);
                                        });
                                    }
                                    waitingDialog.dismiss();
                                })
                                .addOnProgressListener(snapshot -> {
                                    double progress = (100.00 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                                    waitingDialog.setMessage(new StringBuilder("Updating: ").append(progress).append(" %"));
                                });
                    }

                })
                .setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialog1 -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(getResources().getColor(R.color.black));
        });

        dialog.show();
    }

    private void init() {

        waitingDialog =  new AlertDialog.Builder(this)
                .setCancelable(false)
                .setMessage("Waiting....")
                .create();

        storageReference = FirebaseStorage.getInstance().getReference();

        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_sign_ou) {
                AlertDialog.Builder builder = new AlertDialog.Builder(DriverHomeActivity.this);
                builder.setTitle("Sign Out")
                        .setMessage("Do you really want to sing out?")
                        .setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss())
                        .setPositiveButton("SIGN OUT", (dialog, which) -> {
                            FirebaseAuth.getInstance().signOut();
                            Intent intent = new Intent(DriverHomeActivity.this, SplashScreenActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            fileList();
                        })
                        .setCancelable(false);
                AlertDialog dialog = builder.create();
                dialog.setOnShowListener(dialog1 -> {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                            .setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                            .setTextColor(getResources().getColor(R.color.black));
                });

                dialog.show();
            }
            return false;
        });

        // Set data for user
        View headView = navigationView.getHeaderView(0);
        TextView textName = headView.findViewById(R.id.txt_name);
        TextView textPhone = headView.findViewById(R.id.textPhoneNumber);
        TextView textStar = headView.findViewById(R.id.txt_star);

        imageAvatar = headView.findViewById(R.id.image_avatar);

        textName.setText(_common.buildWelcomeMessage());
        textPhone.setText(_common.currentUser != null ? _common.currentUser.getPhoneNumber() : "");
        textStar.setText(_common.currentUser != null ? String.valueOf(_common.currentUser.getRating()) : "0.0");

        imageAvatar.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        // Checking if current user and image for avatar are null and if not loading image for avatar
        if (_common.currentUser != null && _common.currentUser.getAvatar() != null && !TextUtils.isEmpty(_common.currentUser.getAvatar())) {
                Glide.with(this)
                        .load(_common.currentUser.getAvatar())
                        .into(imageAvatar);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.driver_home, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}