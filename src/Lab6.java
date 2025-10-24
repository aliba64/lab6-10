import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Path2D;
import java.text.DecimalFormat;
import java.util.function.DoubleUnaryOperator;

public class Lab6 {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Лаба 6: интегрирование — прямоугольники, трапеция, Симпсон, Гаусс");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(new AppPanel());
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

    // интегрируемая функция
    static double f(double x) { return Math.sqrt(Math.exp(x) - 0.5); }

    static final double a = 0.2, b = 2.5;

    // составная трапеция
    static double trap(int n) {
        double h = (b - a) / n;
        double sum = 0.5 * (f(a) + f(b));
        for (int i = 1; i < n; i++) sum += f(a + i * h);
        return h * sum;
    }

    // составный Симпсон (n четно)
    static double simpson(int n) {
        if ((n & 1) == 1) throw new IllegalArgumentException("n must be even for Simpson");
        double h = (b - a) / n;
        double s1 = 0, s2 = 0;
        for (int j = 1; j <= n / 2; j++) s1 += f(a + (2 * j - 1) * h);        // нечетные
        for (int j = 1; j <= n / 2 - 1; j++) s2 += f(a + (2 * j) * h);        // четные, кроме концов
        return (h / 3.0) * (f(a) + 2 * s2 + 4 * s1 + f(b));
    }

    // составные прямоугольники
    // Левые прямоугольники (left Riemann sum)
    static double rectLeft(int n) {
        double h = (b - a) / n;
        double sum = 0;
        for (int i = 0; i < n; i++) sum += f(a + i * h);
        return h * sum;
    }

    // Правые прямоугольники (right Riemann sum)
    static double rectRight(int n) {
        double h = (b - a) / n;
        double sum = 0;
        for (int i = 1; i <= n; i++) sum += f(a + i * h);
        return h * sum;
    }

    // Средние прямоугольники (midpoint rule)
    static double rectMid(int n) {
        double h = (b - a) / n;
        double sum = 0;
        for (int i = 0; i < n; i++) sum += f(a + (i + 0.5) * h);
        return h * sum;
    }

    // Гаусс–Лежандр на [a,b] с узлами на [0,1] из методички
    static double gauss(int m) {
        GLRule r = GLRule.of(m);
        double res = 0;
        for (int i = 0; i < r.t.length; i++) {
            double x = a + (b - a) * r.t[i];
            res += r.A[i] * f(x);
        }
        return (b - a) * res;
    }

    // Наборы узлов/весов на [0,1] (симметричны относительно 0.5)
    static final class GLRule {
        final int m; final double[] t; final double[] A;
        GLRule(int m, double[] t, double[] A) { this.m = m; this.t = t; this.A = A; }
        static GLRule of(int m) {
            switch (m) {
                case 3:  return new GLRule(3,
                        new double[]{0.112701665, 0.500000000, 0.887298335},
                        new double[]{0.277777778, 0.444444444, 0.277777778});
                case 5:  return new GLRule(5,
                        new double[]{0.046910077, 0.230765345, 0.500000000, 0.769234655, 0.953089923},
                        new double[]{0.118463443, 0.239314335, 0.284444444, 0.239314335, 0.118463443});
                case 7:  return new GLRule(7,
                        new double[]{0.025446044, 0.129234407, 0.297077424, 0.500000000, 0.702922576, 0.870765593, 0.974553956},
                        new double[]{0.064742483, 0.139852696, 0.190915025, 0.208979592, 0.190915025, 0.139852696, 0.064742483});
                case 9:  return new GLRule(9,
                        new double[]{0.015919880, 0.081934446, 0.193314284, 0.337873288, 0.500000000,
                                0.662126712, 0.806685716, 0.918065554, 0.984080120},
                        new double[]{0.040637194, 0.090324080, 0.130305348, 0.156173539, 0.165119678,
                                0.156173539, 0.130305348, 0.090324080, 0.040637194});
                case 11: return new GLRule(11,
                        new double[]{0.010885671, 0.056468700, 0.134923997, 0.240451935, 0.365228422, 0.500000000,
                                0.634771578, 0.759548065, 0.865076003, 0.943531300, 0.989114329},
                        new double[]{0.027834284, 0.062790185, 0.093145105, 0.116596882, 0.131402272, 0.136462543,
                                0.131402272, 0.116596882, 0.093145105, 0.062790185, 0.027834284});
                default: throw new IllegalArgumentException("m ∈ {3,5,7,9,11}");
            }
        }
    }

    static final class AppPanel extends JPanel {
        private final JSpinner nSpin = new JSpinner(new SpinnerNumberModel(40, 4, 2000, 2));
        private final JComboBox<Integer> mBox = new JComboBox<>(new Integer[]{3,5,7,9,11});

        private final JLabel rectLeftLbl = new JLabel();
        private final JLabel rectRightLbl = new JLabel();
        private final JLabel rectMidLbl = new JLabel();

        private final JLabel trapLbl = new JLabel(), simpLbl = new JLabel(),
                gaussLbl = new JLabel(), refLbl = new JLabel();

        private final JLabel eRectLeftLbl = new JLabel(), eRectRightLbl = new JLabel(), eRectMidLbl = new JLabel(),
                eTrapLbl = new JLabel(), eSimpLbl = new JLabel(), eGaussLbl = new JLabel();

        private final PlotPanel plot = new PlotPanel();

        AppPanel() {
            super(new BorderLayout(10,10));
            setBorder(new EmptyBorder(10,10,10,10));

            JPanel top = new JPanel(new BorderLayout());
            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
            controls.add(new JLabel("n (для составных):"));
            controls.add(nSpin);
            controls.add(new JLabel("m (Гаусс):"));
            controls.add(mBox);
            JButton btn = new JButton("Вычислить");
            controls.add(btn);
            top.add(controls, BorderLayout.NORTH);

            // Цель работы
            JLabel goal = new JLabel("<html><b>Цель работы:</b> изучение методов численного интегрирования, вычисление определённого интеграла от заданной функции методами прямоугольников и Гаусса.</html>");
            goal.setBorder(new EmptyBorder(6,6,6,6));
            top.add(goal, BorderLayout.SOUTH);

            add(top, BorderLayout.NORTH);

            JPanel center = new JPanel(new GridLayout(1,2,10,10));
            center.add(new JPanel(new BorderLayout()) {{
                setBorder(BorderFactory.createTitledBorder("График f(x)=sqrt(e^x-0.5) на [0.2,2.5]"));
                add(plot, BorderLayout.CENTER);
            }});
            center.add(new JPanel(new BorderLayout()) {{
                setBorder(BorderFactory.createTitledBorder("Результаты"));
                // увеличим количество строк, чтобы вместить прямоугольники
                JPanel g = new JPanel(new GridLayout(10,1,6,6));
                g.add(line("Левые прямоугольники:", rectLeftLbl));
                g.add(line("Правые прямоугольники:", rectRightLbl));
                g.add(line("Средние прямоугольники:", rectMidLbl));
                g.add(line("Трапеции:", trapLbl));
                g.add(line("Симпсон:", simpLbl));
                g.add(line("Гаусс (m):", gaussLbl));
                g.add(line("Эталон (Gauss11):", refLbl));
                g.add(line("|ε| Левые:", eRectLeftLbl));
                g.add(line("|ε| Средние:", eRectMidLbl));
                g.add(line("|ε| Правые:", eRectRightLbl));
                g.add(line("|ε| Трапеции:", eTrapLbl));
                // подсимпсон и гаусс ошибки отдельно ниже
                JPanel bottomErrors = new JPanel(new GridLayout(2,1));
                bottomErrors.add(line("|ε| Симпсон:", eSimpLbl));
                bottomErrors.add(line("|ε| Гаусс(m):", eGaussLbl));
                JPanel wrapper = new JPanel(new BorderLayout());
                wrapper.add(g, BorderLayout.NORTH);
                wrapper.add(bottomErrors, BorderLayout.SOUTH);
                add(wrapper, BorderLayout.NORTH);
            }});
            add(center, BorderLayout.CENTER);

            Runnable run = this::computeAndShow;
            btn.addActionListener(e -> run.run());
            nSpin.addChangeListener(e -> run.run());
            mBox.addActionListener(e -> run.run());

            computeAndShow();
        }

        private JPanel line(String name, JLabel value) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            p.add(new JLabel(name));
            p.add(value);
            return p;
        }

        private void computeAndShow() {
            int n = (Integer) nSpin.getValue();
            int m = (Integer) mBox.getSelectedItem();

            // вычисления
            double IrectLeft = rectLeft(n);
            double IrectRight = rectRight(n);
            double IrectMid = rectMid(n);
            double Itrap = trap(n);
            double Isim = (n % 2 == 0) ? simpson(n) : simpson(n+1); // если нечётно, используем n+1
            double Igauss = gauss(m);
            double Iref = gauss(11); // эталон

            DecimalFormat df = new DecimalFormat("0.0000000000");
            rectLeftLbl.setText(df.format(IrectLeft));
            rectRightLbl.setText(df.format(IrectRight));
            rectMidLbl.setText(df.format(IrectMid));
            trapLbl.setText(df.format(Itrap));
            simpLbl.setText(df.format(Isim));
            gaussLbl.setText(df.format(Igauss));
            refLbl.setText(df.format(Iref));

            eRectLeftLbl.setText(df.format(Math.abs(IrectLeft - Iref)));
            eRectRightLbl.setText(df.format(Math.abs(IrectRight - Iref)));
            eRectMidLbl.setText(df.format(Math.abs(IrectMid - Iref)));
            eTrapLbl.setText(df.format(Math.abs(Itrap - Iref)));
            eSimpLbl.setText(df.format(Math.abs(Isim - Iref)));
            eGaussLbl.setText(df.format(Math.abs(Igauss - Iref)));

            plot.repaint();
        }
    }

    static final class PlotPanel extends JPanel {
        PlotPanel() {
            setPreferredSize(new Dimension(780, 520));
            setBackground(Color.white);
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Insets ins = getInsets();
                int W = getWidth() - ins.left - ins.right;
                int H = getHeight() - ins.top - ins.bottom;
                int pad = 48;
                int x0 = ins.left + pad, y0 = ins.top + pad;
                int w = W - 2*pad, h = H - 2*pad;

                // диапазон по f(x)
                double ymin = Double.POSITIVE_INFINITY, ymax = Double.NEGATIVE_INFINITY;
                int S = 1500;
                for (int i = 0; i <= S; i++) {
                    double x = a + (b - a) * (i/(double)S);
                    double y = f(x);
                    ymin = Math.min(ymin, y);
                    ymax = Math.max(ymax, y);
                }
                if (ymax == ymin) { ymax += 1; ymin -= 1; }
                double margin = 0.08*(ymax - ymin);
                ymin -= margin; ymax += margin;

                final int x0f=x0, y0f=y0, wf=w, hf=h;
                final double yminf=ymin, ymaxf=ymax;
                final double scale = hf / (ymaxf - yminf);

                DoubleUnaryOperator X = xx -> x0f + (xx - a) * (wf / (b - a));
                DoubleUnaryOperator Y = yy -> y0f + (ymaxf - yy) * scale;

                // сетка
                g2.setColor(new Color(235,235,235));
                for (int i=0;i<=10;i++){
                    int xx = x0 + (int)Math.round(w*i/10.0);
                    int yy = y0 + (int)Math.round(h*i/10.0);
                    g2.drawLine(xx,y0,xx,y0+h);
                    g2.drawLine(x0,yy,x0+w,yy);
                }
                g2.setColor(Color.black);
                g2.drawRect(x0,y0,w,h);

                // кривая
                Path2D path = new Path2D.Double();
                for (int i = 0; i <= S; i++) {
                    double x = a + (b - a) * (i/(double)S);
                    double y = f(x);
                    double px = X.applyAsDouble(x);
                    double py = Y.applyAsDouble(y);
                    if (i==0) path.moveTo(px,py); else path.lineTo(px,py);
                }
                g2.setStroke(new BasicStroke(2.0f));
                g2.setColor(new Color(200,0,0));
                g2.draw(path);

                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 13f));
                g2.drawString(String.format("a=%.2f, b=%.2f, f(x)=sqrt(e^x-0.5)", a, b), x0, y0-16);
            } finally {
                g2.dispose();
            }
        }
    }
}
