import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class Interpolation {
    public static double interpolate(double x, double[] xNodes, double[] yNodes, double[][] dividedDiff) {
        int n = xNodes.length - 1;
        double result = yNodes[0];
        double term = 1.0;

        for (int i = 1; i <= n; i++) {
            term *= (x - xNodes[i - 1]);
            result += dividedDiff[0][i] * term;
        }
        return result;
    }

    public static double[][] computeDividedDifferences(double[] x, double[] y) {
        int n = x.length;
        double[][] divDiff = new double[n][n];
        for (int i = 0; i < n; i++) {
            divDiff[i][0] = y[i];
        }
        for (int j = 1; j < n; j++) {
            for (int i = 0; i < n - j; i++) {
                divDiff[i][j] = (divDiff[i + 1][j - 1] - divDiff[i][j - 1]) / (x[i + j] - x[i]);
            }
        }
        return divDiff;
    }

    public static void main(String[] args) {
        double a = 0, b = 1;
        int n = 20;
        double h = (b - a) / (n - 1);
        double[] xNodes = new double[n];
        double[] yNodes = new double[n];


        for (int i = 0; i < n; i++) {
            xNodes[i] = a + i * h;
            yNodes[i] = Math.sin(Math.PI * Math.pow(xNodes[i], 1.0 / 4.0));
        }

        double[][] dividedDiff = computeDividedDifferences(xNodes, yNodes);

        double[] halfIndices = new double[n / 2];
        for (int i = 0; i < n / 2; i++) {
            halfIndices[i] = a + (i + 0.5) * h;
        }

        double[] interpolatedValues = new double[halfIndices.length];
        for (int i = 0; i < halfIndices.length; i++) {
            interpolatedValues[i] = interpolate(halfIndices[i], xNodes, yNodes, dividedDiff);
        }

        double maxError = 0, meanSquareError = 0;
        for (int i = 0; i < halfIndices.length; i++) {
            double trueValue = Math.sin(Math.PI * Math.pow(halfIndices[i], 1.0 / 4.0));
            double error = Math.abs(trueValue - interpolatedValues[i]);
            maxError = Math.max(maxError, error);
            meanSquareError += error * error;
        }
        meanSquareError /= halfIndices.length;
        double rmsError = Math.sqrt(meanSquareError);

        JFrame frame = new JFrame("Interpolation Graph");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                int w = getWidth();
                int h = getHeight();
                g2d.setColor(Color.BLACK);
                g2d.drawLine(50, h - 50, 50, 50);
                g2d.drawLine(50, h - 50, w - 50, h - 50);

                g2d.setColor(Color.BLUE);
                for (int i = 0; i < n - 1; i++) {
                    double x1 = 50 + i * (w - 100) / (n - 1);
                    double y1 = h - 50 - yNodes[i] * (h - 100);
                    double x2 = 50 + (i + 1) * (w - 100) / (n - 1);
                    double y2 = h - 50 - yNodes[i + 1] * (h - 100);
                    g2d.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
                }

                g2d.setColor(Color.RED);
                for (int i = 0; i < halfIndices.length - 1; i++) {
                    double x1 = 50 + (halfIndices[i] - a) * (w - 100) / (b - a);
                    double y1 = h - 50 - interpolatedValues[i] * (h - 100);
                    double x2 = 50 + (halfIndices[i + 1] - a) * (w - 100) / (b - a);
                    double y2 = h - 50 - interpolatedValues[i + 1] * (h - 100);
                    g2d.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
                }
            }
        };

        frame.add(panel);
        frame.setVisible(true);

        System.out.println("Max Error: " + maxError);
        System.out.println("RMS Error: " + rmsError);
    }
}