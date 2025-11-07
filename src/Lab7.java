import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
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
    private EllipsoidPanel3D ellipsoidPanel;

    public Lab7() {
        setTitle("Вычисление кратных интегралов методом Монте-Карло");
        setSize(1200, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));

        JPanel inputPanel = createInputPanel();
        leftPanel.add(inputPanel, BorderLayout.NORTH);

        ellipsoidPanel = new EllipsoidPanel3D();
        ellipsoidPanel.setBorder(BorderFactory.createTitledBorder("Визуализация области интегрирования (3D)"));
        ellipsoidPanel.setPreferredSize(new Dimension(400, 350));
        leftPanel.add(ellipsoidPanel, BorderLayout.CENTER);

        add(leftPanel, BorderLayout.WEST);

        JPanel resultPanel = createResultPanel();
        add(resultPanel, BorderLayout.CENTER);

        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(6, 4, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Параметры задачи"));

        panel.add(new JLabel("a₁ (от -5 до 5):"));
        txtA1 = new JTextField("-2.5");
        panel.add(txtA1);

        panel.add(new JLabel("a₂ (от -5 до 5):"));
        txtA2 = new JTextField("1.0");
        panel.add(txtA2);

        panel.add(new JLabel("a₃ (от -5 до 5):"));
        txtA3 = new JTextField("3.0");
        panel.add(txtA3);

        panel.add(new JLabel("β₁ (от 0.5 до 4):"));
        txtBeta1 = new JTextField("1.5");
        panel.add(txtBeta1);

        panel.add(new JLabel("β₂ (от 0.5 до 4):"));
        txtBeta2 = new JTextField("2.0");
        panel.add(txtBeta2);

        panel.add(new JLabel("β₃ (от 0.5 до 4):"));
        txtBeta3 = new JTextField("2.5");
        panel.add(txtBeta3);

        panel.add(new JLabel("R₁ (полуось):"));
        txtR1 = new JTextField("5.0");
        panel.add(txtR1);

        panel.add(new JLabel("R₂ (полуось):"));
        txtR2 = new JTextField("5.0");
        panel.add(txtR2);

        panel.add(new JLabel("R₃ (полуось):"));
        txtR3 = new JTextField("5.0");
        panel.add(txtR3);

        panel.add(new JLabel("Мин. N:"));
        txtMinN = new JTextField("100");
        panel.add(txtMinN);

        panel.add(new JLabel("Макс. N:"));
        txtMaxN = new JTextField("10000");
        panel.add(txtMaxN);

        panel.add(new JLabel("Шаги:"));
        txtSteps = new JTextField("5");
        panel.add(txtSteps);

        return panel;
    }

    private JPanel createResultPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Результаты вычислений"));

        String[] columnNames = {"N (точек)", "M (в области)", "Объём V", "Интеграл Ĩ", "Ошибка σ"};
        tableModel = new DefaultTableModel(columnNames, 0);
        resultTable = new JTable(tableModel);
        JScrollPane tableScrollPane = new JScrollPane(resultTable);
        tableScrollPane.setPreferredSize(new Dimension(700, 200));

        panel.add(tableScrollPane, BorderLayout.NORTH);

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane textScrollPane = new JScrollPane(resultArea);
        textScrollPane.setPreferredSize(new Dimension(700, 200));

        panel.add(textScrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));

        calculateButton = new JButton("Вычислить");
        calculateButton.setFont(new Font("Arial", Font.BOLD, 14));
        calculateButton.addActionListener(e -> performCalculation());

        clearButton = new JButton("Очистить");
        clearButton.setFont(new Font("Arial", Font.BOLD, 14));
        clearButton.addActionListener(e -> clearResults());

        panel.add(calculateButton);
        panel.add(clearButton);

        return panel;
    }

    private void performCalculation() {
        try {
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

            ellipsoidPanel.setEllipsoidParameters(R1, R2, R3);
            ellipsoidPanel.repaint();

            tableModel.setRowCount(0);
            resultArea.setText("");

            double volumeW = (2 * R1) * (2 * R2) * (2 * R3);
            double theoreticalVolume = (4.0 / 3.0) * Math.PI * R1 * R2 * R3;

            StringBuilder info = new StringBuilder();
            info.append("ПАРАМЕТРЫ ЗАДАЧИ\n");
            info.append("==================================================\n");
            info.append(String.format("Функция плотности: ρ(x) = |x₁-a₁|^β₁ · |x₂-a₂|^β₂ · |x₃-a₃|^β₃\n"));
            info.append(String.format("a₁=%.2f, a₂=%.2f, a₃=%.2f\n", a1, a2, a3));
            info.append(String.format("β₁=%.2f, β₂=%.2f, β₃=%.2f\n\n", beta1, beta2, beta3));
            info.append(String.format("Область: эллипсоид (x₁²/R₁²)+(x₂²/R₂²)+(x₃²/R₃²) ≤ 1\n"));
            info.append(String.format("Полуоси: R₁=%.2f, R₂=%.2f, R₃=%.2f\n\n", R1, R2, R3));
            info.append(String.format("Объём параллелепипеда W = %.4f\n", volumeW));
            info.append(String.format("Теоретический объём эллипсоида V = %.4f\n\n", theoreticalVolume));

            int[] nValues = generateNValues(minN, maxN, steps);
            info.append("РЕЗУЛЬТАТЫ ВЫЧИСЛЕНИЙ\n");
            info.append("==================================================\n");

            Random random = new Random();

            for (int N : nValues) {
                int M = 0;
                double sumF = 0.0;
                double sumF2 = 0.0;

                for (int i = 0; i < N; i++) {
                    double x1 = -R1 + 2 * R1 * random.nextDouble();
                    double x2 = -R2 + 2 * R2 * random.nextDouble();
                    double x3 = -R3 + 2 * R3 * random.nextDouble();

                    double ellipsoid = (x1 * x1) / (R1 * R1) +
                            (x2 * x2) / (R2 * R2) +
                            (x3 * x3) / (R3 * R3);

                    if (ellipsoid <= 1.0) {
                        M++;
                        double rho = Math.pow(Math.abs(x1 - a1), beta1) *
                                Math.pow(Math.abs(x2 - a2), beta2) *
                                Math.pow(Math.abs(x3 - a3), beta3);
                        sumF += rho;
                        sumF2 += rho * rho;
                    }
                }

                double volumeV = volumeW * M / N;
                double integral = 0.0;
                if (M > 0) {
                    integral = volumeW * sumF / M;
                }

                double standardError = 0.0;
                if (M > 1) {
                    double mean = sumF / M;
                    double variance = (sumF2 / M - mean * mean) * volumeW * volumeW / M;
                    standardError = Math.sqrt(variance);
                }

                Object[] row = {N, M, String.format("%.6f", volumeV),
                        String.format("%.6f", integral),
                        String.format("%.6f", standardError)};
                tableModel.addRow(row);

                info.append(String.format("N=%6d: M=%6d, V≈%.6f, Ĩ≈%.6f±%.6f\n",
                        N, M, volumeV, integral, standardError));
            }

            info.append("\n=====================================\n");
            info.append("Вычисления завершены успешно!\n");
            info.append(String.format("Точность метода: O(N^(-1/2))\n"));
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

    // Класс для 3D точки
    static class Point3D {
        double x, y, z;

        Point3D(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        Point project(double angleX, double angleY, double scale, int centerX, int centerY) {
            // Поворот вокруг оси Y
            double cosY = Math.cos(angleY);
            double sinY = Math.sin(angleY);
            double x1 = x * cosY - z * sinY;
            double z1 = x * sinY + z * cosY;

            // Поворот вокруг оси X
            double cosX = Math.cos(angleX);
            double sinX = Math.sin(angleX);
            double y1 = y * cosX - z1 * sinX;
            double z2 = y * sinX + z1 * cosX;

            // Перспективная проекция
            double distance = 10;
            double factor = distance / (distance + z2);

            int screenX = (int) (centerX + x1 * scale * factor);
            int screenY = (int) (centerY - y1 * scale * factor);

            return new Point(screenX, screenY);
        }
    }

    // Панель для 3D визуализации эллипсоида
    class EllipsoidPanel3D extends JPanel {
        private double R1 = 5.0, R2 = 5.0, R3 = 5.0;
        private double angleX = 0.3, angleY = 0.4;
        private Point lastMouse;
        private List<Point3D> vertices;
        private List<int[]> edges;

        public EllipsoidPanel3D() {
            generateEllipsoidMesh();

            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    lastMouse = e.getPoint();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (lastMouse != null) {
                        int dx = e.getX() - lastMouse.x;
                        int dy = e.getY() - lastMouse.y;

                        angleY += dx * 0.01;
                        angleX += dy * 0.01;

                        lastMouse = e.getPoint();
                        repaint();
                    }
                }
            };

            addMouseListener(mouseAdapter);
            addMouseMotionListener(mouseAdapter);

            // Автоматическое вращение
            Timer timer = new Timer(50, e -> {
                if (lastMouse == null) {
                    angleY += 0.02;
                    repaint();
                }
            });
            timer.start();
        }

        public void setEllipsoidParameters(double r1, double r2, double r3) {
            this.R1 = r1;
            this.R2 = r2;
            this.R3 = r3;
            generateEllipsoidMesh();
        }

        private void generateEllipsoidMesh() {
            vertices = new ArrayList<>();
            edges = new ArrayList<>();

            int latitudes = 20;
            int longitudes = 30;

            // Генерация вершин эллипсоида
            for (int i = 0; i <= latitudes; i++) {
                double theta = Math.PI * i / latitudes;
                for (int j = 0; j <= longitudes; j++) {
                    double phi = 2 * Math.PI * j / longitudes;

                    double x = R1 * Math.sin(theta) * Math.cos(phi);
                    double y = R2 * Math.sin(theta) * Math.sin(phi);
                    double z = R3 * Math.cos(theta);

                    vertices.add(new Point3D(x, y, z));
                }
            }

            // Генерация рёбер (wireframe)
            for (int i = 0; i < latitudes; i++) {
                for (int j = 0; j < longitudes; j++) {
                    int current = i * (longitudes + 1) + j;
                    int next = current + longitudes + 1;

                    // Вертикальные линии
                    edges.add(new int[]{current, next});

                    // Горизонтальные линии
                    edges.add(new int[]{current, current + 1});
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int centerX = width / 2;
            int centerY = height / 2;

            // Фон
            g2d.setColor(new Color(240, 240, 250));
            g2d.fillRect(0, 0, width, height);

            double scale = Math.min(width, height) / (3.0 * Math.max(Math.max(R1, R2), R3));

            // Рисуем оси координат
            drawAxes(g2d, centerX, centerY, scale);

            // Рисуем wireframe эллипсоида
            g2d.setColor(new Color(50, 100, 200));
            g2d.setStroke(new BasicStroke(1.5f));

            for (int[] edge : edges) {
                Point3D p1 = vertices.get(edge[0]);
                Point3D p2 = vertices.get(edge[1]);

                Point proj1 = p1.project(angleX, angleY, scale, centerX, centerY);
                Point proj2 = p2.project(angleX, angleY, scale, centerX, centerY);

                g2d.drawLine(proj1.x, proj1.y, proj2.x, proj2.y);
            }

            // Информация
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 11));
            g2d.drawString(String.format("Эллипсоид: R₁=%.1f, R₂=%.1f, R₃=%.1f", R1, R2, R3), 10, height - 30);
            g2d.setFont(new Font("Arial", Font.PLAIN, 10));
            g2d.drawString("Перетащите мышью для вращения", 10, height - 10);
        }

        private void drawAxes(Graphics2D g2d, int centerX, int centerY, double scale) {
            int axisLength = 100;

            // Ось X (красная)
            Point3D xAxis = new Point3D(axisLength / scale, 0, 0);
            Point xProj = xAxis.project(angleX, angleY, scale, centerX, centerY);
            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.drawLine(centerX, centerY, xProj.x, xProj.y);
            g2d.drawString("x₁", xProj.x + 5, xProj.y);

            // Ось Y (зеленая)
            Point3D yAxis = new Point3D(0, axisLength / scale, 0);
            Point yProj = yAxis.project(angleX, angleY, scale, centerX, centerY);
            g2d.setColor(Color.GREEN);
            g2d.drawLine(centerX, centerY, yProj.x, yProj.y);
            g2d.drawString("x₂", yProj.x + 5, yProj.y);

            // Ось Z (синяя)
            Point3D zAxis = new Point3D(0, 0, axisLength / scale);
            Point zProj = zAxis.project(angleX, angleY, scale, centerX, centerY);
            g2d.setColor(Color.BLUE);
            g2d.drawLine(centerX, centerY, zProj.x, zProj.y);
            g2d.drawString("x₃", zProj.x + 5, zProj.y);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Lab7());
    }
}
