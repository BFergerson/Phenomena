import java.util.*;
import java.util.regex.*;

public class WeirdNullControlFlow {

    private double real;
    private double imaginary;

    WeirdNullControlFlow(double _real, double _imaginary) {
        real = _real;
        imaginary = _imaginary;
    }

    public WeirdNullControlFlow cos() {
        return new WeirdNullControlFlow(
                Math.cos(real) * Math.cosh(imaginary),
                -Math.sin(real) * imaginary
        );
    }

 }