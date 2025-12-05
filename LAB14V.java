import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

public class LAB14V extends JFrame {

    // --- Параметры модели (Сетка и Физика) ---
    private static final double L = 1.0;          // Длина стержня
    private static final double T_MAX = 1.5;      // Время моделирования
    private static final int Nz = 60;             // Узлы по пространству
    private static final int Nt = 400;            // Узлы по времени (должно быть много для устойчивости)

    private final double h = L / (Nz - 1);        // Шаг по z
    private final double tau = T_MAX / (Nt - 1);  // Шаг по t

    // Коэффициенты уравнения: eps * u_tt + sigma * u_t = (1/mu)*u_zz ...
    private final double eps = 1.0;
    private final double mu = 1.0;
    private final double lambdaSq = 0.5;

    // --- Данные ---
    private double[] sigmaTrue;     // Истинное значение (скрытое)
    private double[] sigmaCurrent;  // Наше приближение
    private double[] observedSignal; // То, что измерил датчик f(t) = u(0,t)

    // --- Компоненты GUI ---
    private PlotPanel mainPlot;
    private PlotPanel errorPlot;
    private JLabel statusLabel;
    private JButton startButton;
    private JProgressBar progressBar;
    private SwingWorker<Void, IterationData> solverWorker;

    public LAB14V() {
        super("Лаб. 14: Обратная задача (Восстановление Sigma)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 700);
        setLayout(new BorderLayout());

        // Проверка условия Куранта (устойчивость численной схемы)
        // c = 1/sqrt(eps*mu). Условие: c * tau / h <= 1
        double c = 1.0 / Math.sqrt(eps * mu);
        double courant = c * tau / h;
        System.out.println("Число Куранта: " + courant);
        if (courant > 1.0) {
            JOptionPane.showMessageDialog(this, "Внимание! Схема неустойчива (Courant > 1). Увеличьте Nt.");
        }

        // Инициализация данных
        initData();

        // Настройка GUI
        setupUI();
    }

    private void initData() {
        sigmaTrue = new double[Nz];
        sigmaCurrent = new double[Nz];

        // 1. Создаем "Загадку": Истинная сигма имеет форму купола в центре
        for (int i = 0; i < Nz; i++) {
            double z = i * h;
            // Базовое затухание + аномалия
            sigmaTrue[i] = 0.5 + 4.0 * Math.exp(-50 * Math.pow(z - 0.6, 2));
            // Начальное приближение - просто ровная линия (мы ничего не знаем)
            sigmaCurrent[i] = 0.5;
        }

        // 2. Генерируем "Эксперимент": Решаем прямую задачу с истинной сигмой
        double[][] uTrue = solveDirectProblem(sigmaTrue);

        // Сохраняем показания датчика на границе z=0
        observedSignal = new double[Nt];
        for (int j = 0; j < Nt; j++) {
            observedSignal[j] = uTrue[0][j];
        }
    }

    private void setupUI() {
        JPanel topPanel = new JPanel(new FlowLayout());
        startButton = new JButton("Запустить восстановление");
        JButton resetButton = new JButton("Сброс");
        statusLabel = new JLabel("Готов к работе. Шаг по времени: " + String.format("%.4f", tau));

        topPanel.add(startButton);
        topPanel.add(resetButton);
        topPanel.add(statusLabel);

        mainPlot = new PlotPanel("Коэффициент трения Sigma(z)", "Z (координата)", "Значение");
        errorPlot = new PlotPanel("Ошибка (Невязка)", "Итерации", "J(p)");

        // Добавляем линии на графики
        mainPlot.addSeries("Истинная Sigma", Color.BLUE, sigmaTrue);
        mainPlot.addSeries("Текущая Sigma", Color.RED, sigmaCurrent);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainPlot, errorPlot);
        splitPane.setResizeWeight(0.6);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(progressBar, BorderLayout.SOUTH);

        startButton.addActionListener(e -> startSolver());
        resetButton.addActionListener(e -> {
            if(solverWorker != null) solverWorker.cancel(true);
            initData();
            mainPlot.updateSeries("Текущая Sigma", sigmaCurrent);
            errorPlot.clearSeries("Ошибка");
            progressBar.setValue(0);
            statusLabel.setText("Сброс выполнен.");
            repaint();
        });
    }

    // --- МАТЕМАТИЧЕСКОЕ ЯДРО ---

    /**
     * Прямая задача: Решение гиперболического уравнения
     * eps * u_tt + sigma * u_t = (1/mu)*u_zz - ... + g
     */
    private double[][] solveDirectProblem(double[] sigma) {
        double[][] u = new double[Nz][Nt];

        // Начальные условия (покой)
        // u[i][0] = 0; u[i][1] = 0; (Java и так заполняет нулями)

        // Коэффициенты схемы "Крест"
        double A = eps / (tau * tau);
        double C = 1.0 / (mu * h * h);

        // Источник g(z,t) - импульс ("удар" по среде)
        // Пусть удар происходит близко к z=0 в начале времени

        for (int j = 1; j < Nt - 1; j++) {
            for (int i = 1; i < Nz - 1; i++) {
                double B = sigma[i] / (2.0 * tau); // sigma * u_t (центральная разность)
                double D = lambdaSq / mu;

                // Источник g (гауссов импульс во времени и пространстве)
                double tVal = j * tau;
                double zVal = i * h;
                double g = 10.0 * Math.exp(-100 * Math.pow(tVal - 0.2, 2)) * Math.exp(-50 * zVal * zVal);

                // Разностная схема (выражаем u[i][j+1])
                // A*(u_next - 2u_curr + u_prev) + B*(u_next - u_prev) = C*(u_right - 2u_curr + u_left) - D*u_curr + g

                double laplacian = u[i+1][j] - 2*u[i][j] + u[i-1][j];
                double term_tt_prev = A * (2*u[i][j] - u[i][j-1]);
                double term_t_prev = B * u[i][j-1];

                double rhs = C * laplacian - D * u[i][j] + g;

                // (A + B) * u_next = rhs + A*...
                u[i][j+1] = (rhs + term_tt_prev + term_t_prev) / (A + B);
            }

            // Граничные условия
            // z=0: u_z = 0 (изолированный конец) -> u[0] = u[1]
            u[0][j+1] = u[1][j+1];

            // z=L: u = 0 (закрепленный конец)
            u[Nz-1][j+1] = 0.0;
        }
        return u;
    }

    /**
     * Сопряженная задача (решается от T к 0)
     * Источник в граничном условии: dphi/dz = 2*(u - f)
     */
    private double[][] solveAdjointProblem(double[] sigma, double[] residual) {
        double[][] phi = new double[Nz][Nt];

        // Конечные условия phi(T) = 0 -> последние слои нули

        double A = eps / (tau * tau);
        double C = 1.0 / (mu * h * h);

        for (int j = Nt - 2; j > 0; j--) {
            for (int i = 1; i < Nz - 1; i++) {
                // В сопряженном операторе знак при первой производной меняется на противоположный
                // Но так как мы идем назад по времени, схема симметрична
                double B = sigma[i] / (2.0 * tau);
                double D = lambdaSq / mu;

                // Схема "назад": вычисляем phi[i][j-1]
                double laplacian = phi[i+1][j] - 2*phi[i][j] + phi[i-1][j];

                // Уравнение: eps*phi_tt - sigma*phi_t = ...
                // Дискретизация назад по времени приводит к похожему виду

                double term1 = A * (2*phi[i][j] - phi[i][j+1]); // от второй производной
                double term2 = B * phi[i][j+1];                 // от первой производной
                double rhs = C * laplacian - D * phi[i][j];

                // (A + B) * phi_prev = rhs + ...
                // Примечание: знаки зависят от точного вывода сопряженного оператора.
                // Для самосопряженной части (волновое) знаки те же. Для диссипативной (sigma) знак меняется.
                // При обращении времени dt -> -dt, u_t меняет знак.

                phi[i][j-1] = (rhs + term1 + term2) / (A + B);
            }

            // ГУ для сопряженной задачи (Самое важное!)
            // (1/mu) * dphi/dz|_{z=0} = 2 * residual(t)
            // Разностная аппроксимация: (phi[1] - phi[0])/h = mu * 2 * res
            // phi[0] = phi[1] - 2 * mu * h * residual[j]

            phi[0][j-1] = phi[1][j-1] - 2.0 * mu * h * residual[j];
            phi[Nz-1][j-1] = 0;
        }
        return phi;
    }

    // --- ВОРКЕР ДЛЯ РАСЧЕТОВ ---

    private void startSolver() {
        startButton.setEnabled(false);

        solverWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                int maxIter = 500;
                double learningRate = 2.0; // Шаг градиентного спуска

                for (int iter = 0; iter < maxIter; iter++) {
                    if (isCancelled()) break;

                    // 1. Прямая задача
                    double[][] u = solveDirectProblem(sigmaCurrent);

                    // 2. Невязка и функционал
                    double[] residual = new double[Nt];
                    double errorJ = 0;
                    for (int j = 0; j < Nt; j++) {
                        double diff = u[0][j] - observedSignal[j];
                        residual[j] = diff;
                        errorJ += diff * diff * tau;
                    }

                    // 3. Сопряженная задача
                    double[][] phi = solveAdjointProblem(sigmaCurrent, residual);

                    // 4. Градиент функционала
                    // grad = Integral( u_t * phi ) dt
                    double[] grad = new double[Nz];
                    for (int i = 0; i < Nz; i++) {
                        double sum = 0;
                        for (int j = 1; j < Nt - 1; j++) {
                            // u_t центральная разность
                            double u_t = (u[i][j+1] - u[i][j-1]) / (2 * tau);
                            sum += u_t * phi[i][j] * tau;
                        }
                        grad[i] = sum;
                    }

                    // 5. Шаг спуска
                    for (int i = 0; i < Nz; i++) {
                        sigmaCurrent[i] = sigmaCurrent[i] - learningRate * grad[i];
                        // Физическое ограничение: трение не может быть отрицательным
                        if (sigmaCurrent[i] < 0) sigmaCurrent[i] = 0;
                    }

                    // Публикуем прогресс
                    publish(new IterationData(iter, errorJ, sigmaCurrent.clone()));

                    Thread.sleep(5); // Чтобы GUI успевал обновляться
                }
                return null;
            }

            @Override
            protected void process(List<IterationData> chunks) {
                IterationData last = chunks.get(chunks.size() - 1);
                mainPlot.updateSeries("Текущая Sigma", last.sigma);
                errorPlot.addPoint("Ошибка", last.error);
                statusLabel.setText(String.format("Итерация: %d, Ошибка: %.6f", last.iter, last.error));
                progressBar.setValue(Math.min(100, last.iter / 5)); // 500 итераций = 100%
                repaint();
            }

            @Override
            protected void done() {
                startButton.setEnabled(true);
                statusLabel.setText("Решение завершено.");
            }
        };

        solverWorker.execute();
    }

    // Контейнер для передачи данных из потока
    private static class IterationData {
        int iter;
        double error;
        double[] sigma;
        public IterationData(int i, double e, double[] s) { iter=i; error=e; sigma=s; }
    }

    // --- ПРОСТОЙ КЛАСС ДЛЯ ГРАФИКОВ (БЕЗ ВНЕШНИХ БИБЛИОТЕК) ---
    class PlotPanel extends JPanel {
        private java.util.Map<String, double[]> seriesMap = new java.util.HashMap<>();
        private java.util.Map<String, Color> colorMap = new java.util.HashMap<>();
        private java.util.Map<String, java.util.List<Double>> historyMap = new java.util.HashMap<>();

        private String title, xLabel, yLabel;

        public PlotPanel(String t, String x, String y) {
            this.title = t; this.xLabel = x; this.yLabel = y;
            setBackground(Color.WHITE);
        }

        public void addSeries(String name, Color c, double[] data) {
            seriesMap.put(name, data);
            colorMap.put(name, c);
        }

        public void updateSeries(String name, double[] data) {
            seriesMap.put(name, data);
        }

        public void addPoint(String name, double val) {
            historyMap.computeIfAbsent(name, k -> new java.util.ArrayList<>()).add(val);
            colorMap.putIfAbsent(name, Color.MAGENTA);
        }

        public void clearSeries(String name) {
            if(historyMap.containsKey(name)) historyMap.get(name).clear();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int pad = 40;

            // Рамка и оси
            g2.setColor(Color.BLACK);
            g2.drawLine(pad, h-pad, w-pad, h-pad); // X
            g2.drawLine(pad, pad, pad, h-pad);     // Y
            g2.drawString(title, w/2 - 50, 20);
            g2.drawString(xLabel, w/2, h - 10);

            // Рисование массивов (Sigma)
            for(String name : seriesMap.keySet()) {
                double[] data = seriesMap.get(name);
                if(data == null) continue;
                g2.setColor(colorMap.get(name));
                drawPolyLine(g2, data, w, h, pad, 6.0); // 6.0 - макс значение по Y

                // Легенда
                if(name.contains("Истинная")) g2.drawString(name, w-120, pad);
                else g2.drawString(name, w-120, pad + 15);
            }

            // Рисование истории (Ошибка)
            for(String name : historyMap.keySet()) {
                java.util.List<Double> hist = historyMap.get(name);
                if(hist.isEmpty()) continue;
                double[] arr = hist.stream().mapToDouble(d->d).toArray();
                double maxVal = hist.get(0);
                g2.setColor(colorMap.get(name));
                drawPolyLine(g2, arr, w, h, pad, maxVal);
            }
        }

        private void drawPolyLine(Graphics2D g2, double[] data, int w, int h, int pad, double maxY) {
            double stepX = (double)(w - 2*pad) / (data.length - 1);
            if (maxY == 0) maxY = 1;

            for (int i = 0; i < data.length - 1; i++) {
                int x1 = pad + (int)(i * stepX);
                int y1 = h - pad - (int)((data[i] / maxY) * (h - 2*pad));
                int x2 = pad + (int)((i+1) * stepX);
                int y2 = h - pad - (int)((data[i+1] / maxY) * (h - 2*pad));
                g2.drawLine(x1, y1, x2, y2);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LAB14V().setVisible(true));
    }
}
