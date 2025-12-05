import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

public class LAB11V extends JFrame {

    // --- Параметры задачи ---
    private double L = 1.0;
    private double A = 1.0;
    private int N = 100;
    private double TAU = 0.005;
    private double T_MAX = 2.0;
    private int delay = 50;

    // --- Вычисляемые параметры ---
    private double H;
    private double c_courant; // Число Куранта

    // --- Массивы ---
    private double[] u_prev; // Слой n-1
    private double[] u_curr; // Слой n
    private double[] u_next; // Слой n+1

    private double currentTime = 0;
    private int currentStep = 0;
    private boolean isRunning = false;

    // --- GUI Компоненты ---
    private WavePanel wavePanel;
    private Timer animationTimer;
    private JButton startButton, stopButton, resetButton, applyButton;
    private JLabel timeLabel, stepLabel, energyLabel, courantLabel;
    private JSlider speedSlider;

    // Поля ввода
    private JTextField nField, tauField, aField, lField, tmaxField;
    private JComboBox<String> initialConditionBox;
    private JComboBox<String> methodBox; // Выбор метода

    // --- Константы методов ---
    private static final int METHOD_EXPLICIT = 0; // Явная схема
    private static final int METHOD_IMPLICIT = 1; // Неявная схема

    public LAB11V() {
        setTitle("Лабораторная работа 11: Волновое уравнение (Явная + Неявная схемы)");
        setSize(1250, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Инициализация по умолчанию
        calculateDerivedParameters();
        initializeArrays();

        // Создание панелей
        createParametersPanel();
        createControlPanel();
        createInfoPanel();

        // Панель отрисовки
        wavePanel = new WavePanel();
        add(wavePanel, BorderLayout.CENTER);

        // Таймер анимации
        animationTimer = new Timer(delay, e -> updateSimulation());

        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ----------------------------------------------------------------------
    // ЛОГИКА РАСЧЕТА
    // ----------------------------------------------------------------------

    private void calculateDerivedParameters() {
        H = L / N;
        c_courant = (A * TAU) / H;
    }

    private void initializeArrays() {
        u_prev = new double[N + 1];
        u_curr = new double[N + 1];
        u_next = new double[N + 1];

        // Заполняем нулевой и первый слои (одинаково для обоих методов)
        initializeConditions();
    }

    // --- Начальные и граничные условия (функции mu) ---

    // μ1(x) - Начальное положение
    private double mu1(double x) {
        int choice = initialConditionBox != null ? initialConditionBox.getSelectedIndex() : 0;
        switch (choice) {
            case 0: // Стоячая волна (тест)
                return Math.sin(Math.PI * x / L);
            case 1: // 2 периода
                return Math.sin(2 * Math.PI * x / L);
            case 2: // Гауссов импульс
                return Math.exp(-80 * Math.pow(x - L/2, 2));
            case 3: // Треугольник
                if (x < L/3) return 0;
                if (x < L/2) return 6 * (x - L/3) / L;
                if (x < 2*L/3) return 6 * (2*L/3 - x) / L;
                return 0;
            default:
                return Math.sin(Math.PI * x / L);
        }
    }

    // μ2(x) - Начальная скорость (везде 0 по умолчанию)
    private double mu2(double x) {
        return 0.0;
    }

    // μ3(t) - Левая граница
    private double mu3(double t) {
        return 0.0;
    }

    // μ4(t) - Правая граница
    private double mu4(double t) {
        return 0.0;
    }

    // Вторая производная начального условия (для ряда Тейлора)
    private double d2mu1_dx2(double x) {
        int choice = initialConditionBox != null ? initialConditionBox.getSelectedIndex() : 0;
        switch (choice) {
            case 0:
                double k1 = Math.PI / L;
                return -k1 * k1 * Math.sin(k1 * x);
            case 1:
                double k2 = 2 * Math.PI / L;
                return -k2 * k2 * Math.sin(k2 * x);
            case 2:
                // Производная для exp(-a(x-c)^2) -> (4a^2(x-c)^2 - 2a) * exp...
                double arg = x - L/2;
                double alpha = 80;
                return Math.exp(-alpha * arg * arg) * (4 * alpha * alpha * arg * arg - 2 * alpha);
            default:
                return 0; // Для треугольника сложно аналитически, пусть будет 0 (погрешность на 1 шаге)
        }
    }

    private void initializeConditions() {
        // Слой 0 (t = 0)
        for (int j = 0; j <= N; j++) {
            double x = j * H;
            u_prev[j] = mu1(x);
        }

        // Слой 1 (t = τ). Используем ряд Тейлора (формула из методички)
        // u(x, τ) = u(x,0) + τ*u_t(x,0) + (τ^2/2)*u_tt(x,0)
        // u_tt = a^2 * u_xx
        for (int j = 0; j <= N; j++) {
            double x = j * H;
            u_curr[j] = mu1(x) + TAU * mu2(x) + 0.5 * TAU * TAU * A * A * d2mu1_dx2(x);
        }

        // Граничные условия на 1-м слое
        u_curr[0] = mu3(TAU);
        u_curr[N] = mu4(TAU);
    }

    // --- ШАГ ПО ВРЕМЕНИ ---
    private void timeStep() {
        int method = methodBox.getSelectedIndex();

        if (method == METHOD_EXPLICIT) {
            solveExplicit();
        } else {
            solveImplicit();
        }

        // Сдвиг массивов: prev <- curr, curr <- next
        // Копируем данные, чтобы не путать ссылки
        System.arraycopy(u_curr, 0, u_prev, 0, N + 1);
        System.arraycopy(u_next, 0, u_curr, 0, N + 1);
    }

    // 1. ЯВНАЯ СХЕМА ("Крест")
    private void solveExplicit() {
        double sigma = c_courant * c_courant; // (a*tau/h)^2

        for (int j = 1; j < N; j++) {
            // Формула (17.2) преобразованная
            u_next[j] = 2 * u_curr[j] - u_prev[j] +
                    sigma * (u_curr[j + 1] - 2 * u_curr[j] + u_curr[j - 1]);
        }

        // Границы
        double nextTime = currentTime + TAU;
        u_next[0] = mu3(nextTime);
        u_next[N] = mu4(nextTime);
    }

    // 2. НЕЯВНАЯ СХЕМА (Метод прогонки)
    // Схема (17.1) с весами (обычно чисто неявная или симметричная).
    // В методичке формула (17.1) описывает схему с весом 0.5 (полусумма на слоях j+1 и j-1 для аппроксимации второй производной по t?
    // Нет, обычно неявная схема для волнового уравнения выглядит так:
    // (y_new - 2y_curr + y_old)/tau^2 = A^2 * (y_new_xx * sigma + y_old_xx * (1-sigma)... )
    // В методичке приведена схема (17.1) c оператором "Лямбда".
    // Обычно реализуют схему с весами:
    // (y^{n+1} - 2y^n + y^{n-1})/tau^2 = a^2 * Lambda( sigma*y^{n+1} + (1-2sigma)y^n + sigma*y^{n-1} )
    // Для абсолютно устойчивой схемы часто берут вес 0.25 или 0.5.
    // Реализуем схему с весом 0.25 (абсолютно устойчивая) для y^{n+1} и y^{n-1}.
    // A*y_{i+1}^{n+1} + B*y_{i}^{n+1} + C*y_{i-1}^{n+1} = F
    private void solveImplicit() {
        double sigma = 0.25; // Вес схемы (для безусловной устойчивости >= 0.25)
        double gamma = (A * A * TAU * TAU) / (H * H); // Число Куранта в квадрате

        // Коэффициенты для трехдиагональной матрицы (для слоя n+1)
        // Вид: alpha * y_{j+1} + beta * y_j + alpha * y_{j-1} = RHS
        // После преобразования схемы с весами:
        // y^{n+1} - gamma*sigma*Lambda(y^{n+1}) = 2y^n - y^{n-1} + gamma*(1-2sigma)*Lambda(y^n) + gamma*sigma*Lambda(y^{n-1})

        // Коэффициенты прогонки a*y_{j-1} + b*y_j + c*y_{j+1} = d
        double a_coef = -sigma * gamma;
        double c_coef = -sigma * gamma;
        double b_coef = 1 + 2 * sigma * gamma;

        double[] alpha = new double[N + 1];
        double[] beta = new double[N + 1];

        // Прямая прогонка
        // Левая граница: u_0 = mu3
        double nextTime = currentTime + TAU;
        alpha[0] = 0;
        beta[0] = mu3(nextTime);

        for (int j = 1; j < N; j++) {
            // Правая часть F (включает значения со слоев n и n-1)
            double laplace_curr = u_curr[j + 1] - 2 * u_curr[j] + u_curr[j - 1];
            double laplace_prev = u_prev[j + 1] - 2 * u_prev[j] + u_prev[j - 1];

            double rhs = 2 * u_curr[j] - u_prev[j] +
                    gamma * (1 - 2 * sigma) * laplace_curr +
                    gamma * sigma * laplace_prev;

            double denom = b_coef + a_coef * alpha[j - 1];
            alpha[j] = -c_coef / denom;
            beta[j] = (rhs - a_coef * beta[j - 1]) / denom;
        }

        // Обратная прогонка
        // Правая граница: u_N = mu4
        u_next[N] = mu4(nextTime);

        for (int j = N - 1; j >= 0; j--) {
            u_next[j] = alpha[j] * u_next[j + 1] + beta[j];
        }
    }

    // --- Остальная логика (Энергия, Обновление) ---

    private double calculateEnergy() {
        double energy = 0.0;
        // Кинетическая + Потенциальная (упрощенно)
        for (int j = 1; j < N; j++) {
            double du_dt = (u_curr[j] - u_prev[j]) / TAU;
            double du_dx = (u_curr[j] - u_curr[j-1]) / H;
            energy += (du_dt * du_dt + A * A * du_dx * du_dx);
        }
        return 0.5 * energy * H;
    }

    private void updateSimulation() {
        if (currentTime >= T_MAX) {
            stopSimulation();
            JOptionPane.showMessageDialog(this, "Время вышло!", "Конец", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        timeStep();
        currentTime += TAU;
        currentStep++;

        timeLabel.setText(String.format("Время: %.4f с", currentTime));
        stepLabel.setText("Шаг: " + currentStep);
        energyLabel.setText(String.format("Энергия: %.4f", calculateEnergy()));
        wavePanel.repaint();
    }

    // ----------------------------------------------------------------------
    // GUI КОМПОНЕНТЫ
    // ----------------------------------------------------------------------

    private void createParametersPanel() {
        JPanel paramPanel = new JPanel(new GridLayout(9, 2, 5, 5));
        paramPanel.setBorder(BorderFactory.createTitledBorder("Параметры"));
        paramPanel.setPreferredSize(new Dimension(300, 0));

        // Метод решения
        paramPanel.add(new JLabel("Метод решения:"));
        String[] methods = {"Явная схема (Крест)", "Неявная схема (Прогонка)"};
        methodBox = new JComboBox<>(methods);
        paramPanel.add(methodBox);

        // Числовые поля
        paramPanel.add(new JLabel("N (узлы):"));
        nField = new JTextField(String.valueOf(N)); paramPanel.add(nField);

        paramPanel.add(new JLabel("τ (шаг времени):"));
        tauField = new JTextField(String.valueOf(TAU)); paramPanel.add(tauField);

        paramPanel.add(new JLabel("a (скорость):"));
        aField = new JTextField(String.valueOf(A)); paramPanel.add(aField);

        paramPanel.add(new JLabel("L (длина):"));
        lField = new JTextField(String.valueOf(L)); paramPanel.add(lField);

        paramPanel.add(new JLabel("T_max:"));
        tmaxField = new JTextField(String.valueOf(T_MAX)); paramPanel.add(tmaxField);

        // Начальные условия
        paramPanel.add(new JLabel("Начальное условие:"));
        String[] conditions = {
                "sin(πx/L) - Тест",
                "sin(2πx/L) - 2 волны",
                "exp(...) - Импульс",
                "Треугольник"
        };
        initialConditionBox = new JComboBox<>(conditions);
        paramPanel.add(initialConditionBox);

        // Кнопка
        applyButton = new JButton("✓ Применить и Сбросить");
        applyButton.addActionListener(e -> applyParameters());
        paramPanel.add(new JLabel("")); // placeholder
        paramPanel.add(applyButton);

        add(paramPanel, BorderLayout.WEST);
    }

    private void applyParameters() {
        try {
            int newN = Integer.parseInt(nField.getText());
            double newTau = Double.parseDouble(tauField.getText());
            double newA = Double.parseDouble(aField.getText());
            double newL = Double.parseDouble(lField.getText());
            double newTmax = Double.parseDouble(tmaxField.getText());

            if (newN < 5) throw new Exception("N слишком мало");
            if (newTau <= 0) throw new Exception("Tau <= 0");

            N = newN; TAU = newTau; A = newA; L = newL; T_MAX = newTmax;

            calculateDerivedParameters();
            updateCourantLabel();

            // Проверка устойчивости для явной схемы
            if (methodBox.getSelectedIndex() == METHOD_EXPLICIT && Math.abs(c_courant) > 1.0) {
                int res = JOptionPane.showConfirmDialog(this,
                        "Для ЯВНОЙ схемы число Куранта > 1 (" + String.format("%.3f", c_courant) + ").\n" +
                                "Решение будет неустойчивым (взрыв).\nПродолжить?",
                        "Внимание", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (res != JOptionPane.YES_OPTION) return;
            }

            resetSimulation();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + ex.getMessage());
        }
    }

    private void createControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        startButton = new JButton("▶ Старт");
        startButton.addActionListener(e -> startSimulation());

        stopButton = new JButton("⏸ Стоп");
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopSimulation());

        resetButton = new JButton("↺ Сброс");
        resetButton.addActionListener(e -> resetSimulation());

        speedSlider = new JSlider(0, 100, 50);
        speedSlider.addChangeListener(e -> {
            delay = 110 - speedSlider.getValue();
            if (animationTimer.isRunning()) animationTimer.setDelay(delay);
        });

        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        controlPanel.add(resetButton);
        controlPanel.add(new JLabel("  Скорость:"));
        controlPanel.add(speedSlider);
        add(controlPanel, BorderLayout.NORTH);
    }

    private void createInfoPanel() {
        JPanel infoPanel = new JPanel(new GridLayout(5, 1));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        timeLabel = new JLabel("Время: 0.0000");
        stepLabel = new JLabel("Шаг: 0");
        energyLabel = new JLabel("Энергия: 0.0000");
        courantLabel = new JLabel("Число Куранта: -");

        infoPanel.add(timeLabel);
        infoPanel.add(stepLabel);
        infoPanel.add(energyLabel);
        infoPanel.add(courantLabel);

        // Добавляем в правый угол (или вниз панели параметров, если места мало)
        // Но у нас BorderLayout.EAST занят ничем, положим туда
        JPanel container = new JPanel(new BorderLayout());
        container.setBorder(BorderFactory.createTitledBorder("Инфо"));
        container.add(infoPanel, BorderLayout.NORTH);
        container.setPreferredSize(new Dimension(150, 0));
        add(container, BorderLayout.EAST);

        updateCourantLabel();
    }

    private void updateCourantLabel() {
        String text = String.format("<html>Курант c = %.3f<br>", c_courant);
        if (Math.abs(c_courant) <= 1.0) text += "<font color='green'>Устойчиво</font>";
        else text += "<font color='red'>Опасно (для явной)</font>";
        text += "</html>";
        courantLabel.setText(text);
    }

    // --- Управление анимацией ---

    private void startSimulation() {
        isRunning = true;
        animationTimer.start();
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        applyButton.setEnabled(false);
        methodBox.setEnabled(false); // Нельзя менять метод на лету
    }

    private void stopSimulation() {
        isRunning = false;
        animationTimer.stop();
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        applyButton.setEnabled(true);
        methodBox.setEnabled(true);
    }

    private void resetSimulation() {
        stopSimulation();
        currentTime = 0;
        currentStep = 0;
        initializeArrays();
        wavePanel.repaint();
        timeLabel.setText("Время: 0.0000");
        stepLabel.setText("Шаг: 0");
        energyLabel.setText(String.format("Энергия: %.4f", calculateEnergy()));
    }

    // --- Отрисовка ---
    class WavePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, w, h);

            // Оси
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawLine(50, h/2, w-50, h/2); // Ось X
            g2.drawLine(50, 50, 50, h-50);   // Ось U

            // График
            g2.setColor(Color.BLUE);
            g2.setStroke(new BasicStroke(2));

            double maxAmp = 1.5; // Масштаб по вертикали
            double scaleX = (double)(w - 100) / N;
            double scaleY = (h - 100) / (2 * maxAmp);

            Path2D path = new Path2D.Double();
            for (int i = 0; i <= N; i++) {
                double px = 50 + i * scaleX;
                double py = h/2 - u_curr[i] * scaleY;
                if (i == 0) path.moveTo(px, py);
                else path.lineTo(px, py);
            }
            g2.draw(path);

            // Точки
            g2.setColor(Color.RED);
            for (int i = 0; i <= N; i += Math.max(1, N/40)) {
                double px = 50 + i * scaleX;
                double py = h/2 - u_curr[i] * scaleY;
                g2.fillOval((int)px-2, (int)py-2, 4, 4);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LAB11V::new);
    }
}
