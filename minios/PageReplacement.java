package minios;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import static minios.Common.*;

/** Module 4 - Page Replacement: FIFO, LRU, Optimal. */
public class PageReplacement extends JPanel {

    // ------------------------------------------------------------------ logic
    public static class Step {
        public final int page;
        public final List<Integer> frames;
        public final boolean hit;
        public final Integer victim;
        Step(int page, List<Integer> frames, boolean hit, Integer victim) {
            this.page = page; this.frames = frames; this.hit = hit; this.victim = victim;
        }
    }

    public static class Result {
        public final List<Step> steps;
        public final int faults;
        Result(List<Step> s, int f) { steps = s; faults = f; }
    }

    public static final String[] ALGOS = {"FIFO", "LRU", "Optimal"};

    /** First index in sorted list pos with value > i, or -1 -> infinity. */
    private static int nextUse(List<Integer> pos, int i) {
        int lo = 0, hi = pos.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (pos.get(mid) <= i) lo = mid + 1; else hi = mid;
        }
        return lo < pos.size() ? pos.get(lo) : Integer.MAX_VALUE;
    }

    public static Result simulate(String algo, int[] refs, int nframes) {
        List<Integer> frames = new ArrayList<>();
        List<Step> steps = new ArrayList<>();
        int faults = 0;
        Deque<Integer> fifo = new ArrayDeque<>();
        Map<Integer, Integer> lastUsed = new HashMap<>();
        Map<Integer, List<Integer>> positions = new HashMap<>();
        if (algo.equals("Optimal"))
            for (int i = 0; i < refs.length; i++) positions.computeIfAbsent(refs[i], k -> new ArrayList<>()).add(i);
        for (int i = 0; i < refs.length; i++) {
            int r = refs[i];
            boolean hit = frames.contains(r);
            Integer victim = null;
            if (hit) {
                if (algo.equals("LRU")) lastUsed.put(r, i);
            } else {
                faults++;
                if (frames.size() < nframes) {
                    frames.add(r);
                } else {
                    if (algo.equals("FIFO")) {
                        victim = fifo.pollFirst();
                    } else if (algo.equals("LRU")) {
                        int best = Integer.MAX_VALUE;
                        for (int p : frames) if (lastUsed.get(p) < best) { best = lastUsed.get(p); victim = p; }
                    } else { // Optimal: evict page whose next use is farthest away
                        int far = -1;
                        for (int p : frames) {
                            int nu = nextUse(positions.get(p), i);
                            if (nu > far) { far = nu; victim = p; }
                        }
                    }
                    frames.set(frames.indexOf(victim), r);
                }
                fifo.addLast(r);
                lastUsed.put(r, i);
            }
            steps.add(new Step(r, new ArrayList<>(frames), hit, victim));
        }
        return new Result(steps, faults);
    }

    // --------------------------------------------------------------------- GUI
    private final JTextField refs = field("", 60);
    private final JTextField nframes = field("", 6);
    private final JComboBox<String> algo = combo(ALGOS);
    private final JTextField rlen = field("", 7), rmax = field("", 5);
    private final OutputBox out = new OutputBox();

    public PageReplacement() {
        setLayout(new BorderLayout(4, 4));
        pad(this);
        JPanel f = row();
        f.add(label("Page reference string:")); f.add(refs);
        JPanel g = row();
        g.add(label("Number of frames:")); g.add(nframes);
        g.add(label("  Algorithm:")); g.add(algo);
        g.add(button("Run", this::run));
        g.add(softButton("Compare all", this::compare));
        g.add(label("   Random string: length")); g.add(rlen);
        g.add(label("pages 0..")); g.add(rmax);
        g.add(softButton("Generate", this::rand));
        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(clearRow(this)); north.add(f); north.add(g);
        add(north, BorderLayout.NORTH);
        add(out, BorderLayout.CENTER);
    }

    private void rand() {
        int n = toInt(rlen.getText(), "Length", 1);
        int hi = toInt(rmax.getText(), "Highest page number", 0);
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(' ');
            sb.append(rnd.nextInt(hi + 1));
        }
        refs.setText(sb.toString());
    }

    private void run() {
        int[] rs = parseIntList(refs.getText(), "Reference string", 0, null);
        int nf = toInt(nframes.getText(), "Number of frames", 1);
        String a = (String) algo.getSelectedItem();
        Result res = simulate(a, rs, nf);
        out.clear();
        out.write(a + " | frames = " + nf + " | references = " + rs.length + "\n");
        String[] headers = new String[nf + 4];
        headers[0] = "Step"; headers[1] = "Ref";
        for (int k = 0; k < nf; k++) headers[2 + k] = "F" + (k + 1);
        headers[nf + 2] = "Result"; headers[nf + 3] = "Replaced";
        List<Object[]> rows = new ArrayList<>();
        int i = 1;
        for (Step s : res.steps) {
            Object[] row = new Object[nf + 4];
            row[0] = i++; row[1] = s.page;
            for (int k = 0; k < nf; k++) row[2 + k] = k < s.frames.size() ? s.frames.get(k) : "-";
            row[nf + 2] = s.hit ? "HIT" : "FAULT";
            row[nf + 3] = s.victim == null ? "" : s.victim;
            rows.add(row);
        }
        out.write(fmtTable(headers, rows));
        int hits = rs.length - res.faults;
        out.write("\nPage faults: " + res.faults);
        out.write("Page hits  : " + hits);
        out.write(String.format("Hit ratio  : %.1f%%   Fault ratio: %.1f%%", 100.0 * hits / rs.length, 100.0 * res.faults / rs.length));
    }

    private void compare() {
        int[] rs = parseIntList(refs.getText(), "Reference string", 0, null);
        int nf = toInt(nframes.getText(), "Number of frames", 1);
        out.clear();
        List<Object[]> rows = new ArrayList<>();
        for (String a : ALGOS) {
            int faults = simulate(a, rs, nf).faults;
            rows.add(new Object[]{a, faults, rs.length - faults, String.format("%.1f%%", 100.0 * (rs.length - faults) / rs.length)});
        }
        out.write("Comparison | frames = " + nf + " | references = " + rs.length + "\n");
        out.write(fmtTable(new String[]{"Algorithm", "Faults", "Hits", "Hit ratio"}, rows));
    }
}
