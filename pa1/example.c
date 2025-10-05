int global_counter = 0;
float global_scale_factor = 1.5;
int final_result;


int factorial(int n) {
    if (n <= 1) {
        return 1;
    } else {
        global_counter = global_counter + 1;
        return n * factorial(n - 1);
    }
}

float adjust_value(float base, int iterations) {
    int i = 0;
    float adjusted;
    adjusted = base;

    while (i < iterations) {
        adjusted = (adjusted + i) / 2.0;
        i = i + 1;
    }
    
    global_scale_factor = global_scale_factor - 0.1;
    return adjusted;
}

int main() {
    int loop_limit = 5;
    int i; 
    int fact_val;
    float temp_float;

    for (i = 1; i <= loop_limit; i = i + 1) {
        
        fact_val = factorial(i) + global_counter;

        if (fact_val > 20) {
            temp_float = adjust_value(10.0, i);
            final_result = fact_val + global_counter;

        } else {
            final_result = fact_val - global_counter;
            
            if (i == 4) {
                final_result = 999; 
            }
        }
    }
    
    return final_result;
}