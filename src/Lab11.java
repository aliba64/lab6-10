import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

public class Lab11 extends JFrame {
    // Исходные данные
    private static final double a = 1.0;
    private static final double b = 2.0;
    private static final double beta = 0.5;
    private static final int m = 2;
    private static final int n = 3;
    private static final double epsilon = 1e-6;

    // Начальные условия
    private static final double d = 10.0;
    private static final double x0 = a + beta * (b - a);
    private static final double y0 = a + beta * (b - a);

    // Результаты оптимизации
    private List<Point2D.Double> trajectory;
    private List<Double> qValues;
    private double finalX, finalY;
    private int iterations;

    public Lab11() {
        setTitle("Lab 11 - Координатный спуск");
        setSize(1600, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Выполняем оптимизацию
        coordinateDescent();

        // Панель с исходными данными и результатами (сверху)
        add(new DataPanel(), BorderLayout.NORTH);

        // Панель с графиками
        JPanel graphPanel = new JPanel(new GridLayout(1, 3));
        graphPanel.add(new SurfacePlotPanel());
        graphPanel.add(new TrajectoryPanel());
        graphPanel.add(new QPlotPanel());
        add(graphPanel, BorderLayout.CENTER);
    }

    // Целевая функция f(x, y)
    private double f(double x, double y) {
        double term1 = Math.pow(x, 2 * m);
        double term2 = Math.pow(y, 2 * n);
        return term1 + term2;
    }

    // Метод золотого сечения для одномерной минимизации
    private double goldenSectionSearch(double left, double right, boolean isX, double fixedCoord) {
        final double phi = (1.0 + Math.sqrt(5.0)) / 2.0;
        final double resphi = 2.0 - phi;

        double a = left;
        double b = right;
        double tol = epsilon;

        double x1 = a + resphi * (b - a);
        double x2 = b - resphi * (b - a);

        double f1 = isX ? f(x1, fixedCoord) : f(fixedCoord, x1);
        double f2 = isX ? f(x2, fixedCoord) : f(fixedCoord, x2);

        while (Math.abs(b - a) > tol) {
            if (f1 < f2) {
                b = x2;
                x2 = x1;
                f2 = f1;
                x1 = a + resphi * (b - a);
                f1 = isX ? f(x1, fixedCoord) : f(fixedCoord, x1);
            } else {
                a = x1;
                x1 = x2;
                f1 = f2;
                x2 = b - resphi * (b - a);
                f2 = isX ? f(x2, fixedCoord) : f(fixedCoord, x2);
            }
        }

        return (a + b) / 2.0;
    }

    // Координатный спуск
    private void coordinateDescent() {
        trajectory = new ArrayList<>();
        qValues = new ArrayList<>();

        double x = x0;
        double y = y0;
        double prevF = f(x, y);

        trajectory.add(new Point2D.Double(x, y));

        int k = 0;
        int maxIterations = 1000;

        while (k < maxIterations) {
            // Минимизация по x
            double newX = goldenSectionSearch(-d, d, true, y);

            // Минимизация по y
            double newY = goldenSectionSearch(-d, d, false, newX);

            double currentF = f(newX, newY);
            double delta = Math.abs(currentF - prevF);

            // Вычисляем q^(k) = -ln(delta^(k)) / ln(10)
            if (delta > 0) {
                double q = -Math.log(delta) / Math.log(10);
                qValues.add(q);
            }

            k++;
            trajectory.add(new Point2D.Double(newX, newY));

            // Проверка условий завершения
            if (delta < epsilon || Math.abs(newX - x) < epsilon && Math.abs(newY - y) < epsilon) {
                break;
            }

            x = newX;
            y = newY;
            prevF = currentF;
        }

        finalX = x;
        finalY = y;
        iterations = k;

        printResults();
    }

    // Вывод результатов в консоль
    private void printResults() {
        System.out.println("=== ИСХОДНЫЕ ДАННЫЕ ===");
        System.out.println("a = " + a);
        System.out.println("b = " + b);
        System.out.println("β = " + beta);
        System.out.println("m = " + m);
        System.out.println("n = " + n);
        System.out.println("ε = " + epsilon);
        System.out.println();

        System.out.println("=== РЕЗУЛЬТАТЫ РЕШЕНИЯ ===");
        System.out.println("Количество итераций k = " + iterations);
        System.out.println("x^(k) = " + finalX);
        System.out.println("y^(k) = " + finalY);
        System.out.println("f(x^(k), y^(k)) = " + f(finalX, finalY));
    }

    // Панель с исходными данными и результатами
    class DataPanel extends JPanel {
        public DataPanel() {
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(1600, 180));
            setBackground(new Color(240, 240, 245));
            setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

            JPanel mainPanel = new JPanel(new GridLayout(1, 2, 30, 0));
            mainPanel.setOpaque(false);

            // Левая панель - исходные данные
            JPanel leftPanel = new JPanel(new BorderLayout());
            leftPanel.setOpaque(false);
            leftPanel.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(Color.DARK_GRAY, 2),
                    "ИСХОДНЫЕ ДАННЫЕ",
                    0, 0, new Font("Arial", Font.BOLD, 16)));

            JTextArea inputData = new JTextArea();
            inputData.setEditable(false);
            inputData.setFont(new Font("Monospaced", Font.PLAIN, 14));
            inputData.setBackground(new Color(250, 250, 250));
            inputData.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            inputData.setText(String.format(
                    "  a = %.1f\n" +
                            "  b = %.1f\n" +
                            "  β = %.1f\n" +
                            "  m = %d\n" +
                            "  n = %d\n" +
                            "  ε = %.0e\n\n" +
                            "  Функция: f(x,y) = x^%d + y^%d\n" +
                            "  Начальная точка: (%.2f, %.2f)",
                    a, b, beta, m, n, epsilon, 2*m, 2*n, x0, y0
            ));
            leftPanel.add(inputData, BorderLayout.CENTER);

            // Правая панель - результаты
            JPanel rightPanel = new JPanel(new BorderLayout());
            rightPanel.setOpaque(false);
            rightPanel.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(Color.DARK_GRAY, 2),
                    "РЕЗУЛЬТАТЫ РЕШЕНИЯ",
                    0, 0, new Font("Arial", Font.BOLD, 16)));

            JTextArea results = new JTextArea();
            results.setEditable(false);
            results.setFont(new Font("Monospaced", Font.PLAIN, 14));
            results.setBackground(new Color(250, 250, 250));
            results.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            results.setText(String.format(
                    "  Количество итераций: k = %d\n\n" +
                            "  Найденный минимум:\n" +
                            "    x^(k) = %.10f\n" +
                            "    y^(k) = %.10f\n\n" +
                            "  Значение функции:\n" +
                            "    f(x^(k), y^(k)) = %.10e\n\n" +
                            "  Величина q^(k) на последней итерации: %.4f",
                    iterations, finalX, finalY, f(finalX, finalY),
                    qValues.isEmpty() ? 0.0 : qValues.get(qValues.size() - 1)
            ));
            rightPanel.add(results, BorderLayout.CENTER);

            mainPanel.add(leftPanel);
            mainPanel.add(rightPanel);
            add(mainPanel, BorderLayout.CENTER);
        }
    }

    // Панель для графика поверхности f(x,y)
    class SurfacePlotPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int margin = 50;

            // Заголовок
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString("График функции f(x,y)", width / 2 - 80, 20);

            // Контурный график
            int plotWidth = width - 2 * margin;
            int plotHeight = height - 2 * margin;

            double range = 3.0;
            int steps = 50;
            double step = 2 * range / steps;

            // Находим максимум для нормализации цвета
            double maxF = 0;
            for (int i = 0; i <= steps; i++) {
                for (int j = 0; j <= steps; j++) {
                    double x = -range + i * step;
                    double y = -range + j * step;
                    double val = f(x, y);
                    if (val < 100) maxF = Math.max(maxF, val);
                }
            }

            // Рисуем контуры
            for (int i = 0; i < steps; i++) {
                for (int j = 0; j < steps; j++) {
                    double x = -range + i * step;
                    double y = -range + j * step;
                    double val = f(x, y);

                    int screenX = margin + (int) ((x + range) / (2 * range) * plotWidth);
                    int screenY = margin + plotHeight - (int) ((y + range) / (2 * range) * plotHeight);

                    float intensity = (float) Math.min(1.0, val / maxF);
                    g2d.setColor(new Color(intensity, 0, 1 - intensity));
                    g2d.fillRect(screenX, screenY, (int) (plotWidth / steps) + 1, (int) (plotHeight / steps) + 1);
                }
            }

            // Оси
            g2d.setColor(Color.BLACK);
            g2d.drawLine(margin, height - margin, width - margin, height - margin);
            g2d.drawLine(margin, margin, margin, height - margin);

            // Метки
            g2d.drawString("x", width - margin + 10, height - margin);
            g2d.drawString("y", margin - 20, margin - 10);
            g2d.drawString(String.format("%.1f", -range), margin - 20, height - margin + 5);
            g2d.drawString(String.format("%.1f", range), margin - 20, margin + 5);
            g2d.drawString(String.format("%.1f", -range), margin - 5, height - margin + 15);
            g2d.drawString(String.format("%.1f", range), width - margin - 5, height - margin + 15);
        }
    }

    // Панель для траектории оптимизации
    class TrajectoryPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int margin = 50;

            // Заголовок
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString("Траектория оптимизации", width / 2 - 80, 20);

            int plotWidth = width - 2 * margin;
            int plotHeight = height - 2 * margin;

            // Находим диапазон для масштабирования
            double minX = trajectory.stream().mapToDouble(p -> p.x).min().orElse(-1);
            double maxX = trajectory.stream().mapToDouble(p -> p.x).max().orElse(1);
            double minY = trajectory.stream().mapToDouble(p -> p.y).min().orElse(-1);
            double maxY = trajectory.stream().mapToDouble(p -> p.y).max().orElse(1);

            double rangeX = Math.max(Math.abs(minX), Math.abs(maxX)) * 1.2;
            double rangeY = Math.max(Math.abs(minY), Math.abs(maxY)) * 1.2;
            double range = Math.max(rangeX, rangeY);

            // Оси
            g2d.setColor(Color.GRAY);
            int centerX = margin + plotWidth / 2;
            int centerY = margin + plotHeight / 2;
            g2d.drawLine(margin, centerY, width - margin, centerY);
            g2d.drawLine(centerX, margin, centerX, height - margin);

            // Траектория
            g2d.setColor(Color.BLUE);
            g2d.setStroke(new BasicStroke(2));

            for (int i = 0; i < trajectory.size() - 1; i++) {
                Point2D.Double p1 = trajectory.get(i);
                Point2D.Double p2 = trajectory.get(i + 1);

                int x1 = centerX + (int) (p1.x / range * plotWidth / 2);
                int y1 = centerY - (int) (p1.y / range * plotHeight / 2);
                int x2 = centerX + (int) (p2.x / range * plotWidth / 2);
                int y2 = centerY - (int) (p2.y / range * plotHeight / 2);

                g2d.drawLine(x1, y1, x2, y2);
            }

            // Начальная точка
            Point2D.Double start = trajectory.get(0);
            int startX = centerX + (int) (start.x / range * plotWidth / 2);
            int startY = centerY - (int) (start.y / range * plotHeight / 2);
            g2d.setColor(Color.GREEN);
            g2d.fillOval(startX - 5, startY - 5, 10, 10);

            // Конечная точка
            Point2D.Double end = trajectory.get(trajectory.size() - 1);
            int endX = centerX + (int) (end.x / range * plotWidth / 2);
            int endY = centerY - (int) (end.y / range * plotHeight / 2);
            g2d.setColor(Color.RED);
            g2d.fillOval(endX - 5, endY - 5, 10, 10);

            // Метки
            g2d.setColor(Color.BLACK);
            g2d.drawString("x", width - margin + 10, centerY);
            g2d.drawString("y", centerX, margin - 10);
            g2d.drawString(String.format("%.2f", -range), margin - 30, centerY + 5);
            g2d.drawString(String.format("%.2f", range), width - margin - 20, centerY + 15);
        }
    }

    // Панель для графика q^(k)
    class QPlotPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int margin = 50;

            // Заголовок
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString("График q(k) = -ln(δ(k))/ln(10)", width / 2 - 100, 20);

            if (qValues.isEmpty()) return;

            int plotWidth = width - 2 * margin;
            int plotHeight = height - 2 * margin;

            // Находим диапазон значений
            double maxQ = qValues.stream().filter(q -> !Double.isInfinite(q) && !Double.isNaN(q))
                    .mapToDouble(Double::doubleValue).max().orElse(10);
            double minQ = qValues.stream().filter(q -> !Double.isInfinite(q) && !Double.isNaN(q))
                    .mapToDouble(Double::doubleValue).min().orElse(0);

            maxQ = Math.min(maxQ, 20); // Ограничиваем для читаемости

            // Оси
            g2d.setColor(Color.BLACK);
            g2d.drawLine(margin, height - margin, width - margin, height - margin);
            g2d.drawLine(margin, margin, margin, height - margin);

            // График
            g2d.setColor(Color.BLUE);
            g2d.setStroke(new BasicStroke(2));

            for (int i = 0; i < qValues.size() - 1; i++) {
                double q1 = qValues.get(i);
                double q2 = qValues.get(i + 1);

                if (Double.isInfinite(q1) || Double.isNaN(q1)) continue;
                if (Double.isInfinite(q2) || Double.isNaN(q2)) continue;

                int x1 = margin + (int) ((double) i / qValues.size() * plotWidth);
                int y1 = height - margin - (int) ((q1 - minQ) / (maxQ - minQ) * plotHeight);
                int x2 = margin + (int) ((double) (i + 1) / qValues.size() * plotWidth);
                int y2 = height - margin - (int) ((q2 - minQ) / (maxQ - minQ) * plotHeight);

                g2d.drawLine(x1, y1, x2, y2);
            }

            // Точки
            g2d.setColor(Color.RED);
            for (int i = 0; i < qValues.size(); i++) {
                double q = qValues.get(i);
                if (Double.isInfinite(q) || Double.isNaN(q)) continue;

                int x = margin + (int) ((double) i / qValues.size() * plotWidth);
                int y = height - margin - (int) ((q - minQ) / (maxQ - minQ) * plotHeight);
                g2d.fillOval(x - 3, y - 3, 6, 6);
            }

            // Метки
            g2d.setColor(Color.BLACK);
            g2d.drawString("k", width - margin + 10, height - margin);
            g2d.drawString("q(k)", margin - 40, margin - 10);
            g2d.drawString("0", margin - 10, height - margin + 15);
            g2d.drawString(String.format("%d", qValues.size()), width - margin - 10, height - margin + 15);
            g2d.drawString(String.format("%.1f", minQ), margin - 30, height - margin + 5);
            g2d.drawString(String.format("%.1f", maxQ), margin - 30, margin + 5);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Lab11 frame = new Lab11();
            frame.setVisible(true);
        });
    }
}
