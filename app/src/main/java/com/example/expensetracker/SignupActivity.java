package com.example.expensetracker;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Patterns; // Import for email validation
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SignupActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    Button btnRegister;
    TextView tvGoToLogin;
    DatabaseHelper dbHelper;

    // --- Validation Constants ---
    private static final int MIN_PASSWORD_LENGTH = 8;

    // --- Helper Method for Validation ---
    private boolean isInputValid(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return false;
        }

        // 1. Email Format Check (@ and .com/.net, etc.)
        if (!Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
            Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_LONG).show();
            return false;
        }

        // 2. Password Length Check (Minimum 8 characters)
        if (password.length() < MIN_PASSWORD_LENGTH) {
            Toast.makeText(this, "Password must be at least " + MIN_PASSWORD_LENGTH + " characters.", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // 1. Initialize UI and Database Helper
        etUsername = findViewById(R.id.etSignupUsername);
        etPassword = findViewById(R.id.etSignupPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);
        dbHelper = new DatabaseHelper(this);

        // 2. Register Button Logic
        btnRegister.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // *** Apply Validation Here ***
            if (isInputValid(username, password)) {

                // If validation passes, attempt registration
                boolean isRegistered = dbHelper.registerUser(username, password);

                if (isRegistered) {
                    Toast.makeText(this, "Account created successfully! Please log in.", Toast.LENGTH_LONG).show();
                    finish(); // Navigate back to Login screen
                } else {
                    Toast.makeText(this, "Registration failed. Username may already exist.", Toast.LENGTH_LONG).show();
                }
            }
            // If validation fails, the isInputValid method already shows a Toast message
        });

        // 3. Go to Login Link Logic
        tvGoToLogin.setOnClickListener(v -> finish());
    }
}