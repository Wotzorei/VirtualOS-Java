package minios;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import static minios.Common.*;

/** Module 3 - Paging: page table + logical -> physical address translation. */
public class Paging extends JPanel {

    // ------------------------------------------------------------------ logic
    public static class Trans {
        public String status;
        public Integer page, offset, frame;
        public Long physical;
        public String message;
    }

    /** table: page -> frame (null = not in memory). */
    public static Trans translate(long addr, int pageSize, int numPages, Map<Integer, Integer> table) {
        Trans t = new Trans();
        long limit = (long) pageSize * numPages;
        if (addr >= limit) {
            t.status = "ERROR";
            t.message = "address outside logical address space (0.." + (limit - 1) + ")";
            return t;
        }
        int page = (int) (addr / pageSize), off = (int) (addr % pageSize);
        t.page = page; t.offset = off;
        if (table.get(page) == null) {
            t.status = "PAGE FAULT";
            t.message = "page " + page + " is not in memory";
            return t;
        }
        t.frame = table.get(page);
        t.physical = (long) t.frame * pageSize + off;
        t.status = "OK";
        t.message = "";
        return t;
    }

    // --------------------------------------------------------------------- GUI
    private final JTextField size = field("1024", 8), pages = field("8", 8), frames = field("16", 8);
    private final DynamicTable pt;
    private final JTextField addrs = field("0 1500 2100 4200 5000 8191 9000", 40);
    private final OutputBox out = new OutputBox();

    public Paging() {
        setLayout(new BorderLayout(4, 4));
        pad(this);
        JPanel cfg = row();
        cfg.add(label("Page/frame size (bytes):")); cfg.add(size);
        cfg.add(label("Logical pages:")); cfg.add(pages);
        cfg.add(label("Physical frames:")); cfg.add(frames);
        cfg.add(softButton("Auto-build random page table", this::autoTable));

        pt = new DynamicTable("Page table (leave Frame blank = page not in memory)",
                new String[]{"Page", "Frame"}, null, 5);
        pt.setRows(Arrays.asList(new Object[]{0, 5}, new Object[]{1, 2}, new Object[]{2, 9}, new Object[]{3, 0},
                new Object[]{4, ""}, new Object[]{5, 7}, new Object[]{6, 12}, new Object[]{7, 3}));

        JPanel adr = row();
        adr.add(label("Logical addresses (comma/space separated):"));
        adr.add(addrs);
        adr.add(button("Translate", this::run));
        adr.add(softButton("Random addresses", this::randAddrs));

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(cfg); north.add(pt); north.add(adr);
        add(north, BorderLayout.NORTH);
        add(out, BorderLayout.CENTER);
    }

    private void autoTable() {
        int np = toInt(pages.getText(), "Logical pages", 1);
        int nf = toInt(frames.getText(), "Physical frames", 1);
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < nf; i++) all.add(i);
        Collections.shuffle(all);
        List<Object[]> rows = new ArrayList<>();
        int take = Math.min(np, nf);
        for (int p = 0; p < np; p++) rows.add(new Object[]{p, p < take ? all.get(p) : ""});
        pt.setRows(rows);
    }

    private void randAddrs() {
        int sz = toInt(size.getText(), "Page size", 1);
        int np = toInt(pages.getText(), "Logical pages", 1);
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder();
        long hi = (long) sz * np + sz;
        for (int i = 0; i < 12; i++) {
            if (i > 0) sb.append(' ');
            sb.append((long) (rnd.nextDouble() * (hi + 1)));
        }
        addrs.setText(sb.toString());
    }

    private Map<Integer, Integer> readTable(int np, int nf) {
        Map<Integer, Integer> table = new LinkedHashMap<>();
        Map<Integer, Integer> used = new HashMap<>();
        int i = 0;
        for (String[] row : pt.getRows()) {
            i++;
            int p = toInt(row[0], "Page table row " + i + " page", 0, np - 1);
            if (table.containsKey(p)) throw new InputError("Page " + p + " appears twice in the page table.");
            if (row[1].trim().isEmpty()) { table.put(p, null); continue; }
            int f = toInt(row[1], "Page table row " + i + " frame", 0, nf - 1);
            if (used.containsKey(f))
                throw new InputError("Frame " + f + " is mapped by both page " + used.get(f) + " and page " + p + ".");
            used.put(f, p);
            table.put(p, f);
        }
        return table;
    }

    private void run() {
        int sz = toInt(size.getText(), "Page size", 1);
        int np = toInt(pages.getText(), "Logical pages", 1);
        int nf = toInt(frames.getText(), "Physical frames", 1);
        Map<Integer, Integer> table = readTable(np, nf);
        int[] list = parseIntList(addrs.getText(), "Logical addresses", 0, null);
        out.clear();
        out.write("Logical space  : " + np + " pages x " + sz + " B = " + ((long) np * sz) + " B");
        out.write("Physical memory: " + nf + " frames x " + sz + " B = " + ((long) nf * sz) + " B\n");
        out.write("Formula: page = addr // size, offset = addr % size, physical = frame * size + offset\n");
        List<Object[]> rows = new ArrayList<>();
        for (int a : list) {
            Trans t = translate(a, sz, np, table);
            rows.add(new Object[]{a, t.page != null ? t.page : "-", t.offset != null ? t.offset : "-",
                    t.frame != null ? t.frame : "-", t.physical != null ? t.physical : "-",
                    t.status + (t.message.isEmpty() ? "" : " (" + t.message + ")")});
        }
        out.write(fmtTable(new String[]{"Logical", "Page", "Offset", "Frame", "Physical", "Status"}, rows));
        Map<Integer, Integer> owner = new HashMap<>();
        for (Map.Entry<Integer, Integer> e : table.entrySet()) if (e.getValue() != null) owner.put(e.getValue(), e.getKey());
        int shown = Math.min(nf, 200);
        List<String> cells = new ArrayList<>();
        for (int f = 0; f < shown; f++) cells.add("[" + f + ":" + (owner.containsKey(f) ? "P" + owner.get(f) : "free") + "]");
        out.write("\nPhysical memory map (frame: page):");
        out.write("  " + join("  ", cells) + (nf > shown ? " ..." : ""));
    }
}
