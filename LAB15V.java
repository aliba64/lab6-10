import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LAB15V extends JFrame {

    // --- Поля ввода ---
    private JTextField tfEpsilon, tfMu, tfSigma, tfLambda;
    private JTextField tfL, tfT, tfN1, tfN2;
    private JTextField tfAlpha, tfBeta, tfGamma;

    // --- Компоненты вывода ---
    private JTextArea logArea;
    private JTable resultTable;
    private GraphPanel graphPanel; // Наш кастомный график
    private JSlider timeSlider;    // Ползунок для прокрутки времени
    private JLabel timeLabel;

    // Данные решения для графика
    private Solver lastSolverResult = null;

    public LAB15V() {
        super("Решение уравнения геоэлектрики + График");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        Locale.setDefault(Locale.US);
        setLayout(new BorderLayout(5, 5));

        // 1. ВЕРХНЯЯ ПАНЕЛЬ (Параметры)
        JPanel inputPanel = new JPanel(new GridLayout(3, 1, 2, 2));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));

        JPanel physPanel = createTitledPanel("Физика", new GridLayout(1, 8, 5, 0));
        physPanel.add(new JLabel("ε:")); tfEpsilon = new JTextField("1.0"); physPanel.add(tfEpsilon);
        physPanel.add(new JLabel("μ:")); tfMu = new JTextField("1.0"); physPanel.add(tfMu);
        physPanel.add(new JLabel("σ:")); tfSigma = new JTextField("0.5"); physPanel.add(tfSigma);
        physPanel.add(new JLabel("λ:")); tfLambda = new JTextField("1.0"); physPanel.add(tfLambda);
        inputPanel.add(physPanel);

        JPanel gridPanel = createTitledPanel("Сетка", new GridLayout(1, 8, 5, 0));
        gridPanel.add(new JLabel("l:")); tfL = new JTextField("1.0"); gridPanel.add(tfL);
        gridPanel.add(new JLabel("T:")); tfT = new JTextField("1.0"); gridPanel.add(tfT);
        gridPanel.add(new JLabel("N1 (z):")); tfN1 = new JTextField("40"); gridPanel.add(tfN1);
        gridPanel.add(new JLabel("N2 (t):")); tfN2 = new JTextField("200"); gridPanel.add(tfN2);
        inputPanel.add(gridPanel);

        JPanel exactPanel = createTitledPanel("Точное решение", new GridLayout(1, 6, 5, 0));
        exactPanel.add(new JLabel("α:")); tfAlpha = new JTextField("1.0"); exactPanel.add(tfAlpha);
        exactPanel.add(new JLabel("β:")); tfBeta = new JTextField("2.0"); exactPanel.add(tfBeta);
        exactPanel.add(new JLabel("γ:")); tfGamma = new JTextField("1.0"); exactPanel.add(tfGamma);
        inputPanel.add(exactPanel);

        add(inputPanel, BorderLayout.NORTH);

        // 2. ЦЕНТРАЛЬНАЯ ПАНЕЛЬ (Вкладки: Таблица / График)
        JTabbedPane tabbedPane = new JTabbedPane();

        // Вкладка 1: Таблица и Лог
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        String[] columns = {"i", "z", "j", "t", "Численное y", "Точное u", "Ошибка"};
        resultTable = new JTable(new DefaultTableModel(columns, 0));
        splitPane.setTopComponent(new JScrollPane(resultTable));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        splitPane.setBottomComponent(new JScrollPane(logArea));
        splitPane.setDividerLocation(350);

        tabbedPane.addTab("Таблица и Данные", splitPane);

        // Вкладка 2: График
        JPanel graphContainer = new JPanel(new BorderLayout());
        graphPanel = new GraphPanel();
        graphContainer.add(graphPanel, BorderLayout.CENTER);

        // Панель управления временем для графика
        JPanel sliderPanel = new JPanel(new BorderLayout());
        sliderPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        timeSlider = new JSlider(0, 100, 0);
        timeSlider.setEnabled(false);
        timeSlider.addChangeListener(e -> updateGraphTime());

        timeLabel = new JLabel("Время t = 0.00 (Слой j=0)");
        timeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        timeLabel.setFont(new Font("Arial", Font.BOLD, 14));

        sliderPanel.add(timeLabel, BorderLayout.NORTH);
        sliderPanel.add(timeSlider, BorderLayout.CENTER);
        graphContainer.add(sliderPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("График решения", graphContainer);

        add(tabbedPane, BorderLayout.CENTER);

        // 3. КНОПКА РАСЧЕТА
        JButton btnSolve = new JButton("РАССЧИТАТЬ");
        btnSolve.setFont(new Font("Arial", Font.BOLD, 16));
        btnSolve.setBackground(new Color(220, 230, 255));
        btnSolve.addActionListener(this::onSolveClick);
        add(btnSolve, BorderLayout.SOUTH);
    }

    private JPanel createTitledPanel(String title, LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setBorder(BorderFactory.createTitledBorder(title));
        return p;
    }

    private void onSolveClick(ActionEvent e) {
        try {
            double eps = Double.parseDouble(tfEpsilon.getText());
            double mu = Double.parseDouble(tfMu.getText());
            double sigma = Double.parseDouble(tfSigma.getText());
            double lambda = Double.parseDouble(tfLambda.getText());
            double l = Double.parseDouble(tfL.getText());
            double T = Double.parseDouble(tfT.getText());
            int N1 = Integer.parseInt(tfN1.getText());
            int N2 = Integer.parseInt(tfN2.getText());
            double alpha = Double.parseDouble(tfAlpha.getText());
            double beta = Double.parseDouble(tfBeta.getText());
            double gamma = Double.parseDouble(tfGamma.getText());

            lastSolverResult = new Solver(eps, mu, sigma, lambda, l, T, N1, N2, alpha, beta, gamma);
            lastSolverResult.solve();

            // Обновляем таблицу
            updateTable(lastSolverResult);

            // Настраиваем слайдер графика
            timeSlider.setMinimum(0);
            timeSlider.setMaximum(lastSolverResult.N2);
            timeSlider.setValue(lastSolverResult.N2); // Показать последний момент времени
            timeSlider.setEnabled(true);

            // Рисуем график
            updateGraphTime();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void updateTable(Solver s) {
        logArea.setText("");
        logArea.append(String.format("Расчет окончен. h=%.5f, tau=%.5f, MaxErr=%.2e\n", s.h, s.tau, s.maxError));

        DefaultTableModel model = (DefaultTableModel) resultTable.getModel();
        model.setRowCount(0);
        int stepI = Math.max(1, s.N1 / 5);
        int stepJ = Math.max(1, s.N2 / 10);

        for (int j = 0; j <= s.N2; j += stepJ) {
            for (int i = 0; i <= 2 * s.N1; i += stepI) {
                double z = (i - s.N1) * s.h;
                double t = j * s.tau;
                double exact = s.getExact(z, t);
                model.addRow(new Object[]{(i-s.N1), String.format("%.3f", z), j, String.format("%.3f", t),
                        String.format("%.5f", s.y[i][j]), String.format("%.5f", exact), String.format("%.2e", Math.abs(s.y[i][j]-exact))});
            }
        }
    }

    private void updateGraphTime() {
        if (lastSolverResult == null) return;
        int j = timeSlider.getValue();
        double t = j * lastSolverResult.tau;

        timeLabel.setText(String.format("Время t = %.3f (Слой j=%d)", t, j));

        // Подготовка данных для отрисовки
        List<Point2D.Double> numericPoints = new ArrayList<>();
        List<Point2D.Double> exactPoints = new ArrayList<>();

        for (int i = 0; i <= 2 * lastSolverResult.N1; i++) {
            double z = (i - lastSolverResult.N1) * lastSolverResult.h;
            numericPoints.add(new Point2D.Double(z, lastSolverResult.y[i][j]));
            exactPoints.add(new Point2D.Double(z, lastSolverResult.getExact(z, t)));
        }

        graphPanel.setData(numericPoints, exactPoints);
    }

    // --- КЛАСС ГРАФИКА (CUSTOM PAINTING) ---
    static class GraphPanel extends JPanel {
        private List<Point2D.Double> numericData;
        private List<Point2D.Double> exactData;

        public void setData(List<Point2D.Double> num, List<Point2D.Double> ex) {
            this.numericData = num;
            this.exactData = ex;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Фон
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, w, h);

            if (numericData == null || numericData.isEmpty()) {
                g2.setColor(Color.GRAY);
                g2.drawString("Нет данных для отображения. Нажмите 'Рассчитать'.", w/2 - 100, h/2);
                return;
            }

            // 1. Находим мин/макс для масштабирования
            double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
            double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

            for (Point2D.Double p : exactData) {
                if (p.x < minX) minX = p.x;
                if (p.x > maxX) maxX = p.x;
                if (p.y < minY) minY = p.y;
                if (p.y > maxY) maxY = p.y;
            }
            // Добавляем отступ 10%
            double rangeY = maxY - minY;
            if (Math.abs(rangeY) < 1e-9) rangeY = 1.0; // Защита от плоского графика
            minY -= rangeY * 0.1;
            maxY += rangeY * 0.1;

            // Поля
            int padLeft = 50, padRight = 30, padTop = 30, padBottom = 40;
            int graphW = w - padLeft - padRight;
            int graphH = h - padTop - padBottom;

            // 2. Рисуем оси
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawRect(padLeft, padTop, graphW, graphH);

            // Ось X (z=0) и Ось Y (u=0)
            int y0 = padTop + (int)((maxY - 0) / (maxY - minY) * graphH);
            int x0 = padLeft + (int)((0 - minX) / (maxX - minX) * graphW);

            g2.setColor(new Color(220, 220, 220));
            if (y0 >= padTop && y0 <= padTop + graphH) g2.drawLine(padLeft, y0, padLeft + graphW, y0); // Ось Y=0
            if (x0 >= padLeft && x0 <= padLeft + graphW) g2.drawLine(x0, padTop, x0, padTop + graphH); // Ось X=0

            // 3. Рисуем графики
            drawCurve(g2, exactData, Color.BLUE, 2f, minX, maxX, minY, maxY, padLeft, padTop, graphW, graphH);
            drawCurve(g2, numericData, Color.RED, 1.5f, minX, maxX, minY, maxY, padLeft, padTop, graphW, graphH); // Поверх синего

            // 4. Легенда
            g2.setColor(Color.BLUE);
            g2.drawString("— Точное решение", padLeft + 20, padTop + 20);
            g2.setColor(Color.RED);
            g2.drawString("— Численное решение", padLeft + 150, padTop + 20);

            // 5. Подписи осей (min/max)
            g2.setColor(Color.BLACK);
            g2.drawString(String.format("%.2f", maxY), 5, padTop + 10);
            g2.drawString(String.format("%.2f", minY), 5, padTop + graphH);
            g2.drawString(String.format("%.2f", minX), padLeft, h - 15);
            g2.drawString(String.format("z = %.2f", maxX), w - padRight - 40, h - 15);
        }

        private void drawCurve(Graphics2D g2, List<Point2D.Double> data, Color c, float strokeW,
                               double minX, double maxX, double minY, double maxY,
                               int xOff, int yOff, int w, int h) {
            g2.setColor(c);
            g2.setStroke(new BasicStroke(strokeW));
            Path2D path = new Path2D.Double();
            boolean first = true;

            for (Point2D.Double p : data) {
                double px = xOff + (p.x - minX) / (maxX - minX) * w;
                double py = yOff + (maxY - p.y) / (maxY - minY) * h; // Y инвертирован

                if (first) { path.moveTo(px, py); first = false; }
                else { path.lineTo(px, py); }
            }
            g2.draw(path);

            // Рисуем точки, если их немного
            if (data.size() < 50) {
                for (Point2D.Double p : data) {
                    double px = xOff + (p.x - minX) / (maxX - minX) * w;
                    double py = yOff + (maxY - p.y) / (maxY - minY) * h;
                    g2.fillOval((int)px-2, (int)py-2, 4, 4);
                }
            }
        }
    }

    // --- МАТЕМАТИЧЕСКИЙ КЛАСС (Без изменений логики) ---
    static class Solver {
        double eps, mu, sigma, lambda, l, T;
        int N1, N2;
        double alpha, beta, gamma;
        double h, tau, rho;
        double[][] y;
        double maxError = 0;

        public Solver(double eps, double mu, double sigma, double lambda, double l, double T, int n1, int n2, double alpha, double beta, double gamma) {
            this.eps = eps; this.mu = mu; this.sigma = sigma; this.lambda = lambda;
            this.l = l; this.T = T; this.N1 = n1; this.N2 = n2;
            this.alpha = alpha; this.beta = beta; this.gamma = gamma;
            this.h = (2.0 * l) / (2.0 * N1);
            this.tau = T / N2;
            this.rho = (tau * tau) / (h * h);
            y = new double[2 * N1 + 1][N2 + 1];
        }

        public double getExact(double z, double t) {
            double a2 = alpha * alpha;
            return (1.0 / Math.sqrt(alpha)) * Math.exp(-a2 * z * z) * (l * l - z * z) * (gamma * Math.sin(beta * t) - gamma * beta * t);
        }
        private double u_t(double z, double t) {
            double a2 = alpha * alpha;
            return (1.0 / Math.sqrt(alpha)) * Math.exp(-a2 * z * z) * (l * l - z * z) * (gamma * beta * Math.cos(beta * t) - gamma * beta);
        }
        private double u_tt(double z, double t) {
            double a2 = alpha * alpha;
            return (1.0 / Math.sqrt(alpha)) * Math.exp(-a2 * z * z) * (l * l - z * z) * (-gamma * beta * beta * Math.sin(beta * t));
        }
        private double u_zz(double z, double t) {
            double a2 = alpha * alpha; double a4 = a2 * a2; double z2 = z * z; double l2 = l * l;
            double bracket = -2.0 * a4 * z2 * z2 + 2.0 * a4 * l2 * z2 + 5.0 * a2 * z2 - a2 * l2 - 1.0;
            return (2.0 / Math.sqrt(alpha)) * Math.exp(-a2 * z2) * bracket * (gamma * Math.sin(beta * t) - gamma * beta * t);
        }
        private double getF0(double z, double t) {
            return eps * mu * u_tt(z, t) + mu * sigma * u_t(z, t) - u_zz(z, t) + lambda * lambda * getExact(z, t);
        }

        public void solve() {
            for (int i = 0; i <= 2 * N1; i++) { y[i][0] = 0; y[i][1] = 0; } // Начальные
            for (int j = 1; j < N2; j++) stepTridiagonal(j);
            for (int j = 0; j <= N2; j++)
                for (int i = 0; i <= 2 * N1; i++)
                    maxError = Math.max(maxError, Math.abs(y[i][j] - getExact((i-N1)*h, j*tau)));
        }

        private void stepTridiagonal(int j) {
            int size = 2 * N1 + 1;
            double[] A_prog = new double[size], B_prog = new double[size];
            double A = -rho, C = -rho;
            double B = eps * mu + 0.5 * mu * sigma * tau + 2 * rho + lambda * lambda * tau * tau;
            A_prog[0] = 0; B_prog[0] = 0;

            for (int i = 1; i < size - 1; i++) {
                double z = (i - N1) * h;
                double D = 2 * eps * mu * y[i][j] - eps * mu * y[i][j - 1] + 0.5 * mu * sigma * tau * y[i][j - 1] + tau * tau * getF0(z, j * tau);
                double denom = B + C * A_prog[i - 1];
                A_prog[i] = -A / denom;
                B_prog[i] = (D - C * B_prog[i - 1]) / denom;
            }
            y[size - 1][j + 1] = 0;
            for (int i = size - 2; i >= 0; i--) y[i][j + 1] = A_prog[i] * y[i + 1][j + 1] + B_prog[i];
            y[0][j + 1] = 0;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LAB15V().setVisible(true));
    }
}
