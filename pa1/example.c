/*
 * test_comprehensive.c
 *
 * includes:
 *  - Global and local declarations (int, float, mixed).
 *  - All control flow statements (if, if-else, for, while).
 *  - Nested control structures.
 *  - All expression operators (arithmetic, relational, equality, unary).
 *  - Function calls (in expressions and standalone).
 *  - Edge cases (empty statements, unreachable code, return without value).
 */

// === Global Declarations ===
int global_result = 0;
float PI = 3.14;


// === Function Definitions ===

// A helper function to be used in various contexts.
int calculate_offset(int input) {
    int offset;
    offset = (input * 2) - 1;
    return offset;
}


// --- Test Section 1: Control Flow and Expressions ---

// Tests all expression operators and nested if-else structures.
int test_expressions_and_nesting(int a, int b) {
    int result = 0;

    // Test relational and equality operators in nested if-else
    if (a > b) {
        if (a >= 10) {
            result = 1;
        } else {
            result = 2;
        }
    } else {
        if (a < b) {
            if (b <= 0) {
                result = -1;
            }
        } else {
            // This block is for when a == b
            if (a != 0) {
                result = 0;
            }
        }
    }
    
    // Test arithmetic and unary operators
    result = result + (a * b);
    result = result - (b / 2);
    result = -result;

    return result;
}


// --- Test Section 2: Loops and Function Calls ---

// Tests for/while loops, with function calls inside.
float test_loops_and_calls(int limit) {
    int i;
    float sum = 0.0;
    
    // Test a for loop with a function call inside its body
    for (i = 0; i < limit; i = i + 1) {
        sum = sum + calculate_offset(i);
    }

    // Test a while loop
    while (limit > 0) {
        limit = limit - 1;
        sum = sum - 1.0;
    }

    return sum;
}


// --- Test Section 3: Edge Cases and Grammar Completeness ---

// A function that simulates a void return type for standalone call tests.
int update_global_status(int status) {
    global_result = status;
    return 0; // Return value is ignored
}

// Tests grammar edge cases like empty statements and unreachable code.
int test_edge_cases() {
    int local_var = 10;

    // Test standalone function call (callStmt)
    update_global_status(1);

    // Test empty statement (stmt -> SEMI)
    if (local_var > 5) {
        ; // An empty 'then' block
    } else {
        update_global_status(-1);
    }
    
    // Test unreachable code after a return
    return local_var;
    
    local_var = 99; // This line should be identified as dead code.
}

// Tests the grammatically valid but semantically odd 'return;' statement.
int test_return_without_value() {
    return;
}


// --- Main Execution Block ---

int main() {
    // Test various declaration styles
    int x = 20, y = 10;
    float f1, f2 = 9.9;
    int final_code;

    // Execute tests
    final_code = test_expressions_and_nesting(x, y);
    f1 = test_loops_and_calls(5);
    final_code = test_edge_cases();
    
    update_global_status(final_code);
    
    test_return_without_value();

    return 0;
}