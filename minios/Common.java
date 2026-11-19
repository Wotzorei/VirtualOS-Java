package minios;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/** Shared GUI helpers and input validation for the Mini OS Simulator. */
public final class Common {
    private Common() {}

    /** Raised for blank, invalid or inconsistent user input. */
    public static class InputError extends RuntimeException {
        public InputError(String msg) { super(msg); }
    }

    /** A Gantt segment: label runs from s to e. */
    public static class Seg {
        public final String label;
        public final int s, e;
        public Seg(String label, int s, int e) { this.label = label; this.s = s; this.e = e; }
    }

    // ------------------------------------------------------------ validation
    public static int toInt(String text, String name) { return toInt(text, name, null, null); }
    public static int toInt(String text, String name, Integer min) { return toInt(text, name, min, null); }

    public static int toInt(String text, String name, Integer min, Integer max) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) throw new InputError(name + " cannot be blank.");
        int v;
        try {
            v = Integer.parseInt(t);
        } catch (NumberFormatException ex) {
            throw new InputError(name + " must be a whole number (got '" + t + "').");
        }
        if (min != null && v < min) throw new InputError(name + " must be at least " + min + " (got " + v + ").");
        if (max != null && v > max) throw new InputError(name + " must be at most " + max + " (got " + v + ").");
        return v;
    }

    public static int[] parseIntList(String text, String name, Integer min, Integer max) {
        List<String> tokens = new ArrayList<>();
        for (String t : String.valueOf(text).trim().split("[,\\s;]+")) if (!t.isEmpty()) tokens.add(t);
        if (tokens.isEmpty()) throw new InputError(name + " cannot be empty.");
        int[] out = new int[tokens.size()];
        for (int k = 0; k < out.length; k++) out[k] = toInt(tokens.get(k), name + " item " + (k + 1), min, max);
        return out;
    }

    /** Wraps an action so bad input shows a dialog instead of crashing. */
    public static Runnable guarded(Runnable fn) {
        return () -> {
            try {
                fn.run();
            } catch (InputError e) {
                JOptionPane.showMessageDialog(null, e.getMessage(), "Invalid input", JOptionPane.ERROR_MESSAGE);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Unexpected problem: " + e, "Error", JOptionPane.ERROR_MESSAGE);
            }
        };
    }

    /** Rounded flat button in three flavours. */
    public static class Btn extends JButton {
        public enum Kind { PRIMARY, SOFT, DANGER }
        private final Kind kind;
        private boolean hover;

        public Btn(String text, Kind kind) {
            super(text);
            this.kind = kind;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setFont(Theme.BOLD);
            setBorder(new EmptyBorder(3, 12, 3, 12));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        /** Read live so a theme change needs no re-colouring of buttons. */
        @Override public Color getForeground() { return kind == Kind.SOFT ? Theme.PRIMARY_D : Color.WHITE; }

        @Override public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(d.width, Math.max(26, d.height));
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color base = kind == Kind.PRIMARY ? Theme.PRIMARY : kind == Kind.DANGER ? Theme.CORAL : Theme.PRIMARY_L;
            if (!isEnabled()) base = Theme.BORDER;
            else if (getModel().isPressed()) base = kind == Kind.SOFT ? Theme.mix(Theme.PRIMARY_L, Theme.PRIMARY, .30f) : base.darker();
            else if (hover) base = kind == Kind.SOFT ? Theme.mix(Theme.PRIMARY_L, Theme.PRIMARY, .16f) : kind == Kind.PRIMARY ? Theme.PRIMARY_H : base.brighter();
            g.setColor(base);
            g.fill(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
            g.dispose();
            super.paintComponent(g0);
        }
    }

    /** Main action button (filled indigo). */
    public static JButton button(String text, Runnable action) { return make(text, action, Btn.Kind.PRIMARY); }
    /** Secondary action button (soft tint). */
    public static JButton softButton(String text, Runnable action) { return make(text, action, Btn.Kind.SOFT); }
    public static JButton dangerButton(String text, Runnable action) { return make(text, action, Btn.Kind.DANGER); }

    private static JButton make(String text, Runnable action, Btn.Kind kind) {
        Btn b = new Btn(text, kind);
        b.addActionListener(ev -> guarded(action).run());
        return b;
    }

    public static JTextField field(String value, int columns) {
        JTextField f = new JTextField(value, columns);
        f.setFont(Theme.BASE);
        return f;
    }

    public static <T> JComboBox<T> combo(T[] items) {
        JComboBox<T> c = new JComboBox<>(items);
        c.setFont(Theme.BASE);
        c.setBackground(Theme.FIELD);
        c.setFocusable(false);
        c.setUI(new BasicComboBoxUI() {
            @Override protected JButton createArrowButton() {
                // painted from the live theme colours, so it follows light / dark switches
                BasicArrowButton b = new BasicArrowButton(SwingConstants.SOUTH) {
                    @Override public void paint(Graphics g) {
                        g.setColor(Theme.FIELD);
                        g.fillRect(0, 0, getWidth(), getHeight());
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(Theme.PRIMARY_D);
                        int cx = getWidth() / 2, cy = getHeight() / 2;
                        g2.fillPolygon(new int[]{cx - 4, cx + 4, cx}, new int[]{cy - 2, cy - 2, cy + 3}, 3);
                        g2.dispose();
                    }
                };
                b.setBorder(BorderFactory.createEmptyBorder());
                return b;
            }
        });
        c.setBorder(Theme.input());
        return c;
    }

    /** Anything that needs more than "empty the text boxes" to reset can implement this. */
    public interface Clearable { void clearInputs(); }

    /**
     * Resets a whole module: every text box and table empty, results and charts blank.
     * (Drop-downs keep their selection - they are choices, not values.)
     */
    public static void clearInputs(Container root) {
        if (root instanceof Clearable) ((Clearable) root).clearInputs();
        for (Component c : root.getComponents()) {
            if (c instanceof DynamicTable) ((DynamicTable) c).clear();
            else if (c instanceof OutputBox) ((OutputBox) c).clear();
            else if (c instanceof GanttChart) ((GanttChart) c).draw(new ArrayList<>());
            else if (c instanceof DrawPane) ((DrawPane) c).redraw(10, 10, g -> { });
            else if (c instanceof JTextField) ((JTextField) c).setText("");
            if (c instanceof Container) clearInputs((Container) c);
        }
    }

    /** The red "Clear all inputs" button every module has. */
    public static JButton clearButton(Container module) {
        return dangerButton("Clear all inputs", () -> clearInputs(module));
    }

    /** A row of its own for the clear button (module input rows are too long to share). */
    public static JPanel clearRow(Container module) {
        JPanel r = row();
        r.add(clearButton(module));
        return r;
    }

    /** Small muted caption used in front of inputs. */
    public static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Theme.MUTED);
        l.setFont(Theme.BASE);
        return l;
    }

    /** A compact left-aligned toolbar row (tight gaps). */
    public static JPanel row() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        p.setOpaque(false);
        return p;
    }

    /** Vertical stack of panels with no wasted space. */
    public static JPanel stack(Component... parts) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        for (Component c : parts) {
            if (c instanceof JComponent) ((JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(c);
        }
        return p;
    }

    /** Rounded titled outline used for every card-like section. */
    public static javax.swing.border.Border titled(String title) { return Theme.card(title); }

    /** Page padding for each module panel. */
    public static void pad(JPanel p) { p.setBorder(new EmptyBorder(6, 8, 8, 8)); }

    public static String fmtTable(String[] headers, List<Object[]> rows) {
        int n = headers.length;
        List<String[]> cells = new ArrayList<>();
        for (Object[] r : rows) {
            String[] c = new String[n];
            for (int i = 0; i < n; i++) c[i] = String.valueOf(r[i]);
            cells.add(c);
        }
        int[] w = new int[n];
        for (int i = 0; i < n; i++) {
            w[i] = headers[i].length();
            for (String[] c : cells) w[i] = Math.max(w[i], c[i].length());
        }
        StringBuilder sb = new StringBuilder();
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) line.append("  ");
            line.append(pad(headers[i], w[i]));
        }
        sb.append(line).append('\n');
        for (int i = 0; i < line.length(); i++) sb.append('-');
        for (String[] c : cells) {
            sb.append('\n');
            for (int i = 0; i < n; i++) {
                if (i > 0) sb.append("  ");
                sb.append(pad(c[i], w[i]));
            }
        }
        return sb.toString();
    }

    private static String pad(String s, int w) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < w) sb.append(' ');
        return sb.toString();
    }

    public static String join(String sep, Iterable<?> items) {
        StringBuilder sb = new StringBuilder();
        for (Object o : items) {
            if (sb.length() > 0) sb.append(sep);
            sb.append(o);
        }
        return sb.toString();
    }

    // --------------------------------------------------------------- widgets
    public static class OutputBox extends JScrollPane {
        private final JTextArea text = new JTextArea();

        public OutputBox() { this(12); }

        public OutputBox(int rows) {
            text.setRows(rows);
            text.setEditable(false);
            text.setFont(Theme.MONO);
            text.setMargin(new Insets(8, 10, 8, 10));
            text.setBackground(Theme.INSET);
            text.setForeground(Theme.TEXT);
            text.setCaretColor(Theme.TEAL);
            setBorder(Theme.card("Results"));
            setViewportView(text);
        }

        public void clear() { text.setText(""); }
        public void write() { write(""); }
        public void write(String s) {
            text.append(s + "\n");
            text.setCaretPosition(0);
        }
    }

    /** A scrollable drawing surface. Call redraw() with a painter. */
    public static class DrawPane extends JScrollPane {
        private final Surface surface = new Surface();

        public DrawPane(int height) {
            setViewportView(surface);
            setBorder(new javax.swing.border.LineBorder(Theme.BORDER, 1, true));
            setPreferredSize(new Dimension(400, height));
        }

        public void redraw(int w, int h, Consumer<Graphics2D> painter) {
            surface.painter = painter;
            surface.setPreferredSize(new Dimension(w, h));
            surface.revalidate();
            surface.repaint();
        }

        private static class Surface extends JPanel {
            Consumer<Graphics2D> painter;
            Surface() { setBackground(Theme.INSET); }
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (painter == null) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                painter.accept(g2);
                g2.dispose();
            }
        }
    }

    /** Add/remove any number of rows (no fixed limit). Optional random filler. */
    public static class DynamicTable extends JPanel {
        private final String[] columns;
        private final Function<Integer, List<Object[]>> randomRows;
        private final JTextField[] entries;
        private final DefaultTableModel model;
        private final JTable table;
        private final JLabel countLbl = new JLabel("Rows: 0");
        private JTextField nField;

        public DynamicTable(String title, String[] columns, Function<Integer, List<Object[]>> randomRows, int height) {
            this.columns = columns;
            this.randomRows = randomRows;
            setLayout(new BorderLayout(4, 4));
            setOpaque(false);
            setBorder(titled(title));

            JPanel top = row();
            entries = new JTextField[columns.length];
            for (int i = 0; i < columns.length; i++) {
                top.add(label(columns[i]));
                entries[i] = field("", 6);
                entries[i].addActionListener(ev -> addFromEntries());
                top.add(entries[i]);
            }
            top.add(softButton("+ Add", this::addFromEntries));
            add(top, BorderLayout.NORTH);

            model = new DefaultTableModel(columns, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            table = new JTable(model) {
                @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                    Component c = super.prepareRenderer(r, row, col);
                    if (!isRowSelected(row)) c.setBackground(row % 2 == 0 ? Theme.CARD : Theme.STRIPE);
                    return c;
                }
            };
            table.setRowHeight(22);
            table.setShowVerticalLines(false);
            table.setIntercellSpacing(new Dimension(0, 1));
            table.setFillsViewportHeight(true);
            ((DefaultTableCellRenderer) table.getDefaultRenderer(Object.class)).setHorizontalAlignment(SwingConstants.CENTER);
            JTableHeader th = table.getTableHeader();
            th.setReorderingAllowed(false);
            th.setPreferredSize(new Dimension(10, 24));
            th.setDefaultRenderer(new DefaultTableCellRenderer() {
                { setHorizontalAlignment(SwingConstants.CENTER); }
                @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                    super.getTableCellRendererComponent(t, v, false, false, r, c);
                    setBackground(Theme.PRIMARY);
                    setForeground(Color.WHITE);
                    setFont(Theme.BOLD);
                    setBorder(new EmptyBorder(2, 6, 2, 6));
                    return this;
                }
            });
            JScrollPane sp = new JScrollPane(table);
            sp.setColumnHeaderView(th);
            sp.setPreferredSize(new Dimension(300, 23 * height + 26));
            add(sp, BorderLayout.CENTER);

            JPanel bot = row();
            bot.add(softButton("Remove selected", this::removeSelected));
            bot.add(softButton("Clear all", this::clear));
            if (randomRows != null) {
                bot.add(label("  Random rows:"));
                nField = field("", 5);
                bot.add(nField);
                bot.add(softButton("Generate", this::generate));
            }
            countLbl.setForeground(Theme.MUTED);
            bot.add(countLbl);
            add(bot, BorderLayout.SOUTH);
        }

        private void refresh() { countLbl.setText("   Rows: " + model.getRowCount()); }

        public void addRow(Object... values) {
            String[] s = new String[values.length];
            for (int i = 0; i < s.length; i++) s[i] = String.valueOf(values[i]);
            model.addRow(s);
            refresh();
        }

        private void addFromEntries() {
            String[] vals = new String[entries.length];
            boolean any = false;
            for (int i = 0; i < vals.length; i++) {
                vals[i] = entries[i].getText().trim();
                any |= !vals[i].isEmpty();
            }
            if (!any) {
                JOptionPane.showMessageDialog(this, "Type at least one value before adding a row.",
                        "Empty row", JOptionPane.WARNING_MESSAGE);
                return;
            }
            addRow((Object[]) vals);
            for (JTextField f : entries) f.setText("");
        }

        private void removeSelected() {
            int[] sel = table.getSelectedRows();
            for (int i = sel.length - 1; i >= 0; i--) model.removeRow(sel[i]);
            refresh();
        }

        public void clear() {
            model.setRowCount(0);
            refresh();
        }

        public void setRows(List<Object[]> rows) {
            clear();
            for (Object[] r : rows) addRow(r);
        }

        public List<String[]> getRows() {
            List<String[]> out = new ArrayList<>();
            for (int i = 0; i < model.getRowCount(); i++) {
                String[] r = new String[columns.length];
                for (int j = 0; j < r.length; j++) {
                    Object v = model.getValueAt(i, j);
                    r[j] = v == null ? "" : v.toString();
                }
                out.add(r);
            }
            return out;
        }

        private void generate() {
            int n = toInt(nField.getText(), "Number of random rows", 1);
            if (n > 20000 && JOptionPane.showConfirmDialog(this, "Generate " + n + " rows?", "Large dataset",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            clear();
            for (Object[] r : randomRows.apply(n)) addRow(r);
        }
    }

    // ---------------------------------------------------------------- Gantt
    private static final Color[] PALETTE = {
        new Color(0x6C72F5), new Color(0x14B8A6), new Color(0xF59E0B), new Color(0xF4577A), new Color(0x38A9F0),
        new Color(0xA66CFF), new Color(0x7CB518), new Color(0xFB7C3C), new Color(0x0FA3B1), new Color(0xE05CB0)
    };

    public static Color colourFor(String label) {
        int sum = 0;
        for (char c : label.toCharArray()) sum += c;
        return PALETTE[sum % PALETTE.length];
    }

    public static class GanttChart extends JPanel {
        private final DrawPane pane = new DrawPane(82);

        public GanttChart() {
            setLayout(new BorderLayout());
            setOpaque(false);
            setBorder(titled("Gantt chart"));
            add(pane, BorderLayout.CENTER);
        }

        public void draw(List<Seg> segs) {
            if (segs.isEmpty()) { pane.redraw(10, 10, g -> {}); return; }
            int t0 = segs.get(0).s, t1 = segs.get(segs.size() - 1).e;
            double unit = Math.max(3, Math.min(40, 900.0 / Math.max(1, t1 - t0)));
            int x0 = 10, y = 10, h = 34;
            int width = (int) (x0 + (t1 - t0) * unit + 25);
            pane.redraw(width, y + h + 24, g -> {
                g.setFont(Theme.SMALL);
                double lastTick = -100, xb = x0;
                for (Seg sg : segs) {
                    double xa = x0 + (sg.s - t0) * unit;
                    xb = x0 + (sg.e - t0) * unit;
                    boolean idle = sg.label.equals("Idle");
                    g.setColor(idle ? Theme.GRID : colourFor(sg.label));
                    g.fillRect((int) xa, y, (int) (xb - xa), h);
                    g.setColor(Theme.INSET);
                    g.drawRect((int) xa, y, (int) (xb - xa), h);
                    g.setColor(idle ? Theme.MUTED : Color.WHITE);
                    if (xb - xa >= 22) {
                        g.setFont(Theme.BOLD);
                        FontMetrics fm = g.getFontMetrics();
                        g.drawString(sg.label, (int) ((xa + xb) / 2 - fm.stringWidth(sg.label) / 2.0), y + h / 2 + 4);
                    }
                    g.setFont(Theme.SMALL);
                    g.setColor(Theme.MUTED);
                    if (xa - lastTick >= 24) {
                        g.drawString(String.valueOf(sg.s), (int) xa, y + h + 14);
                        lastTick = xa;
                    }
                }
                g.drawString(String.valueOf(t1), (int) xb, y + h + 14);
            });
        }
    }
}