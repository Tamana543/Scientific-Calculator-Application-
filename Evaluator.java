import java.util.Collections;
import java.util.Map;

public final class Evaluator {
     private final String s;
    private final boolean deg;
    private final double xValue;
    private final Map<Character, Double> vars;
    private int i = -1;
    private int c;
 
    
    public Evaluator(String s, boolean deg, double xValue) {
        this(s, deg, xValue, null);
    }

     public Evaluator(String s, boolean deg, double xValue, Map<Character, Double> vars) {
        this.s = s;
        this.deg = deg;
        this.xValue = xValue;
        this.vars = vars != null ? vars : Collections.emptyMap();
    }
 
    public double evaluate() {
        next();
        double v = expr();
        if (i < s.length()) throw new RuntimeException("bad expression");
        return v;
    }
     public static double[] sampleFunction(String funcText, boolean degMode, double xMin, double xMax, int points) {
        return sampleFunction(funcText, degMode, xMin, xMax, points, null);
    }
    public static double[] sampleFunction(String funcText, boolean degMode, double xMin, double xMax, int points, Map<Character, Double> vars) {
        double[] ys = new double[points];
        for (int i = 0; i < points; i++) {
            double x = xMin + (xMax - xMin) * i / (points - 1);
            try {
                ys[i] = new Evaluator(funcText, degMode, x, vars).evaluate();
            } catch (Exception ex) {
                ys[i] = Double.NaN;
            }
        }
        return ys;
    }

    private void next() { i++; c = i < s.length() ? s.charAt(i) : -1; }
    private boolean eat(int ch) { if (c == ch) { next(); return true; } return false; }
    private double expr() {
        double v = term();
        while (true) {
            if (eat('+')) v += term();
            else if (eat('-')) v -= term();
            else return v;
        }
    }

    private double term() {
        double v = power();
        while (true) {
            if (eat('*')) v *= power();
            else if (eat('/')) { double d = power(); if (d == 0) throw new ArithmeticException("Div by 0"); v /= d; }
            else if (startsAtom()) v *= power(); // implicit multiplication: "8X", "2(X+1)", "2sin(X)"
            else return v;
        }
    }

    private boolean startsAtom() {
        return c == '(' || c == 'X' || (c >= 'A' && c <= 'F') || (c >= '0' && c <= '9') || c == '.' || (c >= 'a' && c <= 'z');
    }
 
    private double power() {
        double v = unary();
        return eat('^') ? Math.pow(v, power()) : v;
    }

    private double unary() {
        if (eat('-')) return -unary();
        if (eat('+')) return unary();
        double v = atom();
        while (eat('!')) v = factorial(v);
        return v;
    }

    private double atom() {
        if (eat('(')) { double v = expr(); if (!eat(')')) throw new RuntimeException("missing )"); return v; }
        if (eat('X')) return xValue;
        if (c >= 'A' && c <= 'F') { char name = (char) c; next(); return vars.getOrDefault(name, 0.0); }
        int start = i;
        if ((c >= '0' && c <= '9') || c == '.') {
            while ((c >= '0' && c <= '9') || c == '.') next();
            return Double.parseDouble(s.substring(start, i));
        }
        if (c >= 'a' && c <= 'z') {
            while (c >= 'a' && c <= 'z') next();
            String name = s.substring(start, i);
            return switch (name) {
                case "pi" -> Math.PI;
                case "e" -> Math.E;
                case "sin", "cos", "tan", "sinh", "cosh", "tanh", "log", "ln", "sqrt", "cbrt" -> {
                    if (!eat('(')) throw new RuntimeException("expected ( after " + name);
                    double arg = expr();
                    if (!eat(')')) throw new RuntimeException("missing )");
                    yield apply(name, arg);
                }
                default -> throw new RuntimeException("unknown: " + name);
            };
        }
        throw new RuntimeException("unexpected char");
    }


    private double apply(String fn, double arg) {
        double rad = deg ? Math.toRadians(arg) : arg;
        return switch (fn) {
            case "sin" -> Math.sin(rad);
            case "cos" -> Math.cos(rad);
            case "tan" -> Math.tan(rad);
            case "sinh" -> Math.sinh(arg);
            case "cosh" -> Math.cosh(arg);
            case "tanh" -> Math.tanh(arg);
            case "log" -> { if (arg <= 0) throw new ArithmeticException("log of non-positive"); yield Math.log10(arg); }
            case "ln" -> { if (arg <= 0) throw new ArithmeticException("ln of non-positive"); yield Math.log(arg); }
            case "sqrt" -> { if (arg < 0) throw new ArithmeticException("sqrt of negative"); yield Math.sqrt(arg); }
            case "cbrt" -> Math.cbrt(arg);
            default -> throw new RuntimeException("unknown fn");
        };
    }

   private double factorial(double v) {
        if (v < 0 || v != Math.floor(v)) throw new ArithmeticException("factorial needs non-negative integer");
        double r = 1;
        for (long n = (long) v; n > 1; n--) r *= n;
        return r;
    }
}