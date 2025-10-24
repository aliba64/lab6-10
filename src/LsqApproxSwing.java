import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Path2D;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;

public class LsqApproxSwing {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("МНК: y(x)=sin(x^q), полином m=2 ");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(new AppPanel());
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

    static final double A = 0.0;
    static final double B =  2* Math.PI;

    static final class AppPanel extends JPanel {
        private final JComboBox<Integer> qBox = new JComboBox<>(new Integer[]{1, 2, 3});
        private final JSpinner nSpin = new JSpinner(new SpinnerNumberModel(20, 10, 200, 1));
        private final JLabel c0Lbl = new JLabel();
        private final JLabel c1Lbl = new JLabel();
        private final JLabel c2Lbl = new JLabel();
        private final JLabel mseLbl = new JLabel();
        private final JLabel maxErrLbl = new JLabel();
        private final PlotPanel plot = new PlotPanel();

        AppPanel() {
            super(new BorderLayout());
            var top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
            top.setBorder(new EmptyBorder(6, 6, 6, 6));
            top.add(new JLabel("q:"));
            top.add(qBox);
            top.add(new JLabel("n:"));
            top.add(nSpin);
            JButton recompute = new JButton("Пересчитать");
            top.add(recompute);

            top.add(new JLabel("c0:")); top.add(c0Lbl);
            top.add(new JLabel("c1:")); top.add(c1Lbl);
            top.add(new JLabel("c2:")); top.add(c2Lbl);
            top.add(new JLabel("MSE:")); top.add(mseLbl);
            top.add(new JLabel("MaxErr:")); top.add(maxErrLbl);

            add(top, BorderLayout.NORTH);
            add(plot, BorderLayout.CENTER);

            Runnable update = this::recomputeAndRefresh;
            recompute.addActionListener(e -> update.run());
            qBox.addActionListener(e -> update.run());
            nSpin.addChangeListener(e -> update.run());

            recomputeAndRefresh();
        }

        private void recomputeAndRefresh() {
            int q = (Integer) qBox.getSelectedItem();
            int n = (Integer) nSpin.getValue();
            FitResult fit = computeFit(n, q);
            plot.setFit(fit);
            DecimalFormat df = new DecimalFormat("0.000000");
            c0Lbl.setText(df.format(fit.c[0]));
            c1Lbl.setText(df.format(fit.c[1]));
            c2Lbl.setText(df.format(fit.c[2]));
            mseLbl.setText(new DecimalFormat("0.00000").format(fit.mse));
            maxErrLbl.setText(new DecimalFormat("0.00000").format(fit.maxAbsError));
            plot.repaint();
        }
    }

    static final class FitResult {
        final int n, q;
        final double[] c; // c0,c1,c2
        final double mse;
        final double maxAbsError;

        FitResult(int n, int q, double[] c, double mse, double maxAbsError) {
            this.n = n;
            this.q = q;
            this.c = c;
            this.mse = mse;
            this.maxAbsError = maxAbsError;
        }

        double f(double x) {
            return Math.sin(Math.pow(x, q));
        }

        double p(double x) {
            return c[0] + c[1] * x + c[2] * x * x;
        }
    }

    static FitResult computeFit(int n, int q) {
        double h = (B - A) / n;
        double[] s = new double[5];
        double[] t = new double[3];
        Arrays.fill(s, 0.0);
        Arrays.fill(t, 0.0);

        for (int j = 0; j <= n; j++) {
            double x = A + j * h;
            double y = Math.sin(Math.pow(x, q));
            s[0] += 1.0;
            s[1] += x;
            s[2] += x * x;
            s[3] += x * x * x;
            s[4] += x * x * x * x;
            t[0] += y;
            t[1] += x * y;
            t[2] += x * x * y;
        }

        double[][] M = {
                {s[0], s[1], s[2]},
                {s[1], s[2], s[3]},
                {s[2], s[3], s[4]}
        };
        double[] b = {t[0], t[1], t[2]};

        double[] c;
        try {
            c = choleskySolve(M, b);
        } catch (RuntimeException ex) {
            c = gaussSolve(M, b);
        }

        double sse = 0.0;
        double maxAbs = 0.0;
        for (int j = 0; j <= n; j++) {
            double x = A + j * h;
            double y = Math.sin(Math.pow(x, q));
            double p = c[0] + c[1] * x + c[2] * x * x;
            double e = p - y;
            sse += e * e;
            maxAbs = Math.max(maxAbs, Math.abs(e));
        }
        double mse = sse / (n + 1);

        return new FitResult(n, q, c, mse, maxAbs);
    }

    static double[] choleskySolve(double[][] A, double[] b) {
        int m = b.length;
        double[][] L = new double[m][m];

        for (int i = 0; i < m; i++) {
            for (int j = 0; j <= i; j++) {
                double sum = A[i][j];
                for (int k = 0; k < j; k++) sum -= L[i][k] * L[j][k];
                if (i == j) {
                    if (sum <= 0) throw new RuntimeException("Matrix not SPD");
                    L[i][i] = Math.sqrt(sum);
                } else {
                    L[i][j] = sum / L[j][j];
                }
            }
        }

        double[] z = new double[m];
        for (int i = 0; i < m; i++) {
            double sum = b[i];
            for (int k = 0; k < i; k++) sum -= L[i][k] * z[k];
            z[i] = sum / L[i][i];
        }

        double[] x = new double[m];
        for (int i = m - 1; i >= 0; i--) {
            double sum = z[i];
            for (int k = i + 1; k < m; k++) sum -= L[k][i] * x[k];
            x[i] = sum / L[i][i];
        }
        return x;
    }

    static double[] gaussSolve(double[][] Aorig, double[] borig) {
        int n = borig.length;
        double[][] A = new double[n][n];
        double[] b = new double[n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(Aorig[i], 0, A[i], 0, n);
            b[i] = borig[i];
        }

        for (int k = 0; k < n; k++) {
            int piv = k;
            double max = Math.abs(A[k][k]);
            for (int i = k + 1; i < n; i++) {
                double v = Math.abs(A[i][k]);
                if (v > max) { max = v; piv = i; }
            }
            if (max < 1e-15) throw new RuntimeException("Matrix is singular or nearly singular");
            if (piv != k) {
                double[] tmp = A[k]; A[k] = A[piv]; A[piv] = tmp;
                double tt = b[k]; b[k] = b[piv]; b[piv] = tt;
            }
            for (int i = k + 1; i < n; i++) {
                double factor = A[i][k] / A[k][k];
                b[i] -= factor * b[k];
                for (int j = k; j < n; j++) A[i][j] -= factor * A[k][j];
            }
        }

        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            double sum = b[i];
            for (int j = i + 1; j < n; j++) sum -= A[i][j] * x[j];
            x[i] = sum / A[i][i];
        }
        return x;
    }

    static final class PlotPanel extends JPanel {
        private FitResult fit;

        PlotPanel() {
            setPreferredSize(new Dimension(1000, 560));
            setBackground(Color.WHITE);
        }

        void setFit(FitResult fit) { this.fit = fit; }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (fit == null) return;
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Insets ins = getInsets();
                int W = getWidth() - ins.left - ins.right;
                int H = getHeight() - ins.top - ins.bottom;
                int padLeft = 70, padRight = 30, padTop = 40, padBottom = 70;
                int x0 = ins.left + padLeft;
                int y0 = ins.top + padTop;
                int w = W - padLeft - padRight;
                int h = H - padTop - padBottom;

                // диапазон Y по графикам
                double ymin = Double.POSITIVE_INFINITY, ymax = Double.NEGATIVE_INFINITY;
                int S = 2000;
                for (int i = 0; i <= S; i++) {
                    double x = A + (B - A) * i / S;
                    double yf = fit.f(x);
                    double yp = fit.p(x);
                    ymin = Math.min(ymin, Math.min(yf, yp));
                    ymax = Math.max(ymax, Math.max(yf, yp));
                }
                if (ymax - ymin < 1e-6) { ymax += 1; ymin -= 1; }
                double yMargin = 0.12 * (ymax - ymin);
                ymin -= yMargin; ymax += yMargin;

                final int x0f = x0, y0f = y0, wf = w, hf = h;
                final double yminf = ymin, ymaxf = ymax;
                final double scaleY = hf / (ymaxf - yminf);

                DoubleUnaryOperator X = xv -> x0f + (xv - A) / (B - A) * wf;
                DoubleUnaryOperator Y = yv -> y0f + (ymaxf - yv) * scaleY;

                // фон сетки
                g2.setColor(new Color(245, 245, 245));
                g2.fillRect(x0, y0, w, h);

                // вертикальная и горизонтальная сетка (тонко)
                g2.setColor(new Color(220, 220, 220));
                for (int i = 0; i <= 10; i++) {
                    int xx = x0 + (int) Math.round(w * (i / 10.0));
                    g2.drawLine(xx, y0, xx, y0 + h);
                }
                for (int i = 0; i <= 10; i++) {
                    int yy = y0 + (int) Math.round(h * (i / 10.0));
                    g2.drawLine(x0, yy, x0 + w, yy);
                }

                // рамка графика
                g2.setColor(Color.BLACK);
                g2.drawRect(x0, y0, w, h);

                // оси: подписи X: 0, π/2, π, 3π/2, 2π
                double[] xt = {0, 0.5 * Math.PI, 1.0 * Math.PI, 1.5 * Math.PI, 2.0 * Math.PI};
                String[] xlabels = {"0", "\u03C0/2", "\u03C0", "3\u03C0/2", "2\u03C0"};
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 13f));
                for (int i = 0; i < xt.length; i++) {
                    int px = (int) Math.round(X.applyAsDouble(xt[i]));
                    int py = y0 + h;
                    g2.drawLine(px, py, px, py + 6);
                    FontMetrics fm = g2.getFontMetrics();
                    int tw = fm.stringWidth(xlabels[i]);
                    g2.drawString(xlabels[i], px - tw / 2, py + 6 + fm.getAscent() + 2);
                }

                // подписи Y (5 делений)
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
                int yTicks = 6;
                for (int i = 0; i <= yTicks; i++) {
                    double yyv = yminf + (ymaxf - yminf) * i / yTicks;
                    int py = (int) Math.round(Y.applyAsDouble(yyv));
                    int px = x0;
                    g2.drawLine(px - 6, py, px, py);
                    String lab = String.format("%.2f", yyv);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(lab, px - 8 - fm.stringWidth(lab), py + fm.getAscent() / 2 - 3);
                }

                // кривые: исходная (красная, сплошная) и полином (синяя, пунктир)
                Path2D fpath = new Path2D.Double();
                Path2D ppath = new Path2D.Double();
                for (int i = 0; i <= S; i++) {
                    double x = A + (B - A) * i / S;
                    double yf = fit.f(x);
                    double yp = fit.p(x);
                    double px = X.applyAsDouble(x);
                    double pyf = Y.applyAsDouble(yf);
                    double pyp = Y.applyAsDouble(yp);
                    if (i == 0) {
                        fpath.moveTo(px, pyf);
                        ppath.moveTo(px, pyp);
                    } else {
                        fpath.lineTo(px, pyf);
                        ppath.lineTo(px, pyp);
                    }
                }

                g2.setStroke(new BasicStroke(2.2f));
                g2.setColor(new Color(200, 0, 0)); // красная — функция
                g2.draw(fpath);

                // пунктирная синяя линия для полинома
                float[] dash = {8f, 6f};
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
                g2.setColor(new Color(0, 80, 200));
                g2.draw(ppath);

                // узлы и вертикальные остатки (thin semi-transparent)
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(new Color(0,0,0,140));
                int r = 3;
                for (int j = 0; j <= fit.n; j++) {
                    double x = A + (B - A) * j / fit.n;
                    double yf = fit.f(x);
                    double yp = fit.p(x);
                    int px = (int) Math.round(X.applyAsDouble(x));
                    int pyf = (int) Math.round(Y.applyAsDouble(yf));
                    int pyp = (int) Math.round(Y.applyAsDouble(yp));
                    // остаток как вертикальная линия
                    g2.setColor(new Color(120, 120, 120, 120));
                    g2.drawLine(px, pyf, px, pyp);
                    // точки исходной функции (чёрные)
                    g2.setColor(new Color(0,0,0,200));
                    g2.fillOval(px - r, pyf - r, 2*r, 2*r);
                }

                // легенда (прямоугольник с текстом)
                int lx = x0 + w - 220;
                int ly = y0 + 8;
                g2.setColor(new Color(255,255,255,230));
                g2.fillRoundRect(lx-8, ly-6, 210, 60, 8, 8);
                g2.setColor(Color.BLACK);
                g2.drawRoundRect(lx-8, ly-6, 210, 60, 8, 8);

                // легенда: красная линия — функция
                g2.setStroke(new BasicStroke(3f));
                g2.setColor(new Color(200,0,0));
                g2.drawLine(lx, ly+12, lx+30, ly+12);
                g2.setColor(Color.BLACK);
                g2.drawString("Исходная y(x)=sin(x^" + fit.q + ")", lx+38, ly+16);

                // легенда: пунктир синяя — полином
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
                g2.setColor(new Color(0,80,200));
                g2.drawLine(lx, ly+36, lx+30, ly+36);
                g2.setColor(Color.BLACK);
                g2.drawString("Полином МНК p(x)", lx+38, ly+40);

                // подпись снизу: вывод MSE и max error
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
                String foot = String.format("Диапазон: [0, 2π]. Узлы: n=%d. MSE=%.6g, max|err|=%.6g",
                        fit.n, fit.mse, fit.maxAbsError);
                g2.drawString(foot, x0, y0 + h + 45);

            } finally {
                g2.dispose();
            }
        }
    }
}
