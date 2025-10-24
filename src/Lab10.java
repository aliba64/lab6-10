import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;

public class Lab10 extends JFrame {
    private static final double GOLDEN_RATIO = (3 - Math.sqrt(5)) / 2; // ≈ 0.382
    private static final int MAX_ITERATIONS = 50;

    private JTextField tfA, tfB, tfEpsilon;
    private JTextField[] tfCoefficients;
    private JTextArea taResults;
    private JTable tableIterations;
    private DefaultTableModel tableModel;
    private double[] coefficients;

    public Lab10() {
        setTitle("Лабораторная работа №10: Метод золотого сечения");
        setSize(1000, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel panelInput = createInputPanel();
        add(panelInput, BorderLayout.NORTH);

        JPanel panelResults = createResultsPanel();
        add(panelResults, BorderLayout.CENTER);

        JButton btnCalculate = new JButton("Вычислить минимум");
        btnCalculate.setFont(new Font("Arial", Font.BOLD, 14));
        btnCalculate.addActionListener(e -> calculateMinimum());

        JPanel panelButton = new JPanel();
        panelButton.add(btnCalculate);
        add(panelButton, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Параметры задачи"));
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
        tfEpsilon = new JTextField("0.001", 10);
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

        taResults = new JTextArea(6, 50);
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
        splitPane.setDividerLocation(150);
        panel.add(splitPane, BorderLayout.CENTER);

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

            GoldenSectionResult result = goldenSectionMethod(a, b, epsilon);

            displayResults(result);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка в формате чисел! Проверьте введенные данные.",
                    "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
        }
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
        taResults.append("=== РЕЗУЛЬТАТЫ ВЫЧИСЛЕНИЙ ===\n\n");
        taResults.append(String.format("Функция: Ф(x) = Σ(c_i * |x|^i), i = 0..5\n"));
        taResults.append(String.format("Коэффициенты: ["));
        for (int i = 0; i < coefficients.length; i++) {
            taResults.append(String.format("%.3f", coefficients[i]));
            if (i < coefficients.length - 1) taResults.append(", ");
        }
        taResults.append("]\n\n");
        taResults.append(String.format("Найденный минимум:\n"));
        taResults.append(String.format("  x* = %.8f\n", result.xMin));
        taResults.append(String.format("  Ф(x*) = %.8f\n", result.fMin));
        taResults.append(String.format("\nКоличество итераций: %d\n", result.iterations));

        tableModel.setRowCount(0);
        for (IterationData data : result.iterationsList) {
            tableModel.addRow(new Object[]{
                    data.k,
                    String.format("%.8f", data.a),
                    String.format("%.8f", data.b),
                    String.format("%.8f", data.y),
                    String.format("%.8f", data.z),
                    String.format("%.8f", data.fy),
                    String.format("%.8f", data.fz),
                    String.format("%.8f", data.delta)
            });
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
