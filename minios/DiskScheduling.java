package minios;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import static minios.Common.*;

/** Module 7 - Disk Scheduling: FCFS, SSTF, SCAN, C-SCAN, LOOK, C-LOOK. */
public class DiskScheduling extends JPanel {
    public static final String[] ALGOS = {"FCFS", "SSTF", "SCAN", "C-SCAN", "LOOK", "C-LOOK"};
    public static final String UP = "Towards higher cylinders", DOWN = "Towards lower cylinders";

    // ------------------------------------------------------------------ logic
    public static class Plan {
        public final List<Integer> order, path;
        Plan(List<Integer> order, List<Integer> path) { this.order = order; this.path = path; }
    }

    private static void edge(List<Integer> path, int c) {
        if (path.get(path.size() - 1) != c) path.add(c);
    }

    private static List<Integer> rev(List<Integer> l) {
        List<Integer> r = new ArrayList<>(l);
        Collections.reverse(r);
        return r;
    }

    private static List<Integer> cat(List<Integer> a, List<Integer> b) {
        List<Integer> r = new ArrayList<>(a);
        r.addAll(b);
        return r;
    }

    /** Cylinders are 0..size-1. Jumps (C-SCAN / C-LOOK) are counted as head movement. */
    public static Plan schedule(String algo, int[] reqs, int head, int size, String direction) {
        int lo = 0, hi = size - 1;
        boolean up = direction.equals(UP);
        List<Integer> all = new ArrayList<>();
        for (int r : reqs) all.add(r);
        if (algo.equals("FCFS")) return new Plan(all, cat(Collections.singletonList(head), all));
        if (algo.equals("SSTF")) {
            List<Integer> rest = new ArrayList<>(all), order = new ArrayList<>();
            int cur = head;
            while (!rest.isEmpty()) {
                int bi = 0;
                for (int k = 1; k < rest.size(); k++)
                    if (Math.abs(rest.get(k) - cur) < Math.abs(rest.get(bi) - cur)) bi = k;
                int nxt = rest.remove(bi);
                order.add(nxt);
                cur = nxt;
            }
            return new Plan(order, cat(Collections.singletonList(head), order));
        }
        List<Integer> right = new ArrayList<>(), left = new ArrayList<>();
        for (int r : all) {
            if (up) { if (r >= head) right.add(r); else left.add(r); }
            else { if (r <= head) left.add(r); else right.add(r); }
        }
        Collections.sort(right);
        Collections.sort(left);
        List<Integer> path = new ArrayList<>();
        path.add(head);
        List<Integer> order;
        switch (algo) {
            case "SCAN":
                if (up) {
                    path.addAll(right); edge(path, hi); path.addAll(rev(left));
                    order = cat(right, rev(left));
                } else {
                    path.addAll(rev(left)); edge(path, lo); path.addAll(right);
                    order = cat(rev(left), right);
                }
                break;
            case "C-SCAN":
                if (up) {
                    path.addAll(right); edge(path, hi);
                    if (!left.isEmpty()) { edge(path, lo); path.addAll(left); }
                    order = cat(right, left);
                } else {
                    path.addAll(rev(left)); edge(path, lo);
                    if (!right.isEmpty()) { edge(path, hi); path.addAll(rev(right)); }
                    order = cat(rev(left), rev(right));
                }
                break;
            case "LOOK":
                order = up ? cat(right, rev(left)) : cat(rev(left), right);
                path.addAll(order);
                break;
            default: // C-LOOK
                order = up ? cat(right, left) : cat(rev(left), rev(right));
                path.addAll(order);
        }
        return new Plan(order, path);
    }

    public static long totalMovement(List<Integer> path) {
        long s = 0;
        for (int i = 0; i + 1 < path.size(); i++) s += Math.abs(path.get(i) - path.get(i + 1));
        return s;
    }

    // --------------------------------------------------------------------- GUI
    private final JTextField reqs = field("98 183 37 122 14 124 65 67", 50);
    private final JTextField rn = field("20", 6), head = field("53", 7), size = field("200", 7);
    private final JComboBox<String> dir = combo(new String[]{UP, DOWN});
    private final JComboBox<String> algo = combo(ALGOS);
    private final DrawPane plot = new DrawPane(230);
    private final OutputBox out = new OutputBox(9);

    public DiskScheduling() {
        setLayout(new BorderLayout(4, 4));
        pad(this);
        JPanel a = row();
        a.add(label("Request queue (cylinders):")); a.add(reqs);
        a.add(label("  Random: count")); a.add(rn);
        a.add(softButton("Generate", this::rand));
        JPanel b = row();
        b.add(label("Initial head:")); b.add(head);
        b.add(label("  Disk size (cylinders 0..N-1):")); b.add(size);
        b.add(dir); b.add(algo);
        b.add(button("Run", this::run));
        b.add(softButton("Compare all", this::compare));
        JPanel box = new JPanel(new BorderLayout());
        box.setBorder(titled("Head movement (x = cylinder, y = step)"));
        box.add(plot, BorderLayout.CENTER);
        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(a); north.add(b); north.add(box);
        add(north, BorderLayout.NORTH);
        add(out, BorderLayout.CENTER);
    }

    private void rand() {
        int n = toInt(rn.getText(), "Count", 1);
        int sz = toInt(size.getText(), "Disk size", 1);
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(' ');
            sb.append(rnd.nextInt(sz));
        }
        reqs.setText(sb.toString());
    }

    private Object[] read() {
        int sz = toInt(size.getText(), "Disk size", 1);
        int h = toInt(head.getText(), "Initial head position", 0, sz - 1);
        int[] r = parseIntList(reqs.getText(), "Request queue", 0, sz - 1);
        return new Object[]{r, h, sz};
    }

    private void plot(List<Integer> path, int sz) {
        final int W = 760, m = 30;
        final double rowh = Math.max(4, Math.min(26, 600.0 / Math.max(1, path.size())));
        plot.redraw(W + 40, (int) (40 + rowh * path.size()), g -> {
            g.setFont(Theme.SMALL);
            java.util.function.IntToDoubleFunction x = cyl -> m + (double) cyl / Math.max(1, sz - 1) * (W - 2 * m);
            for (int t : new int[]{0, sz / 2, sz - 1}) {
                g.setColor(Theme.MUTED);
                g.drawString(String.valueOf(t), (int) x.applyAsDouble(t), 12);
                g.setColor(new Color(0x2A2F4C));
                g.drawLine((int) x.applyAsDouble(t), 18, (int) x.applyAsDouble(t), (int) (20 + rowh * path.size()));
            }
            g.setColor(Theme.PRIMARY);
            g.setStroke(new BasicStroke(2));
            for (int i = 0; i + 1 < path.size(); i++)
                g.drawLine((int) x.applyAsDouble(path.get(i)), (int) (25 + i * rowh),
                        (int) x.applyAsDouble(path.get(i + 1)), (int) (25 + (i + 1) * rowh));
            g.setStroke(new BasicStroke(1));
            if (path.size() <= 60) {
                for (int i = 0; i < path.size(); i++) {
                    int cyl = path.get(i);
                    g.setColor(Theme.CORAL);
                    g.fillOval((int) x.applyAsDouble(cyl) - 3, (int) (22 + i * rowh), 6, 6);
                    g.setColor(Theme.TEXT);
                    g.drawString(String.valueOf(cyl), (int) x.applyAsDouble(cyl) + 6, (int) (29 + i * rowh));
                }
            }
        });
    }

    private void run() {
        Object[] d = read();
        int[] rq = (int[]) d[0];
        int h = (Integer) d[1], sz = (Integer) d[2];
        Plan p = schedule((String) algo.getSelectedItem(), rq, h, sz, (String) dir.getSelectedItem());
        plot(p.path, sz);
        out.clear();
        out.write(algo.getSelectedItem() + " | head = " + h + " | disk = 0.." + (sz - 1) + " | requests = " + rq.length);
        out.write("(C-SCAN / C-LOOK return jumps are counted as head movement)\n");
        out.write("Service order: " + join(" -> ", p.order.subList(0, Math.min(300, p.order.size())))
                + (p.order.size() > 300 ? " ..." : ""));
        if (p.path.size() <= 300) {
            out.write("\nSeek steps:");
            List<Object[]> rows = new ArrayList<>();
            for (int i = 0; i + 1 < p.path.size(); i++)
                rows.add(new Object[]{p.path.get(i), p.path.get(i + 1), Math.abs(p.path.get(i) - p.path.get(i + 1))});
            out.write(fmtTable(new String[]{"From", "To", "Distance"}, rows));
        }
        long tm = totalMovement(p.path);
        out.write("\nTotal head movement: " + tm + " cylinders");
        out.write(String.format("Average seek per request: %.2f", (double) tm / rq.length));
    }

    private void compare() {
        Object[] d = read();
        int[] rq = (int[]) d[0];
        int h = (Integer) d[1], sz = (Integer) d[2];
        out.clear();
        List<Object[]> rows = new ArrayList<>();
        for (String a : ALGOS) {
            long tm = totalMovement(schedule(a, rq, h, sz, (String) dir.getSelectedItem()).path);
            rows.add(new Object[]{a, tm, String.format("%.2f", (double) tm / rq.length)});
        }
        out.write("Comparison | head = " + h + " | requests = " + rq.length + "\n");
        out.write(fmtTable(new String[]{"Algorithm", "Total movement", "Avg per request"}, rows));
    }
}