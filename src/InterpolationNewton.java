import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class InterpolationNewton {
    // Параметры (поля класса)
    private double a, b;
    private int k;
    private double m; // Changed to double
    private int n;

    // Данные (поля класса)
    private double[] xNodes, yNodes;
    private double[][] firstDiff;
    private double[][] secondDiff;
    private double[][] dividedDiff;
    private double[] xHalf, yTrueHalf, yInterpHalf;

    // GUI / stepping
    private int currentStep = 1;
    private Timer playTimer;
    private int displaySteps = 20;

    public InterpolationNewton(double a, double b, int k, double m, int n) {
        this.a = a;
        this.b = b;
        this.k = k;
        this.m = m;
        this.n = Math.max(3, n);
        prepareData();
    }

    private double safeF(double x) {
        try {
            double v = f(x);
            if (!Double.isFinite(v)) return Double.NaN;
            return v;
        } catch (Exception ex) {
            return Double.NaN;
        }
    }

    private double f(double x) {
        double arg = Math.PI * Math.pow(x, m);
        double sinVal = Math.sin(arg);
        return Math.pow(sinVal, k);
    }

    private void prepareData() {
        double h = (b - a) / (n - 1);
        xNodes = new double[n];
        yNodes = new double[n];

        // 1. y в узлах
        for (int i = 0; i < n; i++) {
            xNodes[i] = a + i * h;
            yNodes[i] = safeF(xNodes[i]);
        }

        // 2. первые разности (в треугольнике)
        firstDiff = new double[n][n];
        for (int i = 0; i < n; i++) firstDiff[i][0] = yNodes[i];
        for (int j = 1; j < n; j++) {
            for (int i = 0; i < n - j; i++) {
                double top = firstDiff[i + 1][j - 1];
                double bot = firstDiff[i][j - 1];
                if (Double.isNaN(top) || Double.isNaN(bot)) firstDiff[i][j] = Double.NaN;
                else firstDiff[i][j] = top - bot;
            }
        }

        // 3. вторые разности (до 2 порядка для удобства вывода)
        secondDiff = new double[n][n];
        for (int i = 0; i < n; i++) secondDiff[i][0] = yNodes[i];
        for (int j = 1; j <= 2 && j < n; j++) {
            for (int i = 0; i < n - j; i++) {
                double top = secondDiff[i + 1][j - 1];
                double bot = secondDiff[i][j - 1];
                if (Double.isNaN(top) || Double.isNaN(bot)) secondDiff[i][j] = Double.NaN;
                else secondDiff[i][j] = top - bot;
            }
        }

        int numHalf = n - 1;
        xHalf = new double[numHalf];
        yTrueHalf = new double[numHalf];
        yInterpHalf = new double[numHalf];
        for (int i = 0; i < numHalf; i++) {
            xHalf[i] = a + (i + 0.5) * h;
            yTrueHalf[i] = safeF(xHalf[i]);
        }

        // 4. разделённые разности и локальная интерполяция в полуточках
        dividedDiff = computeDividedDifferences(xNodes, yNodes);
        for (int i = 0; i < numHalf; i++) {
            int start = Math.max(0, i - 1);
            int end = Math.min(n, i + 2);
            int localN = end - start;
            double[] localX = new double[localN];
            double[] localY = new double[localN];
            for (int j = 0; j < localN; j++) {
                localX[j] = xNodes[start + j];
                localY[j] = yNodes[start + j];
            }
            double[][] localDiv = computeDividedDifferences(localX, localY);
            yInterpHalf[i] = localNewtonInterpolate(xHalf[i], localX, localY, localDiv);
        }

        currentStep = 1;
    }

    private double localNewtonInterpolate(double x, double[] xNodesLoc, double[] yNodesLoc, double[][] divDiff) {
        int deg = Math.min(2, xNodesLoc.length - 1);
        if (deg < 1) return yNodesLoc[0];
        double result = yNodesLoc[0];
        double term = 1.0;
        for (int j = 1; j <= deg; j++) {
            term *= (x - xNodesLoc[j - 1]);
            double coeff = divDiff[0][j];
            if (Double.isNaN(coeff)) return Double.NaN;
            result += coeff * term;
        }
        return result;
    }

    private double[][] computeDividedDifferences(double[] x, double[] y) {
        int N = x.length;
        double[][] div = new double[N][N];
        for (int i = 0; i < N; i++) div[i][0] = y[i];
        for (int j = 1; j < N; j++) {
            for (int i = 0; i < N - j; i++) {
                double top = div[i + 1][j - 1];
                double bot = div[i][j - 1];
                if (Double.isNaN(top) || Double.isNaN(bot) || x[i + j] == x[i]) div[i][j] = Double.NaN;
                else div[i][j] = (top - bot) / (x[i + j] - x[i]);
            }
        }
        return div;
    }

    public void printTables() {
        double h = (b - a) / (n - 1);

        System.out.println("\n1. Таблица значений yj = f(xj) в узлах xj = " + a + " + j*h, h=" + String.format("%.4f", h));
        System.out.println("j\t xj\t\t yj");
        for (int j = 0; j < n; j++) {
            System.out.printf("%d\t %.4f\t %.6f%n", j, xNodes[j], yNodes[j]);
        }

        System.out.println("\n2. Таблица первых разностей Δyj");
        System.out.println("j\\k\t f[x_k]\t Δf[x_k]\t Δ²f[x_k] ...");
        for (int i = 0; i < n; i++) {
            System.out.print(i + "\t");
            for (int j = 0; j < Math.min(3, n - i); j++) {
                System.out.printf(" %.6f", firstDiff[i][j]);
                if (j < 2) System.out.print("\t");
            }
            System.out.println();
        }

        System.out.println("\n3. Таблица вторых разностей (до 2-го порядка)");
        for (int i = 0; i < n; i++) {
            System.out.print(i + "\t");
            for (int j = 0; j < Math.min(3, n - i); j++) {
                System.out.printf(" %.6f", secondDiff[i][j]);
                if (j < 2) System.out.print("\t");
            }
            System.out.println();
        }
    }

    public void printInterpolationAndErrors() {
        System.out.println("\n4-5. Интерполяция P(x) и погрешности в полуцелых точках x_{j+0.5}");
        System.out.println("Idx\t x\t\t y_true\t P(x)\t\t Error");
        double maxErr = 0;
        double sumSqErr = 0;
        for (int i = 0; i < xHalf.length; i++) {
            double err = Math.abs(yTrueHalf[i] - yInterpHalf[i]);
            if (Double.isNaN(err)) continue;
            maxErr = Math.max(maxErr, err);
            sumSqErr += err * err;
            System.out.printf("%d.5\t %.4f\t %.6f\t %.6f\t %.2e%n", i, xHalf[i], yTrueHalf[i], yInterpHalf[i], err);
        }

        double meanSqErr = sumSqErr / xHalf.length;
        double rmsErr = Math.sqrt(meanSqErr);

        System.out.printf("\nМаксимальная погрешность ε_max = %.6e%n", maxErr);
        System.out.printf("Средний квадрат погрешности = %.6e%n", meanSqErr);
        System.out.printf("Среднеквадратичная погрешность ε_rms = %.6e%n", rmsErr);
    }

    public void studyErrorsVsN(double a, double b, int k, double m, int[] ns) {
        System.out.println("\n6. Исследование погрешностей от n:");
        System.out.println("n\t ε_max\t\t ε_rms");
        for (int nn : ns) {
            InterpolationNewton app = new InterpolationNewton(a, b, k, m, nn);
            double maxErr = 0;
            double sumSqErr = 0;
            for (int i = 0; i < app.xHalf.length; i++) {
                double err = Math.abs(app.yTrueHalf[i] - app.yInterpHalf[i]);
                maxErr = Math.max(maxErr, err);
                sumSqErr += err * err;
            }
            double rmsErr = Math.sqrt(sumSqErr / app.xHalf.length);
            System.out.printf("%d\t %.6e\t %.6e%n", nn, maxErr, rmsErr);
        }
    }

    public void showGUI() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Ньютоновская интерполяция - визуализатор");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1100, 700);

            DrawingPanel panel = new DrawingPanel();
            frame.add(panel, BorderLayout.CENTER);

            JPanel controls = new JPanel();
            JButton prevBtn = new JButton("⟵ Назад");
            JButton nextBtn = new JButton("Вперёд ⟶");
            JButton playBtn = new JButton("Пуск");
            JButton resetBtn = new JButton("Сброс");
            JButton recomputeBtn = new JButton("Пересчитать");
            JButton tablesBtn = new JButton("Показать таблицы");

            SpinnerNumberModel spinnerModel = new SpinnerNumberModel(displaySteps, 20, 100, 1);
            JSpinner stepsSpinner = new JSpinner(spinnerModel);
            JLabel stepsLabel = new JLabel("Число шагов:");

            controls.add(prevBtn);
            controls.add(playBtn);
            controls.add(nextBtn);
            controls.add(resetBtn);
            controls.add(recomputeBtn);
            controls.add(tablesBtn);
            controls.add(Box.createHorizontalStrut(12));
            controls.add(stepsLabel);
            controls.add(stepsSpinner);

            frame.add(controls, BorderLayout.SOUTH);

            JPanel infoPanel = new JPanel();
            infoPanel.setPreferredSize(new Dimension(300, 0));
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            JLabel stepInfo = new JLabel("Шаг: 0");
            JLabel valueInfo = new JLabel("x: -   y_истинн: -   y_интерп: -   ошибка: -");
            JLabel scaleInfo = new JLabel("Масштаб (глобальный): ymin=-, ymax=-");
            stepInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
            valueInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
            scaleInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
            infoPanel.add(Box.createVerticalStrut(10));
            infoPanel.add(stepInfo);
            infoPanel.add(Box.createVerticalStrut(10));
            infoPanel.add(valueInfo);
            infoPanel.add(Box.createVerticalStrut(10));
            infoPanel.add(scaleInfo);
            frame.add(infoPanel, BorderLayout.EAST);

            prevBtn.addActionListener(e -> {
                if (currentStep > 1) currentStep--;
                panel.repaint();
                updateInfo(stepInfo, valueInfo, scaleInfo);
            });

            nextBtn.addActionListener(e -> {
                if (currentStep < displaySteps) currentStep++;
                panel.repaint();
                updateInfo(stepInfo, valueInfo, scaleInfo);
            });

            playTimer = new Timer(500, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (currentStep < displaySteps) {
                        currentStep++;
                        panel.repaint();
                        updateInfo(stepInfo, valueInfo, scaleInfo);
                    } else {
                        playTimer.stop();
                        playBtn.setText("Пуск");
                    }
                }
            });

            playBtn.addActionListener(e -> {
                if (playTimer.isRunning()) {
                    playTimer.stop();
                    playBtn.setText("Пуск");
                } else {
                    if (currentStep >= displaySteps) currentStep = 1;
                    playTimer.start();
                    playBtn.setText("Пауза");
                }
            });

            resetBtn.addActionListener(e -> {
                currentStep = 1;
                playTimer.stop();
                playBtn.setText("Пуск");
                panel.repaint();
                updateInfo(stepInfo, valueInfo, scaleInfo);
            });

            recomputeBtn.addActionListener(e -> {
                prepareData();
                panel.repaint();
                updateInfo(stepInfo, valueInfo, scaleInfo);
            });

            tablesBtn.addActionListener(e -> {
                // открыть окно с таблицами
                SwingUtilities.invokeLater(() -> {
                    TablesFrame tf = new TablesFrame();
                    tf.setVisible(true);
                });
            });

            stepsSpinner.addChangeListener(e -> {
                displaySteps = (Integer) stepsSpinner.getValue();
                if (currentStep > displaySteps) currentStep = displaySteps;
                panel.repaint();
                updateInfo(stepInfo, valueInfo, scaleInfo);
            });

            updateInfo(stepInfo, valueInfo, scaleInfo);

            frame.setVisible(true);
        });
    }

    private void updateInfo(JLabel stepInfo, JLabel valueInfo, JLabel scaleInfo) {
        int idx = mapStepToIndex(currentStep);
        stepInfo.setText(String.format("Шаг: %d / %d   (mapped idx: %d)", currentStep, displaySteps, idx));
        if (idx >= 0 && idx < xHalf.length) {
            double x = xHalf[idx];
            double yT = yTrueHalf[idx];
            double yI = yInterpHalf[idx];
            double err = Double.NaN;
            if (!Double.isNaN(yT) && !Double.isNaN(yI)) err = Math.abs(yT - yI);
            valueInfo.setText(String.format("x=%.6f   y_ист= %s   y_инт= %s   err=%s",
                    x,
                    Double.isNaN(yT) ? "NaN" : String.format("%.6e", yT),
                    Double.isNaN(yI) ? "NaN" : String.format("%.6e", yI),
                    Double.isNaN(err) ? "-" : String.format("%.2e", err)));
        } else {
            valueInfo.setText("x: -   y_ист: -   y_инт: -   err: -");
        }

        double ymin = Double.POSITIVE_INFINITY, ymax = Double.NEGATIVE_INFINITY;
        for (double v : yNodes) if (!Double.isNaN(v)) { ymin = Math.min(ymin, v); ymax = Math.max(ymax, v); }
        for (double v : yTrueHalf) if (!Double.isNaN(v)) { ymin = Math.min(ymin, v); ymax = Math.max(ymax, v); }
        for (double v : yInterpHalf) if (!Double.isNaN(v)) { ymin = Math.min(ymin, v); ymax = Math.max(ymax, v); }
        if (!Double.isFinite(ymin) || !Double.isFinite(ymax)) {
            scaleInfo.setText("Масштаб (глобальный): ymin=-, ymax=-");
        } else {
            scaleInfo.setText(String.format("Масштаб (глобальный): ymin=%.6e, ymax=%.6e", ymin, ymax));
        }
    }

    private int mapStepToIndex(int step) {
        if (xHalf == null || xHalf.length == 0) return -1;
        if (displaySteps <= 1) return 0;
        double t = (double) (step - 1) / (displaySteps - 1);
        int idx = (int) Math.round(t * (xHalf.length - 1));
        idx = Math.max(0, Math.min(xHalf.length - 1, idx));
        return idx;
    }

    private class DrawingPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            int W = getWidth(), H = getHeight();

            g.setColor(Color.WHITE);
            g.fillRect(0,0,W,H);

            g.setColor(Color.BLACK);
            g.drawLine(50, H - 50, W - 20, H - 50);
            g.drawLine(50, H - 50, 50, 20);

            double h = (b - a) / (n - 1);

            double ymin = Double.POSITIVE_INFINITY, ymax = Double.NEGATIVE_INFINITY;
            for (double v : yNodes) if (!Double.isNaN(v)) { ymin = Math.min(ymin, v); ymax = Math.max(ymax, v); }
            for (double v : yTrueHalf) if (!Double.isNaN(v)) { ymin = Math.min(ymin, v); ymax = Math.max(ymax, v); }
            for (double v : yInterpHalf) if (!Double.isNaN(v)) { ymin = Math.min(ymin, v); ymax = Math.max(ymax, v); }
            if (!Double.isFinite(ymin) || !Double.isFinite(ymax)) { ymin = -1; ymax = 1; }
            if (Math.abs(ymax - ymin) < 1e-9) { ymax = ymin + 1; }

            int mappedIndex = mapStepToIndex(currentStep);

            // 1) Истинная функция (синяя) по плотной сетке
            int samples = Math.max(300, W);
            int prevX = -1, prevY = -1;
            g.setStroke(new BasicStroke(2f));
            for (int s = 0; s <= samples; s++) {
                double tx = a + (b - a) * s / (double) samples;
                double ty = safeF(tx);
                if (Double.isNaN(ty)) { prevX = -1; prevY = -1; continue; }
                int px = 50 + (int) ((tx - a) / (b - a) * (W - 70));
                int py = 20 + (int) ((ymax - ty) / (ymax - ymin) * (H - 80));
                if (prevX != -1) {
                    g.setColor(Color.BLUE);
                    g.drawLine(prevX, prevY, px, py);
                }
                prevX = px; prevY = py;
            }

            // 2) Интерполированная (красная) — нарисовать непрерывно от x=a до границы, соответствующей mappedIndex.
            prevX = -1; prevY = -1;
            // Максимальная x для отрисовки интерполяции: правый конец сегмента mappedIndex
            double maxInterpolX = a + (mappedIndex + 1) * h;
            // гарантируем не выход за границы
            maxInterpolX = Math.min(maxInterpolX, b);

            g.setStroke(new BasicStroke(2f));
            for (int s = 0; s <= samples; s++) {
                double tx = a + (b - a) * s / (double) samples;
                if (tx > maxInterpolX) break; // рисуем только до maxInterpolX включительно
                // выбираем сегмент
                int seg = (int) Math.floor((tx - a) / h);
                seg = Math.max(0, Math.min(n - 2, seg));
                int start = Math.max(0, seg - 1);
                int end = Math.min(n, seg + 2);
                int localN = end - start;
                double[] localX = new double[localN];
                double[] localY = new double[localN];
                for (int j = 0; j < localN; j++) {
                    localX[j] = xNodes[start + j];
                    localY[j] = yNodes[start + j];
                }
                double[][] localDiv = computeDividedDifferences(localX, localY);
                double ty = localNewtonInterpolate(tx, localX, localY, localDiv);
                if (Double.isNaN(ty)) { prevX = -1; prevY = -1; continue; }
                int px = 50 + (int) ((tx - a) / (b - a) * (W - 70));
                int py = 20 + (int) ((ymax - ty) / (ymax - ymin) * (H - 80));
                if (prevX != -1) {
                    g.setColor(Color.RED);
                    g.drawLine(prevX, prevY, px, py);
                }
                prevX = px; prevY = py;
            }

            // 3) Узлы и полуточки
            g.setStroke(new BasicStroke(1f));
            g.setColor(Color.LIGHT_GRAY);
            for (int i = 0; i < n; i++) {
                int x = 50 + (int) ((xNodes[i] - a) / (b - a) * (W - 70));
                double val = yNodes[i];
                int y = 20 + (int) ((ymax - (Double.isNaN(val) ? 0 : val)) / (ymax - ymin) * (H - 80));
                g.fillOval(x - 3, y - 3, 6, 6);
            }

            for (int i = 0; i <= mappedIndex && i < xHalf.length; i++) {
                int x = 50 + (int) ((xHalf[i] - a) / (b - a) * (W - 70));
                double val = yTrueHalf[i];
                int y = 20 + (int) ((ymax - (Double.isNaN(val) ? 0 : val)) / (ymax - ymin) * (H - 80));
                if (Double.isNaN(val)) {
                    g.setColor(Color.GRAY);
                    g.fillOval(x - 2, y - 2, 4, 4);
                } else {
                    g.setColor(Color.GREEN.darker());
                    g.fillOval(x - 2, y - 2, 4, 4);
                }
            }

            for (int i = 0; i <= mappedIndex && i < xHalf.length; i++) {
                int x = 50 + (int) ((xHalf[i] - a) / (b - a) * (W - 70));
                double val = yInterpHalf[i];
                int y = 20 + (int) ((ymax - (Double.isNaN(val) ? 0 : val)) / (ymax - ymin) * (H - 80));
                if (Double.isNaN(val)) {
                    g.setColor(Color.DARK_GRAY);
                    g.fillOval(x - 2, y - 2, 4, 4);
                } else {
                    g.setColor(Color.RED.darker());
                    g.fillOval(x - 2, y - 2, 4, 4);
                }
            }

            // Легенда (на русском)
            int lx = W - 450, ly = 30;
            g.setColor(Color.WHITE);
            g.fillRect(lx - 6, ly - 16, 300, 120);
            g.setColor(Color.white);
            g.drawRect(lx - 6, ly - 16, 300, 120);

            g.setColor(Color.BLUE);
            g.fillRect(lx, ly, 18, 8);
            g.setColor(Color.BLACK);
            g.drawString("Истинная функция f(x) (синяя)", lx + 26, ly + 8);

            g.setColor(Color.RED);
            g.fillRect(lx, ly + 20, 18, 8);
            g.setColor(Color.BLACK);
            g.drawString("Интерполянт (лок. Ньютон, показывается до текущего шага) (красная)", lx + 26, ly + 28);

            g.setColor(Color.GREEN.darker());
            g.fillRect(lx, ly + 40, 18, 8);
            g.setColor(Color.BLACK);
            g.drawString("Истинные полуточки, показанные до текущего шага (зелёные)", lx + 26, ly + 48);

            g.setColor(Color.RED.darker());
            g.fillRect(lx, ly + 60, 18, 8);
            g.setColor(Color.BLACK);
            g.drawString("Интерполированные полуточки, показанные до текущего шага (красные)", lx + 26, ly + 68);

            g.setColor(Color.BLACK);
            g.drawString("Анимационный шаг: " + currentStep + " / " + displaySteps + "   mapped idx: " + mappedIndex, 60, 30);
        }
    }

    // Окно с таблицами
    private class TablesFrame extends JFrame {
        public TablesFrame() {
            super("Таблицы значений и разностей");
            setSize(900, 600);
            setLocationRelativeTo(null);
            setLayout(new BorderLayout());

            JTabbedPane tabs = new JTabbedPane();

            // 1) Узлы (xj, yj)
            String[] nodesCols = {"j", "x_j", "y_j"};
            DefaultTableModel nodesModel = new DefaultTableModel(nodesCols, 0);
            for (int i = 0; i < xNodes.length; i++) {
                nodesModel.addRow(new Object[] {i, fmt(xNodes[i]), fmtNaN(yNodes[i])});
            }
            JTable nodesTable = new JTable(nodesModel);
            tabs.addTab("Узлы (x_j, y_j)", new JScrollPane(nodesTable));

            // 2) Первые разности (несколько столбцов)
            int maxCols = Math.min(6, n); // ограничим число столбцов для удобства
            String[] diffCols = new String[maxCols + 1];
            diffCols[0] = "i";
            for (int j = 0; j < maxCols; j++) diffCols[j + 1] = "Δ^" + j;
            DefaultTableModel diffModel = new DefaultTableModel(diffCols, 0);
            for (int i = 0; i < n; i++) {
                Object[] row = new Object[maxCols + 1];
                row[0] = i;
                for (int j = 0; j < maxCols; j++) {
                    if (j < n && i + j < n) row[j + 1] = fmtNaN(firstDiff[i][j]);
                    else row[j + 1] = "";
                }
                diffModel.addRow(row);
            }
            JTable diffTable = new JTable(diffModel);
            tabs.addTab("Первые разности Δ^k", new JScrollPane(diffTable));

            // 3) Вторые разности (до 2-го порядка)
            String[] secCols = {"i", "Δ^0", "Δ^1", "Δ^2"};
            DefaultTableModel secModel = new DefaultTableModel(secCols, 0);
            for (int i = 0; i < n; i++) {
                Object[] row = new Object[4];
                row[0] = i;
                row[1] = fmtNaN(secondDiff[i][0]);
                row[2] = (n - i > 1) ? fmtNaN(secondDiff[i][1]) : "";
                row[3] = (n - i > 2) ? fmtNaN(secondDiff[i][2]) : "";
                secModel.addRow(row);
            }
            JTable secTable = new JTable(secModel);
            tabs.addTab("Вторые разности", new JScrollPane(secTable));

            // 4) Разделённые разности (вертикально: i и несколько коэффициентов)
            int divColsCount = Math.min(6, n);
            String[] divCols = new String[divColsCount + 1];
            divCols[0] = "i";
            for (int j = 0; j < divColsCount; j++) divCols[j + 1] = "f[.,..]^" + j;
            DefaultTableModel divModel = new DefaultTableModel(divCols, 0);
            for (int i = 0; i < n; i++) {
                Object[] row = new Object[divColsCount + 1];
                row[0] = i;
                for (int j = 0; j < divColsCount; j++) {
                    if (i + j < n) row[j + 1] = fmtNaN(dividedDiff[i][j]);
                    else row[j + 1] = "";
                }
                divModel.addRow(row);
            }
            JTable divTable = new JTable(divModel);
            tabs.addTab("Разделённые разности", new JScrollPane(divTable));

            add(tabs, BorderLayout.CENTER);

            // Закрыть
            JButton close = new JButton("Закрыть");
            close.addActionListener(e -> dispose());
            JPanel south = new JPanel();
            south.add(close);
            add(south, BorderLayout.SOUTH);
        }
    }

    private String fmt(double v) {
        return String.format("%.6e", v);
    }

    private Object fmtNaN(double v) {
        if (Double.isNaN(v)) return "NaN";
        return String.format("%.6e", v);
    }

    // Точка входа
    public static void main(String[] args) {
        double a = 0.0;
        double b = 1.0;
        int k = 1;
        double mm = 1.0 / 4.0;
        int n = 21;

        try {
            if (args.length >= 5) {
                a = Double.parseDouble(args[0]);
                b = Double.parseDouble(args[1]);
                k = Integer.parseInt(args[2]);
                mm = Double.parseDouble(args[3]);
                n = Integer.parseInt(args[4]);
            }
        } catch (Exception e) {
            System.out.println("Используем параметры по умолчанию: a=0, b=1, k=1, m=0.25, n=21");
        }

        InterpolationNewton app = new InterpolationNewton(a, b, k, mm, n);
        app.printTables();
        app.printInterpolationAndErrors();

        int[] ns = {20, 40, 60, 80, 100};
        app.studyErrorsVsN(a, b, k, mm, ns);

        app.showGUI();
    }
}
