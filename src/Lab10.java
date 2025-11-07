import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;

public class Lab10 extends JFrame {
    private static final double GOLDEN_RATIO = (3 - Math.sqrt(5)) / 2;
    private static final int MAX_ITERATIONS = 50;

    private JTextField tfA, tfB, tfEpsilon;
    private JTextField[] tfCoefficients;
    private JTextArea taResults;
    private JTable tableIterations;
    private DefaultTableModel tableModel;
    private double[] coefficients;
    private GoldenSectionResult lastResult;

    public Lab10() {
        setTitle("Лабораторная работа №10: Метод золотого сечения - Вариант 1");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel panelInput = createInputPanel();
        add(panelInput, BorderLayout.NORTH);

        JPanel panelResults = createResultsPanel();
        add(panelResults, BorderLayout.CENTER);

        JPanel panelButtons = createButtonPanel();
        add(panelButtons, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Параметры задачи - Вариант 1"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Начало интервала a:"), gbc);
        gbc.gridx = 1;
        tfA = new JTextField("-1", 10);
        panel.add(tfA, gbc);

        gbc.gridx = 2; gbc.gridy = 0;
        panel.add(new JLabel("Конец интервала b:"), gbc);
        gbc.gridx = 3;
        tfB = new JTextField("1", 10);
        panel.add(tfB, gbc);

        gbc.gridx = 4; gbc.gridy = 0;
        panel.add(new JLabel("Точность ε:"), gbc);
        gbc.gridx = 5;
        tfEpsilon = new JTextField("0.0001", 10);
        panel.add(tfEpsilon, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 6;
        panel.add(new JLabel("Коэффициенты c_i (i = 0..5), 0 ≤ c_i ≤ 1:"), gbc);

        tfCoefficients = new JTextField[6];
        JPanel panelCoeff = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (int i = 0; i <= 5; i++) {
            panelCoeff.add(new JLabel("c" + i + ":"));
            tfCoefficients[i] = new JTextField("0.5", 6);
            panelCoeff.add(tfCoefficients[i]);
        }

        gbc.gridy = 2;
        panel.add(panelCoeff, gbc);

        return panel;
    }

    private JPanel createResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        taResults = new JTextArea(8, 50);
        taResults.setEditable(false);
        taResults.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollResults = new JScrollPane(taResults);
        scrollResults.setBorder(BorderFactory.createTitledBorder("Итоговые результаты"));

        String[] columns = {"k", "a_k", "b_k", "y_k", "z_k", "Ф(y_k)", "Ф(z_k)", "δ_k"};
        tableModel = new DefaultTableModel(columns, 0);
        tableIterations = new JTable(tableModel);
        tableIterations.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JScrollPane scrollTable = new JScrollPane(tableIterations);
        scrollTable.setBorder(BorderFactory.createTitledBorder("Итерации метода"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollResults, scrollTable);
        splitPane.setDividerLocation(200);

        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));

        JButton btnCalculate = new JButton("Вычислить минимум");
        btnCalculate.setFont(new Font("Arial", Font.BOLD, 14));
        btnCalculate.addActionListener(e -> calculateMinimum());

        JButton btnShowCharts = new JButton("Показать графики");
        btnShowCharts.setFont(new Font("Arial", Font.BOLD, 14));
        btnShowCharts.addActionListener(e -> showCharts());

        panel.add(btnCalculate);
        panel.add(btnShowCharts);

        return panel;
    }

    private void calculateMinimum() {
        try {
            double a = Double.parseDouble(tfA.getText().trim());
            double b = Double.parseDouble(tfB.getText().trim());
            double epsilon = Double.parseDouble(tfEpsilon.getText().trim());

            coefficients = new double[6];
            for (int i = 0; i <= 5; i++) {
                coefficients[i] = Double.parseDouble(tfCoefficients[i].getText().trim());
                if (coefficients[i] < 0 || coefficients[i] > 1) {
                    JOptionPane.showMessageDialog(this,
                            "Коэффициент c" + i + " должен быть в диапазоне [0, 1]",
                            "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            if (a >= b) {
                JOptionPane.showMessageDialog(this,
                        "Начало интервала должно быть меньше конца!",
                        "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (epsilon <= 0) {
                JOptionPane.showMessageDialog(this,
                        "Точность должна быть положительным числом!",
                        "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
                return;
            }

            lastResult = goldenSectionMethod(a, b, epsilon);
            displayResults(lastResult);

            JOptionPane.showMessageDialog(this,
                    "Вычисления завершены! Нажмите 'Показать графики' для просмотра графиков.",
                    "Готово", JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка в формате чисел! Проверьте введенные данные.",
                    "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showCharts() {
        if (lastResult == null || coefficients == null) {
            JOptionPane.showMessageDialog(this,
                    "Сначала выполните вычисления!",
                    "Предупреждение", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ChartWindow chartWindow = new ChartWindow(lastResult, coefficients);
        chartWindow.setVisible(true);
    }

    private double phi(double x) {
        double sum = 0;
        double absX = Math.abs(x);
        double power = 1.0;

        for (int i = 0; i <= 5; i++) {
            sum += coefficients[i] * power;
            power *= absX;
        }

        return sum;
    }

    private GoldenSectionResult goldenSectionMethod(double a, double b, double epsilon) {
        ArrayList<IterationData> iterations = new ArrayList<>();

        double y = a + GOLDEN_RATIO * (b - a);
        double z = b - GOLDEN_RATIO * (b - a);
        double fy = phi(y);
        double fz = phi(z);

        int k = 0;
        double delta = b - a;

        iterations.add(new IterationData(k, a, b, y, z, fy, fz, delta));

        while (delta > epsilon && k < MAX_ITERATIONS) {
            k++;

            if (fy < fz) {
                b = z;
                z = y;
                fz = fy;
                y = a + GOLDEN_RATIO * (b - a);
                fy = phi(y);
            } else {
                a = y;
                y = z;
                fy = fz;
                z = b - GOLDEN_RATIO * (b - a);
                fz = phi(z);
            }

            delta = b - a;
            iterations.add(new IterationData(k, a, b, y, z, fy, fz, delta));
        }

        double xMin = (a + b) / 2;
        double fMin = phi(xMin);

        return new GoldenSectionResult(xMin, fMin, k, iterations);
    }

    private void displayResults(GoldenSectionResult result) {
        taResults.setText("");
        taResults.append("=== РЕЗУЛЬТАТЫ ВЫЧИСЛЕНИЙ (ВАРИАНТ 1) ===\n\n");
        taResults.append("Функция: Ф(x) = Σ(c_i * |x|^i), i = 0..5\n");
        taResults.append("Параметры: a = -1, b = 1, ε = 10^-3 ÷ 10^-4\n");
        taResults.append(String.format("Коэффициенты: ["));
        for (int i = 0; i < coefficients.length; i++) {
            taResults.append(String.format("%.3f", coefficients[i]));
            if (i < coefficients.length - 1) taResults.append(", ");
        }
        taResults.append("]\n\n");
        taResults.append(String.format("Найденный минимум:\n"));
        taResults.append(String.format("  x̄ = %.10f\n", result.xMin));
        taResults.append(String.format("  Ф(x̄) = %.10f\n", result.fMin));
        taResults.append(String.format("\nКоличество итераций k = %d\n", result.iterations));
        taResults.append(String.format("Финальная точность δ_k = %.10f\n",
                result.iterationsList.get(result.iterationsList.size() - 1).delta));

        tableModel.setRowCount(0);
        for (IterationData data : result.iterationsList) {
            tableModel.addRow(new Object[]{
                    data.k,
                    String.format("%.10f", data.a),
                    String.format("%.10f", data.b),
                    String.format("%.10f", data.y),
                    String.format("%.10f", data.z),
                    String.format("%.10f", data.fy),
                    String.format("%.10f", data.fz),
                    String.format("%.10e", data.delta)
            });
        }
    }

    // Класс для окна с графиками
    class ChartWindow extends JFrame {
        private ChartPanel functionChartPanel;
        private ChartPanel convergenceChartPanel;

        public ChartWindow(GoldenSectionResult result, double[] coeff) {
            setTitle("Графики - Метод золотого сечения");
            setSize(1200, 600);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setLayout(new GridLayout(1, 2, 15, 15));

            functionChartPanel = new ChartPanel("График функции Ф(x) = Σ(c_i·|x|^i)");
            convergenceChartPanel = new ChartPanel("Зависимость δ_k от k (сходимость)");

            add(functionChartPanel);
            add(convergenceChartPanel);

            displayCharts(result, coeff);

            setLocationRelativeTo(Lab10.this);
        }

        private void displayCharts(GoldenSectionResult result, double[] coeff) {
            // График функции
            ArrayList<Point2D> functionPoints = new ArrayList<>();
            double rangeStart = -1.5;
            double rangeEnd = 1.5;
            int numPoints = 500;
            double step = (rangeEnd - rangeStart) / numPoints;

            double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
            for (int i = 0; i <= numPoints; i++) {
                double x = rangeStart + i * step;
                double y = phi(x, coeff);
                functionPoints.add(new Point2D(x, y));
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }
            functionChartPanel.setData(functionPoints, rangeStart, rangeEnd, minY, maxY, "x", "Ф(x)");

            // График сходимости
            ArrayList<Point2D> convergencePoints = new ArrayList<>();
            double maxDelta = 0;
            for (IterationData data : result.iterationsList) {
                convergencePoints.add(new Point2D(data.k, data.delta));
                maxDelta = Math.max(maxDelta, data.delta);
            }
            convergenceChartPanel.setData(convergencePoints, 0, result.iterations, 0, maxDelta * 1.1, "k (итерация)", "δ_k = b_k - a_k");
        }

        private double phi(double x, double[] coeff) {
            double sum = 0;
            double absX = Math.abs(x);
            double power = 1.0;

            for (int i = 0; i <= 5; i++) {
                sum += coeff[i] * power;
                power *= absX;
            }

            return sum;
        }
    }

    // Вложенный класс для отрисовки графиков
    class ChartPanel extends JPanel {
        private String title;
        private ArrayList<Point2D> points;
        private double minX, maxX, minY, maxY;
        private String xLabel, yLabel;

        public ChartPanel(String title) {
            this.title = title;
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        }

        public void setData(ArrayList<Point2D> points, double minX, double maxX, double minY, double maxY, String xLabel, String yLabel) {
            this.points = points;
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.xLabel = xLabel;
            this.yLabel = yLabel;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (points == null || points.isEmpty()) {
                g2.setFont(new Font("Arial", Font.BOLD, 14));
                g2.setColor(Color.GRAY);
                String msg = "Нет данных для отображения";
                g2.drawString(msg, getWidth() / 2 - g2.getFontMetrics().stringWidth(msg) / 2, getHeight() / 2);
                return;
            }

            int padding = 60;
            int width = getWidth();
            int height = getHeight();

            // Заголовок
            g2.setFont(new Font("Arial", Font.BOLD, 16));
            g2.setColor(Color.BLACK);
            g2.drawString(title, width / 2 - g2.getFontMetrics().stringWidth(title) / 2, 25);

            // Оси
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(padding, height - padding, width - padding, height - padding); // X axis
            g2.drawLine(padding, padding, padding, height - padding); // Y axis

            // Стрелки осей
            int[] xArrow = {width - padding, width - padding - 10, width - padding - 10};
            int[] yArrowX = {padding - 7, padding, padding + 7};
            int[] yArrowY = {padding + 10, padding, padding + 10};
            g2.fillPolygon(xArrow, new int[]{height - padding, height - padding - 7, height - padding + 7}, 3);
            g2.fillPolygon(yArrowX, yArrowY, 3);

            // Сетка
            g2.setColor(new Color(220, 220, 220));
            g2.setStroke(new BasicStroke(1f));
            for (int i = 0; i <= 10; i++) {
                int x = padding + (i * (width - 2 * padding) / 10);
                int y = height - padding - (i * (height - 2 * padding) / 10);
                g2.drawLine(x, height - padding, x, padding);
                g2.drawLine(padding, y, width - padding, y);
            }

            // Метки на осях
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.PLAIN, 11));
            for (int i = 0; i <= 5; i++) {
                // Ось X
                double xValue = minX + i * (maxX - minX) / 5;
                int xPos = padding + (i * (width - 2 * padding) / 5);
                String xStr = String.format("%.2f", xValue);
                g2.drawString(xStr, xPos - 15, height - padding + 20);

                // Ось Y
                double yValue = minY + i * (maxY - minY) / 5;
                int yPos = height - padding - (i * (height - 2 * padding) / 5);
                String yStr = String.format("%.2f", yValue);
                g2.drawString(yStr, 5, yPos + 5);
            }

            // Подписи осей
            g2.setFont(new Font("Arial", Font.BOLD, 13));
            g2.drawString(xLabel, width / 2 - g2.getFontMetrics().stringWidth(xLabel) / 2, height - 10);
            g2.drawString(yLabel, 15, 20);

            // График - линия
            g2.setColor(new Color(30, 144, 255)); // DodgerBlue
            g2.setStroke(new BasicStroke(2.5f));

            for (int i = 0; i < points.size() - 1; i++) {
                Point2D p1 = points.get(i);
                Point2D p2 = points.get(i + 1);

                int x1 = padding + (int) ((p1.x - minX) / (maxX - minX) * (width - 2 * padding));
                int y1 = height - padding - (int) ((p1.y - minY) / (maxY - minY) * (height - 2 * padding));
                int x2 = padding + (int) ((p2.x - minX) / (maxX - minX) * (width - 2 * padding));
                int y2 = height - padding - (int) ((p2.y - minY) / (maxY - minY) * (height - 2 * padding));

                g2.draw(new Line2D.Double(x1, y1, x2, y2));
            }

            // График - точки
            g2.setColor(new Color(220, 20, 60)); // Crimson
            for (Point2D p : points) {
                int x = padding + (int) ((p.x - minX) / (maxX - minX) * (width - 2 * padding));
                int y = height - padding - (int) ((p.y - minY) / (maxY - minY) * (height - 2 * padding));
                g2.fill(new Ellipse2D.Double(x - 3, y - 3, 6, 6));
            }
        }
    }

    static class Point2D {
        double x, y;

        Point2D(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private static class IterationData {
        int k;
        double a, b, y, z, fy, fz, delta;

        IterationData(int k, double a, double b, double y, double z,
                      double fy, double fz, double delta) {
            this.k = k;
            this.a = a;
            this.b = b;
            this.y = y;
            this.z = z;
            this.fy = fy;
            this.fz = fz;
            this.delta = delta;
        }
    }

    private static class GoldenSectionResult {
        double xMin, fMin;
        int iterations;
        ArrayList<IterationData> iterationsList;

        GoldenSectionResult(double xMin, double fMin, int iterations,
                            ArrayList<IterationData> iterationsList) {
            this.xMin = xMin;
            this.fMin = fMin;
            this.iterations = iterations;
            this.iterationsList = iterationsList;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Lab10 frame = new Lab10();
            frame.setVisible(true);
        });
    }
}
