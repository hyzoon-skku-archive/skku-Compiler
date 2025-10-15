int global_counter = 0;

// This function specifically tests the empty block scenario
int test_empty_blocks(int x) {
    int result = 0;
    int i;
    
    for (i = 0; i < 5; i = i + 1) {
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

// Test if without else (should not create empty merge block)
int test_simple_if(int n) {
    if (n > 0) {
        n = n + 1;
    }
    return n;
}

// Test if-else where else has nested if (creates empty blocks)
int test_nested_if_in_else(int n) {
    int val = 0;
    
    if (n > 10) {
        val = 100;
    } else {
        val = 50;
        if (n == 5) {
            val = 999;
        }
    }
    
    return val;
}

int main() {
    int x = 3;
    int result;
    
    result = test_empty_blocks(x);
    result = test_simple_if(x);
    result = test_nested_if_in_else(x);
    
    return 0;
}