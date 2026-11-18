package minios;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static minios.Common.*;

/** Module 1 - CPU Scheduling: FCFS, SJF, SRTF, Priority (both), Round Robin. */
public class CpuScheduling extends JPanel {

    // ------------------------------------------------------------------ logic
    public static class Proc {
        public final String pid;
        public final int at, bt, pr, idx;
        public Proc(String pid, int at, int bt, int pr, int idx) {
            this.pid = pid; this.at = at; this.bt = bt; this.pr = pr; this.idx = idx;
        }
    }

    public static class Row {
        public String pid;
        public int at, bt, pr, ct, tat, wt, rt;
    }

    private static void add(List<Seg> segs, String label, int s, int e) {
        if (e <= s) return;
        if (!segs.isEmpty()) {
            Seg last = segs.get(segs.size() - 1);
            if (last.label.equals(label) && last.e == s) {
                segs.set(segs.size() - 1, new Seg(label, last.s, e));
                return;
            }
        }
        segs.add(new Seg(label, s, e));
    }

    private static List<Seg> nonPreemptive(List<Proc> procs, Comparator<Proc> key) {
        List<Proc> pending = new ArrayList<>(procs);
        pending.sort(Comparator.comparingInt((Proc p) -> p.at).thenComparingInt(p -> p.idx));
        List<Seg> segs = new ArrayList<>();
        int time = 0;
        while (!pending.isEmpty()) {
            Proc best = null;
            for (Proc p : pending)
                if (p.at <= time && (best == null || key.compare(p, best) < 0)) best = p;
            if (best == null) {
                int nxt = pending.get(0).at;
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

    private static List<Seg> preemptive(List<Proc> procs, Function<int[], Comparator<Proc>> mkKey) {
        int[] rem = new int[procs.size()];
        for (Proc p : procs) rem[p.idx] = p.bt;
        Comparator<Proc> key = mkKey.apply(rem);
        List<Seg> segs = new ArrayList<>();
        int time = 0, left = procs.size();
        while (left > 0) {
            Proc best = null;
            for (Proc p : procs)
                if (p.at <= time && rem[p.idx] > 0 && (best == null || key.compare(p, best) < 0)) best = p;
            if (best == null) {
                int nxt = Integer.MAX_VALUE;
                for (Proc p : procs) if (rem[p.idx] > 0) nxt = Math.min(nxt, p.at);
                add(segs, "Idle", time, nxt);
                time = nxt;
                continue;
            }
            int run = rem[best.idx];
            int nextArrival = Integer.MAX_VALUE;
            for (Proc q : procs) if (q.at > time) nextArrival = Math.min(nextArrival, q.at);
            if (nextArrival != Integer.MAX_VALUE) run = Math.min(run, nextArrival - time);
            add(segs, best.pid, time, time + run);
            time += run;
            rem[best.idx] -= run;
            if (rem[best.idx] == 0) left--;
        }
        return segs;
    }

    public static List<Seg> roundRobin(List<Proc> procs, int quantum) {
        List<Proc> order = new ArrayList<>(procs);
        order.sort(Comparator.comparingInt((Proc p) -> p.at).thenComparingInt(p -> p.idx));
        int n = order.size(), i = 0, time = 0, done = 0;
        int[] rem = new int[n];
        for (Proc p : order) rem[p.idx] = p.bt;
        Deque<Proc> queue = new ArrayDeque<>();
        List<Seg> segs = new ArrayList<>();
        while (done < n) {
            while (i < n && order.get(i).at <= time) queue.addLast(order.get(i++));
            if (queue.isEmpty()) {
                int nxt = order.get(i).at;
                add(segs, "Idle", time, nxt);
                time = nxt;
                continue;
            }
            Proc p = queue.pollFirst();
            int run = Math.min(quantum, rem[p.idx]);
            add(segs, p.pid, time, time + run);
            time += run;
            rem[p.idx] -= run;
            while (i < n && order.get(i).at <= time) queue.addLast(order.get(i++));
            if (rem[p.idx] > 0) queue.addLast(p);
            else done++;
        }
        return segs;
    }

    public static final Map<String, BiFunction<List<Proc>, Integer, List<Seg>>> ALGORITHMS = new LinkedHashMap<>();
    static {
        ALGORITHMS.put("FCFS", (ps, q) -> nonPreemptive(ps,
                Comparator.comparingInt((Proc p) -> p.at).thenComparingInt(p -> p.idx)));
        ALGORITHMS.put("SJF (non-preemptive)", (ps, q) -> nonPreemptive(ps,
                Comparator.comparingInt((Proc p) -> p.bt).thenComparingInt(p -> p.at).thenComparingInt(p -> p.idx)));
        ALGORITHMS.put("SRTF (preemptive SJF)", (ps, q) -> preemptive(ps, rem ->
                Comparator.comparingInt((Proc p) -> rem[p.idx]).thenComparingInt(p -> p.at).thenComparingInt(p -> p.idx)));
        ALGORITHMS.put("Priority (non-preemptive)", (ps, q) -> nonPreemptive(ps,
                Comparator.comparingInt((Proc p) -> p.pr).thenComparingInt(p -> p.at).thenComparingInt(p -> p.idx)));
        ALGORITHMS.put("Priority (preemptive)", (ps, q) -> preemptive(ps, rem ->
                Comparator.comparingInt((Proc p) -> p.pr).thenComparingInt(p -> p.at).thenComparingInt(p -> p.idx)));
        ALGORITHMS.put("Round Robin", CpuScheduling::roundRobin);
    }
    public static final Set<String> NEEDS_PRIORITY =
            new HashSet<>(Arrays.asList("Priority (non-preemptive)", "Priority (preemptive)"));

    public static List<Row> metrics(List<Proc> procs, List<Seg> segs) {
        Map<String, Integer> ct = new HashMap<>(), first = new HashMap<>();
        for (Seg sg : segs) {
            if (!sg.label.equals("Idle")) {
                first.putIfAbsent(sg.label, sg.s);
                ct.put(sg.label, sg.e);
            }
        }
        List<Row> rows = new ArrayList<>();
        for (Proc p : procs) {
            Row r = new Row();
            int c = ct.get(p.pid);
            r.pid = p.pid; r.at = p.at; r.bt = p.bt; r.pr = p.pr; r.ct = c;
            r.tat = c - p.at;
            r.wt = r.tat - p.bt;
            r.rt = first.get(p.pid) - p.at;
            rows.add(r);
        }
        return rows;
    }

    public static double average(List<Row> rows, Function<Row, Integer> key) {
        double sum = 0;
        for (Row r : rows) sum += key.apply(r);
        return sum / rows.size();
    }

    // --------------------------------------------------------------------- GUI
    private final DynamicTable table;
    private final JComboBox<String> algo = combo(ALGORITHMS.keySet().toArray(new String[0]));
    private final JTextField quantum = field("3", 6);
    private final GanttChart gantt = new GanttChart();
    private final OutputBox out = new OutputBox();

    public CpuScheduling() {
        setLayout(new BorderLayout(4, 4));
        pad(this);
        table = new DynamicTable("Processes (Priority: lower number = higher priority)",
                new String[]{"PID", "Arrival", "Burst", "Priority"}, CpuScheduling::randomRows, 4);
        table.setRows(Arrays.asList(new Object[]{"P1", 0, 8, 3}, new Object[]{"P2", 1, 4, 1},
                new Object[]{"P3", 2, 9, 4}, new Object[]{"P4", 3, 5, 2}));

        JPanel opt = row();
        opt.add(label("Algorithm:"));
        opt.add(algo);
        opt.add(label("   Time quantum (Round Robin):"));
        opt.add(quantum);
        opt.add(button("Run", this::run));
        opt.add(softButton("Compare all algorithms", this::compare));

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(table);
        north.add(opt);
        north.add(gantt);
        add(north, BorderLayout.NORTH);
        add(out, BorderLayout.CENTER);
    }

    private static List<Object[]> randomRows(int n) {
        Random rnd = new Random();
        int span = Math.max(10, n);
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < n; i++)
            rows.add(new Object[]{"P" + (i + 1), rnd.nextInt(span + 1), 1 + rnd.nextInt(12), 1 + rnd.nextInt(5)});
        return rows;
    }

    private List<Proc> getProcs(boolean needPriority) {
        List<String[]> rows = table.getRows();
        if (rows.isEmpty()) throw new InputError("Add at least one process.");
        List<Proc> procs = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            String[] r = rows.get(i);
            String pid = r[0].isEmpty() ? "P" + (i + 1) : r[0];
            if (pid.equalsIgnoreCase("idle")) throw new InputError("'Idle' is a reserved PID.");
            if (!seen.add(pid)) throw new InputError("Duplicate PID '" + pid + "'.");
            int at = toInt(r[1], "Row " + (i + 1) + " arrival time", 0);
            int bt = toInt(r[2], "Row " + (i + 1) + " burst time", 1);
            int pr = (needPriority || !r[3].trim().isEmpty()) ? toInt(r[3], "Row " + (i + 1) + " priority", 0) : 0;
            procs.add(new Proc(pid, at, bt, pr, i));
        }
        return procs;
    }

    private void run() {
        String name = (String) algo.getSelectedItem();
        List<Proc> procs = getProcs(NEEDS_PRIORITY.contains(name));
        int q = name.equals("Round Robin") ? toInt(quantum.getText(), "Time quantum", 1) : 1;
        List<Seg> segs = ALGORITHMS.get(name).apply(procs, q);
        List<Row> rows = metrics(procs, segs);
        gantt.draw(segs);
        out.clear();
        out.write("Algorithm: " + name);
        out.write("Processes: " + procs.size() + "\n");
        List<Object[]> data = new ArrayList<>();
        for (Row r : rows) data.add(new Object[]{r.pid, r.at, r.bt, r.pr, r.ct, r.tat, r.wt, r.rt});
        out.write(fmtTable(new String[]{"PID", "Arrival", "Burst", "Prio", "Completion", "Turnaround", "Waiting", "Response"}, data));
        int busy = 0;
        for (Seg s : segs) if (!s.label.equals("Idle")) busy += s.e - s.s;
        out.write(String.format("%nAverage waiting time    : %.2f", average(rows, r -> r.wt)));
        out.write(String.format("Average turnaround time : %.2f", average(rows, r -> r.tat)));
        out.write(String.format("Average response time   : %.2f", average(rows, r -> r.rt)));
        out.write(String.format("CPU utilisation         : %.1f%%", 100.0 * busy / segs.get(segs.size() - 1).e));
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < Math.min(200, segs.size()); i++) labels.add(segs.get(i).label);
        out.write("\nScheduling order: " + join(" -> ", labels) + (segs.size() > 200 ? " ..." : ""));
    }

    private void compare() {
        List<Proc> procs = getProcs(false);
        int q = toInt(quantum.getText(), "Time quantum", 1);
        out.clear();
        out.write("Comparison (priority algorithms treat blank priority as 0)\n");
        List<Object[]> data = new ArrayList<>();
        for (Map.Entry<String, BiFunction<List<Proc>, Integer, List<Seg>>> e : ALGORITHMS.entrySet()) {
            List<Row> rows = metrics(procs, e.getValue().apply(procs, q));
            data.add(new Object[]{e.getKey(), String.format("%.2f", average(rows, r -> r.wt)),
                    String.format("%.2f", average(rows, r -> r.tat)), String.format("%.2f", average(rows, r -> r.rt))});
        }
        out.write(fmtTable(new String[]{"Algorithm", "Avg waiting", "Avg turnaround", "Avg response"}, data));
    }
}
