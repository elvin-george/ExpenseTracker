package com.example.expensetracker;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SimpleCursorAdapter;

// Chart Imports (Now using PieChart components)
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private long currentUserId = -1;
    private DatabaseHelper dbHelper;
    private PieChart expenseChart; // Changed from BarChart
    private ListView lvExpenses;
    private Calendar currentMonth;
    private TextView tvCurrentMonth;
    private Button btnNextMonth;
    private TextView tvSeeMore; // New TextView for navigation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        dbHelper = new DatabaseHelper(this);
        expenseChart = findViewById(R.id.expenseChart); // Now a PieChart
        lvExpenses = findViewById(R.id.lvExpenses);
        tvCurrentMonth = findViewById(R.id.tvCurrentMonth);
        Button btnPreviousMonth = findViewById(R.id.btnPreviousMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        tvSeeMore = findViewById(R.id.tvSeeMore); // Initialize See More link

        // Initialize to current month
        currentMonth = Calendar.getInstance();

        // FAB to open the Add/Edit screen
        findViewById(R.id.fabAddExpense).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditExpenseActivity.class);
            intent.putExtra("USER_ID", currentUserId);
            startActivity(intent);
        });

        // 1. Retrieve the User ID
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            currentUserId = extras.getLong("USER_ID", -1);
        }

        if (currentUserId == -1) {
            Toast.makeText(this, "Error: User session expired. Please log in.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }

        // 2. Setup Month Navigation Listeners
        btnPreviousMonth.setOnClickListener(v -> changeMonth(-1));
        btnNextMonth.setOnClickListener(v -> changeMonth(1));

        // 3. Setup See More Listener
        tvSeeMore.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, CategoryDetailActivity.class);
            intent.putExtra("USER_ID", currentUserId);
            intent.putExtra("MONTH_YEAR", getMonthYearString()); // Pass YYYY-MM
            intent.putExtra("DISPLAY_MONTH", getDisplayMonthYearString()); // Pass Oct 2025
            startActivity(intent);
        });
    }

    // Changes the month by increment (1 for next, -1 for previous)
    private void changeMonth(int increment) {
        currentMonth.add(Calendar.MONTH, increment);
        updateUI();
    }

    // Helper method to format the current month for SQL query (YYYY-MM)
    private String getMonthYearString() {
        SimpleDateFormat sqlFormat = new SimpleDateFormat("yyyy-MM", Locale.US);
        return sqlFormat.format(currentMonth.getTime());
    }

    // Helper method to format the current month for display (e.g., October 2025)
    private String getDisplayMonthYearString() {
        SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM yyyy", Locale.US);
        return displayFormat.format(currentMonth.getTime());
    }

    // Updates the UI text and refreshes the data
    private void updateUI() {
        tvCurrentMonth.setText(getDisplayMonthYearString());
        loadExpenseData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentUserId != -1) {
            updateUI(); // Load data for the currently selected month
        }
    }

    private void loadExpenseData() {
        String monthYear = getMonthYearString();

        loadExpenseList(monthYear);
        loadExpenseChart(monthYear);
    }

    // Updated to use getTopNExpenses (limit 5) and include amount and date
    private void loadExpenseList(String monthYear) {
        // Use the new method to limit transactions to 5
        final int MAX_ITEMS = 5;
        Cursor cursor = dbHelper.getTopNExpenses(currentUserId, monthYear, MAX_ITEMS);

        // --- CUSTOM CURSOR ADAPTER FOR FORMATTING FIX ---
        // SimpleCursorAdapter cannot format data like adding '$' or combining columns.
        // We must use a custom CursorAdapter to display Category and Amount correctly side-by-side.
        // For simplicity (to stick with SimpleCursorAdapter), we will use an anonymous CursorAdapter
        // and format the amount manually in a custom view binder.

        String[] fromColumns = {DatabaseHelper.COL_EXP_CATEGORY, DatabaseHelper.COL_EXP_AMOUNT};
        int[] toViews = {android.R.id.text1, android.R.id.text2};

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,
                android.R.layout.simple_list_item_2,
                cursor,
                fromColumns,
                toViews,
                0
        );

        // *** NEW: Custom View Binder to display Category and Amount/Date ***
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                // columnIndex 0 is _id, 1 is COL_EXP_DATE, 2 is COL_EXP_AMOUNT, 3 is COL_EXP_CATEGORY
                // We use getColumnIndexOrThrow for safer indexing.

                int categoryIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXP_CATEGORY);
                int amountIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXP_AMOUNT);
                int dateIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXP_DATE);

                if (view.getId() == android.R.id.text1) {
                    // Line 1: Category
                    TextView tv = (TextView) view;
                    String category = cursor.getString(categoryIndex);
                    String amount = String.format(Locale.US, "$%.2f", cursor.getDouble(amountIndex));
                    tv.setText(category);

                    // Note: We need a way to combine text or place the amount on the right.
                    // Since simple_list_item_2 text1 is left-aligned, we can display: Category: $Amount
                    // We will put Category on text1 and Date/Amount on text2 for readability.
                    tv.setText(category);
                    return true;
                }

                if (view.getId() == android.R.id.text2) {
                    // Line 2: Amount | Date
                    TextView tv = (TextView) view;
                    String date = cursor.getString(dateIndex);
                    String amount = String.format(Locale.US, "$%.2f", cursor.getDouble(amountIndex));

                    // Display amount and date on the second line
                    tv.setText(amount + " on " + date);
                    return true;
                }

                return false;
            }
        });

        lvExpenses.setAdapter(adapter);

        // See More link visibility
        if (cursor.getCount() > 0) {
            tvSeeMore.setVisibility(View.VISIBLE);
        } else {
            tvSeeMore.setVisibility(View.GONE);
        }

        // Set click listener for updating/deleting an existing expense
        lvExpenses.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(this, AddEditExpenseActivity.class);
            intent.putExtra("USER_ID", currentUserId);
            intent.putExtra("EXPENSE_ID", id);
            startActivity(intent);
        });
    }

    // Updated to accept monthYear filter and use PieChart logic
    private void loadExpenseChart(String monthYear) {
        Cursor cursor = dbHelper.getCategoryTotalsForMonth(currentUserId, monthYear);
        List<PieEntry> entries = new ArrayList<>();
        float totalAmount = 0f;

        if (cursor.moveToFirst()) {
            do {
                String category = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXP_CATEGORY));
                float amount = cursor.getFloat(cursor.getColumnIndexOrThrow("TotalAmount"));

                entries.add(new PieEntry(amount, category));
                totalAmount += amount;
            } while (cursor.moveToNext());
        }
        cursor.close();

        // 1. Setup the DataSet
        PieDataSet dataSet = new PieDataSet(entries, "Expense Categories");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        // Define colors
        final int[] MATERIAL_COLORS_ARRAY = {
                ColorTemplate.rgb("#F44336"),
                ColorTemplate.rgb("#2196F3"),
                ColorTemplate.rgb("#FFEB3B"),
                ColorTemplate.rgb("#009688"),
                ColorTemplate.rgb("#9C27B0"),
                ColorTemplate.rgb("#FF9800")
        };
        dataSet.setColors(MATERIAL_COLORS_ARRAY);

        // 2. Setup PieData and Chart
        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new PercentFormatter(expenseChart));
        pieData.setValueTextSize(14f);
        pieData.setValueTextColor(Color.WHITE);

        expenseChart.setData(pieData);
        expenseChart.setUsePercentValues(true);
        expenseChart.getDescription().setEnabled(false);
        expenseChart.setCenterText("Total:\n$" + String.format("%.2f", totalAmount));
        expenseChart.setCenterTextSize(18f);
        expenseChart.setDrawHoleEnabled(true);
        expenseChart.setHoleColor(Color.TRANSPARENT);
        expenseChart.invalidate(); // Refresh chart

        if (totalAmount == 0) {
            expenseChart.setCenterText("No expenses this month.");
        }
    }
}