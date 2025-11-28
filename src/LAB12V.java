import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;

public class LAB12V extends JFrame {

    private JTextField nField, mField, epsilonField, lxField, lyField;
    private JComboBox<String> methodComboBox, testComboBox;
    private JButton solveButton, clearButton;
    private JTextArea resultArea;
    private JTable solutionTable;
    private DefaultTableModel tableModel;
    private JLabel omegaLabel;
    private JTextField omegaField;
    private DecimalFormat df = new DecimalFormat("#0.0000");
    private double[][] currentSolution;

    public LAB12V() {
        setTitle("LAB12V - Уравнение Пуассона (Лабораторная №12)");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(createParameterPanel(), BorderLayout.NORTH);
        leftPanel.add(createInfoPanel(), BorderLayout.CENTER);

        JPanel rightPanel = createResultPanel();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                leftPanel, rightPanel);
        splitPane.setDividerLocation(450);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
        setVisible(true);
    }

    private JPanel createParameterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                "Параметры решения уравнения Пуассона"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Тестовая задача
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Тестовая задача:"), gbc);
        gbc.gridx = 1;
        String[] tests = {
                "Тест 1: u=sin(πx)sin(πy), f=-2π²sin(πx)sin(πy)",
                "Тест 2: u=x(1-x)y(1-y), f=2[x(1-x)+y(1-y)]",
                "Тест 3: u=0, f=1 (единичный источник)",
                "Тест 4: u=x² на границе, f=-4"
        };
        testComboBox = new JComboBox<>(tests);
        panel.add(testComboBox, gbc);
        row++;

        // Область
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Длина по X (lx):"), gbc);
        gbc.gridx = 1;
        lxField = new JTextField("1.0", 10);
        panel.add(lxField, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Длина по Y (ly):"), gbc);
        gbc.gridx = 1;
        lyField = new JTextField("1.0", 10);
        panel.add(lyField, gbc);
        row++;

        // Сетка
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Число шагов N:"), gbc);
        gbc.gridx = 1;
        nField = new JTextField("10", 10);
        panel.add(nField, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Число шагов M:"), gbc);
        gbc.gridx = 1;
        mField = new JTextField("10", 10);
        panel.add(mField, gbc);
        row++;

        // Точность
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Точность ε:"), gbc);
        gbc.gridx = 1;
        epsilonField = new JTextField("0.0001", 10);
        panel.add(epsilonField, gbc);
        row++;

        // Метод
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Метод:"), gbc);
        gbc.gridx = 1;
        String[] methods = {"Зейделя (Либмана)", "SOR"};
        methodComboBox = new JComboBox<>(methods);
        methodComboBox.addActionListener(e -> updateOmegaField());
        panel.add(methodComboBox, gbc);
        row++;

        // Omega
        omegaLabel = new JLabel("Параметр ω:");
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(omegaLabel, gbc);
        gbc.gridx = 1;
        omegaField = new JTextField("авто", 10);
        omegaField.setEnabled(false);
        panel.add(omegaField, gbc);

        return panel;
    }

    private void updateOmegaField() {
        boolean isSOR = methodComboBox.getSelectedIndex() == 1;
        omegaField.setEnabled(isSOR);
        omegaLabel.setEnabled(isSOR);
    }

    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Ход решения"));

        resultArea = new JTextArea(15, 35);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JScrollPane scrollPane = new JScrollPane(resultArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createResultPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Численное решение u(x,y)"));

        tableModel = new DefaultTableModel();
        solutionTable = new JTable(tableModel);
        solutionTable.setEnabled(false);
        solutionTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        solutionTable.setFont(new Font("Monospaced", Font.PLAIN, 10));
        JScrollPane scrollPane = new JScrollPane(solutionTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));

        solveButton = new JButton("▶ РЕШИТЬ УРАВНЕНИЕ");
        solveButton.setFont(new Font("Arial", Font.BOLD, 14));
        solveButton.setBackground(new Color(34, 139, 34));
        /*solveButton.setForeground(Color.WHITE);*/
        solveButton.setFocusPainted(false);
        solveButton.setPreferredSize(new Dimension(220, 40));
        solveButton.addActionListener(e -> solvePoissonEquation());

        clearButton = new JButton("Очистить");
        clearButton.setPreferredSize(new Dimension(100, 40));
        clearButton.addActionListener(e -> clearResults());

        panel.add(solveButton);
        panel.add(clearButton);

        return panel;
    }

    private void solvePoissonEquation() {
        try {
            double lx = Double.parseDouble(lxField.getText());
            double ly = Double.parseDouble(lyField.getText());
            int N = Integer.parseInt(nField.getText());
            int M = Integer.parseInt(mField.getText());
            double epsilon = Double.parseDouble(epsilonField.getText());
            int methodIndex = methodComboBox.getSelectedIndex();
            int testIndex = testComboBox.getSelectedIndex();

            if (N < 2 || M < 2) {
                JOptionPane.showMessageDialog(this,
                        "N и M должны быть >= 2", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            resultArea.setText("╔════════════════════════════════════╗\n");
            resultArea.append("║  РЕШЕНИЕ УРАВНЕНИЯ ПУАССОНА       ║\n");
            resultArea.append("╚════════════════════════════════════╝\n\n");

            resultArea.append(String.format("Тест: %s\n\n",
                    testComboBox.getSelectedItem().toString().split(":")[0]));
            resultArea.append(String.format("Область: [0,%.2f]×[0,%.2f]\n", lx, ly));
            resultArea.append(String.format("Сетка: %d×%d узлов\n", N+1, M+1));

            double hx = lx / N;
            double hy = ly / M;
            resultArea.append(String.format("Шаги: hx=%.4f, hy=%.4f\n", hx, hy));
            resultArea.append(String.format("Точность: ε=%.6f\n\n", epsilon));

            long startTime = System.currentTimeMillis();

            Object[] result;
            if (methodIndex == 0) {
                resultArea.append("→ Метод Зейделя (схема Либмана)\n");
                result = solveGaussSeidel(N, M, hx, hy, epsilon, lx, ly, testIndex);
            } else {
                double omega;
                String omegaText = omegaField.getText();
                if (omegaText.equals("авто") || omegaText.isEmpty()) {
                    // Оптимальный параметр для квадратной сетки
                    if (N == M) {
                        double lambdaMax = Math.cos(Math.PI / N);
                        omega = 2.0 / (1.0 + Math.sqrt(1.0 - lambdaMax * lambdaMax));
                    } else {
                        omega = 1.8; // Эмпирическое значение
                    }
                    omegaField.setText(String.format("%.6f", omega));
                } else {
                    omega = Double.parseDouble(omegaText);
                }

                resultArea.append(String.format("→ Метод SOR с ω=%.6f\n", omega));
                result = solveSOR(N, M, hx, hy, epsilon, omega, lx, ly, testIndex);
            }

            long endTime = System.currentTimeMillis();
            double[][] solution = (double[][]) result[0];
            int iterations = (int) result[1];
            double finalError = (double) result[2];

            currentSolution = solution;

            resultArea.append(String.format("\n✓ Решение получено!\n"));
            resultArea.append(String.format("  Итераций: %d\n", iterations));
            resultArea.append(String.format("  Погрешность: %.8f\n", finalError));
            resultArea.append(String.format("  Время: %d мс\n\n", endTime - startTime));

            // Проверка с точным решением (для тестов с известным решением)
            if (testIndex == 0 || testIndex == 1) {
                double maxExactError = computeExactError(solution, N, M, hx, hy, testIndex);
                resultArea.append(String.format("  Макс. ошибка от точного: %.6f\n", maxExactError));
            }

            displaySolution(solution, N, M, hx, hy);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка ввода: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private double getBoundaryValue(int i, int j, int N, int M, double x, double y, int testIndex) {
        switch (testIndex) {
            case 0: // sin(πx)sin(πy)
                if (i == 0 || i == M || j == 0 || j == N) return 0;
                break;
            case 1: // x(1-x)y(1-y)
                if (i == 0 || i == M || j == 0 || j == N) return 0;
                break;
            case 2: // Единичный источник, нулевая граница
                return 0;
            case 3: // u = x² на границе
                return x * x;
        }
        return 0;
    }

    private double getRightHandSide(double x, double y, int testIndex) {
        switch (testIndex) {
            case 0: // f = -2π²sin(πx)sin(πy)
                return -2.0 * Math.PI * Math.PI * Math.sin(Math.PI * x) * Math.sin(Math.PI * y);
            case 1: // f = 2[x(1-x) + y(1-y)]
                return 2.0 * (x * (1 - x) + y * (1 - y));
            case 2: // f = 1
                return 1.0;
            case 3: // f = -4
                return -4.0;
            default:
                return 0;
        }
    }

    private double getExactSolution(double x, double y, int testIndex) {
        switch (testIndex) {
            case 0:
                return Math.sin(Math.PI * x) * Math.sin(Math.PI * y);
            case 1:
                return x * (1 - x) * y * (1 - y);
            default:
                return 0;
        }
    }

    private Object[] solveGaussSeidel(int N, int M, double hx, double hy,
                                      double epsilon, double lx, double ly, int testIndex) {
        double[][] u = new double[M+1][N+1];

        // Установка граничных условий
        for (int i = 0; i <= M; i++) {
            for (int j = 0; j <= N; j++) {
                double x = j * hx;
                double y = i * hy;
                if (i == 0 || i == M || j == 0 || j == N) {
                    u[i][j] = getBoundaryValue(i, j, N, M, x, y, testIndex);
                } else {
                    u[i][j] = 0; // Начальное приближение
                }
            }
        }

        int iterations = 0;
        double maxDiff;
        double hx2 = hx * hx;
        double hy2 = hy * hy;
        double denom = 2.0 * (1.0/hx2 + 1.0/hy2);

        do {
            maxDiff = 0;

            // Метод Зейделя - обновляем узлы, используя уже обновленные значения
            for (int i = 1; i < M; i++) {
                for (int j = 1; j < N; j++) {
                    double x = j * hx;
                    double y = i * hy;
                    double f = getRightHandSide(x, y, testIndex);

                    double uOld = u[i][j];

                    // Формула (А1) из лабораторной работы
                    u[i][j] = ((u[i][j+1] + u[i][j-1]) / hx2 +
                            (u[i+1][j] + u[i-1][j]) / hy2 - f) / denom;

                    double diff = Math.abs(u[i][j] - uOld);
                    if (diff > maxDiff) maxDiff = diff;
                }
            }

            iterations++;

            if (iterations % 100 == 0) {
                resultArea.append(String.format("  Итерация %d: погр. = %.8f\n", iterations, maxDiff));
            }

        } while (maxDiff > epsilon && iterations < 100000);

        return new Object[]{u, iterations, maxDiff};
    }

    private Object[] solveSOR(int N, int M, double hx, double hy,
                              double epsilon, double omega, double lx, double ly, int testIndex) {
        double[][] u = new double[M+1][N+1];

        // Граничные условия
        for (int i = 0; i <= M; i++) {
            for (int j = 0; j <= N; j++) {
                double x = j * hx;
                double y = i * hy;
                if (i == 0 || i == M || j == 0 || j == N) {
                    u[i][j] = getBoundaryValue(i, j, N, M, x, y, testIndex);
                } else {
                    u[i][j] = 0;
                }
            }
        }

        int iterations = 0;
        double maxDiff;
        double hx2 = hx * hx;
        double hy2 = hy * hy;
        double denom = 2.0 * (1.0/hx2 + 1.0/hy2);

        do {
            maxDiff = 0;

            for (int i = 1; i < M; i++) {
                for (int j = 1; j < N; j++) {
                    double x = j * hx;
                    double y = i * hy;
                    double f = getRightHandSide(x, y, testIndex);

                    double uOld = u[i][j];

                    // Сначала Зейдель
                    double uGS = ((u[i][j+1] + u[i][j-1]) / hx2 +
                            (u[i+1][j] + u[i-1][j]) / hy2 - f) / denom;

                    // Затем релаксация (формула из лабораторной)
                    u[i][j] = (1.0 - omega) * uOld + omega * uGS;

                    double diff = Math.abs(u[i][j] - uOld);
                    if (diff > maxDiff) maxDiff = diff;
                }
            }

            iterations++;

            if (iterations % 100 == 0) {
                resultArea.append(String.format("  Итерация %d: погр. = %.8f\n", iterations, maxDiff));
            }

        } while (maxDiff > epsilon && iterations < 100000);

        return new Object[]{u, iterations, maxDiff};
    }

    private double computeExactError(double[][] solution, int N, int M,
                                     double hx, double hy, int testIndex) {
        double maxError = 0;
        for (int i = 0; i <= M; i++) {
            for (int j = 0; j <= N; j++) {
                double x = j * hx;
                double y = i * hy;
                double exact = getExactSolution(x, y, testIndex);
                double error = Math.abs(solution[i][j] - exact);
                if (error > maxError) maxError = error;
            }
        }
        return maxError;
    }

    private void displaySolution(double[][] solution, int N, int M, double hx, double hy) {
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);

        // Заголовки столбцов
        tableModel.addColumn("y\\x");
        for (int j = 0; j <= N; j++) {
            tableModel.addColumn(String.format("%.2f", j * hx));
        }

        // Строки (от верха к низу, т.е. от M к 0)
        for (int i = M; i >= 0; i--) {
            Object[] row = new Object[N + 2];
            row[0] = String.format("%.2f", i * hy);
            for (int j = 0; j <= N; j++) {
                row[j + 1] = df.format(solution[i][j]);
            }
            tableModel.addRow(row);
        }

        // Устанавливаем ширину столбцов
        solutionTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        for (int j = 1; j <= N + 1; j++) {
            solutionTable.getColumnModel().getColumn(j).setPreferredWidth(70);
        }
    }

    private void clearResults() {
        resultArea.setText("");
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);
        currentSolution = null;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> new LAB12V());
    }
}
