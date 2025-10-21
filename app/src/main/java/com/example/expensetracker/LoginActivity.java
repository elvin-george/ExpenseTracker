package com.example.expensetracker;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Patterns; // Import for email validation
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    Button btnLogin;
    TextView tvGoToSignup;
    DatabaseHelper dbHelper;

    // --- Validation Constants ---
    private static final int MIN_PASSWORD_LENGTH = 8;

    // --- Helper Method for Validation (Reusable) ---
    private boolean isInputValid(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter username and password.", Toast.LENGTH_SHORT).show();
            return false;
        }

        // 1. Email Format Check
        if (!Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
            Toast.makeText(this, "Please enter a valid email format.", Toast.LENGTH_LONG).show();
            return false;
        }

        // 2. Password Length Check (Prevents extremely short database checks)
        if (password.length() < MIN_PASSWORD_LENGTH) {
            Toast.makeText(this, "Password must be at least " + MIN_PASSWORD_LENGTH + " characters.", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Initialize Components
        etUsername = findViewById(R.id.etLoginUsername);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToSignup = findViewById(R.id.tvGoToSignup);
        dbHelper = new DatabaseHelper(this);

        // 2. Go to Signup Logic (Explicit Intent)
        tvGoToSignup.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        // 3. Login Button Logic
        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // *** Apply Validation Here ***
            if (isInputValid(username, password)) {

                // If validation passes, attempt login
                Cursor cursor = dbHelper.checkUser(username, password);

                if (cursor.moveToFirst()) {
                    // Login Successful
                    long userId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_ID));
                    Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show();

                    // Navigate to Dashboard
                    Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                    intent.putExtra("USER_ID", userId);
                    startActivity(intent);
                    finish();
                } else {
                    // Login Failed
                    Toast.makeText(this, "Invalid username or password.", Toast.LENGTH_SHORT).show();
                }
                cursor.close();
            }
        });
    }
}