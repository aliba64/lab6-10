import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;

public class Lab8 extends JFrame {
    private JTextField nField, pField, qField, rField, tField;
    private JTextArea resultArea;
    private JTable matrixTable;
    private JButton solveButton, clearButton;
    private DecimalFormat df = new DecimalFormat("#.##########");
    private DecimalFormat dfShort = new DecimalFormat("#.####");

    public Lab8() {
        setTitle("Лабораторная работа №8 - Решение СЛАУ методами факторизации и Зейделя");
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Создание главной панели с отступами
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Панель ввода параметров варианта
        JPanel inputPanel = createInputPanel();
        mainPanel.add(inputPanel, BorderLayout.NORTH);

        // Центральная панель с разделителем
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(550);

        // Левая панель - вывод результатов
        JPanel leftPanel = createResultPanel();
        splitPane.setLeftComponent(leftPanel);

        // Правая панель - матрица
        JPanel rightPanel = createMatrixPanel();
        splitPane.setRightComponent(rightPanel);

        mainPanel.add(splitPane, BorderLayout.CENTER);

        add(mainPanel);
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.BLUE, 2),
                "Параметры варианта задания",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14)
        ));

        JPanel fieldsPanel = new JPanel(new GridLayout(2, 5, 10, 5));

        // Первая строка параметров
        fieldsPanel.add(new JLabel("n = 5 +:", SwingConstants.RIGHT));
        nField = new JTextField("20");
        fieldsPanel.add(nField);

        fieldsPanel.add(new JLabel("p (1÷4):", SwingConstants.RIGHT));
        pField = new JTextField("2");
        fieldsPanel.add(pField);

        fieldsPanel.add(new JLabel("q (1÷4):", SwingConstants.RIGHT));
        qField = new JTextField("2");
        fieldsPanel.add(qField);

        // Вторая строка параметров
        fieldsPanel.add(new JLabel("r (0.1÷1.2):", SwingConstants.RIGHT));
        rField = new JTextField("0.5");
        fieldsPanel.add(rField);

        fieldsPanel.add(new JLabel("t (0.1÷1.2):", SwingConstants.RIGHT));
        tField = new JTextField("0.5");
        fieldsPanel.add(tField);

        fieldsPanel.add(new JLabel("")); // Пустое место
        fieldsPanel.add(new JLabel(""));

        panel.add(fieldsPanel, BorderLayout.CENTER);

        // Панель кнопок
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        solveButton = new JButton("Решить систему");
        solveButton.setFont(new Font("Arial", Font.BOLD, 13));
        solveButton.setBackground(new Color(50, 150, 50));
        solveButton.setForeground(Color.green);
        solveButton.setFocusPainted(false);
        solveButton.addActionListener(e -> solveSystems());

        clearButton = new JButton("Очистить");
        clearButton.setFont(new Font("Arial", Font.BOLD, 13));
        clearButton.setBackground(new Color(200, 50, 50));
        clearButton.setForeground(Color.RED);
        clearButton.setFocusPainted(false);
        clearButton.addActionListener(e -> clearResults());

        buttonPanel.add(solveButton);
        buttonPanel.add(clearButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createResultPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Результаты вычислений"));

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        resultArea.setBackground(new Color(245, 245, 245));

        JScrollPane scrollPane = new JScrollPane(resultArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createMatrixPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Матрица системы (фрагмент)"));

        matrixTable = new JTable();
        matrixTable.setFont(new Font("Monospaced", Font.PLAIN, 10));
        matrixTable.setEnabled(false);

        JScrollPane scrollPane = new JScrollPane(matrixTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void clearResults() {
        resultArea.setText("");
        matrixTable.setModel(new javax.swing.table.DefaultTableModel());
    }

    private void solveSystems() {
        try {
            // Считывание параметров
            int nAdd = Integer.parseInt(nField.getText());
            int n = 5 + nAdd;
            double p = Double.parseDouble(pField.getText());
            double q = Double.parseDouble(qField.getText());
            double r = Double.parseDouble(rField.getText());
            double t = Double.parseDouble(tField.getText());

            // Проверка диапазонов
            if (p < 1 || p > 4 || q < 1 || q > 4) {
                JOptionPane.showMessageDialog(this, "Параметры p и q должны быть в диапазоне [1, 4]");
                return;
            }
            if (r < 0.1 || r > 1.2 || t < 0.1 || t > 1.2) {
                JOptionPane.showMessageDialog(this, "Параметры r и t должны быть в диапазоне [0.1, 1.2]");
                return;
            }

            // Генерация матрицы A и вектора b по формулам варианта 1
            double[][] A = new double[n][n];
            double[] b = new double[n];

            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (i == j) {
                        // Диагональные элементы: a_ii = 5*i^(p/2)
                        A[i][j] = 5.0 * Math.pow(i + 1, p / 2.0);
                    } else {
                        // Внедиагональные элементы: a_ij = ±0.01(i^(p/2) ± j^(q/2))
                        double sign1 = ((i + j) % 2 == 0) ? 1.0 : -1.0;
                        double sign2 = (j % 2 == 0) ? 1.0 : -1.0;
                        A[i][j] = sign1 * 0.01 * (Math.pow(i + 1, p / 2.0) + sign2 * Math.pow(j + 1, q / 2.0));
                    }
                }
                // Вектор правой части: b_i = 4.5*i^(p/2)
                b[i] = 4.5 * Math.pow(i + 1, p / 2.0);
            }

            // Вывод матрицы в таблицу (первые 8x8 элементов)
            displayMatrix(A, b, Math.min(8, n));

            // Формирование вывода результатов
            StringBuilder output = new StringBuilder();
            output.append("------------------------------------------------------\n");
            output.append("          РЕШЕНИЕ СИСТЕМЫ ЛИНЕЙНЫХ УРАВНЕНИЙ           \n");
            output.append("-------------------------------------------------------\n\n");

            output.append("ПАРАМЕТРЫ ВАРИАНТА:\n");
            output.append("--------------------------------------------------------\n");
            output.append(String.format("Размер системы: n = 5 + %d = %d\n", nAdd, n));
            output.append(String.format("Параметры: p = %.2f, q = %.2f, r = %.2f, t = %.2f\n\n", p, q, r, t));

            output.append("ФОРМУЛЫ ВАРИАНТА 1:\n");
            output.append("----------------------------------------------------------\n");
            output.append(String.format("• Диагональные элементы: a_ii = 5·i^(p/2) = 5·i^%.2f\n", p/2));
            output.append(String.format("• Внедиагональные: a_ij = ±0.01(i^(p/2) ± j^(q/2))\n"));
            output.append(String.format("• Правая часть: b_i = 4.5·i^(p/2) = 4.5·i^%.2f\n\n", p/2));

            // Проверка диагонального преобладания
            boolean isDiagonallyDominant = checkDiagonalDominance(A, n);
            output.append("АНАЛИЗ МАТРИЦЫ:\n");
            output.append("-----------------------------------------------------------\n");
            output.append("Диагональное преобладание: ");
            output.append(isDiagonallyDominant ? "ДА ✓\n" : "НЕТ ✗\n");
            output.append("(Условие для сходимости метода Зейделя)\n\n");

            // МЕТОД 1: LU-разложение (метод факторизации)
            output.append("-------------------------------------------------------------\n");
            output.append("    МЕТОД 1: ФАКТОРИЗАЦИЯ (LU-РАЗЛОЖЕНИЕ, метод Дулитла)     \n");
            output.append("-------------------------------------------------------------\n\n");

            long startTime = System.nanoTime();
            double[] xLU = solveLUDoolittle(A, b, n);
            long luTime = System.nanoTime() - startTime;

            output.append("Решение (первые 12 компонент вектора x):\n");
            for (int i = 0; i < Math.min(12, n); i++) {
                output.append(String.format("  x[%2d] = %s\n", i + 1, df.format(xLU[i])));
            }
            if (n > 12) {
                output.append(String.format("  ... (всего %d элементов)\n", n));
            }

            double luResidual = calculateResidual(A, xLU, b, n);
            output.append(String.format("\n✓ Норма невязки ||Ax - b||: %s\n", df.format(luResidual)));
            output.append(String.format("✓ Время выполнения: %.4f мс\n", luTime / 1e6));
            output.append(String.format("✓ Точность: %s\n\n",
                    luResidual < 1e-6 ? "ОТЛИЧНАЯ" : (luResidual < 1e-4 ? "ХОРОШАЯ" : "УДОВЛЕТВОРИТЕЛЬНАЯ")));

            // МЕТОД 2: Метод Зейделя
            output.append("----------------------------------------------------------------\n");
            output.append("        МЕТОД 2: МЕТОД ЗЕЙДЕЛЯ (итерационный)                   \n");
            output.append("----------------------------------------------------------------\n\n");

            startTime = System.nanoTime();
            GaussSeidelResult gsResult = solveGaussSeidelDetailed(A, b, n, 1e-8, 10000);
            long gsTime = System.nanoTime() - startTime;

            output.append(String.format("Параметры метода:\n"));
            output.append(String.format("  • Точность (ε): 1e-8\n"));
            output.append(String.format("  • Максимум итераций: 10000\n"));
            output.append(String.format("  • Выполнено итераций: %d\n", gsResult.iterations));
            output.append(String.format("  • Сходимость: %s\n\n", gsResult.converged ? "ДА ✓" : "НЕТ ✗"));

            output.append("Решение (первые 12 компонент вектора x):\n");
            for (int i = 0; i < Math.min(12, n); i++) {
                output.append(String.format("  x[%2d] = %s\n", i + 1, df.format(gsResult.solution[i])));
            }
            if (n > 12) {
                output.append(String.format("  ... (всего %d элементов)\n", n));
            }

            double gsResidual = calculateResidual(A, gsResult.solution, b, n);
            output.append(String.format("\n✓ Норма невязки ||Ax - b||: %s\n", df.format(gsResidual)));
            output.append(String.format("✓ Время выполнения: %.4f мс\n", gsTime / 1e6));
            output.append(String.format("✓ Точность: %s\n\n",
                    gsResidual < 1e-6 ? "ОТЛИЧНАЯ" : (gsResidual < 1e-4 ? "ХОРОШАЯ" : "УДОВЛЕТВОРИТЕЛЬНАЯ")));

            // СРАВНЕНИЕ МЕТОДОВ
            output.append("------------------------------------------------------------------\n");
            output.append("                      СРАВНЕНИЕ МЕТОДОВ                           \n");
            output.append("------------------------------------------------------------------\n\n");

            double solutionDiff = 0;
            for (int i = 0; i < n; i++) {
                solutionDiff += Math.pow(xLU[i] - gsResult.solution[i], 2);
            }
            solutionDiff = Math.sqrt(solutionDiff);

            output.append(String.format("┌──────────────────────────────┬─────────────┬─────────────┐\n"));
            output.append(String.format("│ Характеристика               │ LU-метод    │ Метод Зейд. │\n"));
            output.append(String.format("├──────────────────────────────┼─────────────┼─────────────┤\n"));
            output.append(String.format("│ Норма невязки                │ %-11s │ %-11s │\n",
                    dfShort.format(luResidual), dfShort.format(gsResidual)));
            output.append(String.format("│ Время (мс)                   │ %-11.4f │ %-11.4f │\n",
                    luTime / 1e6, gsTime / 1e6));
            output.append(String.format("│ Тип метода                   │ %-11s │ %-11s │\n",
                    "Прямой", "Итерационн."));
            output.append(String.format("└──────────────────────────────┴─────────────┴─────────────┘\n\n"));

            output.append(String.format("Расхождение решений ||x_LU - x_GS||: %s\n\n", df.format(solutionDiff)));

            output.append("ЗАКЛЮЧЕНИЕ:\n");
            output.append("-------------------------------------------------------\n");
            if (solutionDiff < 1e-4) {
                output.append("✓ Оба метода дали практически идентичные решения.\n");
                output.append("✓ Система решена корректно.\n");
            } else {
                output.append("⚠ Обнаружено расхождение между методами.\n");
                output.append("  Рекомендуется проверить условия сходимости.\n");
            }

            if (luTime < gsTime) {
                output.append(String.format("✓ Метод LU-разложения быстрее в %.1f раз.\n", (double)gsTime / luTime));
            } else {
                output.append(String.format("✓ Метод Зейделя быстрее в %.1f раз.\n", (double)luTime / gsTime));
            }

            resultArea.setText(output.toString());
            resultArea.setCaretPosition(0);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка: введите корректные числовые значения",
                    "Ошибка ввода",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка вычислений: " + ex.getMessage(),
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void displayMatrix(double[][] A, double[] b, int size) {
        String[] columnNames = new String[size + 2];
        for (int i = 0; i < size; i++) {
            columnNames[i] = "x" + (i + 1);
        }
        columnNames[size] = "|";
        columnNames[size + 1] = "b";

        Object[][] data = new Object[size][size + 2];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                data[i][j] = dfShort.format(A[i][j]);
            }
            data[i][size] = "|";
            data[i][size + 1] = dfShort.format(b[i]);
        }

        matrixTable.setModel(new javax.swing.table.DefaultTableModel(data, columnNames));
    }

    private boolean checkDiagonalDominance(double[][] A, int n) {
        for (int i = 0; i < n; i++) {
            double diag = Math.abs(A[i][i]);
            double sum = 0;
            for (int j = 0; j < n; j++) {
                if (i != j) {
                    sum += Math.abs(A[i][j]);
                }
            }
            if (diag <= sum) {
                return false;
            }
        }
        return true;
    }

    // LU-разложение методом Дулитла (Doolittle)
    private double[] solveLUDoolittle(double[][] A, double[] b, int n) {
        double[][] L = new double[n][n];
        double[][] U = new double[n][n];

        // Инициализация L (единицы на диагонали)
        for (int i = 0; i < n; i++) {
            L[i][i] = 1.0;
        }

        // LU-разложение по методу Дулитла
        for (int i = 0; i < n; i++) {
            // Вычисление элементов U (верхняя треугольная)
            for (int k = i; k < n; k++) {
                double sum = 0;
                for (int j = 0; j < i; j++) {
                    sum += L[i][j] * U[j][k];
                }
                U[i][k] = A[i][k] - sum;
            }

            // Вычисление элементов L (нижняя треугольная)
            for (int k = i + 1; k < n; k++) {
                double sum = 0;
                for (int j = 0; j < i; j++) {
                    sum += L[k][j] * U[j][i];
                }
                L[k][i] = (A[k][i] - sum) / U[i][i];
            }
        }

        // Прямой ход: Ly = b
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            double sum = 0;
            for (int j = 0; j < i; j++) {
                sum += L[i][j] * y[j];
            }
            y[i] = b[i] - sum;
        }

        // Обратный ход: Ux = y
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            double sum = 0;
            for (int j = i + 1; j < n; j++) {
                sum += U[i][j] * x[j];
            }
            x[i] = (y[i] - sum) / U[i][i];
        }

        return x;
    }

    // Класс для хранения результата метода Зейделя
    class GaussSeidelResult {
        double[] solution;
        int iterations;
        boolean converged;

        GaussSeidelResult(double[] solution, int iterations, boolean converged) {
            this.solution = solution;
            this.iterations = iterations;
            this.converged = converged;
        }
    }

    // Метод Зейделя с подробной информацией
    private GaussSeidelResult solveGaussSeidelDetailed(double[][] A, double[] b, int n,
                                                       double epsilon, int maxIterations) {
        double[] x = new double[n];
        double[] xOld = new double[n];

        // Начальное приближение: x = 0
        for (int i = 0; i < n; i++) {
            x[i] = 0.0;
        }

        int iteration = 0;
        boolean converged = false;

        while (!converged && iteration < maxIterations) {
            System.arraycopy(x, 0, xOld, 0, n);

            // Итерация Зейделя
            for (int i = 0; i < n; i++) {
                double sum = b[i];

                // Используем новые значения (уже вычисленные на текущей итерации)
                for (int j = 0; j < i; j++) {
                    sum -= A[i][j] * x[j];
                }

                // Используем старые значения (с предыдущей итерации)
                for (int j = i + 1; j < n; j++) {
                    sum -= A[i][j] * xOld[j];
                }

                x[i] = sum / A[i][i];
            }

            // Проверка сходимости: ||x^(k) - x^(k-1)|| < epsilon
            double diff = 0;
            for (int i = 0; i < n; i++) {
                diff += Math.pow(x[i] - xOld[i], 2);
            }
            diff = Math.sqrt(diff);

            if (diff < epsilon) {
                converged = true;
            }

            iteration++;
        }

        return new GaussSeidelResult(x, iteration, converged);
    }

    // Вычисление нормы невязки ||Ax - b||
    private double calculateResidual(double[][] A, double[] x, double[] b, int n) {
        double residual = 0;

        for (int i = 0; i < n; i++) {
            double sum = 0;
            for (int j = 0; j < n; j++) {
                sum += A[i][j] * x[j];
            }
            residual += Math.pow(sum - b[i], 2);
        }

        return Math.sqrt(residual);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            Lab8 frame = new Lab8();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
