package com.doukyo.expense

// Stored as a string in the `category` column (@Enumerated(STRING)). A closed set
// of categories — an enum makes illegal values unrepresentable (vs a free string).
enum class ExpenseCategory {
    GROCERIES,
    BILLS,
    DINING,
    HOUSEHOLD,
    OTHER,
}
