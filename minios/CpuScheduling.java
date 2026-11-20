package minios;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static minios.Common.*;

/** Module 1 - CPU Scheduling: FCFS, SJF, SRTF, Priority (both), Round Robin (supports decimals). */
public class CpuScheduling extends JPanel {

    private static final double EPS = 1e-9;

    // ------------------------------------------------------------------ logic
    public static class Proc {
        public final String pid;
        public final double at, bt;
        public final int pr, idx;

        public Proc(String pid, double at, double bt, int pr, int idx) {
            this.pid = pid;
            this.at = at;
            this.bt = bt;
            this.pr = pr;
            this.idx = idx;
        }
    }

    public static class Row {
        public String pid;
        public double at, bt, ct, tat, wt, rt;
        public int pr;
    }

    public static class DoubleSeg {
        public final String label;
        public final double s, e;

        public DoubleSeg(String label, double s, double e) {
            this.label = label;
            this.s = s;
            this.e = e;
        }
    }

    private static boolean eq(double a, double b) {
        return Math.abs(a - b) <= EPS;
    }

    private static int cmp(double a, double b) {
        return eq(a, b) ? 0 : Double.compare(a, b);
    }

    private static double clean(double v) {
        return Math.abs(v) < EPS ? 0.0 : Math.round(v * 1_000_000.0) / 1_000_000.0;
    }

    private static String fmtNum(double v) {
        double c = clean(v);
        if (eq(c, Math.rint(c))) {
            return String.valueOf((long) Math.rint(c));
        }
        String s = String.format(Locale.US, "%.4f", c);
        return s.replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static double toDouble(String raw, String fieldName, double minVal) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new InputError(fieldName + " is required.");
        }
        try {
            double val = Double.parseDouble(raw.trim());
            if (Double.isNaN(val) || Double.isInfinite(val) || val < minVal) {
                throw new InputError(fieldName + " must be >= " + fmtNum(minVal) + ".");
            }
            return val;
        } catch (NumberFormatException ex) {
            throw new InputError(fieldName + " must be a valid number.");
        }
    }

    private static void add(List<DoubleSeg> segs, String label, double s, double e) {
        if (e - s <= EPS) return;
        if (!segs.isEmpty()) {
            DoubleSeg last = segs.get(segs.size() - 1);
            if (last.label.equals(label) && eq(last.e, s)) {
                segs.set(segs.size() - 1, new DoubleSeg(label, last.s, e));
                return;
            }
        }
        segs.add(new DoubleSeg(label, s, e));
    }

    private static List<DoubleSeg> nonPreemptive(List<Proc> procs, Comparator<Proc> key) {
        List<Proc> pending = new ArrayList<>(procs);
        pending.sort((a, b) -> {
            int c = cmp(a.at, b.at);
            return c != 0 ? c : Integer.compare(a.idx, b.idx);
        });
        List<DoubleSeg> segs = new ArrayList<>();
        double time = 0.0;
        while (!pending.isEmpty()) {
            Proc best = null;
            for (Proc p : pending) {
                if (p.at <= time + EPS && (best == null || key.compare(p, best) < 0)) {
                    best = p;
                }
            }
            if (best == null) {
                double nxt = pending.get(0).at;
                add(segs, "Idle", time, nxt);
                time = nxt;
                continue;
            }
            pending.remove(best);
            add(segs, best.pid, time, time + best.bt);
            time += best.bt;
        }
        return segs;
    }

    private static List<DoubleSeg> preemptive(List<Proc> procs, Function<double[], Comparator<Proc>> mkKey) {
        double[] rem = new double[procs.size()];
        for (Proc p : procs) rem[p.idx] = p.bt;
        Comparator<Proc> key = mkKey.apply(rem);
        List<DoubleSeg> segs = new ArrayList<>();
        double time = 0.0;
        int left = procs.size();

        while (left > 0) {
            Proc best = null;
            for (Proc p : procs) {
                if (p.at <= time + EPS && rem[p.idx] > EPS && (best == null || key.compare(p, best) < 0)) {
                    best = p;
                }
            }
            if (best == null) {
                double nxt = Double.POSITIVE_INFINITY;
                for (Proc p : procs) {
                    if (rem[p.idx] > EPS) nxt = Math.min(nxt, p.at);
                }
                add(segs, "Idle", time, nxt);
                time = nxt;
                continue;
            }

            double run = rem[best.idx];
            double nextArrival = Double.POSITIVE_INFINITY;
            for (Proc q : procs) {
                if (q.at > time + EPS) {
                    nextArrival = Math.min(nextArrival, q.at);
                }
            }
            if (!Double.isInfinite(nextArrival)) {
                run = Math.min(run, nextArrival - time);
            }

            add(segs, best.pid, time, time + run);
            time += run;
            rem[best.idx] -= run;
            if (rem[best.idx] <= EPS) {
                rem[best.idx] = 0.0;
                left--;
            }
        }
        return segs;
    }

    public static List<DoubleSeg> roundRobin(List<Proc> procs, double quantum) {
        List<Proc> order = new ArrayList<>(procs);
        order.sort((a, b) -> {
            int c = cmp(a.at, b.at);
            return c != 0 ? c : Integer.compare(a.idx, b.idx);
        });
        int n = order.size(), i = 0, done = 0;
        double time = 0.0;
        double[] rem = new double[n];
        for (Proc p : order) rem[p.idx] = p.bt;

        Deque<Proc> queue = new ArrayDeque<>();
        List<DoubleSeg> segs = new ArrayList<>();

        while (done < n) {
            while (i < n && order.get(i).at <= time + EPS) {
                queue.addLast(order.get(i++));
            }
            if (queue.isEmpty()) {
                double nxt = order.get(i).at;
                add(segs, "Idle", time, nxt);
                time = nxt;
                continue;
            }
            Proc p = queue.pollFirst();
            double run = Math.min(quantum, rem[p.idx]);
            add(segs, p.pid, time, time + run);
            time += run;
            rem[p.idx] -= run;

            while (i < n && order.get(i).at <= time + EPS) {
                queue.addLast(order.get(i++));
            }
            if (rem[p.idx] > EPS) {
                queue.addLast(p);
            } else {
                rem[p.idx] = 0.0;
                done++;
            }
        }
        return segs;
    }

    public static final Map<String, BiFunction<List<Proc>, Double, List<DoubleSeg>>> ALGORITHMS = new LinkedHashMap<>();
    static {
        ALGORITHMS.put("FCFS", (ps, q) -> nonPreemptive(ps, (a, b) -> {
            int c = cmp(a.at, b.at);
            return c != 0 ? c : Integer.compare(a.idx, b.idx);
        }));
        ALGORITHMS.put("SJF (non-preemptive)", (ps, q) -> nonPreemptive(ps, (a, b) -> {
            int c = cmp(a.bt, b.bt);
            if (c != 0) return c;
            c = cmp(a.at, b.at);
            return c != 0 ? c : Integer.compare(a.idx, b.idx);
        }));
        ALGORITHMS.put("SRTF (preemptive SJF)", (ps, q) -> preemptive(ps, rem -> (a, b) -> {
            int c = cmp(rem[a.idx], rem[b.idx]);
            if (c != 0) return c;
            c = cmp(a.at, b.at);
            return c != 0 ? c : Integer.compare(a.idx, b.idx);
        }));
        ALGORITHMS.put("Priority (non-preemptive)", (ps, q) -> nonPreemptive(ps, (a, b) -> {
            int c = Integer.compare(a.pr, b.pr);
            if (c != 0) return c;
            c = cmp(a.at, b.at);
            return c != 0 ? c : Integer.compare(a.idx, b.idx);
        }));
        ALGORITHMS.put("Priority (preemptive)", (ps, q) -> preemptive(ps, rem -> (a, b) -> {
            int c = Integer.compare(a.pr, b.pr);
            if (c != 0) return c;
            c = cmp(a.at, b.at);
            return c != 0 ? c : Integer.compare(a.idx, b.idx);
        }));
        ALGORITHMS.put("Round Robin", CpuScheduling::roundRobin);
    }

    public static final Set<String> NEEDS_PRIORITY =
            new HashSet<>(Arrays.asList("Priority (non-preemptive)", "Priority (preemptive)"));

    public static List<Row> metrics(List<Proc> procs, List<DoubleSeg> segs) {
        Map<String, Double> ct = new HashMap<>(), first = new HashMap<>();
        for (DoubleSeg sg : segs) {
            if (!sg.label.equals("Idle")) {
                first.putIfAbsent(sg.label, sg.s);
                ct.put(sg.label, sg.e);
            }
        }
        List<Row> rows = new ArrayList<>();
        for (Proc p : procs) {
            Row r = new Row();
            double c = ct.get(p.pid);
            r.pid = p.pid;
            r.at = clean(p.at);
            r.bt = clean(p.bt);
            r.pr = p.pr;
            r.ct = clean(c);
            r.tat = clean(c - p.at);
            r.wt = clean(r.tat - p.bt);
            r.rt = clean(first.get(p.pid) - p.at);
            rows.add(r);
        }
        return rows;
    }

    public static double average(List<Row> rows, Function<Row, Double> key) {
        double sum = 0.0;
        for (Row r : rows) sum += key.apply(r);
        return sum / rows.size();
    }

    private static List<Seg> toGanttSegs(List<DoubleSeg> dsegs) {
        // Scale fractional units so GanttChart (int-based) preserves exact proportions
        int scale = 1;
        for (DoubleSeg ds : dsegs) {
            for (double v : new double[]{ds.s, ds.e}) {
                if (!eq(v, Math.rint(v))) {
                    if (eq(v * 10, Math.rint(v * 10))) scale = Math.max(scale, 10);
                    else scale = Math.max(scale, 100);
                }
            }
        }
        List<Seg> out = new ArrayList<>(dsegs.size());
        for (DoubleSeg ds : dsegs) {
            int s = (int) Math.round(ds.s * scale);
            int e = (int) Math.round(ds.e * scale);
            if (e <= s) e = s + 1;
            out.add(new Seg(ds.label, s, e));
        }
        return out;
    }

    // --------------------------------------------------------------------- GUI
    private final DynamicTable table;
    private final JComboBox<String> algo = combo(ALGORITHMS.keySet().toArray(new String[0]));
    private final JTextField quantum = field("2", 6);
    private final GanttChart gantt = new GanttChart();
    private final OutputBox out = new OutputBox();
    private boolean comparingMode = false;
    private boolean liveInitialized = false;

    public CpuScheduling() {
        setLayout(new BorderLayout(4, 4));
        pad(this);
        table = new DynamicTable("Processes (Priority: lower number = higher priority; decimals supported)",
                new String[]{"PID", "Arrival", "Burst", "Priority"}, CpuScheduling::randomRows, 4);

        JPanel opt = row();
        opt.add(label("Algorithm:"));
        opt.add(algo);
        opt.add(label("   Time quantum (Round Robin):"));
        opt.add(quantum);
        opt.add(button("Run", () -> {
            comparingMode = false;
            run();
        }));
        opt.add(softButton("Compare all algorithms", () -> {
            comparingMode = true;
            compare();
        }));

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(clearRow(this));
        north.add(table);
        north.add(opt);
        north.add(gantt);
        add(north, BorderLayout.NORTH);
        add(out, BorderLayout.CENTER);

        attachLiveRecalculation(north);
        liveInitialized = true;
        SwingUtilities.invokeLater(this::safeRecalculate);
    }

    private void attachLiveRecalculation(Container root) {
        algo.addActionListener(e -> {
            comparingMode = false;
            safeRecalculate();
        });

        quantum.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { safeRecalculate(); }
            @Override public void removeUpdate(DocumentEvent e) { safeRecalculate(); }
            @Override public void changedUpdate(DocumentEvent e) { safeRecalculate(); }
        });

        hookDescendants(root);
    }

    private void hookDescendants(Component comp) {
        if (comp instanceof JTable) {
            JTable jt = (JTable) comp;
            jt.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
            jt.getModel().addTableModelListener(e -> safeRecalculate());
        } else if (comp instanceof AbstractButton && comp != algo) {
            ((AbstractButton) comp).addActionListener(e -> SwingUtilities.invokeLater(this::safeRecalculate));
        } else if (comp instanceof JSpinner) {
            ((JSpinner) comp).addChangeListener(e -> SwingUtilities.invokeLater(this::safeRecalculate));
        }

        // Catch cell edits as soon as user clicks anywhere outside the table
        comp.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                commitActiveTableEdits(CpuScheduling.this);
            }
        });

        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                hookDescendants(child);
            }
        }
    }

    private void commitActiveTableEdits(Component comp) {
        if (comp instanceof JTable) {
            JTable jt = (JTable) comp;
            if (jt.isEditing() && jt.getCellEditor() != null) {
                jt.getCellEditor().stopCellEditing();
            }
        } else if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                commitActiveTableEdits(child);
            }
        }
    }

    private void safeRecalculate() {
        if (!liveInitialized) return;
        try {
            if (comparingMode) {
                compare();
            } else {
                run();
            }
        } catch (InputError ex) {
            out.clear();
            out.write("Waiting for valid input: " + ex.getMessage());
        } catch (Exception ignored) {
        }
    }

    private static List<Object[]> randomRows(int n) {
        Random rnd = new Random();
        int span = Math.max(10, n);
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double at = rnd.nextInt(span + 1) + (rnd.nextBoolean() ? 0.5 : 0.0);
            double bt = 1 + rnd.nextInt(12) + (rnd.nextBoolean() ? 0.5 : 0.0);
            rows.add(new Object[]{"P" + (i + 1), fmtNum(at), fmtNum(bt), 1 + rnd.nextInt(5)});
        }
        return rows;
    }

    private List<Proc> getProcs(boolean needPriority) {
        commitActiveTableEdits(this);
        List<String[]> rows = table.getRows();
        if (rows.isEmpty()) throw new InputError("Add at least one process.");
        List<Proc> procs = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            String[] r = rows.get(i);
            String pid = r[0].isEmpty() ? "P" + (i + 1) : r[0];
            if (pid.equalsIgnoreCase("idle")) throw new InputError("'Idle' is a reserved PID.");
            if (!seen.add(pid)) throw new InputError("Duplicate PID '" + pid + "'.");
            double at = toDouble(r[1], "Row " + (i + 1) + " arrival time", 0.0);
            double bt = toDouble(r[2], "Row " + (i + 1) + " burst time", 1e-6);
            int pr = (needPriority || !r[3].trim().isEmpty()) ? toInt(r[3], "Row " + (i + 1) + " priority", 0) : 0;
            procs.add(new Proc(pid, at, bt, pr, i));
        }
        return procs;
    }

    private void run() {
        String name = (String) algo.getSelectedItem();
        List<Proc> procs = getProcs(NEEDS_PRIORITY.contains(name));
        double q = name.equals("Round Robin") ? toDouble(quantum.getText(), "Time quantum", 1e-6) : 1.0;
        List<DoubleSeg> segs = ALGORITHMS.get(name).apply(procs, q);
        List<Row> rows = metrics(procs, segs);
        gantt.draw(toGanttSegs(segs));
        out.clear();
        out.write("Algorithm: " + name + (name.equals("Round Robin") ? " (Quantum = " + fmtNum(q) + ")" : ""));
        out.write("Processes: " + procs.size() + "\n");
        List<Object[]> data = new ArrayList<>();
        for (Row r : rows) {
            data.add(new Object[]{
                    r.pid,
                    fmtNum(r.at),
                    fmtNum(r.bt),
                    r.pr,
                    fmtNum(r.ct),
                    fmtNum(r.tat),
                    fmtNum(r.wt),
                    fmtNum(r.rt)
            });
        }
        out.write(fmtTable(new String[]{"PID", "Arrival", "Burst", "Prio", "Completion", "Turnaround", "Waiting", "Response"}, data));
        double busy = 0.0;
        for (DoubleSeg s : segs) if (!s.label.equals("Idle")) busy += (s.e - s.s);
        double totalTime = segs.get(segs.size() - 1).e;
        out.write(String.format("%nAverage waiting time    : %.2f", average(rows, r -> r.wt)));
        out.write(String.format("Average turnaround time : %.2f", average(rows, r -> r.tat)));
        out.write(String.format("Average response time   : %.2f", average(rows, r -> r.rt)));
        out.write(String.format("CPU utilisation         : %.1f%%", totalTime > 0 ? (100.0 * busy / totalTime) : 0.0));

        List<String> labels = new ArrayList<>();
        for (int i = 0; i < Math.min(200, segs.size()); i++) {
            DoubleSeg s = segs.get(i);
            labels.add(s.label + " [" + fmtNum(s.s) + "-" + fmtNum(s.e) + "]");
        }
        out.write("\nScheduling order: " + join(" -> ", labels) + (segs.size() > 200 ? " ..." : ""));
    }

    private void compare() {
        List<Proc> procs = getProcs(false);
        double q = quantum.getText().trim().isEmpty() ? 2.0 : toDouble(quantum.getText(), "Time quantum", 1e-6);
        out.clear();
        out.write("Comparison (priority algorithms treat blank priority as 0; RR quantum = " + fmtNum(q) + ")\n");
        List<Object[]> data = new ArrayList<>();
        for (Map.Entry<String, BiFunction<List<Proc>, Double, List<DoubleSeg>>> e : ALGORITHMS.entrySet()) {
            List<Row> rows = metrics(procs, e.getValue().apply(procs, q));
            data.add(new Object[]{
                    e.getKey(),
                    String.format("%.2f", average(rows, r -> r.wt)),
                    String.format("%.2f", average(rows, r -> r.tat)),
                    String.format("%.2f", average(rows, r -> r.rt))
            });
        }
        out.write(fmtTable(new String[]{"Algorithm", "Avg waiting", "Avg turnaround", "Avg response"}, data));
    }
}