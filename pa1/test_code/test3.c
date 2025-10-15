// test_edge_cases.c
// Purpose: To achieve grammar coverage by testing edge cases like
// empty statements, standalone function calls, and unreachable code.

/* Block comments should be ignored. */
int g_counter = 0;

// Helper function that modifies a global, simulating a void return.
int void_like_helper(int val) {
    g_counter = g_counter + val;
    return 0;
}

// Tests standalone function calls (callStmt rule).
void test_standalone_calls() {
    // This call is a statement itself, not part of an assignment.
    void_like_helper(10);
}

// Tests empty statements (stmt -> SEMI rule).
int test_empty_statements(int n) {
    int i = 0;

    // An empty statement as the body of a while loop.
    while (i < n)
        i = i + 1;

    // An empty statement in an if/else body.
    if (n > 10)
        ; // Do nothing.
    else
        ; // Also do nothing.
    
    ;; // Consecutive empty statements.
    return i;
}

// Tests the 'return' statement without an expression (retStmt -> 'return' SEMI).
// NOTE: This is valid by the grammar, even if semantically strange for a non-void function.
int test_no_expr_return() {
    g_counter = 5;
    // The grammar allows 'return;', so this must be parsed correctly.
    // In a real C compiler this would be a warning/error without a 'void' return type.
    return;
}

// Tests unreachable code after a return statement to check CFG dead code elimination.
int test_unreachable_code() {
    int x = 1;
    if (x == 1) {
        return 10;
        x = 99; // This is unreachable code.
    }
    return 20;
}

int main() {
    // This file tests grammar rules that are not commonly used.
    test_standalone_calls();
    test_empty_statements(3);
    test_no_expr_return();
    test_unreachable_code();
    
    return g_counter;
}