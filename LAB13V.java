import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;

public class LAB13V extends JFrame {
    private JTextField txtA, txtB, txtN, txtAlpha, txtBeta;
    private JTextArea txtResults;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private JButton btnCalculate, btnClear;

    public LAB13V() {
        setTitle("Решение краевой задачи методом конечных разностей");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Панель ввода параметров
        JPanel inputPanel = createInputPanel();
        mainPanel.add(inputPanel, BorderLayout.NORTH);

        // Панель результатов
        JPanel resultsPanel = createResultsPanel();
        mainPanel.add(resultsPanel, BorderLayout.CENTER);

        // Панель кнопок
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Параметры задачи"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Интервал [a, b]
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Интервал a:"), gbc);

        gbc.gridx = 1;
        txtA = new JTextField("0", 10);
        panel.add(txtA, gbc);

        gbc.gridx = 2;
        panel.add(new JLabel("b:"), gbc);

        gbc.gridx = 3;
        txtB = new JTextField("1", 10);
        panel.add(txtB, gbc);

        // Количество узлов сетки
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Число узлов N:"), gbc);

        gbc.gridx = 1;
        txtN = new JTextField("10", 10);
        panel.add(txtN, gbc);

        // Граничные условия
        gbc.gridx = 2;
        panel.add(new JLabel("u(a) = α:"), gbc);

        gbc.gridx = 3;
        txtAlpha = new JTextField("0", 10);
        panel.add(txtAlpha, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("u(b) = β:"), gbc);

        gbc.gridx = 1;
        txtBeta = new JTextField("0", 10);
        panel.add(txtBeta, gbc);

        return panel;
    }

    private JPanel createResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Результаты"));

        // Таблица с результатами
        String[] columnNames = {"i", "x_i", "u_i"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resultTable = new JTable(tableModel);
        JScrollPane tableScrollPane = new JScrollPane(resultTable);
        tableScrollPane.setPreferredSize(new Dimension(400, 300));

        // Текстовая область для дополнительной информации
        txtResults = new JTextArea(10, 40);
        txtResults.setEditable(false);
        txtResults.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane textScrollPane = new JScrollPane(txtResults);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                tableScrollPane, textScrollPane);
        splitPane.setResizeWeight(0.5);

        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        btnCalculate = new JButton("Вычислить");
        btnCalculate.addActionListener(e -> calculate());

        btnClear = new JButton("Очистить");
        btnClear.addActionListener(e -> clearResults());

        panel.add(btnCalculate);
        panel.add(btnClear);

        return panel;
    }

    private void calculate() {
        try {
            // Чтение параметров
            double a = Double.parseDouble(txtA.getText());
            double b = Double.parseDouble(txtB.getText());
            int N = Integer.parseInt(txtN.getText());
            double alpha = Double.parseDouble(txtAlpha.getText());
            double beta = Double.parseDouble(txtBeta.getText());

            if (N < 2) {
                JOptionPane.showMessageDialog(this,
                        "Число узлов должно быть не менее 2",
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Вычисление шага сетки
            double h = (b - a) / N;

            // Решение трехдиагональной системы методом прогонки
            double[] x = new double[N + 1];
            double[] u = new double[N + 1];

            // Формирование сетки
            for (int i = 0; i <= N; i++) {
                x[i] = a + i * h;
            }

            // Решение краевой задачи -u'' = f(x)
            // с граничными условиями u(a) = alpha, u(b) = beta

            // Коэффициенты трехдиагональной матрицы
            double[] A = new double[N + 1];
            double[] B = new double[N + 1];
            double[] C = new double[N + 1];
            double[] F = new double[N + 1];

            // Граничные условия
            u[0] = alpha;
            u[N] = beta;

            // Заполнение системы уравнений
            // -u_{i-1} + 2u_i - u_{i+1} = h^2 * f_i
            for (int i = 1; i < N; i++) {
                A[i] = -1.0;           // Коэффициент перед u_{i-1}
                B[i] = 2.0;            // Коэффициент перед u_i
                C[i] = -1.0;           // Коэффициент перед u_{i+1}
                F[i] = h * h * f(x[i]); // Правая часть
            }

            // Метод прогонки
            double[] alpha_prog = new double[N + 1];
            double[] beta_prog = new double[N + 1];

            alpha_prog[1] = 0;
            beta_prog[1] = alpha;

            // Прямой ход
            for (int i = 1; i < N; i++) {
                double denom = B[i] + A[i] * alpha_prog[i];
                alpha_prog[i + 1] = -C[i] / denom;
                beta_prog[i + 1] = (F[i] - A[i] * beta_prog[i]) / denom;
            }

            // Обратный ход
            u[N] = beta;
            for (int i = N - 1; i >= 1; i--) {
                u[i] = alpha_prog[i + 1] * u[i + 1] + beta_prog[i + 1];
            }

            // Отображение результатов
            displayResults(x, u, h, N);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Некорректный ввод данных",
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Функция f(x) из уравнения -u'' = f(x)
    // Пример: f(x) = 2 (решение u(x) = x(1-x) при α=β=0)
    private double f(double x) {
        return 2.0; // Можно изменить на другую функцию
    }

    private void displayResults(double[] x, double[] u, double h, int N) {
        // Очистка предыдущих результатов
        tableModel.setRowCount(0);
        txtResults.setText("");

        // Заполнение таблицы
        for (int i = 0; i <= N; i++) {
            tableModel.addRow(new Object[]{
                    i,
                    String.format("%.6f", x[i]),
                    String.format("%.8f", u[i])
            });
        }

        // Дополнительная информация
        StringBuilder info = new StringBuilder();
        info.append("Параметры решения:\n");
        info.append("==================\n");
        info.append(String.format("Шаг сетки h = %.6f\n", h));
        info.append(String.format("Число узлов N = %d\n", N));
        info.append("\nУравнение: -u'' = f(x)\n");
        info.append("Метод: конечные разности\n");
        info.append("Схема решения: метод прогонки\n\n");

        // Проверка на монотонность
        boolean increasing = true, decreasing = true;
        for (int i = 1; i <= N; i++) {
            if (u[i] < u[i-1]) increasing = false;
            if (u[i] > u[i-1]) decreasing = false;
        }

        if (increasing) info.append("Решение монотонно возрастает\n");
        else if (decreasing) info.append("Решение монотонно убывает\n");
        else info.append("Решение немонотонно\n");

        txtResults.setText(info.toString());
    }

    private void clearResults() {
        tableModel.setRowCount(0);
        txtResults.setText("");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new LAB13V().setVisible(true);
        });
    }
}
