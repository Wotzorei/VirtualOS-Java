package minios;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import static minios.Common.*;

/** Module 6 - Fragmentation: fixed partitions with First/Best/Worst/Next Fit. */
public class Fragmentation extends JPanel {
    public static final String[] STRATEGIES = {"First Fit", "Best Fit", "Worst Fit", "Next Fit"};

    // ------------------------------------------------------------------ logic
    public static class Named {
        public final String name;
        public final int size;
        public Named(String name, int size) { this.name = name; this.size = size; }
    }

    public static class Result {
        public Named[] owner;                                   // process placed in each block (or null)
        public Map<String, Integer> assignment = new LinkedHashMap<>(); // pid -> block index or null
        public int internal, freeTotal;
        public List<Named> unallocated = new ArrayList<>();
        public List<Named> external = new ArrayList<>();
    }

    public static Result allocate(List<Named> blocks, List<Named> procs, String strategy) {
        int n = blocks.size();
        Result r = new Result();
        r.owner = new Named[n];
        int last = 0;
        for (Named p : procs) {
            List<Integer> cands = new ArrayList<>();
            for (int i = 0; i < n; i++) if (r.owner[i] == null && blocks.get(i).size >= p.size) cands.add(i);
            Integer idx = null;
            if (!cands.isEmpty()) {
                switch (strategy) {
                    case "First Fit":
                        idx = cands.get(0);
                        break;
                    case "Best Fit":
                        for (int i : cands) if (idx == null || blocks.get(i).size < blocks.get(idx).size) idx = i;
                        break;
                    case "Worst Fit":
                        for (int i : cands) if (idx == null || blocks.get(i).size > blocks.get(idx).size) idx = i;
                        break;
                    default: // Next Fit: continue searching from last allocation
                        Set<Integer> cs = new HashSet<>(cands);
                        for (int k = 0; k < n; k++) {
                            int j = (last + k) % n;
                            if (cs.contains(j)) { idx = j; break; }
                        }
                }
            }
            if (idx != null) {
                r.owner[idx] = p;
                last = idx;
            }
            r.assignment.put(p.name, idx);
        }
        for (int i = 0; i < n; i++) {
            if (r.owner[i] != null) r.internal += blocks.get(i).size - r.owner[i].size;
            else r.freeTotal += blocks.get(i).size;
        }
        for (Named p : procs) if (r.assignment.get(p.name) == null) r.unallocated.add(p);
        for (Named p : r.unallocated) if (p.size <= r.freeTotal) r.external.add(p);
        return r;
    }

    // --------------------------------------------------------------------- GUI
    private final DynamicTable blocksTable, procsTable;
    private final JComboBox<String> strategy = combo(STRATEGIES);
    private final DrawPane map = new DrawPane(150);
    private final OutputBox out = new OutputBox(10);

    public Fragmentation() {
        setLayout(new BorderLayout(4, 4));
        pad(this);
        blocksTable = new DynamicTable("Memory blocks (partitions)", new String[]{"Block", "Size"}, n -> {
            Random rnd = new Random();
            List<Object[]> rows = new ArrayList<>();
            for (int i = 0; i < n; i++) rows.add(new Object[]{"B" + (i + 1), 20 + rnd.nextInt(581)});
            return rows;
        }, 4);
        blocksTable.setRows(Arrays.asList(new Object[]{"B1", 100}, new Object[]{"B2", 500}, new Object[]{"B3", 200},
                new Object[]{"B4", 300}, new Object[]{"B5", 600}));
        procsTable = new DynamicTable("Processes", new String[]{"PID", "Size"}, n -> {
            Random rnd = new Random();
            List<Object[]> rows = new ArrayList<>();
            for (int i = 0; i < n; i++) rows.add(new Object[]{"P" + (i + 1), 10 + rnd.nextInt(441)});
            return rows;
        }, 4);
        procsTable.setRows(Arrays.asList(new Object[]{"P1", 212}, new Object[]{"P2", 417}, new Object[]{"P3", 112}, new Object[]{"P4", 426}));

        JPanel row = new JPanel(new GridLayout(1, 2, 6, 0));
        row.add(blocksTable); row.add(procsTable);
        JPanel opt = row();
        opt.add(label("Strategy:")); opt.add(strategy);
        opt.add(button("Allocate", this::run));
        opt.add(softButton("Compare all strategies", this::compare));
        JPanel vis = new JPanel(new BorderLayout());
        vis.setBorder(titled("Memory map (teal = used, amber = internal fragmentation, grey = free)"));
        vis.add(map, BorderLayout.CENTER);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(row); north.add(opt); north.add(vis);
        add(north, BorderLayout.NORTH);
        add(out, BorderLayout.CENTER);
    }

    private List<Named>[] read() {
        List<Named> blocks = new ArrayList<>(), procs = new ArrayList<>();
        int i = 0;
        for (String[] r : blocksTable.getRows()) {
            i++;
            blocks.add(new Named(r[0].isEmpty() ? "B" + i : r[0], toInt(r[1], "Block row " + i + " size", 1)));
        }
        i = 0;
        for (String[] r : procsTable.getRows()) {
            i++;
            procs.add(new Named(r[0].isEmpty() ? "P" + i : r[0], toInt(r[1], "Process row " + i + " size", 1)));
        }
        if (blocks.isEmpty() || procs.isEmpty()) throw new InputError("Enter at least one memory block and one process.");
        Set<String> names = new HashSet<>();
        for (Named b : blocks) names.add(b.name);
        if (names.size() != blocks.size()) throw new InputError("Block names must be unique.");
        names.clear();
        for (Named p : procs) names.add(p.name);
        if (names.size() != procs.size()) throw new InputError("Process IDs must be unique.");
        @SuppressWarnings("unchecked")
        List<Named>[] res = new List[]{blocks, procs};
        return res;
    }

    private void draw(List<Named> blocks, Result res) {
        int biggest = 0;
        for (Named b : blocks) biggest = Math.max(biggest, b.size);
        final int big = biggest;
        map.redraw(760, 16 + 24 * blocks.size(), g -> {
            g.setFont(Theme.SMALL);
            for (int i = 0; i < blocks.size(); i++) {
                Named b = blocks.get(i);
                int y = 8 + i * 24, x0 = 120;
                double w = 600.0 * b.size / big;
                g.setColor(Theme.TEXT);
                g.drawString(b.name + " (" + b.size + ")", 8, y + 13);
                g.setColor(new Color(0x2D3350));
                g.fillRect(x0, y, (int) w, 18);
                g.setColor(Theme.BORDER);
                g.drawRect(x0, y, (int) w, 18);
                Named p = res.owner[i];
                if (p != null) {
                    double used = w * p.size / b.size;
                    g.setColor(Theme.TEAL);
                    g.fillRect(x0, y, (int) used, 18);
                    g.setColor(Theme.AMBER);
                    g.fillRect(x0 + (int) used, y, (int) (w - used), 18);
                    g.setColor(Theme.INSET);
                    g.drawRect(x0, y, (int) used, 18);
                    g.drawRect(x0 + (int) used, y, (int) (w - used), 18);
                    String label = p.name + " (" + p.size + ")";
                    g.drawString(label, (int) (x0 + used / 2 - g.getFontMetrics().stringWidth(label) / 2.0), y + 13);
                }
            }
        });
    }

    private static String list(List<Named> l) {
        List<String> s = new ArrayList<>();
        for (Named n : l) s.add(n.name + "(" + n.size + ")");
        return join(", ", s);
    }

    private void run() {
        List<Named>[] d = read();
        List<Named> blocks = d[0], procs = d[1];
        String st = (String) strategy.getSelectedItem();
        Result res = allocate(blocks, procs, st);
        draw(blocks, res);
        out.clear();
        out.write("Strategy: " + st + "\n");
        List<Object[]> rows = new ArrayList<>();
        for (Named p : procs) {
            Integer a = res.assignment.get(p.name);
            rows.add(new Object[]{p.name, p.size, a != null ? blocks.get(a).name : "NOT ALLOCATED",
                    a != null ? blocks.get(a).size - p.size : "-"});
        }
        out.write(fmtTable(new String[]{"Process", "Size", "Allocated to", "Internal frag."}, rows));
        out.write("\nTotal internal fragmentation : " + res.internal);
        out.write("Total free (unused) blocks   : " + res.freeTotal);
        if (!res.unallocated.isEmpty()) out.write("Unallocated processes        : " + list(res.unallocated));
        if (!res.external.isEmpty())
            out.write("EXTERNAL fragmentation detected: " + list(res.external) + " would fit in the " + res.freeTotal
                    + " free units in total,\nbut no single free block is large enough.");
        else out.write("External fragmentation       : none detected");
    }

    private void compare() {
        List<Named>[] d = read();
        out.clear();
        List<Object[]> rows = new ArrayList<>();
        for (String s : STRATEGIES) {
            Result r = allocate(d[0], d[1], s);
            rows.add(new Object[]{s, d[1].size() - r.unallocated.size(), r.unallocated.size(), r.internal, r.freeTotal, r.external.size()});
        }
        out.write(fmtTable(new String[]{"Strategy", "Allocated", "Failed", "Internal frag.", "Free units", "Ext.-frag. victims"}, rows));
    }
}