// test_control_flow.c
// Purpose: To test fundamental control flow structures like if-else, for, and while,
// including nested scenarios.

int global_status = 0;

// Tests a standard if-else statement.
int test_basic_if_else(int n) {
    if (n > 0) {
        return 1;
    } else {
        return -1;
    }
}

// Tests an if statement without an else clause.
int test_if_no_else(int n) {
    int x = 10;
    if (n < 0) {
        x = -10;
    }
    return x;
}

// Tests a standard while loop.
int test_basic_while(int n) {
    int sum = 0;
    int i = 0;
    while (i < n) {
        sum = sum + i;
        i = i + 1;
    }
    return sum;
}

// Tests a standard for loop, respecting the grammar's 'assign; expr; assign' structure.
int test_basic_for() {
    int total = 0;
    int i;
    for (i = 0; i < 5; i = i + 1) {
        total = total + 2;
    }
    return total; // Should be 10
}

// Tests nested control flow: an if-else statement inside a for loop.
int test_nested_control_flow(int limit) {
    int i;
    int special_sum = 0;
    for (i = 0; i < limit; i = i + 1) {
        if (i == 3) {
            special_sum = special_sum + 100;
        } else {
            special_sum = special_sum + 1;
        }
    }
    return special_sum;
}

int main() {
    int result;
    result = test_basic_if_else(10);
    result = test_if_no_else(-5);
    result = test_basic_while(4);
    result = test_basic_for();
    result = test_nested_control_flow(5);
    return 0;
}