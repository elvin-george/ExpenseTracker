package com.example.expensetracker;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CategoryDetailActivity extends AppCompatActivity {

    private long currentUserId;
    private String monthYear; // Format: YYYY-MM (e.g., 2025-10)
    private String displayMonth; // Format: October 2025
    private DatabaseHelper dbHelper;

    private TextView tvDetailTitle;
    private ListView lvCategorySummary;
    private Spinner spWeekSelector;
    private ListView lvWeeklyExpenses;
    private TextView tvWeeklyHeader;

    private List<String> weekRanges = new ArrayList<>();
    private List<WeekBoundary> weekBoundaries = new ArrayList<>();

    private class WeekBoundary {
        String start; // YYYY-MM-DD
        String end;   // YYYY-MM-DD
        public WeekBoundary(String start, String end) {
            this.start = start;
            this.end = end;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_detail);

        dbHelper = new DatabaseHelper(this);

        // 1. Initialize UI components
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        lvCategorySummary = findViewById(R.id.lvCategorySummary);
        spWeekSelector = findViewById(R.id.spWeekSelector);
        lvWeeklyExpenses = findViewById(R.id.lvWeeklyExpenses);
        tvWeeklyHeader = findViewById(R.id.tvWeeklyHeader);

        // 2. Get passed data
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            currentUserId = extras.getLong("USER_ID", -1);
            monthYear = extras.getString("MONTH_YEAR");
            displayMonth = extras.getString("DISPLAY_MONTH");
        }

        if (currentUserId == -1 || monthYear == null) {
            Toast.makeText(this, "Error: Invalid session data.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        tvDetailTitle.setText("Category Breakdown for " + displayMonth);

        // 3. Load Monthly Summary and Week Selector
        loadCategorySummary();
        calculateAndSetupWeeks();

        // 4. Set Listener for Week Selector
        spWeekSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Load expenses for the selected week
                WeekBoundary selectedWeek = weekBoundaries.get(position);
                loadWeeklyExpenses(selectedWeek.start, selectedWeek.end);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Initial load for the first week
        if (!weekBoundaries.isEmpty()) {
            WeekBoundary initialWeek = weekBoundaries.get(0);
            loadWeeklyExpenses(initialWeek.start, initialWeek.end);
        }
    }

    // --- Data Loading Functions ---

    private void loadCategorySummary() {
        Cursor cursor = dbHelper.getCategorySummary(currentUserId, monthYear);

        // Map Category (text1) and TotalAmount (text2)
        String[] fromColumns = {DatabaseHelper.COL_EXP_CATEGORY, "TotalAmount"};
        int[] toViews = {android.R.id.text1, android.R.id.text2};

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,
                android.R.layout.simple_list_item_2,
                cursor,
                fromColumns,
                toViews,
                0
        );

        // *** NEW: Custom View Binder for Summary List Formatting ***
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (view.getId() == android.R.id.text2) {
                    // Line 2: Total Amount (Format as currency)
                    TextView tv = (TextView) view;
                    int amountIndex = cursor.getColumnIndexOrThrow("TotalAmount");
                    String amount = String.format(Locale.US, "$%.2f", cursor.getDouble(amountIndex));
                    tv.setText(amount);
                    return true;
                }
                return false;
            }
        });

        lvCategorySummary.setAdapter(adapter);
    }

    private void loadWeeklyExpenses(String startDate, String endDate) {
        Cursor cursor = dbHelper.getExpensesForWeek(currentUserId, startDate, endDate);

        // Update header text
        tvWeeklyHeader.setText("Transactions: " + startDate + " to " + endDate);

        // Cursor SELECT returns: _id, COL_EXP_DATE, COL_EXP_AMOUNT, COL_EXP_CATEGORY, COL_EXP_NOTE
        // We will map Category (text1) and Date (text2). Amount must be formatted via ViewBinder.
        String[] fromColumns = {DatabaseHelper.COL_EXP_CATEGORY, DatabaseHelper.COL_EXP_DATE};
        int[] toViews = {android.R.id.text1, android.R.id.text2};

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,
                android.R.layout.simple_list_item_2,
                cursor,
                fromColumns,
                toViews,
                0
        );

        // *** NEW: Custom View Binder for Weekly List Formatting ***
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                int categoryIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXP_CATEGORY);
                int amountIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXP_AMOUNT);
                int dateIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXP_DATE);

                if (view.getId() == android.R.id.text1) {
                    // Line 1: Category + $Amount
                    TextView tv = (TextView) view;
                    String category = cursor.getString(categoryIndex);
                    String amount = String.format(Locale.US, "$%.2f", cursor.getDouble(amountIndex));
                    tv.setText(category + ": " + amount);
                    return true;
                }

                if (view.getId() == android.R.id.text2) {
                    // Line 2: Date
                    TextView tv = (TextView) view;
                    String date = cursor.getString(dateIndex);
                    tv.setText(date);
                    return true;
                }

                return false;
            }
        });

        lvWeeklyExpenses.setAdapter(adapter);

        // Enable editing when clicking a weekly transaction
        lvWeeklyExpenses.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(CategoryDetailActivity.this, AddEditExpenseActivity.class);
            intent.putExtra("USER_ID", currentUserId);
            intent.putExtra("EXPENSE_ID", id); // 'id' is the ExpID
            startActivity(intent);
        });
    }

    // --- Week Calculation Function (Key Logic) ---

    private void calculateAndSetupWeeks() {
        // ... (The code for calculating weeks is unchanged from the previous session) ...
        // [omitted for brevity, assume the previous implementation of calculateAndSetupWeeks is here]

        // Parse the YYYY-MM string back to a Calendar object for the first day of the month
        Calendar monthCal = Calendar.getInstance();
        try {
            Date firstDayOfMonth = new SimpleDateFormat("yyyy-MM", Locale.US).parse(monthYear);
            monthCal.setTime(firstDayOfMonth);
            monthCal.set(Calendar.DAY_OF_MONTH, 1); // Ensure we start on the first day
        } catch (Exception e) {
            return; // Should not happen if data is passed correctly
        }

        Calendar current = (Calendar) monthCal.clone();
        Calendar lastDay = (Calendar) monthCal.clone();
        lastDay.set(Calendar.DAY_OF_MONTH, monthCal.getActualMaximum(Calendar.DAY_OF_MONTH));

        SimpleDateFormat sqlFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd", Locale.US);

        weekRanges.clear();
        weekBoundaries.clear();

        while (current.before(lastDay) || current.equals(lastDay)) {
            Calendar startOfWeek = (Calendar) current.clone();

            // Go to the previous Sunday (Start of the week).
            startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);

            Calendar endOfWeek = (Calendar) startOfWeek.clone();
            endOfWeek.add(Calendar.DATE, 6); // End of week (Saturday)

            // Clamp the boundaries to the month limits
            Calendar weekStart = startOfWeek.before(monthCal) ? monthCal : startOfWeek;
            Calendar weekEnd = endOfWeek.after(lastDay) ? lastDay : endOfWeek;

            // Format and store the range
            String startDate = sqlFormat.format(weekStart.getTime());
            String endDate = sqlFormat.format(weekEnd.getTime());
            String displayRange = displayFormat.format(weekStart.getTime()) + " - " + displayFormat.format(weekEnd.getTime());

            weekRanges.add(displayRange);
            weekBoundaries.add(new WeekBoundary(startDate, endDate));

            // Move to the next Monday (to start the next week calculation)
            current.add(Calendar.DATE, 7);
            current.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        }

        // Populate the Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, weekRanges);
        spWeekSelector.setAdapter(adapter);
    }
}