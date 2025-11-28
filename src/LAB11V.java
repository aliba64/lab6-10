import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

public class LAB11V extends JFrame {

    // Параметры задачи (управляемые)
    private double L = 1.0;
    private double A = 1.0;
    private int N = 100;
    private double TAU = 0.005;
    private double T_MAX = 2.0;
    private int delay = 50;

    // Вычисляемые параметры
    private double H;
    private double c;

    // Массивы для решения
    private double[] u_prev;
    private double[] u_curr;
    private double[] u_next;
    private double currentTime = 0;
    private int currentStep = 0;

    // Компоненты GUI
    private WavePanel wavePanel;
    private Timer animationTimer;
    private JButton startButton, stopButton, resetButton, applyButton;
    private JLabel timeLabel, stepLabel, energyLabel, courantLabel;
    private JSlider speedSlider;

    // Поля для параметров
    private JTextField nField, tauField, aField, lField, tmaxField;
    private JComboBox<String> initialConditionBox, boundaryConditionBox;

    private boolean isRunning = false;

    public LAB11V() {
        setTitle("Решение волнового уравнения - Лабораторная работа 11 ");
        setSize(1200, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        calculateDerivedParameters();
        initializeArrays();

        createParametersPanel();
        createControlPanel();
        createInfoPanel();

        wavePanel = new WavePanel();
        add(wavePanel, BorderLayout.CENTER);

        animationTimer = new Timer(delay, e -> updateSimulation());

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void calculateDerivedParameters() {
        H = L / N;
        c = A * TAU / H;
    }

    private void createParametersPanel() {
        JPanel paramPanel = new JPanel(new GridLayout(8, 2, 5, 5));
        paramPanel.setBorder(BorderFactory.createTitledBorder("Параметры задачи"));
        paramPanel.setPreferredSize(new Dimension(250, 0));

        // Поля ввода параметров
        paramPanel.add(new JLabel("N (число шагов):"));
        nField = new JTextField(String.valueOf(N));
        paramPanel.add(nField);

        paramPanel.add(new JLabel("τ (временной шаг):"));
        tauField = new JTextField(String.valueOf(TAU));
        paramPanel.add(tauField);

        paramPanel.add(new JLabel("a (скорость волны):"));
        aField = new JTextField(String.valueOf(A));
        paramPanel.add(aField);

        paramPanel.add(new JLabel("L (длина области):"));
        lField = new JTextField(String.valueOf(L));
        paramPanel.add(lField);

        paramPanel.add(new JLabel("T_max (время):"));
        tmaxField = new JTextField(String.valueOf(T_MAX));
        paramPanel.add(tmaxField);

        // Выбор начальных условий
        paramPanel.add(new JLabel("Начальное условие:"));
        String[] initialConditions = {
                "sin(πx/L) - стоячая волна",
                "sin(2πx/L) - 2 периода",
                "exp(-50(x-0.5)²) - гауссов импульс",
                "Треугольник в центре"
        };
        initialConditionBox = new JComboBox<>(initialConditions);
        paramPanel.add(initialConditionBox);

        // Выбор граничных условий
        paramPanel.add(new JLabel("Граничные условия:"));
        String[] boundaryConditions = {
                "Закрепленные концы (u=0)",
                "Свободные концы",
                "Периодические"
        };
        boundaryConditionBox = new JComboBox<>(boundaryConditions);
        paramPanel.add(boundaryConditionBox);

        // Кнопка применения параметров
        applyButton = new JButton("✓ Применить");
        applyButton.setBackground(new Color(100, 200, 100));
        applyButton.addActionListener(e -> applyParameters());
        paramPanel.add(applyButton);

        add(paramPanel, BorderLayout.WEST);
    }

    private void applyParameters() {
        try {
            // Считываем новые параметры
            int newN = Integer.parseInt(nField.getText());
            double newTau = Double.parseDouble(tauField.getText());
            double newA = Double.parseDouble(aField.getText());
            double newL = Double.parseDouble(lField.getText());
            double newTmax = Double.parseDouble(tmaxField.getText());

            // Проверка корректности
            if (newN < 10 || newN > 1000) {
                JOptionPane.showMessageDialog(this, "N должно быть от 10 до 1000",
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (newTau <= 0 || newTau > 0.1) {
                JOptionPane.showMessageDialog(this, "τ должно быть от 0 до 0.1",
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Применяем параметры
            N = newN;
            TAU = newTau;
            A = newA;
            L = newL;
            T_MAX = newTmax;

            calculateDerivedParameters();

            // Проверка условия Куранта
            if (Math.abs(c) >= 1.0) {
                int response = JOptionPane.showConfirmDialog(this,
                        "ВНИМАНИЕ! Условие Куранта нарушено: |c| = " + String.format("%.4f", c) +
                                "\nСхема может быть неустойчивой. Продолжить?",
                        "Предупреждение", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (response != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            // Обновляем courant label
            updateCourantLabel();

            // Сброс и перезапуск
            resetSimulation();

            JOptionPane.showMessageDialog(this,
                    "Параметры применены!\nЧисло Куранта c = " + String.format("%.4f", c),
                    "Успех", JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка ввода! Проверьте формат чисел.",
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Управление"));

        startButton = new JButton("▶ Старт");
        startButton.setBackground(new Color(100, 200, 100));
        startButton.addActionListener(e -> startSimulation());

        stopButton = new JButton("⏸ Пауза");
        stopButton.setBackground(new Color(255, 200, 100));
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopSimulation());

        resetButton = new JButton("↺ Сброс");
        resetButton.setBackground(new Color(200, 200, 255));
        resetButton.addActionListener(e -> resetSimulation());

        JLabel speedLabel = new JLabel("Скорость анимации:");
        speedSlider = new JSlider(10, 200, 50);
        speedSlider.setPreferredSize(new Dimension(200, 30));
        speedSlider.setMajorTickSpacing(50);
        speedSlider.setPaintTicks(true);
        speedSlider.addChangeListener(e -> {
            delay = 210 - speedSlider.getValue();
            if (animationTimer.isRunning()) {
                animationTimer.setDelay(delay);
            }
        });

        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        controlPanel.add(resetButton);
        controlPanel.add(new JSeparator(SwingConstants.VERTICAL));
        controlPanel.add(speedLabel);
        controlPanel.add(speedSlider);

        add(controlPanel, BorderLayout.NORTH);
    }

    private void createInfoPanel() {
        JPanel infoPanel = new JPanel(new GridLayout(7, 1, 5, 5));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Информация"));
        infoPanel.setPreferredSize(new Dimension(220, 0));

        timeLabel = new JLabel("Время: 0.0000 с");
        stepLabel = new JLabel("Шаг: 0");
        energyLabel = new JLabel("Энергия: 0.0000");

        JLabel paramLabel1 = new JLabel("<html><b>Параметры:</b><br>N = " + N +
                "<br>h = " + String.format("%.4f", H) + "</html>");
        JLabel paramLabel2 = new JLabel("<html>τ = " + TAU +
                "<br>a = " + A +
                "<br>L = " + L + "</html>");

        courantLabel = new JLabel();
        updateCourantLabel();

        JLabel helpLabel = new JLabel("<html><small>Измените параметры<br>слева и нажмите<br>\"Применить\"</small></html>");

        infoPanel.add(timeLabel);
        infoPanel.add(stepLabel);
        infoPanel.add(energyLabel);
        infoPanel.add(paramLabel1);
        infoPanel.add(paramLabel2);
        infoPanel.add(courantLabel);
        infoPanel.add(helpLabel);

        add(infoPanel, BorderLayout.EAST);
    }

    private void updateCourantLabel() {
        String status = Math.abs(c) < 1.0 ? "✓ Устойчиво" : "✗ Неустойчиво!";
        Color color = Math.abs(c) < 1.0 ? new Color(0, 150, 0) : Color.RED;
        courantLabel.setText("<html><b>Число Куранта:</b><br>c = " +
                String.format("%.4f", c) +
                "<br><font color='" +
                (Math.abs(c) < 1.0 ? "green" : "red") + "'>" +
                status + "</font></html>");
    }

    private void initializeArrays() {
        u_prev = new double[N + 1];
        u_curr = new double[N + 1];
        u_next = new double[N + 1];
        initializeConditions();
    }

    // Начальное условие μ1(x)
    private double mu1(double x) {
        int choice = initialConditionBox != null ? initialConditionBox.getSelectedIndex() : 0;
        switch (choice) {
            case 0: // sin(πx/L)
                return Math.sin(Math.PI * x / L);
            case 1: // sin(2πx/L)
                return Math.sin(2 * Math.PI * x / L);
            case 2: // Гауссов импульс
                return Math.exp(-50 * Math.pow(x - L/2, 2));
            case 3: // Треугольник
                if (x < L/3) return 0;
                if (x < L/2) return 6 * (x - L/3) / L;
                if (x < 2*L/3) return 6 * (2*L/3 - x) / L;
                return 0;
            default:
                return Math.sin(Math.PI * x / L);
        }
    }

    private double mu2(double x) {
        return 0.0;
    }

    private double mu3(double t) {
        return 0.0; // можно добавить вынужденные колебания
    }

    private double mu4(double t) {
        return 0.0;
    }

    private double d2mu1_dx2(double x) {
        int choice = initialConditionBox != null ? initialConditionBox.getSelectedIndex() : 0;
        switch (choice) {
            case 0:
                return -(Math.PI / L) * (Math.PI / L) * Math.sin(Math.PI * x / L);
            case 1:
                return -4 * (Math.PI / L) * (Math.PI / L) * Math.sin(2 * Math.PI * x / L);
            case 2:
                double arg = x - L/2;
                return Math.exp(-50 * arg * arg) * (100 * 50 * arg * arg - 100);
            default:
                return 0;
        }
    }

    private void initializeConditions() {
        for (int j = 0; j <= N; j++) {
            double x = j * H;
            u_prev[j] = mu1(x);
        }

        for (int j = 0; j <= N; j++) {
            double x = j * H;
            u_curr[j] = mu1(x) + TAU * mu2(x) +
                    0.5 * TAU * TAU * A * A * d2mu1_dx2(x);
        }

        u_curr[0] = mu3(TAU);
        u_curr[N] = mu4(TAU);
    }

    private void timeStep() {
        double c2 = c * c;

        for (int j = 1; j < N; j++) {
            u_next[j] = 2 * u_curr[j] - u_prev[j] +
                    c2 * (u_curr[j+1] - 2 * u_curr[j] + u_curr[j-1]);
        }

        u_next[0] = mu3(currentTime + TAU);
        u_next[N] = mu4(currentTime + TAU);

        double[] temp = u_prev;
        u_prev = u_curr;
        u_curr = u_next;
        u_next = temp;
    }

    private double calculateEnergy() {
        double energy = 0.0;
        for (int j = 0; j <= N; j++) {
            energy += u_curr[j] * u_curr[j];
        }
        return energy * H;
    }

    private void updateSimulation() {
        if (currentTime >= T_MAX) {
            stopSimulation();
            JOptionPane.showMessageDialog(this,
                    "Моделирование завершено!\nВремя: " + String.format("%.4f", currentTime) + " с" +
                            "\nШагов: " + currentStep,
                    "Готово", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        timeStep();
        currentTime += TAU;
        currentStep++;

        timeLabel.setText("Время: " + String.format("%.4f", currentTime) + " с");
        stepLabel.setText("Шаг: " + currentStep);
        energyLabel.setText("Энергия: " + String.format("%.4f", calculateEnergy()));

        wavePanel.repaint();
    }

    private void startSimulation() {
        isRunning = true;
        animationTimer.start();
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        applyButton.setEnabled(false);
    }

    private void stopSimulation() {
        isRunning = false;
        animationTimer.stop();
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        applyButton.setEnabled(true);
    }

    private void resetSimulation() {
        stopSimulation();
        currentTime = 0;
        currentStep = 0;
        calculateDerivedParameters();
        initializeArrays();
        timeLabel.setText("Время: 0.0000 с");
        stepLabel.setText("Шаг: 0");
        energyLabel.setText("Энергия: " + String.format("%.4f", calculateEnergy()));
        wavePanel.repaint();
    }

    class WavePanel extends JPanel {
        private final int PADDING = 50;

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            g2d.setColor(new Color(245, 245, 250));
            g2d.fillRect(0, 0, width, height);

            drawAxes(g2d, width, height);
            drawWave(g2d, width, height);
            drawLegend(g2d);
        }

        private void drawAxes(Graphics2D g2d, int width, int height) {
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2));

            int yCenter = height / 2;
            g2d.drawLine(PADDING, yCenter, width - PADDING, yCenter);
            g2d.drawString("x", width - PADDING + 10, yCenter + 5);

            g2d.drawLine(PADDING, PADDING, PADDING, height - PADDING);
            g2d.drawString("u(x,t)", PADDING - 30, PADDING - 15);

            g2d.setStroke(new BasicStroke(1));
            g2d.setColor(Color.GRAY);
            for (int i = 0; i <= 10; i++) {
                int x = PADDING + i * (width - 2 * PADDING) / 10;
                g2d.drawLine(x, yCenter - 5, x, yCenter + 5);
                g2d.setColor(Color.BLACK);
                g2d.drawString(String.format("%.2f", i * L / 10), x - 15, yCenter + 20);
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.drawLine(x, PADDING, x, height - PADDING);
            }

            g2d.setColor(Color.GRAY);
            for (int i = -2; i <= 2; i++) {
                if (i == 0) continue;
                int y = yCenter - i * (height - 2 * PADDING) / 5;
                g2d.drawLine(PADDING - 5, y, PADDING + 5, y);
                g2d.setColor(Color.BLACK);
                g2d.drawString(String.format("%.1f", i * 0.5), PADDING - 35, y + 5);
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.drawLine(PADDING, y, width - PADDING, y);
            }
        }

        private void drawWave(Graphics2D g2d, int width, int height) {
            g2d.setColor(new Color(30, 120, 200));
            g2d.setStroke(new BasicStroke(3));

            int yCenter = height / 2;
            double xScale = (width - 2 * PADDING) / (double) N;
            double yScale = (height - 2 * PADDING) / 2.5;

            GeneralPath path = new GeneralPath();

            for (int j = 0; j <= N; j++) {
                double x = PADDING + j * xScale;
                double y = yCenter - u_curr[j] * yScale;

                if (j == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }

            g2d.draw(path);

            g2d.setColor(new Color(220, 50, 50));
            for (int j = 0; j <= N; j += Math.max(1, N/50)) {
                double x = PADDING + j * xScale;
                double y = yCenter - u_curr[j] * yScale;
                g2d.fillOval((int)x - 3, (int)y - 3, 6, 6);
            }
        }

        private void drawLegend(Graphics2D g2d) {
            g2d.setColor(new Color(50, 50, 150));
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            String title = "Волновое уравнение: ∂²u/∂t² = a²∂²u/∂x²";
            g2d.drawString(title, 80, 25);

            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            String condition = "Начальное условие: " +
                    (String)initialConditionBox.getSelectedItem();
            g2d.drawString(condition, 80, 45);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LAB11V());
    }
}
