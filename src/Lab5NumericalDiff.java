import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.Path2D;
import java.text.DecimalFormat;
import java.util.function.DoubleUnaryOperator;

public class Lab5NumericalDiff {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AppPanel().showAll());
    }

    /** Главное окно (таблица + ошибки + кнопки + управление) */
    static final class AppPanel {
        private final JFrame frame;
        private final JSpinner nSpin = new JSpinner(new SpinnerNumberModel(40, 20, 200, 1));
        private final JLabel maxE1Lbl = new JLabel(), idxE1Lbl = new JLabel(), rmsE1Lbl = new JLabel();
        private final JLabel maxE2Lbl = new JLabel(), idxE2Lbl = new JLabel(), rmsE2Lbl = new JLabel();
        private final JTable table = new JTable();

        private final GraphFrame[] graphFrames = new GraphFrame[5];

        AppPanel() {
            frame = new JFrame("Численное дифференцирование: y(x)=sin(πx²/2)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout(8, 8));

            // ✅ setBorder — только для контент-панели, не для JFrame
            ((JComponent) frame.getContentPane()).setBorder(new EmptyBorder(8, 8, 8, 8));

            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
            top.add(new JLabel("n:"));
            top.add(nSpin);
            JButton recompute = new JButton("Пересчитать");
            top.add(recompute);
            frame.add(top, BorderLayout.NORTH);

            JPanel metrics = new JPanel(new GridLayout(2, 3, 10, 4));
            metrics.setBorder(BorderFactory.createTitledBorder("Ошибки"));
            metrics.add(labeled("max|ε₁|", maxE1Lbl));
            metrics.add(labeled("j₁,max", idxE1Lbl));
            metrics.add(labeled("RMS₁", rmsE1Lbl));
            metrics.add(labeled("max|ε₂|", maxE2Lbl));
            metrics.add(labeled("j₂,max", idxE2Lbl));
            metrics.add(labeled("RMS₂", rmsE2Lbl));

            frame.add(metrics, BorderLayout.SOUTH);
            frame.add(new JScrollPane(table), BorderLayout.CENTER);

            recompute.addActionListener(e -> recomputeAndRefresh());
            nSpin.addChangeListener(e -> recomputeAndRefresh());

            frame.setSize(800, 500);
            frame.setLocationRelativeTo(null);
        }

        void showAll() {
            frame.setVisible(true);
            recomputeAndRefresh();
        }

        private JPanel labeled(String name, JLabel value) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            p.add(new JLabel(name + ": "));
            p.add(value);
            return p;
        }

        private void recomputeAndRefresh() {
            int n = (Integer) nSpin.getValue();
            Result res = compute(n);

            DecimalFormat d6 = new DecimalFormat("0.000000");
            maxE1Lbl.setText(d6.format(res.maxAbsE1));
            idxE1Lbl.setText(Integer.toString(res.idxMaxE1));
            rmsE1Lbl.setText(d6.format(res.rmsE1));
            maxE2Lbl.setText(d6.format(res.maxAbsE2));
            idxE2Lbl.setText(Integer.toString(res.idxMaxE2));
            rmsE2Lbl.setText(d6.format(res.rmsE2));

            String[] cols = {"j", "xj", "y", "y'_точн", "y'_числ", "|ε₁|", "y''_точн", "y''_числ", "|ε₂|"};
            DefaultTableModel model = new DefaultTableModel(cols, 0);
            DecimalFormat d4 = new DecimalFormat("0.0000");
            for (int j = 0; j <= n; j++) {
                model.addRow(new Object[]{
                        j,
                        d4.format(res.x[j]),
                        d6.format(res.y[j]),
                        d6.format(res.y1Exact[j]),
                        d6.format(res.y1Approx[j]),
                        d6.format(Math.abs(res.y1Approx[j] - res.y1Exact[j])),
                        d6.format(res.y2Exact[j]),
                        d6.format(res.y2Approx[j]),
                        d6.format(Math.abs(res.y2Approx[j] - res.y2Exact[j]))
                });
            }
            table.setModel(model);

            // Открываем/обновляем 5 окон графиков
            showGraph(0, res, res.x, res.y, "y(x)", new Color(200, 0, 0));
            showGraph(1, res, res.x, res.y1Exact, "y'(x) точн", new Color(0, 120, 0));
            showGraph(2, res, res.x, res.y1Approx, "y'(x) числ", new Color(0, 160, 255));
            showGraph(3, res, res.x, res.y2Exact, "y''(x) точн", new Color(120, 0, 150));
            showGraph(4, res, res.x, res.y2Approx, "y''(x) числ", new Color(255, 140, 0));
        }

        private void showGraph(int idx, Result res, double[] xs, double[] ys, String title, Color color) {
            if (graphFrames[idx] == null) {
                graphFrames[idx] = new GraphFrame(title, xs, ys, color);
                graphFrames[idx].setLocation(100 + idx * 60, 100 + idx * 40);
                graphFrames[idx].setVisible(true);
            }
            graphFrames[idx].updateData(xs, ys);
        }
    }

    /** Окно для одного графика */
    static final class GraphFrame extends JFrame {
        private final GraphPanel panel;

        GraphFrame(String title, double[] xs, double[] ys, Color c) {
            super(title);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            panel = new GraphPanel(xs, ys, c, title);
            add(panel);
            setSize(600, 400);
        }

        void updateData(double[] xs, double[] ys) {
            panel.update(xs, ys);
        }
    }

    /** Панель для рисования одного графика */
    static final class GraphPanel extends JPanel {
        private double[] xs, ys;
        private final Color color;
        private final String label;

        GraphPanel(double[] xs, double[] ys, Color c, String label) {
            this.xs = xs;
            this.ys = ys;
            this.color = c;
            this.label = label;
            setBackground(Color.white);
        }

        void update(double[] xs, double[] ys) {
            this.xs = xs;
            this.ys = ys;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (xs == null || ys == null) return;
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Insets ins = getInsets();
                int W = getWidth() - ins.left - ins.right;
                int H = getHeight() - ins.top - ins.bottom;
                int pad = 40;
                int x0 = ins.left + pad;
                int y0 = ins.top + pad;
                int w = W - 2 * pad;
                int h = H - 2 * pad;

                double ymin = Double.POSITIVE_INFINITY, ymax = Double.NEGATIVE_INFINITY;
                for (double v : ys) {
                    ymin = Math.min(ymin, v);
                    ymax = Math.max(ymax, v);
                }
                if (ymax == ymin) { ymax += 1; ymin -= 1; }
                double margin = 0.1 * (ymax - ymin);
                ymin -= margin; ymax += margin;
                final double yminf = ymin, ymaxf = ymax;
                final DoubleUnaryOperator X = xx -> x0 + xx * w;
                final DoubleUnaryOperator Y = yy -> y0 + (ymaxf - yy) * (h / (ymaxf - yminf));

                // сетка
                g2.setColor(new Color(230, 230, 230));
                for (int i = 0; i <= 10; i++) {
                    int xx = x0 + (int) (i * w / 10.0);
                    g2.drawLine(xx, y0, xx, y0 + h);
                }

                // рамка
                g2.setColor(Color.black);
                g2.drawRect(x0, y0, w, h);

                // график
                Path2D path = new Path2D.Double();
                for (int i = 0; i < xs.length; i++) {
                    double px = X.applyAsDouble(xs[i]);
                    double py = Y.applyAsDouble(ys[i]);
                    if (i == 0) path.moveTo(px, py);
                    else path.lineTo(px, py);
                }
                g2.setStroke(new BasicStroke(1.6f));
                g2.setColor(color);
                g2.draw(path);

                // подпись
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 14f));
                g2.setColor(color);
                g2.drawString(label, x0 + 10, y0 - 10);

            } finally {
                g2.dispose();
            }
        }
    }

    /** Результаты вычислений */
    static final class Result {
        final int n;
        final double h;
        final double[] x, y, y1Exact, y1Approx, y2Exact, y2Approx;
        final double maxAbsE1, rmsE1, maxAbsE2, rmsE2;
        final int idxMaxE1, idxMaxE2;

        Result(int n, double h, double[] x, double[] y,
               double[] y1e, double[] y1a, double[] y2e, double[] y2a,
               double maxE1, double rmsE1, int idx1, double maxE2, double rmsE2, int idx2) {
            this.n = n; this.h = h; this.x = x; this.y = y;
            this.y1Exact = y1e; this.y1Approx = y1a;
            this.y2Exact = y2e; this.y2Approx = y2a;
            this.maxAbsE1 = maxE1; this.rmsE1 = rmsE1; this.idxMaxE1 = idx1;
            this.maxAbsE2 = maxE2; this.rmsE2 = rmsE2; this.idxMaxE2 = idx2;
        }
    }

    /** Расчёт аналитических и численных производных */
    static Result compute(int n) {
        double h = 1.0 / n;
        double[] x = new double[n + 1];
        double[] y = new double[n + 1];
        double[] y1e = new double[n + 1];
        double[] y2e = new double[n + 1];

        for (int j = 0; j <= n; j++) {
            double xx = j * h;
            x[j] = xx;
            double u = Math.PI * xx * xx / 2.0;
            y[j] = Math.sin(u);
            y1e[j] = Math.PI * xx * Math.cos(u);
            y2e[j] = Math.PI * Math.cos(u) - Math.PI * Math.PI * xx * xx * Math.sin(u);
        }

        double[] y1a = new double[n + 1];
        double[] y2a = new double[n + 1];

        y1a[0] = (-3 * y[0] + 4 * y[1] - y[2]) / (2 * h);
        y1a[n] = (3 * y[n] - 4 * y[n - 1] + y[n - 2]) / (2 * h);
        y2a[0] = (2 * y[0] - 5 * y[1] + 4 * y[2] - y[3]) / (h * h);
        y2a[n] = (2 * y[n] - 5 * y[n - 1] + 4 * y[n - 2] - y[n - 3]) / (h * h);

        for (int j = 1; j <= n - 1; j++) {
            y1a[j] = (y[j + 1] - y[j - 1]) / (2 * h);
            y2a[j] = (y[j + 1] - 2 * y[j] + y[j - 1]) / (h * h);
        }

        double maxE1 = 0, maxE2 = 0, sse1 = 0, sse2 = 0;
        int idx1 = 0, idx2 = 0;
        for (int j = 0; j <= n; j++) {
            double e1 = Math.abs(y1a[j] - y1e[j]);
            double e2 = Math.abs(y2a[j] - y2e[j]);
            if (e1 > maxE1) { maxE1 = e1; idx1 = j; }
            if (e2 > maxE2) { maxE2 = e2; idx2 = j; }
            sse1 += e1 * e1;
            sse2 += e2 * e2;
        }
        double rms1 = Math.sqrt(sse1 / (n + 1));
        double rms2 = Math.sqrt(sse2 / (n + 1));

        return new Result(n, h, x, y, y1e, y1a, y2e, y2a, maxE1, rms1, idx1, maxE2, rms2, idx2);
    }
}
