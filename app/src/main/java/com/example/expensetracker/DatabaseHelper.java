package com.example.expensetracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.Nullable;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ExpenseTrackerDB";
    private static final int DATABASE_VERSION = 1;

    // --- Users Table for Login/Signup ---
    public static final String TABLE_USERS = "users";
    public static final String COL_USER_ID = "ID";
    public static final String COL_USERNAME = "Username";
    public static final String COL_PASSWORD = "Password";

    // --- Expenses Table ---
    public static final String TABLE_EXPENSES = "expenses";
    public static final String COL_EXP_ID = "ExpID";
    public static final String COL_EXP_USER_ID = "UserID";
    public static final String COL_EXP_AMOUNT = "Amount";
    public static final String COL_EXP_CATEGORY = "Category";
    public static final String COL_EXP_DATE = "Date"; // Format: YYYY-MM-DD
    public static final String COL_EXP_NOTE = "Note";

    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. Create Users Table
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USERNAME + " TEXT UNIQUE, " +
                COL_PASSWORD + " TEXT)";
        db.execSQL(createUsersTable);

        // 2. Create Expenses Table
        String createExpensesTable = "CREATE TABLE " + TABLE_EXPENSES + " (" +
                COL_EXP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_EXP_USER_ID + " INTEGER, " +
                COL_EXP_AMOUNT + " REAL, " + // Use REAL for currency
                COL_EXP_CATEGORY + " TEXT, " +
                COL_EXP_DATE + " TEXT, " +
                COL_EXP_NOTE + " TEXT, " +
                // Foreign Key links expense back to the user who created it
                "FOREIGN KEY(" + COL_EXP_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + "))";
        db.execSQL(createExpensesTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    // --- USER MANAGEMENT (For Login/Signup) ---

    public boolean registerUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_USERNAME, username);
        cv.put(COL_PASSWORD, password);

        long result = db.insert(TABLE_USERS, null, cv);
        db.close();
        return result != -1;
    }

    public Cursor checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " +
                        COL_USERNAME + " = ? AND " + COL_PASSWORD + " = ?",
                new String[]{username, password});
    }

    // --- EXPENSE MANAGEMENT (CRUD) ---

    // Create: Add new expense
    public boolean addExpense(long userId, double amount, String category, String date, String note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_EXP_USER_ID, userId);
        cv.put(COL_EXP_AMOUNT, amount);
        cv.put(COL_EXP_CATEGORY, category);
        cv.put(COL_EXP_DATE, date);
        cv.put(COL_EXP_NOTE, note);

        long result = db.insert(TABLE_EXPENSES, null, cv);
        db.close();
        return result != -1;
    }

    /**
     * Read: Get all expenses for a specific user, filtered by Month/Year and limited by N.
     * NEW: Added limit parameter for the Dashboard summary.
     * @param userId The ID of the current user.
     * @param monthYear YYYY-MM format (e.g., '2025-10'). Pass null for all months.
     * @param limit Maximum number of records to return.
     */
    public Cursor getTopNExpenses(long userId, @Nullable String monthYear, int limit) {
        SQLiteDatabase db = this.getReadableDatabase();

        String selectionClause = COL_EXP_USER_ID + " = ?";
        String[] selectionArgs;

        if (monthYear != null) {
            selectionClause += " AND strftime('%Y-%m', " + COL_EXP_DATE + ") = ?";
            selectionArgs = new String[]{String.valueOf(userId), monthYear};
        } else {
            selectionArgs = new String[]{String.valueOf(userId)};
        }

        // CRASH FIX: We must alias the primary key (ExpID) as _id for SimpleCursorAdapter to work.
        // We also explicitly select the Date column here.
        String query = "SELECT " + COL_EXP_ID + " AS _id, " + COL_EXP_DATE + ", " + COL_EXP_AMOUNT + ", " + COL_EXP_CATEGORY +
                " FROM " + TABLE_EXPENSES +
                " WHERE " + selectionClause +
                " ORDER BY " + COL_EXP_DATE + " DESC LIMIT " + limit; // LIMIT clause added

        return db.rawQuery(query, selectionArgs);
    }

    // Original getExpensesForUser is no longer used, replaced by getTopNExpenses in Dashboard and new methods for details.

    /**
     * Read: Get aggregated expense totals by Category for the Pie Chart.
     * @param userId The ID of the current user.
     * @param monthYear YYYY-MM format (e.g., '2025-10').
     */
    public Cursor getCategoryTotalsForMonth(long userId, String monthYear) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Query: SELECT Category, SUM(Amount) FROM expenses WHERE UserID = ? AND Date (is in monthYear) GROUP BY Category
        String query = "SELECT " + COL_EXP_CATEGORY + ", SUM(" + COL_EXP_AMOUNT + ") AS TotalAmount " +
                " FROM " + TABLE_EXPENSES +
                " WHERE " + COL_EXP_USER_ID + " = ? AND strftime('%Y-%m', " + COL_EXP_DATE + ") = ?" +
                " GROUP BY " + COL_EXP_CATEGORY;

        return db.rawQuery(query, new String[]{String.valueOf(userId), monthYear});
    }

    /**
     * NEW: Get Aggregated Category Summary for the Detail Page Table
     * @param userId The ID of the current user.
     * @param monthYear YYYY-MM format (e.g., '2025-10').
     */
    public Cursor getCategorySummary(long userId, String monthYear) {
        SQLiteDatabase db = this.getReadableDatabase();
        // NOTE: We alias the category name as _id as well, to make it work with a CursorAdapter (if needed)
        String query = "SELECT " + COL_EXP_CATEGORY + " AS _id, " + COL_EXP_CATEGORY + ", SUM(" + COL_EXP_AMOUNT + ") AS TotalAmount " +
                " FROM " + TABLE_EXPENSES +
                " WHERE " + COL_EXP_USER_ID + " = ? AND strftime('%Y-%m', " + COL_EXP_DATE + ") = ?" +
                " GROUP BY " + COL_EXP_CATEGORY +
                " ORDER BY TotalAmount DESC";

        return db.rawQuery(query, new String[]{String.valueOf(userId), monthYear});
    }

    /**
     * NEW: Get Expenses for a specific week range (used for weekly breakdown)
     * @param userId The ID of the current user.
     * @param weekStart Date (YYYY-MM-DD)
     * @param weekEnd Date (YYYY-MM-DD)
     */
    public Cursor getExpensesForWeek(long userId, String weekStart, String weekEnd) {
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT " + COL_EXP_ID + " AS _id, " + COL_EXP_DATE + ", " + COL_EXP_AMOUNT + ", " + COL_EXP_CATEGORY + ", " + COL_EXP_NOTE +
                " FROM " + TABLE_EXPENSES +
                " WHERE " + COL_EXP_USER_ID + " = ? " +
                " AND " + COL_EXP_DATE + " BETWEEN ? AND ?" +
                " ORDER BY " + COL_EXP_DATE + " DESC";

        return db.rawQuery(query, new String[]{String.valueOf(userId), weekStart, weekEnd});
    }


    // Read: Get an expense by its ID
    public Cursor getExpenseById(long expenseId) {
        SQLiteDatabase db = this.getReadableDatabase();
        // Include the _id alias here for consistency
        String query = "SELECT " + COL_EXP_ID + " AS _id, * " +
                " FROM " + TABLE_EXPENSES +
                " WHERE " + COL_EXP_ID + " = ?";
        return db.rawQuery(query, new String[]{String.valueOf(expenseId)});
    }

    // Update: Modify an existing expense
    public boolean updateExpense(long expenseId, double amount, String category, String date, String note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_EXP_AMOUNT, amount);
        cv.put(COL_EXP_CATEGORY, category);
        cv.put(COL_EXP_DATE, date);
        cv.put(COL_EXP_NOTE, note);

        int result = db.update(TABLE_EXPENSES, cv, COL_EXP_ID + "=?", new String[]{String.valueOf(expenseId)});
        db.close();
        return result > 0;
    }

    // Delete: Remove an expense
    public boolean deleteExpense(long expenseId) {
        SQLiteDatabase db = this.getWritableDatabase();

        int result = db.delete(TABLE_EXPENSES, COL_EXP_ID + "=?", new String[]{String.valueOf(expenseId)});
        db.close();
        return result > 0;
    }
}