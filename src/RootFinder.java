import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

public class RootFinder extends JPanel {
    private final int k, j, m;
    private final double root;
    private final String method;

    public RootFinder(double root, String method, int k, int j, int m) {
        this.root = root;
        this.method = method;
        this.k = k;
        this.j = j;
        this.m = m;
        setPreferredSize(new Dimension(800, 600));
    }

    private double f(double x) {
        return -Math.pow(Math.cos(Math.PI * Math.pow(x, m) / 2.0), k) + 2 * Math.pow(x, j);
    }

    // B
    public static double bisection(double a, double b, double eps, int k, int j, int m) {
        RootFinder instance = new RootFinder(0, "", k, j, m);
        if (instance.f(a) * instance.f(b) >= 0) {
            throw new IllegalArgumentException("Function does not change sign on the interval");
        }
        int iterCount = 0;
        while ((b - a) / 2 > eps && iterCount < 100) {
            double c = (a + b) / 2;
            if (Math.abs(instance.f(c)) < eps) {
                return c;
            }
            if (instance.f(a) * instance.f(c) < 0) {
                b = c;
            } else {
                a = c;
            }
            iterCount++;
        }
        return (a + b) / 2;
    }

    // S
    public static double secant(double x0, double x1, double eps, int k, int j, int m) {
        RootFinder instance = new RootFinder(0, "", k, j, m);
        int iterCount = 0;
        while (Math.abs(instance.f(x1)) > eps && iterCount < 100) {
            double fx0 = instance.f(x0);
            double fx1 = instance.f(x1);
            if (Math.abs(fx1 - fx0) < 1e-15) {
                throw new ArithmeticException("Division by zero in secant method");
            }
            double x2 = x1 - fx1 * (x1 - x0) / (fx1 - fx0);
            x0 = x1;
            x1 = x2;
            iterCount++;
        }
        return x1;
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        int w = getWidth();
        int h = getHeight();

        g2.setColor(Color.LIGHT_GRAY);
        g2.drawLine(40, h - 40, w - 20, h - 40); // X
        g2.drawLine(40, 20, 40, h - 40);         // Y
        g2.drawString("x", w - 30, h - 25);
        g2.drawString("f(x)", 25, 30);

        double xMin = 0, xMax = 1;
        double yMin = Double.MAX_VALUE;
        double yMax = Double.MIN_VALUE;


        for (int i = 0; i <= 2000; i++) {
            double x = xMin + i * (xMax - xMin) / 2000.0;
            double y = f(x);
            yMin = Math.min(yMin, y);
            yMax = Math.max(yMax, y);
        }

        double xScale = (w - 60) / (xMax - xMin);
        double yScale = (h - 60) / (yMax - yMin);

        // Fuc
        g2.setColor(Color.BLUE);
        g2.setStroke(new BasicStroke(2));
        Path2D path = new Path2D.Double();

        for (int i = 0; i <= 2000; i++) {
            double x = xMin + i * (xMax - xMin) / 2000.0;
            double y = f(x);
            double px = 40 + (x - xMin) * xScale;
            double py = h - 40 - (y - yMin) * yScale;

            if (i == 0) {
                path.moveTo(px, py);
            } else {
                path.lineTo(px, py);
            }
        }
        g2.draw(path);

        // Draw the point
        g2.setColor(Color.RED);
        double rootY = f(root);
        double px = 40 + (root - xMin) * xScale;
        double py = h - 40 - (rootY - yMin) * yScale;
        g2.fillOval((int) px - 5, (int) py - 5, 10, 10);
        g2.drawString(String.format("Root (%s): %.10f", method, root), 50, 30);


        g2.setColor(Color.BLACK);
        g2.drawString("f(x) = -cos^" + k + "(πx^" + m + "/2) + 2x^" + j, 50, 15);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            int k = 2, j = 3, m = 4;
            double eps = 1e-10;

            try {
                double rootBisection = bisection(0, 1, eps, k, j, m);
                double rootSecant = secant(0, 1, eps, k, j, m);

                System.out.printf("Function: f(x) = -cos^%d(πx^%d/2) + 2x^%d%n", k, m, j);
                System.out.printf("Root (Bisection): %.10f%n", rootBisection);
                System.out.printf("Root (Secant): %.10f%n", rootSecant);

                // Bi
                JFrame frameBisection = new JFrame("Function Graph (Bisection)");
                frameBisection.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frameBisection.add(new RootFinder(rootBisection, "Bisection", k, j, m));
                frameBisection.pack();
                frameBisection.setLocation(100, 100);
                frameBisection.setVisible(true);

                // Se
                JFrame frameSecant = new JFrame("Function Graph (Secant)");
                frameSecant.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frameSecant.add(new RootFinder(rootSecant, "Secant", k, j, m));
                frameSecant.pack();
                frameSecant.setLocation(900, 100);
                frameSecant.setVisible(true);
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (ArithmeticException e) {
                System.out.println("Error: " + e.getMessage());
            }
        });
    }
}