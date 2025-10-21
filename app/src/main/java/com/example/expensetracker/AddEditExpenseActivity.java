package com.example.expensetracker;

import androidx.appcompat.app.AppCompatActivity;
import android.app.DatePickerDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddEditExpenseActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private long currentUserId;
    private long expenseIdToEdit = -1; // -1 means ADD mode

    private TextView tvTitle;
    private EditText etAmount, etDate, etNote;
    private Spinner spCategory; // Replaced etCategory
    private Button btnSave, btnDelete;

    // Fixed expense categories
    private static final String[] EXPENSE_CATEGORIES = {
            "Food", "Transport", "Rent", "Electricity", "Water", "Others"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_expense);

        dbHelper = new DatabaseHelper(this);

        // 1. Initialize UI components
        tvTitle = findViewById(R.id.tvTitle);
        etAmount = findViewById(R.id.etAmount);
        spCategory = findViewById(R.id.spCategory); // Changed from EditText
        etDate = findViewById(R.id.etDate);
        etNote = findViewById(R.id.etNote);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);

        // 2. Setup Category Spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                EXPENSE_CATEGORIES
        );
        spCategory.setAdapter(spinnerAdapter);

        // 3. Setup Date Picker
        etDate.setOnClickListener(v -> showDatePicker());

        // 4. Get passed data
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            currentUserId = extras.getLong("USER_ID", -1);
            expenseIdToEdit = extras.getLong("EXPENSE_ID", -1);
        }

        // 5. Check for Edit mode
        if (expenseIdToEdit != -1) {
            tvTitle.setText("Edit Existing Expense");
            btnSave.setText("UPDATE EXPENSE");
            btnDelete.setVisibility(View.VISIBLE);
            loadExpenseData(expenseIdToEdit);
        } else {
            // Set default date to today for new expense
            etDate.setText(getDefaultDate());
        }

        // 6. Set Listeners
        btnSave.setOnClickListener(v -> saveOrUpdateExpense());
        btnDelete.setOnClickListener(v -> deleteExpense());
    }

    private String getDefaultDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(Calendar.getInstance().getTime());
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        // If etDate has a date, use it to initialize the picker
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            c.setTime(sdf.parse(etDate.getText().toString()));
            year = c.get(Calendar.YEAR);
            month = c.get(Calendar.MONTH);
            day = c.get(Calendar.DAY_OF_MONTH);
        } catch (Exception ignored) {
            // Ignore if date format is invalid, use current date
        }

        DatePickerDialog dpd = new DatePickerDialog(
                this,
                (view, y, m, d) -> {
                    // Format date as YYYY-MM-DD for storage
                    String selectedDate = String.format(Locale.US, "%04d-%02d-%02d", y, (m + 1), d);
                    etDate.setText(selectedDate);
                },
                year, month, day
        );
        dpd.show();
    }


    private void loadExpenseData(long expenseId) {
        Cursor cursor = dbHelper.getExpenseById(expenseId);
        if (cursor.moveToFirst()) {
            // Retrieve and populate fields
            String amount = String.valueOf(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXP_AMOUNT)));
            String category = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXP_CATEGORY));
            String date = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXP_DATE));
            String note = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXP_NOTE));

            etAmount.setText(amount);
            etDate.setText(date);
            etNote.setText(note);

            // Set the spinner to the correct category
            for (int i = 0; i < EXPENSE_CATEGORIES.length; i++) {
                if (EXPENSE_CATEGORIES[i].equals(category)) {
                    spCategory.setSelection(i);
                    break;
                }
            }
        }
        cursor.close();
    }

    private void saveOrUpdateExpense() {
        String amountStr = etAmount.getText().toString().trim();
        String category = spCategory.getSelectedItem().toString(); // Get value from Spinner
        String date = etDate.getText().toString().trim();
        String note = etNote.getText().toString().trim();

        if (amountStr.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Please fill in Amount and Date.", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount format.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean success;

        if (expenseIdToEdit == -1) {
            // ADD Logic (Create)
            success = dbHelper.addExpense(currentUserId, amount, category, date, note);
            Toast.makeText(this, success ? "Expense added!" : "Failed to add expense.", Toast.LENGTH_SHORT).show();
        } else {
            // UPDATE Logic
            success = dbHelper.updateExpense(expenseIdToEdit, amount, category, date, note);
            Toast.makeText(this, success ? "Expense updated!" : "Failed to update expense.", Toast.LENGTH_SHORT).show();
        }

        if (success) {
            finish(); // Close this activity and return to Dashboard
        }
    }

    private void deleteExpense() {
        if (expenseIdToEdit != -1) {
            boolean success = dbHelper.deleteExpense(expenseIdToEdit);
            Toast.makeText(this, success ? "Expense deleted!" : "Failed to delete expense.", Toast.LENGTH_SHORT).show();
            if (success) {
                finish(); // Close this activity and return to Dashboard
            }
        }
    }
}