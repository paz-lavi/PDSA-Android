package com.paz.pdsa.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseUserMetadata;
import com.paz.logger.EZLog;
import com.paz.pdsa.R;
import com.paz.pdsa.databinding.ActivityLoginBinding;
import com.paz.pdsa.utils.Constants;
import com.paz.prefy_lib.Prefy;

import java.util.Arrays;
import java.util.List;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "MyTag";
    private final int RC_SIGN_IN = 2901;
    private ActivityLoginBinding binding;
    private final EZLog ezLog = EZLog.getInstance();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        //  setButtonsAction();
        if (FirebaseAuth.getInstance().getCurrentUser() == null)
            startLoginUI();
        else
            intentToActivity();
    }

    private void startLoginUI() {

        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build()

        );

        // Create and launch sign-in intent
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers).setLogo(R.drawable.ic_contract).setIsSmartLockEnabled(false)
                        .build(),
                RC_SIGN_IN);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                intentToActivity();

            } else {
                shoWErrorDialog();
            }
        }
    }

    private void intentToActivity() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseUserMetadata metadata = user.getMetadata();
        ezLog.debug("onActivityResult: " + user.toString());
        boolean isNewUser = (metadata.getCreationTimestamp() == metadata.getLastSignInTimestamp());
        ezLog.debug("getCreationTimestamp: " + metadata.getCreationTimestamp());
        ezLog.debug("getLastSignInTimestamp: " + metadata.getLastSignInTimestamp());
        ezLog.debug("isNewUser: " + isNewUser);
        ezLog.setCostumerId(user.getUid());
        boolean registered = Prefy.getInstance().getBoolean(user.getUid(), false);

        Intent intent;
        if (isNewUser && !registered) {
            intent = new Intent(LoginActivity.this, UserActivity.class);
            intent.putExtra(Constants.NEW_USER, true);
        } else {
            intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra(Constants.NEW_USER, false);
            ezLog.debug("onActivityResult: ");
        }
        intent.setData(getIntent().getData());
        startActivity(intent);
        finish();
    }

    private void shoWErrorDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Login failed")
                .setMessage("The login failed. Please try again")
                .setPositiveButton("Dismiss", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}