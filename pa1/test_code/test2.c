int global_x = 10;
float global_y = 3.5;

/* Function with simple if-else and return */
int test_if_return(int n) {
    int result;
    if (n > 0) {
        result = 1;
    } else {
        result = -1;
    }
    return result;
}

/* If without else */
int test_if_no_else(int n) {
    int value = 0;
    if (n > 5) {
        value = 100;
    }
    return value;
}

/* Nested if example */
int test_nested_if(int a, int b) {
    int val = 0;
    if (a > 0) {
        if (b > 0) {
            val = a + b;
        } else {
            val = a - b;
        }
    } else {
        val = -1;
    }
    return val;
}

/* While loop */
int test_while(int n) {
    int sum = 0;
    int i = 0;
    while (i < n) {
        sum = sum + i;
        i = i + 1;
    }
    return sum;
}

/* For loop (must follow the grammar: for(assign; expr; assign)) */
int test_for(int n) {
    int sum = 0;
    int i;
    for (i = 0; i < n; i = i + 1) {
        sum = sum + i;
    }
    return sum;
}

/* Function call (allowed only as standalone statements or right-hand side of assignment) */
int helper(int x) {
    return x * 2;
}

int test_call(int n) {
    int a;
    int b;
    a = helper(n);
    b = helper(a);
    return a + b;
}

/* If inside loop */
int test_if_in_loop(int n) {
    int sum = 0;
    int i;
    for (i = 0; i < n; i = i + 1) {
        if (i == 5) {
            sum = sum + 100;
        }
        sum = sum + i;
    }
    return sum;
}

/* Nested control flow (no function calls inside expressions) */
int test_complex(int n) {
    int result = 0;
    int i;
    for (i = 0; i < n; i = i + 1) {
        int temp;
        temp = i * 2;
        if (temp > 10) {
            result = result + temp;
        } else {
            result = result - temp;
            if (i == 3) {
                result = 999;
            }
        }
    }
    return result;
}

/* Main function combining calls */
int main() {
    int x = 5;
    int y;
    y = test_if_return(x);
    y = test_if_no_else(x);
    y = test_nested_if(x, 3);
    y = test_while(10);
    y = test_for(10);
    y = test_call(x);
    y = test_if_in_loop(10);
    y = test_complex(5);
    return 0;
}

