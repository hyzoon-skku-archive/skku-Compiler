// test_expressions_and_declarations.c
// Purpose: To test all expression operators, data types (int, float),
// and various declaration styles allowed by the grammar.

// Global declarations test
int g_int1, g_int2 = 100;
float g_float1 = 3.14, g_float2;

// A helper function for testing calls inside expressions.
int multiply(int a, int b) {
    return a * b;
}

// Tests all supported binary and unary operators.
int test_all_operators(int a, int b) {
    int res = 0;
    float fres = 0.5;

    // Arithmetic operators
    res = a * b;
    res = a / b;
    res = a + b;
    res = a - b;
    
    // Relational operators
    if (a > b) { res = 1; }
    if (a < b) { res = 2; }
    if (a >= b) { res = 3; }
    if (a <= b) { res = 4; }

    // Equality operators
    if (a == b) { res = 5; }
    if (a != b) { res = 6; }

    // Unary operators
    res = -a;
    res = +b;

    return res;
}

// Tests function calls and parentheses within expressions.
float test_expression_contexts() {
    int x = 10;
    int y = 20;
    float z;

    // Test a function call as part of an expression (atom -> call).
    z = multiply(x, y) + 5.5;

    // Test a parenthesized expression (atom -> LPAREN expr RPAREN).
    z = (x + y) * 2.0;
    
    return z;
}

int main() {
    // Local declarations test (single, multiple, initialized, uninitialized)
    int local_int1;
    int local_int2 = 50, local_int3;
    float local_float = 0.99;
    int result;

    result = test_all_operators(10, 5);
    g_float2 = test_expression_contexts();

    return 0;
}