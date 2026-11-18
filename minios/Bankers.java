package minios;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import static minios.Common.*;

/** Module 2 - Banker's Algorithm (safety check + resource-request check). */
public class Bankers extends JPanel {

    // ------------------------------------------------------------------ logic
    public static int[][] computeNeed(int[][] alloc, int[][] mx) {
        int n = alloc.length, m = alloc[0].length;
        int[][] need = new int[n][m];
        for (int i = 0; i < n; i++) for (int j = 0; j < m; j++) need[i][j] = mx[i][j] - alloc[i][j];
        return need;
    }

    public static class Safety {
        public boolean safe;
        public List<Integer> seq = new ArrayList<>();
        public int[][] need;
        public List<String> steps = new ArrayList<>();
        public List<Integer> stuck = new ArrayList<>();
    }

    public static Safety safetyCheck(int[][] alloc, int[][] mx, int[] avail) {
        int n = alloc.length, m = avail.length;
        Safety r = new Safety();
        r.need = computeNeed(alloc, mx);
        int[] work = avail.clone();
        boolean[] finish = new boolean[n];
        boolean progress = true;
        while (progress) {
            progress = false;
            for (int i = 0; i < n; i++) {
                if (finish[i]) continue;
                boolean ok = true;
                for (int j = 0; j < m; j++) if (r.need[i][j] > work[j]) { ok = false; break; }
                if (ok) {
                    r.steps.add("P" + i + ": Need " + Arrays.toString(r.need[i]) + " <= Work " + Arrays.toString(work)
                            + "  -> can finish, releases " + Arrays.toString(alloc[i]));
                    int[] w2 = new int[m];
                    for (int j = 0; j < m; j++) w2[j] = work[j] + alloc[i][j];
                    work = w2;
                    finish[i] = true;
                    r.seq.add(i);
                    progress = true;
                }
            }
        }
        for (int i = 0; i < n; i++) if (!finish[i]) r.stuck.add(i);
        r.safe = r.stuck.isEmpty();
        return r;
    }

    public static class Reply {
        public final boolean granted;
        public final String message;
        Reply(boolean g, String m) { granted = g; message = m; }
    }

    public static Reply tryRequest(int pid, int[] req, int[][] alloc, int[][] mx, int[] avail) {
        int m = avail.length;
        int[][] need = computeNeed(alloc, mx);
        for (int j = 0; j < m; j++)
            if (req[j] > need[pid][j]) return new Reply(false, "Denied: request exceeds the process's maximum claim (Need).");
        for (int j = 0; j < m; j++)
            if (req[j] > avail[j]) return new Reply(false, "Must wait: not enough resources currently available.");
        int[][] a2 = new int[alloc.length][];
        for (int i = 0; i < alloc.length; i++) a2[i] = alloc[i].clone();
        int[] v2 = avail.clone();
        for (int j = 0; j < m; j++) { a2[pid][j] += req[j]; v2[j] -= req[j]; }
        Safety s = safetyCheck(a2, mx, v2);
        if (s.safe) {
            List<String> names = new ArrayList<>();
            for (int i : s.seq) names.add("P" + i);
            return new Reply(true, "Granted: state stays safe. Safe sequence: " + join(" -> ", names));
        }
        return new Reply(false, "Denied: granting this request would lead to an UNSAFE state.");
    }

    public static String[] resNames(int m) {
        String[] r = new String[m];
        for (int j = 0; j < m; j++) r[j] = m <= 26 ? String.valueOf((char) (65 + j)) : "R" + j;
        return r;
    }

    // --------------------------------------------------------------------- GUI
    static class MatrixInput extends JPanel {
        final String title;
        final String[] rl, cl;
        final JTextField[][] entries;

        MatrixInput(String title, String[] rowLabels, String[] colLabels) {
            this.title = title; this.rl = rowLabels; this.cl = colLabels;
            setBorder(titled(title));
            setOpaque(false);
            setLayout(new GridLayout(rowLabels.length + 1, colLabels.length + 1, 1, 1));
            add(label(""));
            for (String c : colLabels) add(new JLabel(c, SwingConstants.CENTER));
            entries = new JTextField[rowLabels.length][colLabels.length];
            for (int i = 0; i < rowLabels.length; i++) {
                add(new JLabel(rowLabels[i]));
                for (int j = 0; j < colLabels.length; j++) {
                    entries[i][j] = new JTextField(3);
                    entries[i][j].setHorizontalAlignment(SwingConstants.CENTER);
                    add(entries[i][j]);
                }
            }
        }

        int[][] get() {
            int[][] d = new int[entries.length][cl.length];
            for (int i = 0; i < entries.length; i++)
                for (int j = 0; j < cl.length; j++)
                    d[i][j] = toInt(entries[i][j].getText(), title + "[" + rl[i] + "][" + cl[j] + "]", 0);
            return d;
        }

        void set(int[][] data) {
            for (int i = 0; i < entries.length; i++)
                for (int j = 0; j < cl.length; j++) entries[i][j].setText(String.valueOf(data[i][j]));
        }
    }

    private final JTextField nVar = field("5", 6), mVar = field("3", 6);
    private final JPanel inner = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
    private final JTextField reqPid = field("1", 5), reqVec = field("1 0 2", 16);
    private final OutputBox out = new OutputBox();
    private MatrixInput alloc, max, avail;

    public Bankers() {
        setLayout(new BorderLayout(4, 4));
        pad(this);
        JPanel top = row();
        top.add(label("Number of processes:")); top.add(nVar);
        top.add(label("  Number of resource types:")); top.add(mVar);
        top.add(softButton("Create matrices", this::create));
        top.add(softButton("Load textbook example", this::sample));
        top.add(softButton("Random fill", this::randomFill));

        JScrollPane scroll = new JScrollPane(inner);
        scroll.setPreferredSize(new Dimension(400, 175));
        scroll.setBorder(null);
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);

        JPanel act = row();
        act.add(button("Check safety / find safe sequence", this::check));
        act.add(label("   Request: process #")); act.add(reqPid);
        act.add(label("amounts (e.g. 1 0 2):")); act.add(reqVec);
        act.add(softButton("Test request", this::request));

        JPanel north = new JPanel(new BorderLayout());
        north.add(top, BorderLayout.NORTH);
        north.add(scroll, BorderLayout.CENTER);
        north.add(act, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);
        add(out, BorderLayout.CENTER);
        sample();
    }

    private void create() {
        int n = toInt(nVar.getText(), "Number of processes", 1);
        int m = toInt(mVar.getText(), "Number of resource types", 1);
        if ((long) n * m > 2500 && JOptionPane.showConfirmDialog(this,
                "This creates " + (3 * n * m) + " input boxes. Continue?", "Large input",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        inner.removeAll();
        String[] rows = new String[n];
        for (int i = 0; i < n; i++) rows[i] = "P" + i;
        String[] cols = resNames(m);
        alloc = new MatrixInput("Allocation", rows, cols);
        max = new MatrixInput("Max", rows, cols);
        avail = new MatrixInput("Available", new String[]{"Avail"}, cols);
        for (MatrixInput w : new MatrixInput[]{alloc, max, avail}) {
            JPanel holder = new JPanel(new BorderLayout());
            holder.add(w, BorderLayout.NORTH);
            inner.add(holder);
        }
        inner.revalidate();
        inner.repaint();
        out.clear();
    }

    private void sample() {
        nVar.setText("5"); mVar.setText("3");
        guarded(this::create).run();
        alloc.set(new int[][]{{0, 1, 0}, {2, 0, 0}, {3, 0, 2}, {2, 1, 1}, {0, 0, 2}});
        max.set(new int[][]{{7, 5, 3}, {3, 2, 2}, {9, 0, 2}, {2, 2, 2}, {4, 3, 3}});
        avail.set(new int[][]{{3, 3, 2}});
    }

    private void randomFill() {
        int n = toInt(nVar.getText(), "Number of processes", 1);
        int m = toInt(mVar.getText(), "Number of resource types", 1);
        create();
        Random rnd = new Random();
        int[][] mx = new int[n][m], al = new int[n][m], av = new int[1][m];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++) {
                mx[i][j] = 1 + rnd.nextInt(10);
                al[i][j] = rnd.nextInt(mx[i][j] + 1) / 2;
            }
        for (int j = 0; j < m; j++) av[0][j] = 2 + rnd.nextInt(7);
        max.set(mx); alloc.set(al); avail.set(av);
    }

    private int[][][] read() {
        if (alloc == null) throw new InputError("Create the matrices first.");
        int[][] al = alloc.get(), mx = max.get();
        int[] av = avail.get()[0];
        String[] names = resNames(av.length);
        for (int i = 0; i < al.length; i++)
            for (int j = 0; j < av.length; j++)
                if (al[i][j] > mx[i][j])
                    throw new InputError("Inconsistent input: Allocation > Max for P" + i + ", resource " + names[j] + ".");
        return new int[][][]{al, mx, {av}};
    }

    private void check() {
        int[][][] d = read();
        int[][] al = d[0], mx = d[1];
        int[] av = d[2][0];
        Safety s = safetyCheck(al, mx, av);
        out.clear();
        out.write("Need = Max - Allocation");
        String[] headers = new String[av.length + 1];
        headers[0] = "Process";
        System.arraycopy(resNames(av.length), 0, headers, 1, av.length);
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < s.need.length; i++) {
            Object[] r = new Object[av.length + 1];
            r[0] = "P" + i;
            for (int j = 0; j < av.length; j++) r[j + 1] = s.need[i][j];
            rows.add(r);
        }
        out.write(fmtTable(headers, rows));
        out.write("\nSafety algorithm trace:");
        for (String st : s.steps) out.write("  " + st);
        if (s.safe) {
            List<String> names = new ArrayList<>();
            for (int i : s.seq) names.add("P" + i);
            out.write("\nRESULT: SAFE state.");
            out.write("Safe sequence: " + join(" -> ", names));
        } else {
            List<String> names = new ArrayList<>();
            for (int i : s.stuck) names.add("P" + i);
            out.write("\nRESULT: UNSAFE state - no safe sequence exists.");
            out.write("Processes that cannot finish: " + join(", ", names));
        }
    }

    private void request() {
        int[][][] d = read();
        int[][] al = d[0], mx = d[1];
        int[] av = d[2][0];
        int pid = toInt(reqPid.getText(), "Request process number", 0, al.length - 1);
        int[] req = parseIntList(reqVec.getText(), "Request vector", 0, null);
        if (req.length != av.length)
            throw new InputError("Request needs exactly " + av.length + " numbers (one per resource type).");
        Reply r = tryRequest(pid, req, al, mx, av);
        out.write("\nRequest by P" + pid + " for " + Arrays.toString(req) + ": " + r.message);
    }
}
