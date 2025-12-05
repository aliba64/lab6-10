import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class LAB12V extends JFrame {

    private JTextField nField, mField, epsilonField, lxField, lyField;
    private JComboBox<String> testComboBox; // Убрал выбор метода, т.к. считаем оба
    private JButton solveButton, clearButton;
    private JTextArea resultArea;
    private JTable solutionTable;
    private DefaultTableModel tableModel;
    private JLabel omegaLabel;
    private JTextField omegaField;
    private DecimalFormat df = new DecimalFormat("#0.0000");

    // Данные решения
    private double[][] currentSolution;

    // Панели визуализации
    private JTabbedPane tabbedPane;
    private HeatMapPanel heatMapPanel;
    private Surface3DPanel surface3DPanel;

    public LAB12V() {
        setTitle("LAB12V: Уравнение Пуассона (Сравнение методов + 3D)");
        setSize(1300, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- ЛЕВАЯ ПАНЕЛЬ (ВВОД) ---
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(createParameterPanel(), BorderLayout.NORTH);
        leftPanel.add(createInfoPanel(), BorderLayout.CENTER);

        // --- ПРАВАЯ ПАНЕЛЬ (ВКЛАДКИ) ---
        tabbedPane = new JTabbedPane();

        // 1. Таблица
        JPanel tablePanel = createResultPanel();
        tabbedPane.addTab("Таблица", tablePanel);

        // 2. 2D Карта
        heatMapPanel = new HeatMapPanel();
        tabbedPane.addTab("2D Тепловая карта", heatMapPanel);

        // 3. 3D Поверхность
        surface3DPanel = new Surface3DPanel();
        tabbedPane.addTab("3D Поверхность (Вращайте мышью)", surface3DPanel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, tabbedPane);
        splitPane.setDividerLocation(420);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
        setVisible(true);
    }

    // ---------------------------------------------------------
    //   ИНТЕРФЕЙС И ЛОГИКА
    // ---------------------------------------------------------

    private JPanel createParameterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Параметры"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        gbc.gridx = 0; gbc.gridy = row; panel.add(new JLabel("Тест:"), gbc);
        gbc.gridx = 1;
        String[] tests = {
                "Тест 1: u=sin(πx)sin(πy)",
                "Тест 2: u=x(1-x)y(1-y)",
                "Тест 3: Источник в центре",
                "Тест 4: u=x² на границе"
        };
        testComboBox = new JComboBox<>(tests);
        panel.add(testComboBox, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row; panel.add(new JLabel("Размеры Lx / Ly:"), gbc);
        gbc.gridx = 1;
        JPanel sizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        lxField = new JTextField("1.0", 5);
        lyField = new JTextField("1.0", 5);
        sizePanel.add(lxField); sizePanel.add(new JLabel(" / ")); sizePanel.add(lyField);
        panel.add(sizePanel, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row; panel.add(new JLabel("Сетка N x M:"), gbc);
        gbc.gridx = 1;
        JPanel gridPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        nField = new JTextField("30", 5);
        mField = new JTextField("30", 5);
        gridPanel.add(nField); gridPanel.add(new JLabel(" x ")); gridPanel.add(mField);
        panel.add(gridPanel, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row; panel.add(new JLabel("Точность ε:"), gbc);
        gbc.gridx = 1; epsilonField = new JTextField("0.0001", 10); panel.add(epsilonField, gbc);
        row++;

        // Поле для Omega (информационное)
        gbc.gridx = 0; gbc.gridy = row; panel.add(new JLabel("Параметр ω (SOR):"), gbc);
        gbc.gridx = 1; omegaField = new JTextField("авто", 10);
        omegaField.setEnabled(false); panel.add(omegaField, gbc);

        return panel;
    }

    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Лог (Сравнение)"));
        resultArea = new JTextArea(10, 30);
        resultArea.setEditable(false);
        panel.add(new JScrollPane(resultArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createResultPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        tableModel = new DefaultTableModel();
        solutionTable = new JTable(tableModel);
        solutionTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        panel.add(new JScrollPane(solutionTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        solveButton = new JButton("▶ СРАВНИТЬ МЕТОДЫ И ПОСТРОИТЬ");
        solveButton.setBackground(new Color(50, 150, 50));
        solveButton.setForeground(Color.WHITE);
        solveButton.setFont(new Font("Arial", Font.BOLD, 14));
        solveButton.addActionListener(e -> solveAndCompare());

        clearButton = new JButton("Очистить");
        clearButton.addActionListener(e -> clear());

        panel.add(solveButton); panel.add(clearButton);
        return panel;
    }

    // ---------------------------------------------------------
    //   МАТЕМАТИКА, РАСЧЕТ И СРАВНЕНИЕ
    // ---------------------------------------------------------

    private void solveAndCompare() {
        try {
            double lx = Double.parseDouble(lxField.getText());
            double ly = Double.parseDouble(lyField.getText());
            int N = Integer.parseInt(nField.getText());
            int M = Integer.parseInt(mField.getText());
            double eps = Double.parseDouble(epsilonField.getText());
            int test = testComboBox.getSelectedIndex();

            double hx = lx/N;
            double hy = ly/M;

            resultArea.setText("--- Начало сравнения ---\n");

            // 1. ЗАПУСК МЕТОДА ЗЕЙДЕЛЯ
            long start1 = System.currentTimeMillis();
            Object[] resSeidel = solveGaussSeidel(N, M, hx, hy, eps, lx, ly, test);
            long timeSeidel = System.currentTimeMillis() - start1;

            resultArea.append(String.format("Зейдель: %d итераций (%d мс)\n", resSeidel[1], timeSeidel));

            // 2. ЗАПУСК МЕТОДА SOR
            // Считаем оптимальную омегу
            double omega = (N==M) ? 2.0/(1.0+Math.sin(Math.PI/N)) : 1.8;
            omegaField.setText(String.format("%.4f", omega));

            long start2 = System.currentTimeMillis();
            Object[] resSOR = solveSOR(N, M, hx, hy, eps, omega, lx, ly, test);
            long timeSOR = System.currentTimeMillis() - start2;

            resultArea.append(String.format("SOR (ω=%.2f): %d итераций (%d мс)\n", omega, resSOR[1], timeSOR));

            // 3. СРАВНЕНИЕ
            double speedup = (double)(int)resSeidel[1] / (int)resSOR[1];
            resultArea.append(String.format("\nИТОГ: SOR быстрее в %.1f раз!\n", speedup));

            // Используем результат SOR для графиков (он такой же, но быстрее)
            currentSolution = (double[][]) resSOR[0];

            displayTable(currentSolution, N, M, hx, hy);

            // Обновление графиков
            heatMapPanel.setData(currentSolution);
            surface3DPanel.setData(currentSolution, N, M, hx, hy);

            // Переключаем на 3D и показываем всплывающее окно
            tabbedPane.setSelectedIndex(2);

            JOptionPane.showMessageDialog(this,
                    String.format("Сравнение завершено!\n\nМетод Зейделя: %d итераций\nМетод SOR: %d итераций\n\nУскорение: %.1f раз",
                            resSeidel[1], resSOR[1], speedup),
                    "Результаты сравнения", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + ex.getMessage());
        }
    }

    // Методы решения
    private double getBound(double x, double y, int t) { return t==3 ? x*x : 0; }
    private double getRHS(double x, double y, int t) {
        if (t==0) return -2*Math.PI*Math.PI*Math.sin(Math.PI*x)*Math.sin(Math.PI*y);
        if (t==1) return 2*(x*(1-x)+y*(1-y));
        if (t==2) return (Math.abs(x-0.5)<0.1 && Math.abs(y-0.5)<0.1) ? 50.0 : 0.0; // Источник в центре
        return -4.0;
    }

    private Object[] solveGaussSeidel(int N, int M, double hx, double hy, double eps, double lx, double ly, int t) {
        double[][] u = new double[M+1][N+1];
        initBounds(u, N, M, hx, hy, t);
        int iter=0; double maxDiff;
        double denom = 2*(1/(hx*hx) + 1/(hy*hy));
        do {
            maxDiff=0;
            for(int i=1; i<M; i++) for(int j=1; j<N; j++) {
                double old = u[i][j];
                u[i][j] = ((u[i][j+1]+u[i][j-1])/(hx*hx) + (u[i+1][j]+u[i-1][j])/(hy*hy) - getRHS(j*hx, i*hy, t))/denom;
                maxDiff = Math.max(maxDiff, Math.abs(u[i][j]-old));
            }
            iter++;
        } while(maxDiff > eps && iter < 30000);
        return new Object[]{u, iter, maxDiff};
    }

    private Object[] solveSOR(int N, int M, double hx, double hy, double eps, double w, double lx, double ly, int t) {
        double[][] u = new double[M+1][N+1];
        initBounds(u, N, M, hx, hy, t);
        int iter=0; double maxDiff;
        double denom = 2*(1/(hx*hx) + 1/(hy*hy));
        do {
            maxDiff=0;
            for(int i=1; i<M; i++) for(int j=1; j<N; j++) {
                double old = u[i][j];
                double gs = ((u[i][j+1]+u[i][j-1])/(hx*hx) + (u[i+1][j]+u[i-1][j])/(hy*hy) - getRHS(j*hx, i*hy, t))/denom;
                u[i][j] = (1-w)*old + w*gs;
                maxDiff = Math.max(maxDiff, Math.abs(u[i][j]-old));
            }
            iter++;
        } while(maxDiff > eps && iter < 30000);
        return new Object[]{u, iter, maxDiff};
    }

    private void initBounds(double[][] u, int N, int M, double hx, double hy, int t) {
        for(int i=0; i<=M; i++) for(int j=0; j<=N; j++)
            if(i==0||i==M||j==0||j==N) u[i][j] = getBound(j*hx, i*hy, t);
    }

    private void displayTable(double[][] u, int N, int M, double hx, double hy) {
        tableModel.setRowCount(0); tableModel.setColumnCount(0);
        tableModel.addColumn("Y \\ X");
        for(int j=0; j<=N; j++) tableModel.addColumn(String.format("%.2f", j*hx));
        for(int i=M; i>=0; i--) {
            Object[] row = new Object[N+2];
            row[0] = String.format("%.2f", i*hy);
            for(int j=0; j<=N; j++) row[j+1] = df.format(u[i][j]);
            tableModel.addRow(row);
        }
    }

    private void clear() {
        resultArea.setText(""); tableModel.setRowCount(0);
        heatMapPanel.clear(); surface3DPanel.clear();
    }

    // ---------------------------------------------------------
    //   2D HEATMAP PANEL
    // ---------------------------------------------------------
    class HeatMapPanel extends JPanel {
        double[][] data; double min, max;
        public void setData(double[][] d) {
            data = d; min=Double.MAX_VALUE; max=-Double.MAX_VALUE;
            for(double[] r:d) for(double v:r) { if(v<min)min=v; if(v>max)max=v; }
            repaint();
        }
        public void clear() { data=null; repaint(); }
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            if(data==null) return;
            int M = data.length-1, N = data[0].length-1;
            int cw = getWidth()/(N+1), ch = getHeight()/(M+1);
            for(int i=0; i<=M; i++) for(int j=0; j<=N; j++) {
                float val = (float)((max==min)?0.5:(data[i][j]-min)/(max-min));
                g.setColor(Color.getHSBColor(0.66f * (1-val), 1f, 1f)); // Blue to Red
                g.fillRect(j*cw, getHeight()-(i+1)*ch, cw, ch);
            }
        }
    }

    // ---------------------------------------------------------
    //   3D SURFACE PANEL (CUSTOM ENGINE)
    // ---------------------------------------------------------
    class Surface3DPanel extends JPanel {
        private double[][] u;
        private int N, M;
        private double minZ, maxZ;

        // Углы поворота камеры
        private double angleX = -0.8; // Наклон
        private double angleZ = 0.8;  // Вращение
        private int lastMouseX, lastMouseY;

        public Surface3DPanel() {
            setBackground(Color.WHITE);
            // Обработчики мыши для вращения
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { lastMouseX=e.getX(); lastMouseY=e.getY(); }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    angleZ += (e.getX() - lastMouseX) * 0.01;
                    angleX += (e.getY() - lastMouseY) * 0.01;
                    lastMouseX = e.getX(); lastMouseY = e.getY();
                    repaint();
                }
            });
        }

        public void setData(double[][] data, int n, int m, double hx, double hy) {
            this.u = data; this.N = n; this.M = m;
            minZ=Double.MAX_VALUE; maxZ=-Double.MAX_VALUE;
            for(double[] r:data) for(double v:r) { if(v<minZ)minZ=v; if(v>maxZ)maxZ=v; }
            repaint();
        }

        public void clear() { u=null; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (u == null) {
                g.drawString("Нет данных. Рассчитайте задачу.", getWidth()/2-80, getHeight()/2);
                return;
            }
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            double scale = Math.min(w, h) * 0.4; // Масштаб

            // Список полигонов для сортировки (Painter's Algorithm)
            ArrayList<Polygon3D> polys = new ArrayList<>();

            for (int i = 0; i < M; i++) {
                for (int j = 0; j < N; j++) {
                    // Координаты 4 точек квадрата (центрируем вокруг 0,0)
                    double x1 = (double)j/N - 0.5; double y1 = (double)i/M - 0.5; double z1 = norm(u[i][j]);
                    double x2 = (double)(j+1)/N - 0.5; double y2 = (double)i/M - 0.5; double z2 = norm(u[i][j+1]);
                    double x3 = (double)(j+1)/N - 0.5; double y3 = (double)(i+1)/M - 0.5; double z3 = norm(u[i+1][j+1]);
                    double x4 = (double)j/N - 0.5; double y4 = (double)(i+1)/M - 0.5; double z4 = norm(u[i+1][j]);

                    Polygon3D p = new Polygon3D();
                    p.pts = new Point3D[]{
                            project(x1, y1, z1, scale, w, h),
                            project(x2, y2, z2, scale, w, h),
                            project(x3, y3, z3, scale, w, h),
                            project(x4, y4, z4, scale, w, h)
                    };
                    // Средняя глубина для сортировки
                    p.avgDepth = (p.pts[0].depth + p.pts[1].depth + p.pts[2].depth + p.pts[3].depth) / 4.0;

                    // Цвет зависит от средней высоты Z
                    double avgZ = (z1+z2+z3+z4)/4.0;
                    // Корректируем цвет для полигона
                    p.color = Color.getHSBColor(0.66f * (1.0f - (float)avgZ), 0.8f, 0.9f);

                    polys.add(p);
                }
            }

            // Сортировка: рисуем сначала дальние полигоны (Painter's Algorithm)
            Collections.sort(polys, Comparator.comparingDouble(p -> p.avgDepth));

            // Отрисовка
            for (Polygon3D p : polys) {
                Path2D path = new Path2D.Double();
                path.moveTo(p.pts[0].x, p.pts[0].y);
                path.lineTo(p.pts[1].x, p.pts[1].y);
                path.lineTo(p.pts[2].x, p.pts[2].y);
                path.lineTo(p.pts[3].x, p.pts[3].y);
                path.closePath();

                g2.setColor(p.color);
                g2.fill(path);
                g2.setColor(new Color(0,0,0,50)); // Сетка
                g2.draw(path);
            }

            g2.setColor(Color.BLACK);
            g2.drawString("Управление: ЛКМ + Драг для вращения", 10, 20);
            g2.drawString("Z-Max: " + String.format("%.2f", maxZ), 10, 40);
        }

        private double norm(double val) {
            return (maxZ == minZ) ? 0 : (val - minZ) / (maxZ - minZ);
        }

        // Проекция 3D -> 2D
        private Point3D project(double x, double y, double z, double scale, int w, int h) {
            // 1. Вращение вокруг Z
            double x1 = x * Math.cos(angleZ) - y * Math.sin(angleZ);
            double y1 = x * Math.sin(angleZ) + y * Math.cos(angleZ);

            // 2. Вращение вокруг X (наклон)
            double y2 = y1 * Math.cos(angleX) - z * Math.sin(angleX); // Для 3D эффекта Z влияет на Y экрана
            double z2 = y1 * Math.sin(angleX) + z * Math.cos(angleX);

            // 3. Проекция на экран
            // Z теперь высота (0.5 максимум). Усиливаем эффект высоты * 0.5
            double zEffect = z * 0.5;

            // Простая изометрия с учетом вращения
            double px = w/2 + x1 * scale;
            double py = h/2 + y2 * scale - zEffect * scale * 0.5;

            return new Point3D(px, py, z2); // z2 - глубина для сортировки
        }

        class Point3D { double x, y, depth; Point3D(double x, double y, double d){this.x=x;this.y=y;this.depth=d;} }
        class Polygon3D { Point3D[] pts; double avgDepth; Color color; }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LAB12V());
    }
}
