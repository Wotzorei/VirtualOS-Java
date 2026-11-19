package minios;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import static minios.Common.*;

/** Module 5 - Segmentation: segment table, address translation, limit checks. */
public class Segmentation extends JPanel {

    // ------------------------------------------------------------------ logic
    public static class Trans {
        public final boolean ok;
        public final Long physical;
        public final String message;
        Trans(boolean ok, Long physical, String message) { this.ok = ok; this.physical = physical; this.message = message; }
    }

    /** table: segment -> {base, limit}. */
    public static Trans translate(int seg, int offset, Map<Integer, int[]> table) {
        int[] e = table.get(seg);
        if (e == null) return new Trans(false, null, "segment " + seg + " does not exist");
        int base = e[0], limit = e[1];
        if (offset >= limit)
            return new Trans(false, null, "offset " + offset + " >= limit " + limit + " (out-of-range / segmentation fault)");
        return new Trans(true, (long) base + offset, "");
    }

    public static List<int[]> findOverlaps(Map<Integer, int[]> table) {
        List<Map.Entry<Integer, int[]>> items = new ArrayList<>(table.entrySet());
        items.sort(Comparator.comparingInt(en -> en.getValue()[0]));
        List<int[]> out = new ArrayList<>();
        for (int i = 0; i + 1 < items.size(); i++) {
            int[] a = items.get(i).getValue(), b = items.get(i + 1).getValue();
            if (b[0] < a[0] + a[1]) out.add(new int[]{items.get(i).getKey(), items.get(i + 1).getKey()});
        }
        return out;
    }

    // --------------------------------------------------------------------- GUI
    private final DynamicTable segs, reqs;
    private final OutputBox out = new OutputBox();

    public Segmentation() {
        setLayout(new BorderLayout(4, 4));
        pad(this);
        segs = new DynamicTable("Segment table", new String[]{"Segment", "Base", "Limit"}, Segmentation::randSegments, 4);
        reqs = new DynamicTable("Logical addresses (segment, offset)", new String[]{"Segment", "Offset"}, this::randRequests, 4);
        JPanel btn = row();
        btn.add(button("Translate", this::run));
        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(clearRow(this)); north.add(segs); north.add(reqs); north.add(btn);
        add(north, BorderLayout.NORTH);
        add(out, BorderLayout.CENTER);
    }

    private static List<Object[]> randSegments(int n) {
        Random rnd = new Random();
        List<Object[]> rows = new ArrayList<>();
        int cursor = rnd.nextInt(501);
        for (int i = 0; i < n; i++) {
            int limit = 100 + rnd.nextInt(1901);
            rows.add(new Object[]{i, cursor, limit});
            cursor += limit + rnd.nextInt(301);
        }
        return rows;
    }

    private List<Object[]> randRequests(int n) {
        Random rnd = new Random();
        List<int[]> list = new ArrayList<>();
        for (String[] r : segs.getRows())
            if (r[0].matches("\\d+") && r[2].matches("\\d+")) {
                try { list.add(new int[]{Integer.parseInt(r[0]), Integer.parseInt(r[2])}); } catch (NumberFormatException ignored) { }
            }
        List<Object[]> out = new ArrayList<>();
        if (list.isEmpty()) return out;
        for (int i = 0; i < n; i++) {
            int[] sl = list.get(rnd.nextInt(list.size()));
            out.add(new Object[]{sl[0], rnd.nextInt((int) (sl[1] * 1.3) + 1)});
        }
        return out;
    }

    private void run() {
        Map<Integer, int[]> table = new TreeMap<>();
        int i = 0;
        for (String[] r : segs.getRows()) {
            i++;
            int s = toInt(r[0], "Segment row " + i + " number", 0);
            if (table.containsKey(s)) throw new InputError("Segment " + s + " is defined twice.");
            table.put(s, new int[]{toInt(r[1], "Segment row " + i + " base", 0), toInt(r[2], "Segment row " + i + " limit", 1)});
        }
        if (table.isEmpty()) throw new InputError("Define at least one segment.");
        List<int[]> rq = new ArrayList<>();
        i = 0;
        for (String[] r : reqs.getRows()) {
            i++;
            rq.add(new int[]{toInt(r[0], "Address row " + i + " segment", 0), toInt(r[1], "Address row " + i + " offset", 0)});
        }
        if (rq.isEmpty()) throw new InputError("Enter at least one logical address.");
        out.clear();
        List<Object[]> rows = new ArrayList<>();
        for (Map.Entry<Integer, int[]> e : table.entrySet())
            rows.add(new Object[]{e.getKey(), e.getValue()[0], e.getValue()[1], (long) e.getValue()[0] + e.getValue()[1] - 1});
        out.write(fmtTable(new String[]{"Segment", "Base", "Limit", "End (base+limit-1)"}, rows));
        for (int[] ab : findOverlaps(table))
            out.write("WARNING: segments " + ab[0] + " and " + ab[1] + " overlap in physical memory.");
        out.write("");
        rows = new ArrayList<>();
        int errors = 0;
        for (int[] so : rq) {
            Trans t = translate(so[0], so[1], table);
            if (!t.ok) errors++;
            rows.add(new Object[]{"(" + so[0] + ", " + so[1] + ")", t.ok ? t.physical : "-", t.ok ? "VALID" : "ERROR: " + t.message});
        }
        out.write(fmtTable(new String[]{"Logical (seg, off)", "Physical", "Result"}, rows));
        out.write("\n" + (rq.size() - errors) + " valid, " + errors + " out-of-range / invalid.");
    }
}
