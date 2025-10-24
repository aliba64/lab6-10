import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

public class Lab7 extends JFrame {

    private JTextField txtA1, txtA2, txtA3;
    private JTextField txtBeta1, txtBeta2, txtBeta3;
    private JTextField txtR1, txtR2, txtR3;
    private JTextField txtMinN, txtMaxN, txtSteps;
    private JTextArea resultArea;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private JButton calculateButton, clearButton;

    public Lab7() {
        setTitle("Вычисление кратных интегралов методом Монте-Карло");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Панель ввода параметров
        JPanel inputPanel = createInputPanel();
        add(inputPanel, BorderLayout.NORTH);

        // Панель результатов
        JPanel resultPanel = createResultPanel();
        add(resultPanel, BorderLayout.CENTER);

        // Панель кнопок
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(6, 4, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Параметры задачи"));

        // Параметры a_i
        panel.add(new JLabel("a₁ (от -5 до 5):"));
        txtA1 = new JTextField("-2.5");
        panel.add(txtA1);

        panel.add(new JLabel("a₂ (от -5 до 5):"));
        txtA2 = new JTextField("1.0");
        panel.add(txtA2);

        panel.add(new JLabel("a₃ (от -5 до 5):"));
        txtA3 = new JTextField("3.0");
        panel.add(txtA3);

        // Параметры β_i
        panel.add(new JLabel("β₁ (от 0.5 до 4):"));
        txtBeta1 = new JTextField("1.5");
        panel.add(txtBeta1);

        panel.add(new JLabel("β₂ (от 0.5 до 4):"));
        txtBeta2 = new JTextField("2.0");
        panel.add(txtBeta2);

        panel.add(new JLabel("β₃ (от 0.5 до 4):"));
        txtBeta3 = new JTextField("2.5");
        panel.add(txtBeta3);

        // Параметры эллипсоида R_i
        panel.add(new JLabel("R₁ (полуось эллипсоида):"));
        txtR1 = new JTextField("5.0");
        panel.add(txtR1);

        panel.add(new JLabel("R₂ (полуось эллипсоида):"));
        txtR2 = new JTextField("5.0");
        panel.add(txtR2);

        panel.add(new JLabel("R₃ (полуось эллипсоида):"));
        txtR3 = new JTextField("5.0");
        panel.add(txtR3);

        // Параметры для N
        panel.add(new JLabel("Минимальное N:"));
        txtMinN = new JTextField("100");
        panel.add(txtMinN);

        panel.add(new JLabel("Максимальное N:"));
        txtMaxN = new JTextField("10000");
        panel.add(txtMaxN);

        panel.add(new JLabel("Количество шагов:"));
        txtSteps = new JTextField("5");
        panel.add(txtSteps);

        return panel;
    }

    private JPanel createResultPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Результаты вычислений"));

        // Таблица результатов
        String[] columnNames = {"N (точек)", "M (в области)", "Объём V", "Интеграл Ĩ"};
        tableModel = new DefaultTableModel(columnNames, 0);
        resultTable = new JTable(tableModel);
        JScrollPane tableScrollPane = new JScrollPane(resultTable);
        tableScrollPane.setPreferredSize(new Dimension(850, 200));

        panel.add(tableScrollPane, BorderLayout.NORTH);

        // Текстовая область для дополнительной информации
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane textScrollPane = new JScrollPane(resultArea);
        textScrollPane.setPreferredSize(new Dimension(850, 200));

        panel.add(textScrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));

        calculateButton = new JButton("Вычислить");
        calculateButton.setFont(new Font("Arial", Font.BOLD, 14));
        calculateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performCalculation();
            }
        });

        clearButton = new JButton("Очистить");
        clearButton.setFont(new Font("Arial", Font.BOLD, 14));
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearResults();
            }
        });

        panel.add(calculateButton);
        panel.add(clearButton);

        return panel;
    }

    private void performCalculation() {
        try {
            // Чтение параметров
            double a1 = Double.parseDouble(txtA1.getText());
            double a2 = Double.parseDouble(txtA2.getText());
            double a3 = Double.parseDouble(txtA3.getText());

            double beta1 = Double.parseDouble(txtBeta1.getText());
            double beta2 = Double.parseDouble(txtBeta2.getText());
            double beta3 = Double.parseDouble(txtBeta3.getText());

            double R1 = Double.parseDouble(txtR1.getText());
            double R2 = Double.parseDouble(txtR2.getText());
            double R3 = Double.parseDouble(txtR3.getText());

            int minN = Integer.parseInt(txtMinN.getText());
            int maxN = Integer.parseInt(txtMaxN.getText());
            int steps = Integer.parseInt(txtSteps.getText());

            // Очистка предыдущих результатов
            tableModel.setRowCount(0);
            resultArea.setText("");

            // Вычисление объёма параллелепипеда W
            double volumeW = (2 * R1) * (2 * R2) * (2 * R3);

            StringBuilder info = new StringBuilder();
            info.append("Параметры функции плотности ρ(x):\n");
            info.append(String.format("a₁ = %.2f, a₂ = %.2f, a₃ = %.2f\n", a1, a2, a3));
            info.append(String.format("β₁ = %.2f, β₂ = %.2f, β₃ = %.2f\n\n", beta1, beta2, beta3));
            info.append("Область интегрирования: эллипсоид\n");
            info.append(String.format("R₁ = %.2f, R₂ = %.2f, R₃ = %.2f\n", R1, R2, R3));
            info.append(String.format("Объём параллелепипеда W = %.4f\n\n", volumeW));

            // Генерация значений N
            int[] nValues = generateNValues(minN, maxN, steps);

            info.append("Начало вычислений методом Монте-Карло...\n\n");

            Random random = new Random();

            for (int N : nValues) {
                int M = 0; // количество точек в области V
                double sumF = 0.0; // сумма значений функции

                for (int i = 0; i < N; i++) {
                    // Генерация случайной точки в параллелепипеде W
                    double x1 = -R1 + 2 * R1 * random.nextDouble();
                    double x2 = -R2 + 2 * R2 * random.nextDouble();
                    double x3 = -R3 + 2 * R3 * random.nextDouble();

                    // Проверка принадлежности точки эллипсоиду
                    double ellipsoid = (x1 * x1) / (R1 * R1) +
                            (x2 * x2) / (R2 * R2) +
                            (x3 * x3) / (R3 * R3);

                    if (ellipsoid <= 1.0) {
                        M++;
                        // Вычисление функции плотности ρ(x)
                        double rho = Math.pow(Math.abs(x1 - a1), beta1) *
                                Math.pow(Math.abs(x2 - a2), beta2) *
                                Math.pow(Math.abs(x3 - a3), beta3);
                        sumF += rho;
                    }
                }

                // Оценка объёма области V
                double volumeV = volumeW * M / N;

                // Оценка интеграла
                double integral = 0.0;
                if (M > 0) {
                    integral = volumeW * sumF / M;
                }

                // Добавление результатов в таблицу
                Object[] row = {N, M, String.format("%.6f", volumeV), String.format("%.6f", integral)};
                tableModel.addRow(row);

                info.append(String.format("N = %d: M = %d, V ≈ %.6f, Ĩ ≈ %.6f\n", N, M, volumeV, integral));
            }

            info.append("\nВычисления завершены.\n");
            resultArea.setText(info.toString());

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка ввода! Проверьте правильность введённых данных.",
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private int[] generateNValues(int min, int max, int steps) {
        int[] values = new int[steps];
        if (steps == 1) {
            values[0] = max;
        } else {
            double ratio = Math.pow((double) max / min, 1.0 / (steps - 1));
            for (int i = 0; i < steps; i++) {
                values[i] = (int) (min * Math.pow(ratio, i));
            }
        }
        return values;
    }

    private void clearResults() {
        tableModel.setRowCount(0);
        resultArea.setText("");
    }

    // Функция плотности ρ(x)
    private double densityFunction(double x1, double x2, double x3,
                                   double a1, double a2, double a3,
                                   double beta1, double beta2, double beta3) {
        return Math.pow(Math.abs(x1 - a1), beta1) *
                Math.pow(Math.abs(x2 - a2), beta2) *
                Math.pow(Math.abs(x3 - a3), beta3);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Lab7();
            }
        });
    }
}
