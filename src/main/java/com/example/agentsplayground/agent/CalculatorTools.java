package com.example.agentsplayground.agent;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class CalculatorTools {

    @Tool(description = "Add two numbers and return the sum")
    public double add(double a, double b) {
        return a + b;
    }

    @Tool(description = "Subtract b from a and return the difference")
    public double subtract(double a, double b) {
        return a - b;
    }

    @Tool(description = "Multiply two numbers and return the product")
    public double multiply(double a, double b) {
        return a * b;
    }

    @Tool(description = "Divide a by b and return the quotient. Throws if b is zero.")
    public double divide(double a, double b) {
        if (b == 0) {
            throw new IllegalArgumentException("Cannot divide by zero");
        }
        return a / b;
    }
}
