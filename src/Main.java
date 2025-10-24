import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

public class Main extends JPanel {
    private int k, j, m;

    public Main(int k, int j, int m) {
        this.k = k;
        this.j = j;
        this.m = m;
        setPreferredSize(new Dimension(600, 400));
    }


    private double f(double x) {
        return -Math.pow(Math.cos(Math.PI * Math.pow(x, m) / 2.0), k) + 2 * Math.pow(x, j);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        int w = getWidth();
        int h = getHeight();


        g2.setColor(Color.LIGHT_GRAY);
        g2.drawLine(40, h - 40, w - 20, h - 40);
        g2.drawLine(40, 20, 40, h - 40);


        g2.drawString("x", w - 30, h - 25);
        g2.drawString("f(x)", 25, 30);


        double xMin = -1, xMax = 1;
        double yMin = Double.MAX_VALUE, yMax = Double.MIN_VALUE;

        for (int i = 0; i <= 500; i++) {
            double x = xMin + i * (xMax - xMin) / 500.0;
            double y = f(x);
            yMin = Math.min(yMin, y);
            yMax = Math.max(yMax, y);
        }

        double xScale = (w - 60) / (xMax - xMin);
        double yScale = (h - 60) / (yMax - yMin);


        g2.setColor(Color.BLUE);
        Path2D path = new Path2D.Double();

        for (int i = 0; i <= 500; i++) {
            double x = xMin + i * (xMax - xMin) / 500.0;
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


        g2.setColor(Color.BLACK);
        g2.drawString("f(x) = -cos^" + k + "(πx^" + m + "/2) + 2x^" + j, 50, 15);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("График функции");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


            frame.add(new Main(2, 1, 2));

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}