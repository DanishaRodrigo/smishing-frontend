package com.example.smishingdetectionapp.ui.login;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.content.SharedPreferences;
import com.example.smishingdetectionapp.UserRiskManager;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.smishingdetectionapp.BuildConfig;
import com.example.smishingdetectionapp.DataBase.DBresult;
import com.example.smishingdetectionapp.DataBase.Retrofitinterface;
import com.example.smishingdetectionapp.MainActivity;
import com.example.smishingdetectionapp.R;
import com.example.smishingdetectionapp.SharedActivity;
import com.example.smishingdetectionapp.databinding.ActivityLoginBinding;
import com.example.smishingdetectionapp.detections.DatabaseAccess;
import com.example.smishingdetectionapp.ui.Register.RegisterMain;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.HashMap;

public class LoginActivity extends AppCompatActivity {

    private LoginViewModel loginViewModel;
    private ActivityLoginBinding binding;
    private Retrofit retrofit;
    private Retrofitinterface retrofitinterface;
    private String BASE_URL = BuildConfig.SERVERIP;

    private int loginAttempts = 0;

    GoogleSignInOptions gso;
    GoogleSignInClient gsc;
    private boolean isPinLogin = false;  // Flag for PIN login

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate layout
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Retrofit
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        retrofitinterface = retrofit.create(Retrofitinterface.class);

        // Check if user is already logged in
        if (isUserLoggedIn()) {
            navigateToMainActivity();
            return;
        }

        // ViewModel setup
        loginViewModel = new ViewModelProvider(this, new LoginViewModelFactory())
                .get(LoginViewModel.class);

        // View bindings
        final EditText usernameEditText = binding.email;
        final EditText passwordEditText = binding.password;
        final Button loginButton = binding.loginButton;
        final CheckBox rememberMe = binding.rememberMeCheckbox;
        final ProgressBar loadingProgressBar = binding.progressbar;
        final SignInButton googleBtn = binding.googleBtn;
        final Button registerButton = binding.registerButton;
        final ImageButton togglePasswordVisibility = binding.togglePasswordVisibility;
        final Button togglePinLogin = binding.togglePinLogin;  // Added missing reference for togglePinLogin button

        // Toggle functionality for PIN and Password login
        togglePinLogin.setOnClickListener(v -> {
            if (isPinLogin) {
                // Switch to password login
                passwordEditText.setHint("Password");
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                loginButton.setText("Login");
                togglePinLogin.setText("Login with PIN");
                isPinLogin = false;
            } else {
                // Switch to PIN login
                passwordEditText.setHint("Enter 6-digit PIN");
                passwordEditText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
                loginButton.setText("Login with PIN");
                togglePinLogin.setText("Login with Password");
                isPinLogin = true;
            }
        });

        // Handle login button click
        loginButton.setOnClickListener(v -> {

            if (UserRiskManager.isRapidClick()) {
                Toast.makeText(this, "⚠️ Too fast! Suspicious activity", Toast.LENGTH_SHORT).show();
            }

            //  Block high-risk users
            int score = UserRiskManager.getRiskScore(this);

            if (score >= 3) {
                Toast.makeText(this, "⚠️ HIGH RISK detected!", Toast.LENGTH_LONG).show();
            } else if (score == 2) {
                Toast.makeText(this, "⚠️ MEDIUM RISK detected!", Toast.LENGTH_LONG).show();
            }

            loadingProgressBar.setVisibility(View.VISIBLE);

            loginButton.animate()
                    .alpha(0.5f)
                    .setDuration(200)
                    .start();

            loginButton.setEnabled(false);

            binding.loginButton.setEnabled(true);


            String input = passwordEditText.getText().toString();
            String email = usernameEditText.getText().toString();

            if (isPinLogin) {
                if (input.isEmpty()) {
                    passwordEditText.setError("Please enter your PIN");
                    loginAttempts++;
                    UserRiskManager.recordFailedLogin(this);
                    loadingProgressBar.setVisibility(View.GONE);
                    return;
                }

                if (input.length() != 6) {
                    passwordEditText.setError("PIN must be exactly 6 digits");
                    loginAttempts++;
                    UserRiskManager.recordFailedLogin(this);
                    loadingProgressBar.setVisibility(View.GONE);
                    return;
                }

                loginWithPin(input);

            } else {
                if (email.isEmpty()) {
                    usernameEditText.setError("Email is required");
                    loginAttempts++;
                    UserRiskManager.recordFailedLogin(this);
                    loadingProgressBar.setVisibility(View.GONE);
                    return;
                }

                if (!email.contains("@")) {
                    usernameEditText.setError("Enter a valid email");
                    loginAttempts++;
                    UserRiskManager.recordFailedLogin(this);
                    loadingProgressBar.setVisibility(View.GONE);
                    return;
                }

                if (input.isEmpty()) {
                    passwordEditText.setError("Please enter your password");
                    loginAttempts++;
                    UserRiskManager.recordFailedLogin(this);
                    loadingProgressBar.setVisibility(View.GONE);
                    return;
                }

                if (input.length() < 6) {
                    passwordEditText.setError("Password must be at least 6 characters");
                    loginAttempts++;
                    UserRiskManager.recordFailedLogin(this);
                    loadingProgressBar.setVisibility(View.GONE);
                    return;
                }

                loginWithPassword(email, input);
            }
        });

        // Handle register button click
        registerButton.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterMain.class));
            finish();
        });

        // Handle Google Sign-In setup
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        gsc = GoogleSignIn.getClient(this, gso);

        // Sign out of Google account to allow fresh authentication
        gsc.signOut().addOnCompleteListener(task -> {
            Toast.makeText(this, "Signed out. Ready for fresh authentication.", Toast.LENGTH_SHORT).show();
        });

        // Handle Google Sign-In button click
        googleBtn.setOnClickListener(v -> {
            GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
            if (acct != null) {
                signOutGoogle(() -> signInGoogle());
            } else {
                signInGoogle();
            }
        });

        // Observe LoginFormState
        loginViewModel.getLoginFormState().observe(this, loginFormState -> {
            if (loginFormState == null) return;
            loginButton.setEnabled(loginFormState.isDataValid());
            if (loginFormState.getUsernameError() != null) {
                usernameEditText.setError(getString(loginFormState.getUsernameError()));
            }
            if (loginFormState.getPasswordError() != null) {
                passwordEditText.setError(getString(loginFormState.getPasswordError()));
            }
        });

        loginViewModel.getLoginResult().observe(this, new Observer<LoginResult>() {
            @Override
            public void onChanged(@Nullable LoginResult loginResult) {
                if (loginResult == null) {
                    return;
                }
                loadingProgressBar.setVisibility(View.GONE);
                if (loginResult.getError() != null) {
                    showLoginFailed(loginResult.getError());
                }
                if (loginResult.getSuccess() != null) {
                    updateUiWithUser(loginResult.getSuccess());
                }
                setResult(Activity.RESULT_OK);
                finish();
            }
        });

        // Password visibility toggle
        togglePasswordVisibility.setOnClickListener(v -> {
            boolean isPasswordVisible = passwordEditText.getTransformationMethod() == null;
            if (isPasswordVisible) {
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                togglePasswordVisibility.setImageResource(R.drawable.ic_passwords_visibility);
            } else {
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT);
                togglePasswordVisibility.setImageResource(R.drawable.ic_passwords_visibility);
            }
            passwordEditText.setSelection(passwordEditText.getText().length());
        });
    }

    // Google Sign-In
    void signInGoogle() {
        Intent signInIntent = gsc.getSignInIntent();
        startActivityForResult(signInIntent, 1000);
    }

    // Google Sign-Out
    void signOutGoogle(Runnable onSignOutComplete) {
        gsc.signOut().addOnCompleteListener(task -> {
            Toast.makeText(this, "Signed out of Google account.", Toast.LENGTH_SHORT).show();
            onSignOutComplete.run();
        });
    }

    // Handle the result of the Google Sign-In
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1000) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                task.getResult(ApiException.class);
                navigateToMainActivity();
            } catch (ApiException e) {
                Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /*
    private void loginWithPin(String pin) {
        // Open the database
        DatabaseAccess databaseAccess = DatabaseAccess.getInstance(this);
        databaseAccess.open();

        // Validate the PIN
        boolean isValid = databaseAccess.validatePin(pin);

        if (isValid) {
            // PIN is valid
            Toast.makeText(LoginActivity.this, "PIN verified successfully", Toast.LENGTH_SHORT).show();
            navigateToMainActivity();
        } else {
            // Invalid PIN
            Toast.makeText(LoginActivity.this, "Invalid PIN. Please try again.", Toast.LENGTH_LONG).show();
        }

        // Close the database
        databaseAccess.close();
    }

     */

    private void loginWithPin(String pin) {

        SharedPreferences prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (binding.rememberMeCheckbox.isChecked()) {
            editor.putBoolean("isLoggedIn", true);
        } else {
            editor.putBoolean("isLoggedIn", false);
        }

        editor.putLong("loginTime", System.currentTimeMillis());

        editor.apply();

        Toast.makeText(LoginActivity.this, "PIN login successful", Toast.LENGTH_SHORT).show();
        binding.progressbar.setVisibility(View.GONE);
        navigateToMainActivity();
    }


    /*
    private void loginWithPassword(String email, String password) {
        DatabaseAccess databaseAccess = DatabaseAccess.getInstance(this);
        databaseAccess.open();

        boolean isValid = databaseAccess.validateLogin(email, password);

        if (isValid) {
            navigateToMainActivity();
        } else {
            Toast.makeText(LoginActivity.this, "Invalid email or password", Toast.LENGTH_LONG).show();
        }

        databaseAccess.close();
    }

     */

    private void loginWithPassword(String email, String password) {

        SharedPreferences prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // FIX 1: handle both cases
        if (binding.rememberMeCheckbox.isChecked()) {
            editor.putBoolean("isLoggedIn", true);
        } else {
            editor.putBoolean("isLoggedIn", false);
        }

        editor.putString("userEmail", email);

        // FIX 2: save login time BEFORE apply
        editor.putLong("loginTime", System.currentTimeMillis());

        editor.apply(); // apply at the END

        Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
        binding.progressbar.setVisibility(View.GONE);
        navigateToMainActivity();
    }


    private void handleLoginDialog() {
        final EditText usernameEditText = binding.email;
        final EditText passwordEditText = binding.password;

        HashMap<String, String> map = new HashMap<>();
        map.put("email", usernameEditText.getText().toString());
        map.put("password", passwordEditText.getText().toString());

        Call<DBresult> call = retrofitinterface.executeLogin(map);
        call.enqueue(new Callback<DBresult>() {
            @Override
            public void onResponse(Call<DBresult> call, Response<DBresult> response) {
                if (response.code() == 200) {

                    SharedPreferences prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();

                    if (binding.rememberMeCheckbox.isChecked()) {
                        editor.putBoolean("isLoggedIn", true);
                    } else {
                        editor.putBoolean("isLoggedIn", false);
                    }

                    editor.putString("userEmail", binding.email.getText().toString());
                    editor.putLong("loginTime", System.currentTimeMillis());
                    editor.apply();

                    Toast.makeText(LoginActivity.this, "Login successful (API)", Toast.LENGTH_SHORT).show();

                    navigateToMainActivity();

                } else {
                    Toast.makeText(LoginActivity.this, "Invalid credentials", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<DBresult> call, Throwable throwable) {
                Toast.makeText(LoginActivity.this, throwable.getMessage(), Toast.LENGTH_LONG).show();                navigateToMainActivity();
            }
        });
    }

    private boolean isUserLoggedIn() {
        SharedPreferences prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE);

        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        long loginTime = prefs.getLong("loginTime", 0);
        long currentTime = System.currentTimeMillis();

        if (currentTime - loginTime > 600000) { // 10 minutes
            prefs.edit().clear().apply();
            return false;
        }

        return isLoggedIn;
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void updateUiWithUser(LoggedInUserView model) {
        String welcome = getString(R.string.welcome) + model.getDisplayName();
        Toast.makeText(getApplicationContext(), welcome, Toast.LENGTH_LONG).show();
    }

    private void showLoginFailed(@StringRes Integer errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }
}