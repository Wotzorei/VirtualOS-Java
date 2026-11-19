package minios;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import static minios.Common.*;

/** Module 8 - Multiprogramming: processes sharing memory, one CPU and I/O devices. */
public class Multiprogramming extends JPanel {

    // ------------------------------------------------------------------ logic
    public static class Prog {
        public final String pid;
        public final int arr, mem, cpu, idx;
        public int io;
        int phase1, phase2, rem, stage;
        public Prog(String pid, int arr, int mem, int cpu, int io, int idx) {
            this.pid = pid; this.arr = arr; this.mem = mem; this.cpu = cpu; this.io = io; this.idx = idx;
        }
        Prog copy() { return new Prog(pid, arr, mem, cpu, io, idx); }
    }

    public static class Result {
        public List<Object[]> per = new ArrayList<>();
        public List<Object[]> snaps = new ArrayList<>();
        public List<Object[]> log = new ArrayList<>();   // {t, message}
        public int total, peak;
        public double cpuUtil, memUtil, throughput;
    }

    private static String names(Collection<Prog> c) {
        List<String> s = new ArrayList<>();
        for (Prog p : c) s.add(p.pid);
        return s.isEmpty() ? "-" : join(" ", s);
    }

    /** Each process: CPU (first half) -> I/O (parallel, own device) -> CPU (second half).
     *  Jobs wait in a FIFO job queue until enough memory is free; CPU is Round Robin. */
    public static Result simulate(List<Prog> procs, int totalMem, int quantum) {
        return simulate(procs, totalMem, quantum, 500000);
    }

    public static Result simulate(List<Prog> procs, int totalMem, int quantum, int maxTicks) {
        Map<String, Prog> P = new LinkedHashMap<>();
        for (Prog p : procs) P.put(p.pid, p.copy());
        for (Prog p : P.values()) {
            int first = p.io > 0 ? p.cpu - p.cpu / 2 : p.cpu;
            p.phase1 = first; p.phase2 = p.cpu - first; p.rem = first; p.stage = 1;
            if (p.phase2 == 0) p.io = 0;
        }
        List<Prog> order = new ArrayList<>(P.values());
        order.sort(Comparator.comparingInt((Prog p) -> p.arr).thenComparingInt(p -> p.idx));
        int n = order.size(), i = 0, t = 0;
        Deque<Prog> jobq = new ArrayDeque<>(), ready = new ArrayDeque<>();
        Map<String, Integer> blocked = new LinkedHashMap<>();
        Prog running = null;
        int sliceLeft = 0, memUsed = 0, busy = 0, done = 0, peak = 0;
        long memArea = 0;
        Result res = new Result();
        Map<String, Integer> completion = new HashMap<>();
        Object[] last = null;
        while (done < n) {
            if (t > maxTicks)
                throw new InputError("Simulation exceeded " + maxTicks + " time units - use smaller CPU/I-O times.");
            while (i < n && order.get(i).arr <= t) {
                Prog p = order.get(i);
                jobq.addLast(p);
                res.log.add(new Object[]{t, p.pid + " arrives (needs " + p.mem + " memory)"});
                i++;
            }
            while (!jobq.isEmpty() && memUsed + jobq.peekFirst().mem <= totalMem) {
                Prog p = jobq.pollFirst();
                memUsed += p.mem;
                ready.addLast(p);
                res.log.add(new Object[]{t, p.pid + " loaded into memory (" + memUsed + "/" + totalMem + " used)"});
            }
            if (running == null && !ready.isEmpty()) {
                running = ready.pollFirst();
                sliceLeft = quantum;
            }
            Object[] state = {running != null ? running.pid : "-", names(ready),
                    blocked.isEmpty() ? "-" : join(" ", blocked.keySet()), names(jobq), memUsed};
            if (!Arrays.equals(state, last)) {
                Object[] snap = new Object[6];
                snap[0] = t;
                System.arraycopy(state, 0, snap, 1, 5);
                res.snaps.add(snap);
                last = state;
            }
            peak = Math.max(peak, ready.size() + blocked.size() + (running != null ? 1 : 0));
            if (running == null && ready.isEmpty() && blocked.isEmpty()) {
                if (i < n) {
                    t = order.get(i).arr;
                    continue;
                }
                break;
            }
            if (running != null) {
                busy++; running.rem--; sliceLeft--;
            }
            memArea += memUsed;
            List<String> wake = new ArrayList<>();
            for (String pid : new ArrayList<>(blocked.keySet())) {
                int v = blocked.get(pid) - 1;
                blocked.put(pid, v);
                if (v == 0) wake.add(pid);
            }
            t++;
            for (String pid : wake) {
                blocked.remove(pid);
                Prog p = P.get(pid);
                p.stage = 2; p.rem = p.phase2;
                ready.addLast(p);
                res.log.add(new Object[]{t, pid + " finished I/O, back to ready queue"});
            }
            if (running != null) {
                if (running.rem == 0) {
                    if (running.stage == 1 && running.io > 0) {
                        blocked.put(running.pid, running.io);
                        res.log.add(new Object[]{t, running.pid + " starts I/O (" + running.io + " units)"});
                    } else {
                        completion.put(running.pid, t);
                        memUsed -= running.mem;
                        done++;
                        res.log.add(new Object[]{t, running.pid + " terminates, frees " + running.mem + " memory"});
                    }
                    running = null;
                } else if (sliceLeft == 0) {
                    ready.addLast(running);
                    running = null;
                }
            }
        }
        int total = Math.max(t, 1);
        for (Prog p : order) {
            int ct = completion.get(p.pid);
            int tat = ct - p.arr;
            res.per.add(new Object[]{p.pid, p.arr, p.mem, p.cpu, p.io, ct, tat, tat - p.cpu - p.io});
        }
        res.total = total;
        res.cpuUtil = 100.0 * busy / total;
        res.memUtil = 100.0 * memArea / ((double) total * totalMem);
        res.throughput = (double) n / total;
        res.peak = peak;
        return res;
    }

    // --------------------------------------------------------------------- GUI
    private final JTextField mem = field("", 8), q = field("", 6);
    private final DynamicTable table;
    private final OutputBox out = new OutputBox();

    public Multiprogramming() {
        setLayout(new BorderLayout(4, 4));
        pad(this);
        JPanel cfg = row();
        cfg.add(label("Total memory (MB):")); cfg.add(mem);
        cfg.add(label("  CPU time quantum:")); cfg.add(q);
        cfg.add(button("Run simulation", this::run));
        table = new DynamicTable("Programs (I/O time 0 = pure CPU job)",
                new String[]{"PID", "Arrival", "Memory", "CPU time", "I/O time"}, Multiprogramming::rand, 4);
        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(clearRow(this)); north.add(cfg); north.add(table);
        add(north, BorderLayout.NORTH);
        add(out, BorderLayout.CENTER);
    }

    private static List<Object[]> rand(int n) {
        Random rnd = new Random();
        int[] ios = {0, 2, 4, 6};
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < n; i++)
            rows.add(new Object[]{"P" + (i + 1), rnd.nextInt(n + 1), 50 + rnd.nextInt(451), 2 + rnd.nextInt(9), ios[rnd.nextInt(4)]});
        return rows;
    }

    private void run() {
        int totalMem = toInt(mem.getText(), "Total memory", 1);
        int quantum = toInt(q.getText(), "Time quantum", 1);
        List<String[]> rows = table.getRows();
        if (rows.isEmpty()) throw new InputError("Add at least one program.");
        List<Prog> procs = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            String[] r = rows.get(i);
            String pid = r[0].isEmpty() ? "P" + (i + 1) : r[0];
            if (!seen.add(pid)) throw new InputError("Duplicate PID '" + pid + "'.");
            int m = toInt(r[2], "Row " + (i + 1) + " memory", 1);
            if (m > totalMem) throw new InputError(pid + " needs " + m + " MB but the system only has " + totalMem + " MB.");
            procs.add(new Prog(pid, toInt(r[1], "Row " + (i + 1) + " arrival", 0), m,
                    toInt(r[3], "Row " + (i + 1) + " CPU time", 1), toInt(r[4], "Row " + (i + 1) + " I/O time", 0), i));
        }
        Result res = simulate(procs, totalMem, quantum);
        out.clear();
        out.write("Programs: " + procs.size() + " | memory: " + totalMem + " MB | quantum: " + quantum + "\n");
        out.write("SYSTEM UTILISATION");
        out.write("  Total time          : " + res.total);
        out.write(String.format("  CPU utilisation     : %.1f%%", res.cpuUtil));
        out.write(String.format("  Memory utilisation  : %.1f%% (time-averaged)", res.memUtil));
        out.write(String.format("  Throughput          : %.3f programs / time unit", res.throughput));
        out.write("  Peak degree of multiprogramming: " + res.peak + " programs in memory\n");
        out.write(fmtTable(new String[]{"PID", "Arrival", "Mem", "CPU", "I/O", "Finish", "Turnaround", "Waiting"}, res.per));
        out.write("\nSYSTEM STATUS TIMELINE (printed whenever the state changes)");
        out.write(fmtTable(new String[]{"Time", "Running", "Ready queue", "Blocked (I/O)", "Waiting for memory", "Mem used"},
                res.snaps.subList(0, Math.min(300, res.snaps.size()))));
        if (res.snaps.size() > 300) out.write("... " + (res.snaps.size() - 300) + " more rows not shown");
        out.write("\nEVENT LOG");
        for (int i = 0; i < Math.min(400, res.log.size()); i++)
            out.write(String.format("  t=%-5s %s", res.log.get(i)[0], res.log.get(i)[1]));
        if (res.log.size() > 400) out.write("  ... " + (res.log.size() - 400) + " more events not shown");
    }
}
