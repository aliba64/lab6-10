import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Random;

public class Lab9 extends JFrame {
    private JTextField nField, pField, qField, rField, tField, bField, epsilonField, maxIterField;
    private JTable matrixTable;
    private JTextArea outputArea;
    private JButton calculateButton, generateButton;
    private DefaultTableModel matrixModel;

    // Используем локаль US для точки как разделителя
    private DecimalFormat df = new DecimalFormat("#.######", new DecimalFormatSymbols(Locale.US));
    private NumberFormat nf = NumberFormat.getInstance(Locale.US);

    public Lab9() {
        setTitle("Лабораторная работа №9 - Степенной метод");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Панель ввода параметров
        JPanel inputPanel = createInputPanel();
        add(inputPanel, BorderLayout.NORTH);

        // Центральная панель с таблицами
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 10, 10));

        // Панель матрицы
        JPanel matrixPanel = new JPanel(new BorderLayout());
        matrixPanel.setBorder(BorderFactory.createTitledBorder("Исходная матрица A"));
        matrixModel = new DefaultTableModel();
        matrixTable = new JTable(matrixModel);
        JScrollPane matrixScroll = new JScrollPane(matrixTable);
        matrixPanel.add(matrixScroll, BorderLayout.CENTER);

        // Панель результатов
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBorder(BorderFactory.createTitledBorder("Результаты вычислений"));
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane outputScroll = new JScrollPane(outputArea);
        resultPanel.add(outputScroll, BorderLayout.CENTER);

        centerPanel.add(matrixPanel);
        centerPanel.add(resultPanel);
        add(centerPanel, BorderLayout.CENTER);

        // Панель кнопок
        JPanel buttonPanel = new JPanel();
        generateButton = new JButton("Сгенерировать матрицу");
        calculateButton = new JButton("Вычислить λ₁(A)");
        calculateButton.setEnabled(false);

        generateButton.addActionListener(e -> generateMatrix());
        calculateButton.addActionListener(e -> calculateEigenvalue());

        buttonPanel.add(generateButton);
        buttonPanel.add(calculateButton);
        add(buttonPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 8, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Параметры задачи "));

        // Первая строка параметров
        panel.add(new JLabel("n (5-10):"));
        nField = new JTextField("5");
        panel.add(nField);

        panel.add(new JLabel("p (1-4):"));
        pField = new JTextField("2");
        panel.add(pField);

        panel.add(new JLabel("q (1-4):"));
        qField = new JTextField("2");
        panel.add(qField);

        panel.add(new JLabel("r (0.1-1.2):"));
        rField = new JTextField("0.5");
        panel.add(rField);

        // Вторая строка параметров
        panel.add(new JLabel("t (0.1-1.2):"));
        tField = new JTextField("0.5");
        panel.add(tField);

        panel.add(new JLabel("b (0.01-0.1):"));
        bField = new JTextField("0.05");
        panel.add(bField);

        panel.add(new JLabel("ε (точность):"));
        epsilonField = new JTextField("0.001");
        panel.add(epsilonField);

        panel.add(new JLabel("k_max:"));
        maxIterField = new JTextField("1000");
        panel.add(maxIterField);

        return panel;
    }

    // Метод для безопасного парсинга чисел
    private double parseDouble(String str) throws ParseException {
        // Заменяем запятую на точку
        str = str.replace(',', '.');
        return Double.parseDouble(str);
    }

    private void generateMatrix() {
        try {
            int n = Integer.parseInt(nField.getText());
            double p = parseDouble(pField.getText());
            double q = parseDouble(qField.getText());
            double r = parseDouble(rField.getText());
            double t = parseDouble(tField.getText());
            double b = parseDouble(bField.getText());

            if (n < 5 || n > 10) {
                JOptionPane.showMessageDialog(this, "n должно быть от 5 до 10");
                return;
            }

            // Создание модели таблицы
            matrixModel.setRowCount(0);
            matrixModel.setColumnCount(0);

            for (int j = 0; j < n; j++) {
                matrixModel.addColumn("a" + (j + 1));
            }

            // Генерация матрицы по формуле варианта 1:
            // a_ii = 5*i^(p/2)
            // a_ij = b * i^(p/2) + j^(p/2)^(1/q) для i ≠ j
            double[][] matrix = new double[n][n];

            for (int i = 0; i < n; i++) {
                Object[] row = new Object[n];
                for (int j = 0; j < n; j++) {
                    if (i == j) {
                        matrix[i][j] = 5 * Math.pow(i + 1, p / 2);
                    } else {
                        matrix[i][j] = b * Math.pow(i + 1, p / 2) +
                                Math.pow(Math.pow(j + 1, p / 2), 1.0 / q);
                    }
                    row[j] = df.format(matrix[i][j]);
                }
                matrixModel.addRow(row);
            }

            outputArea.setText("Матрица успешно сгенерирована!\n\n");
            outputArea.append("Формулы для варианта 1:\n");
            outputArea.append("a_ii = 5*i^(p/2)\n");
            outputArea.append("a_ij = b*i^(p/2) + j^(p/2)^(1/q), i≠j\n\n");
            outputArea.append("Параметры:\n");
            outputArea.append("n = " + n + ", p = " + p + ", q = " + q + "\n");
            outputArea.append("r = " + r + ", t = " + t + ", b = " + b + "\n");

            calculateButton.setEnabled(true);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ошибка ввода параметров: " + ex.getMessage());
        }
    }

    private void calculateEigenvalue() {
        try {
            int n = Integer.parseInt(nField.getText());
            double epsilon = parseDouble(epsilonField.getText());
            int maxIter = Integer.parseInt(maxIterField.getText());

            // Получение матрицы из таблицы с безопасным парсингом
            double[][] A = new double[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    String cellValue = matrixModel.getValueAt(i, j).toString();
                    A[i][j] = parseDouble(cellValue);
                }
            }

            // Степенной метод
            PowerMethodResult result = powerMethod(A, epsilon, maxIter);

            // Вывод результатов
            outputArea.append("\n" + "-".repeat(50) + "\n");
            outputArea.append("РЕЗУЛЬТАТЫ СТЕПЕННОГО МЕТОДА\n");
            outputArea.append("-".repeat(50) + "\n\n");
            outputArea.append("Максимальное собственное значение λ₁(A) = " +
                    df.format(result.eigenvalue) + "\n\n");
            outputArea.append("Количество итераций: " + result.iterations + "\n\n");
            outputArea.append("Собственный вектор:\n");
            for (int i = 0; i < result.eigenvector.length; i++) {
                outputArea.append("  x[" + (i + 1) + "] = " +
                        df.format(result.eigenvector[i]) + "\n");
            }

            if (result.converged) {
                outputArea.append("\nМетод сошелся с точностью ε = " + epsilon + "\n");
            } else {
                outputArea.append("\nДостигнуто максимальное число итераций!\n");
            }

            // Проверка: A*x = λ*x
            double[] Ax = matrixVectorMultiply(A, result.eigenvector);
            outputArea.append("\nПроверка (A*x):\n");
            for (int i = 0; i < Math.min(3, n); i++) {
                outputArea.append("  (A*x)[" + (i + 1) + "] = " + df.format(Ax[i]) +
                        ", λ*x[" + (i + 1) + "] = " +
                        df.format(result.eigenvalue * result.eigenvector[i]) + "\n");
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ошибка вычисления: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private PowerMethodResult powerMethod(double[][] A, double epsilon, int maxIter) {
        int n = A.length;
        Random rand = new Random();

        // Начальный случайный вектор x^(0)
        double[] x = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = rand.nextDouble();
        }

        // Нормировка начального вектора
        x = normalize(x);

        double lambda = 0;
        double lambdaOld = 0;
        boolean converged = false;
        int k = 0;

        for (k = 1; k <= maxIter; k++) {
            // y = A * x
            double[] y = matrixVectorMultiply(A, x);

            // Находим максимальный элемент по модулю
            double maxVal = 0;
            int maxIdx = 0;
            for (int i = 0; i < n; i++) {
                if (Math.abs(y[i]) > Math.abs(maxVal)) {
                    maxVal = y[i];
                    maxIdx = i;
                }
            }

            lambda = maxVal;

            // Нормировка: x^(k) = y / maxVal
            for (int i = 0; i < n; i++) {
                x[i] = y[i] / maxVal;
            }

            // Проверка сходимости
            if (k > 1 && Math.abs(lambda - lambdaOld) < epsilon) {
                converged = true;
                break;
            }

            lambdaOld = lambda;
        }

        return new PowerMethodResult(lambda, x, k, converged);
    }

    private double[] matrixVectorMultiply(double[][] A, double[] x) {
        int n = A.length;
        double[] result = new double[n];
        for (int i = 0; i < n; i++) {
            result[i] = 0;
            for (int j = 0; j < n; j++) {
                result[i] += A[i][j] * x[j];
            }
        }
        return result;
    }

    private double[] normalize(double[] x) {
        double max = 0;
        for (double val : x) {
            if (Math.abs(val) > Math.abs(max)) {
                max = val;
            }
        }
        double[] normalized = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            normalized[i] = x[i] / max;
        }
        return normalized;
    }

    private static class PowerMethodResult {
        double eigenvalue;
        double[] eigenvector;
        int iterations;
        boolean converged;

        PowerMethodResult(double eigenvalue, double[] eigenvector, int iterations, boolean converged) {
            this.eigenvalue = eigenvalue;
            this.eigenvector = eigenvector;
            this.iterations = iterations;
            this.converged = converged;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Lab9());
    }
}
