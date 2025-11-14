import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.Random;

public class Lab81 extends JFrame {
    private JTextField tfSize;
    private JButton btnCreate, btnGen, btnSolve, btnClear;
    private JTable tableMatrix, tableResult;
    private DefaultTableModel matrixModel, resultModel;
    private JScrollPane scrollMatrix, scrollResult;
    private JTextArea taSteps;
    private JScrollPane scrollSteps;
    private DecimalFormat df = new DecimalFormat("#.####");

    public Lab81() {
        setTitle("СЛАУ методом прогонки - Калькулятор");
        setBounds(50, 50, 1400, 750);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Верхняя панель с кнопками
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Размер n:"));
        tfSize = new JTextField("5", 5);
        topPanel.add(tfSize);

        btnCreate = new JButton("Создать матрицу");
        btnCreate.addActionListener(e -> createMatrix());
        topPanel.add(btnCreate);

        btnGen = new JButton("Генерировать трёхдиагональную");
        btnGen.addActionListener(e -> genTridiagonal());
        topPanel.add(btnGen);

        btnSolve = new JButton("Решить");
        btnSolve.addActionListener(e -> solve());
        topPanel.add(btnSolve);

        btnClear = new JButton("Очистить");
        btnClear.addActionListener(e -> clearAll());
        topPanel.add(btnClear);

        add(topPanel, BorderLayout.NORTH);

        // Центральная панель с таблицами и выводом
        JPanel centerPanel = new JPanel(new GridLayout(1, 3, 10, 10));

        // Таблица ввода ПОЛНОЙ матрицы
        matrixModel = new DefaultTableModel();
        tableMatrix = new JTable(matrixModel);
        scrollMatrix = new JScrollPane(tableMatrix);
        scrollMatrix.setBorder(BorderFactory.createTitledBorder("Матрица системы [A|d]"));
        centerPanel.add(scrollMatrix);

        // Область для вывода хода решения
        taSteps = new JTextArea();
        taSteps.setEditable(false);
        taSteps.setFont(new Font("Monospaced", Font.PLAIN, 11));
        scrollSteps = new JScrollPane(taSteps);
        scrollSteps.setBorder(BorderFactory.createTitledBorder("Ход решения"));
        centerPanel.add(scrollSteps);

        // Таблица результатов
        resultModel = new DefaultTableModel();
        tableResult = new JTable(resultModel);
        tableResult.setEnabled(false);
        scrollResult = new JScrollPane(tableResult);
        scrollResult.setBorder(BorderFactory.createTitledBorder("Результаты"));
        centerPanel.add(scrollResult);

        add(centerPanel, BorderLayout.CENTER);

        setVisible(true);
    }

    // Создание пустой матрицы для ручного ввода
    private void createMatrix() {
        try {
            int n = Integer.parseInt(tfSize.getText());
            if (n < 2 || n > 20) {
                JOptionPane.showMessageDialog(this, "n должно быть от 2 до 20");
                return;
            }
            matrixModel.setColumnCount(0);
            matrixModel.setRowCount(0);

            // Создаём столбцы: n столбцов для матрицы A + 1 столбец для вектора d
            for (int j = 0; j < n; j++) {
                matrixModel.addColumn("a" + (j + 1));
            }
            matrixModel.addColumn("d");

            // Создаём строки
            for (int i = 0; i < n; i++) {
                Object[] row = new Object[n + 1];
                for (int j = 0; j < n + 1; j++) {
                    row[j] = "0";
                }
                matrixModel.addRow(row);
            }

            taSteps.setText("Матрица создана. Введите коэффициенты или нажмите 'Генерировать трёхдиагональную'.\n\n" +
                    "Для метода прогонки матрица должна быть трёхдиагональной:\n" +
                    "- Ненулевые элементы только на главной диагонали и соседних диагоналях\n");
            resultModel.setColumnCount(0);
            resultModel.setRowCount(0);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Введите корректное число");
        }
    }

    // Генерация трёхдиагональной матрицы
    private void genTridiagonal() {
        try {
            int n = Integer.parseInt(tfSize.getText());
            if (n < 2 || n > 20) {
                JOptionPane.showMessageDialog(this, "n должно быть от 2 до 20");
                return;
            }
            matrixModel.setColumnCount(0);
            matrixModel.setRowCount(0);

            for (int j = 0; j < n; j++) {
                matrixModel.addColumn("a" + (j + 1));
            }
            matrixModel.addColumn("d");

            Random rand = new Random();
            for (int i = 0; i < n; i++) {
                Object[] row = new Object[n + 1];
                for (int j = 0; j < n; j++) {
                    // Только главная, верхняя и нижняя диагонали
                    if (j == i) { // главная диагональ
                        row[j] = rand.nextInt(21) - 10;
                    } else if (j == i - 1) { // нижняя диагональ
                        row[j] = rand.nextInt(21) - 10;
                    } else if (j == i + 1) { // верхняя диагональ
                        row[j] = rand.nextInt(21) - 10;
                    } else {
                        row[j] = 0;
                    }
                }
                row[n] = rand.nextInt(21) - 10; // правая часть
                matrixModel.addRow(row);
            }

            taSteps.setText("Трёхдиагональная матрица сгенерирована.\n");
            resultModel.setColumnCount(0);
            resultModel.setRowCount(0);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Введите корректное число");
        }
    }

    // Решение СЛАУ
    private void solve() {
        try {
            int n = matrixModel.getRowCount();
            if (n == 0) {
                JOptionPane.showMessageDialog(this, "Сначала создайте матрицу!");
                return;
            }

            // Считывание полной матрицы
            double[][] A = new double[n][n];
            double[] d = new double[n];

            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    String val = matrixModel.getValueAt(i, j).toString();
                    A[i][j] = Double.parseDouble(val);
                }
                d[i] = Double.parseDouble(matrixModel.getValueAt(i, n).toString());
            }

            // Проверка трёхдиагональности
            StringBuilder output = new StringBuilder();
            output.append("═══════════════════════════════════════════════\n");
            output.append("       ПРОВЕРКА ТРЁХДИАГОНАЛЬНОСТИ\n");
            output.append("═══════════════════════════════════════════════\n\n");

            boolean isTridiagonal = true;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (Math.abs(i - j) > 1 && Math.abs(A[i][j]) > 1e-10) {
                        isTridiagonal = false;
                        output.append(String.format("Элемент A[%d][%d] = %.4f (должен быть 0)\n", i, j, A[i][j]));
                    }
                }
            }

            if (!isTridiagonal) {
                output.append("\n⚠️ ВНИМАНИЕ: Матрица НЕ трёхдиагональная!\n");
                output.append("Метод прогонки работает только для трёхдиагональных матриц.\n");
                taSteps.setText(output.toString());
                JOptionPane.showMessageDialog(this, "Матрица должна быть трёхдиагональной!");
                return;
            }

            output.append("✓ Матрица является трёхдиагональной\n\n");

            // Извлечение диагоналей из матрицы
            double[] c = new double[n]; // нижняя диагональ
            double[] a = new double[n]; // главная диагональ
            double[] b = new double[n]; // верхняя диагональ

            output.append("═══════════════════════════════════════════════\n");
            output.append("         ИЗВЛЕЧЕНИЕ ДИАГОНАЛЕЙ\n");
            output.append("═══════════════════════════════════════════════\n\n");

            for (int i = 0; i < n; i++) {
                a[i] = A[i][i]; // главная диагональ
                if (i > 0) c[i] = A[i][i - 1]; // нижняя диагональ
                if (i < n - 1) b[i] = A[i][i + 1]; // верхняя диагональ

                output.append(String.format("i=%d: c[%d]=%.2f, a[%d]=%.2f, b[%d]=%.2f, d[%d]=%.2f\n",
                        i, i, c[i], i, a[i], i, b[i], i, d[i]));
            }
            output.append("\n");

            // Вывод исходной матрицы системы
            output.append("═══════════════════════════════════════════════\n");
            output.append("            МАТРИЦА СИСТЕМЫ Ax = d\n");
            output.append("═══════════════════════════════════════════════\n\n");

            for (int i = 0; i < n; i++) {
                output.append("│ ");
                for (int j = 0; j < n; j++) {
                    output.append(String.format("%7.2f ", A[i][j]));
                }
                output.append("│ │ x").append(i + 1).append(" │ = │ ")
                        .append(String.format("%7.2f", d[i])).append(" │\n");
            }
            output.append("\n");

            // Решение методом прогонки с выводом хода
            double[] x = thomasWithSteps(c, a, b, d, n, output);

            // Вывод результатов в таблицу
            resultModel.setColumnCount(0);
            resultModel.setRowCount(0);
            resultModel.addColumn("i");
            resultModel.addColumn("x[i]");
            resultModel.addColumn("Проверка: (Ax)[i] ≈ d[i]");

            for (int i = 0; i < n; i++) {
                double check = 0;
                for (int j = 0; j < n; j++) {
                    check += A[i][j] * x[j];
                }

                Object[] row = {
                        i + 1,
                        df.format(x[i]),
                        df.format(check) + " ≈ " + df.format(d[i])
                };
                resultModel.addRow(row);
            }

            taSteps.setText(output.toString());

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Ошибка: заполните все ячейки числами!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Ошибка при решении: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Метод прогонки с подробным выводом хода решения
    private double[] thomasWithSteps(double[] c, double[] a, double[] b, double[] d, int n, StringBuilder output) {
        double[] alpha = new double[n];
        double[] beta = new double[n];
        double[] x = new double[n];

        output.append("═══════════════════════════════════════════════\n");
        output.append("        ПРЯМОЙ ХОД (Forward Sweep)\n");
        output.append("═══════════════════════════════════════════════\n\n");
        output.append("Формулы:\n");
        output.append("α[0] = -b[0] / a[0]\n");
        output.append("β[0] = d[0] / a[0]\n\n");
        output.append("α[i] = -b[i] / (a[i] + c[i]*α[i-1])\n");
        output.append("β[i] = (d[i] - c[i]*β[i-1]) / (a[i] + c[i]*α[i-1])\n\n");
        output.append("───────────────────────────────────────────────\n\n");

        // Начальные значения
        if (Math.abs(a[0]) < 1e-10) {
            throw new ArithmeticException("a[0] = 0, деление на ноль!");
        }
        alpha[0] = -b[0] / a[0];
        beta[0] = d[0] / a[0];
        output.append(String.format("i=0: α[0] = -%.4f / %.4f = %.4f\n", b[0], a[0], alpha[0]));
        output.append(String.format("     β[0] = %.4f / %.4f = %.4f\n\n", d[0], a[0], beta[0]));

        // Прогонка вперед
        for (int i = 1; i < n - 1; i++) {
            double denom = a[i] + c[i] * alpha[i - 1];
            if (Math.abs(denom) < 1e-10) {
                throw new ArithmeticException("Деление на ноль в знаменателе на шаге i=" + i);
            }
            alpha[i] = -b[i] / denom;
            beta[i] = (d[i] - c[i] * beta[i - 1]) / denom;

            output.append(String.format("i=%d: denom = %.4f + %.4f * %.4f = %.4f\n",
                    i, a[i], c[i], alpha[i - 1], denom));
            output.append(String.format("     α[%d] = -%.4f / %.4f = %.4f\n",
                    i, b[i], denom, alpha[i]));
            output.append(String.format("     β[%d] = (%.4f - %.4f * %.4f) / %.4f = %.4f\n\n",
                    i, d[i], c[i], beta[i - 1], denom, beta[i]));
        }

        // Последний элемент
        output.append("\n═══════════════════════════════════════════════\n");
        output.append("       ОБРАТНЫЙ ХОД (Backward Sweep)\n");
        output.append("═══════════════════════════════════════════════\n\n");
        output.append("Формулы:\n");
        output.append("x[n-1] = (d[n-1] - c[n-1]*β[n-2]) / (a[n-1] + c[n-1]*α[n-2])\n");
        output.append("x[i] = α[i]*x[i+1] + β[i]\n\n");
        output.append("───────────────────────────────────────────────\n\n");

        double denomLast = a[n - 1] + c[n - 1] * alpha[n - 2];
        if (Math.abs(denomLast) < 1e-10) {
            throw new ArithmeticException("Деление на ноль в последнем элементе");
        }
        x[n - 1] = (d[n - 1] - c[n - 1] * beta[n - 2]) / denomLast;

        output.append(String.format("x[%d] = (%.4f - %.4f * %.4f) / (%.4f + %.4f * %.4f)\n",
                n - 1, d[n - 1], c[n - 1], beta[n - 2], a[n - 1], c[n - 1], alpha[n - 2]));
        output.append(String.format("     = %.4f\n\n", x[n - 1]));

        // Обратная подстановка
        for (int i = n - 2; i >= 0; i--) {
            x[i] = alpha[i] * x[i + 1] + beta[i];
            output.append(String.format("x[%d] = %.4f * %.4f + %.4f = %.4f\n",
                    i, alpha[i], x[i + 1], beta[i], x[i]));
        }

        output.append("\n═══════════════════════════════════════════════\n");
        output.append("              РЕШЕНИЕ СИСТЕМЫ\n");
        output.append("═══════════════════════════════════════════════\n\n");
        for (int i = 0; i < n; i++) {
            output.append(String.format("x[%d] = %.4f\n", i, x[i]));
        }
        output.append("\n");

        return x;
    }

    // Очистка всех данных
    private void clearAll() {
        matrixModel.setColumnCount(0);
        matrixModel.setRowCount(0);
        resultModel.setColumnCount(0);
        resultModel.setRowCount(0);
        taSteps.setText("");
        tfSize.setText("5");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Lab81());
    }
}
