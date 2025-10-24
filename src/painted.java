import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

public class painted extends JPanel {
    private int k, j, m;

    public painted(int k, int j, int m) {
        this.k = k;
        this.j = j;
        this.m = m;
        setPreferredSize(new Dimension(800, 600));
    }

    // функция
    private double f(double x) {
        return -Math.pow(Math.cos(Math.PI * Math.pow(x, m) / 2.0), k) + 2 * Math.pow(x, j);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        int w = getWidth();
        int h = getHeight();

        // оси
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawLine(40, h - 40, w - 20, h - 40); // ось X
        g2.drawLine(40, 20, 40, h - 40);         // ось Y
        g2.drawString("x", w - 30, h - 25);
        g2.drawString("f(x)", 25, 30);

        // диапазон по X
        double xMin = -10, xMax = 10;
        double yMin = Double.MAX_VALUE, yMax = Double.MIN_VALUE;

        // находим диапазон значений функции
        for (int i = 0; i <= 2000; i++) {
            double x = xMin + i * (xMax - xMin) / 2000.0;
            double y = f(x);
            yMin = Math.min(yMin, y);
            yMax = Math.max(yMax, y);
        }

        double xScale = (w - 60) / (xMax - xMin);
        double yScale = (h - 60) / (yMax - yMin);

        // если m=1 → рисуем все периоды T=4
        if (m == 1) {
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(
                    1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1.0f, new float[]{5, 5}, 0));

            double T = 4.0;
            // начинаем с ближайшего кратного T слева
            for (double X = Math.floor(xMin / T) * T; X <= xMax; X += T) {
                double px = 40 + (X - xMin) * xScale;
                g2.drawLine((int) px, 20, (int) px, h - 40);
                g2.drawString("T", (int) px + 2, h - 45);
            }
        }

        // график функции
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


        g2.setColor(Color.BLACK);
        g2.drawString("f(x) = -cos^" + k + "(πx^" + m + "/2) + 2x^" + j, 50, 15);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            int k = Integer.parseInt(JOptionPane.showInputDialog("Введите k:"));
            int j = Integer.parseInt(JOptionPane.showInputDialog("Введите j:"));
            int m = Integer.parseInt(JOptionPane.showInputDialog("Введите m:"));

            JFrame frame = new JFrame("График функции");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new painted(k, j, m));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
