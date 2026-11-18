package minios;

import com.sun.management.OperatingSystemMXBean;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * VirtualOS - a small desktop shell that hosts the Mini OS Simulator modules.
 * Desktop icons (double-click) open each module in its own window; the taskbar has a Start menu,
 * program search, window buttons, battery and clock. Right-click the desktop to change the wallpaper.
 */
public class VirtualOS extends JPanel {

    // ------------------------------------------------------------------ apps
    static final class App {
        final String title;
        final Color color;
        final int glyph;
        final Supplier<JPanel> factory;
        App(String title, Color color, int glyph, Supplier<JPanel> factory) {
            this.title = title; this.color = color; this.glyph = glyph; this.factory = factory;
        }
    }

    static final App[] APPS = {
        new App("CPU Scheduling", new Color(0xe63946), 0, CpuScheduling::new),
        new App("Banker's Algorithm", new Color(0x2f6fdb), 1, Bankers::new),
        new App("Paging", new Color(0xf06292), 2, Paging::new),
        new App("Page Replacement", new Color(0x2f7fe0), 3, PageReplacement::new),
        new App("Segmentation", new Color(0x2bb673), 4, Segmentation::new),
        new App("Fragmentation", new Color(0x7e57c2), 5, Fragmentation::new),
        new App("Disk Scheduling", new Color(0x1f9d55), 6, DiskScheduling::new),
        new App("Multiprogramming", new Color(0x1e88e5), 7, Multiprogramming::new),
    };

    private static Color lighten(Color c, float f) {
        return new Color(Math.round(c.getRed() + (255 - c.getRed()) * f),
                Math.round(c.getGreen() + (255 - c.getGreen()) * f),
                Math.round(c.getBlue() + (255 - c.getBlue()) * f));
    }

    /** Draws an app icon (rounded square + white glyph) into the square at x,y with side s. */
    static void paintGlyph(Graphics2D g0, App a, float x, float y, float s) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setPaint(new GradientPaint(x, y, lighten(a.color, 0.22f), x, y + s, a.color));
        g.fill(new RoundRectangle2D.Float(x, y, s, s, s * 0.26f, s * 0.26f));
        float u = s / 10f;
        g.setColor(Color.WHITE);
        switch (a.glyph) {
            case 0: { // CPU chip
                for (int k = 0; k < 3; k++) {
                    float p = 3.5f + k * 1.3f;
                    g.fill(new Rectangle2D.Float(x + p * u, y + 1.7f * u, 0.6f * u, 1.3f * u));
                    g.fill(new Rectangle2D.Float(x + p * u, y + 7f * u, 0.6f * u, 1.3f * u));
                    g.fill(new Rectangle2D.Float(x + 1.7f * u, y + p * u, 1.3f * u, 0.6f * u));
                    g.fill(new Rectangle2D.Float(x + 7f * u, y + p * u, 1.3f * u, 0.6f * u));
                }
                g.fill(new RoundRectangle2D.Float(x + 2.8f * u, y + 2.8f * u, 4.4f * u, 4.4f * u, u, u));
                g.setColor(a.color);
                g.fill(new Rectangle2D.Float(x + 4f * u, y + 4f * u, 2f * u, 2f * u));
                break;
            }
            case 1: { // shield with check
                Path2D.Float p = new Path2D.Float();
                p.moveTo(x + 5f * u, y + 1.6f * u);
                p.lineTo(x + 8.2f * u, y + 2.8f * u);
                p.lineTo(x + 8.2f * u, y + 5.4f * u);
                p.curveTo(x + 8.2f * u, y + 7.2f * u, x + 6.6f * u, y + 8.3f * u, x + 5f * u, y + 8.9f * u);
                p.curveTo(x + 3.4f * u, y + 8.3f * u, x + 1.8f * u, y + 7.2f * u, x + 1.8f * u, y + 5.4f * u);
                p.lineTo(x + 1.8f * u, y + 2.8f * u);
                p.closePath();
                g.fill(p);
                g.setColor(a.color);
                g.setStroke(new BasicStroke(0.9f * u, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D.Float chk = new Path2D.Float();
                chk.moveTo(x + 3.6f * u, y + 5.1f * u);
                chk.lineTo(x + 4.7f * u, y + 6.2f * u);
                chk.lineTo(x + 6.5f * u, y + 4f * u);
                g.draw(chk);
                break;
            }
            case 2: { // house
                Path2D.Float p = new Path2D.Float();
                p.moveTo(x + 5f * u, y + 1.6f * u);
                p.lineTo(x + 8.8f * u, y + 5f * u);
                p.lineTo(x + 7.7f * u, y + 5f * u);
                p.lineTo(x + 7.7f * u, y + 8.2f * u);
                p.lineTo(x + 2.3f * u, y + 8.2f * u);
                p.lineTo(x + 2.3f * u, y + 5f * u);
                p.lineTo(x + 1.2f * u, y + 5f * u);
                p.closePath();
                g.fill(p);
                g.setColor(a.color);
                g.fill(new Rectangle2D.Float(x + 4.2f * u, y + 5.7f * u, 1.6f * u, 2.5f * u));
                break;
            }
            case 3: { // "C" ring
                g.setStroke(new BasicStroke(1.4f * u, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Arc2D.Float(x + 2.4f * u, y + 2.4f * u, 5.2f * u, 5.2f * u, 50, 260, Arc2D.OPEN));
                break;
            }
            case 4: { // stacked segments
                g.fill(new RoundRectangle2D.Float(x + 2.2f * u, y + 2.2f * u, 5.6f * u, 2.4f * u, u, u));
                g.fill(new RoundRectangle2D.Float(x + 2.2f * u, y + 5.4f * u, 5.6f * u, 2.4f * u, u, u));
                g.setColor(a.color);
                g.fill(new Ellipse2D.Float(x + 3f * u, y + 3f * u, 1f * u, 1f * u));
                g.fill(new Ellipse2D.Float(x + 3f * u, y + 6.2f * u, 1f * u, 1f * u));
                break;
            }
            case 5: { // 2x2 grid
                g.fill(new RoundRectangle2D.Float(x + 2.1f * u, y + 2.1f * u, 2.8f * u, 2.8f * u, 0.6f * u, 0.6f * u));
                g.fill(new RoundRectangle2D.Float(x + 5.1f * u, y + 2.1f * u, 2.8f * u, 2.8f * u, 0.6f * u, 0.6f * u));
                g.fill(new RoundRectangle2D.Float(x + 2.1f * u, y + 5.1f * u, 2.8f * u, 2.8f * u, 0.6f * u, 0.6f * u));
                g.setColor(new Color(255, 255, 255, 140));
                g.fill(new RoundRectangle2D.Float(x + 5.1f * u, y + 5.1f * u, 2.8f * u, 2.8f * u, 0.6f * u, 0.6f * u));
                break;
            }
            case 6: { // disk / radar
                g.setStroke(new BasicStroke(0.7f * u, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Ellipse2D.Float(x + 1.9f * u, y + 1.9f * u, 6.2f * u, 6.2f * u));
                g.draw(new Ellipse2D.Float(x + 3.5f * u, y + 3.5f * u, 3f * u, 3f * u));
                g.fill(new Ellipse2D.Float(x + 4.5f * u, y + 4.5f * u, 1f * u, 1f * u));
                g.setStroke(new BasicStroke(0.8f * u, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Float(x + 5f * u, y + 5f * u, x + 7.4f * u, y + 2.6f * u));
                break;
            }
            default: { // bar chart
                g.fill(new RoundRectangle2D.Float(x + 2.2f * u, y + 5.4f * u, 1.7f * u, 2.8f * u, 0.5f * u, 0.5f * u));
                g.fill(new RoundRectangle2D.Float(x + 4.2f * u, y + 3.4f * u, 1.7f * u, 4.8f * u, 0.5f * u, 0.5f * u));
                g.fill(new RoundRectangle2D.Float(x + 6.2f * u, y + 1.9f * u, 1.7f * u, 6.3f * u, 0.5f * u, 0.5f * u));
            }
        }
        g.dispose();
    }

    static ImageIcon iconFor(App a, int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        paintGlyph(g, a, 0, 0, size);
        g.dispose();
        return new ImageIcon(img);
    }

    // ---------------------------------------------------------------- fields
    private static final Preferences PREFS = Preferences.userNodeForPackage(VirtualOS.class);

    private final Desktop desktop = new Desktop();
    private final List<DesktopIcon> icons = new ArrayList<>();
    private final CalendarWidget calendar = new CalendarWidget();
    private final SystemWidget system = new SystemWidget();
    private final Map<App, JInternalFrame> windows = new LinkedHashMap<>();
    private final Map<JInternalFrame, JToggleButton> buttons = new HashMap<>();
    private final JPanel windowStrip = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 7));
    private final BatteryLabel battery = new BatteryLabel();
    private final ClockLabel clock = new ClockLabel();
    private final SearchField search = new SearchField();
    private JPopupMenu suggestions;
    private int cascade = 0;

    public VirtualOS() {
        setLayout(new BorderLayout());
        desktop.setDesktopManager(new TaskbarDesktopManager());
        for (App a : APPS) {
            DesktopIcon ic = new DesktopIcon(a);
            icons.add(ic);
            desktop.add(ic, Integer.valueOf(-5));
        }
        desktop.add(calendar, Integer.valueOf(-5));
        desktop.add(system, Integer.valueOf(-5));
        desktop.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { relayout(); }
        });
        desktop.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) desktopMenu(e); else select(null);
            }
            @Override public void mouseReleased(MouseEvent e) { if (e.isPopupTrigger()) desktopMenu(e); }
        });
        add(desktop, BorderLayout.CENTER);
        add(buildTaskbar(), BorderLayout.SOUTH);
        loadWallpaper();

        new javax.swing.Timer(30000, e -> calendar.repaint()).start();
        startBatteryPoller();
    }

    // ---------------------------------------------------------------- layout
    void relayout() {
        int w = desktop.getWidth(), h = desktop.getHeight();
        int cellW = 96, cellH = 84, top = 8, x = 10, y = top;
        for (DesktopIcon ic : icons) {
            if (y + cellH > h && y > top) { y = top; x += cellW; }
            ic.setBounds(x, y, cellW, cellH);
            y += cellH;
        }
        Dimension cd = calendar.getPreferredSize(), sd = system.getPreferredSize();
        calendar.setBounds(w - cd.width - 14, 12, cd.width, cd.height);
        system.setBounds(w - sd.width - 14, 12 + cd.height + 12, sd.width, sd.height);
    }

    private void select(DesktopIcon sel) {
        for (DesktopIcon ic : icons) ic.setSelected(ic == sel);
    }

    // --------------------------------------------------------------- windows
    void open(App a) {
        JInternalFrame existing = windows.get(a);
        if (existing != null && !existing.isClosed()) { restore(existing); return; }
        final JInternalFrame f = new JInternalFrame(a.title, true, true, true, true);
        f.setFrameIcon(iconFor(a, 16));
        f.setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
        f.setContentPane(a.factory.get());
        f.addInternalFrameListener(new InternalFrameAdapter() {
            @Override public void internalFrameClosed(InternalFrameEvent e) {
                windows.remove(a);
                JToggleButton b = buttons.remove(f);
                if (b != null) { windowStrip.remove(b); windowStrip.revalidate(); windowStrip.repaint(); }
            }
            @Override public void internalFrameActivated(InternalFrameEvent e) { syncButtons(); }
            @Override public void internalFrameDeactivated(InternalFrameEvent e) { syncButtons(); }
        });
        int dw = Math.max(400, desktop.getWidth()), dh = Math.max(300, desktop.getHeight());
        int w = Math.min(1000, dw - 40), h = Math.min(700, dh - 20);
        int off = (cascade++ % 6) * 28;
        f.setSize(w, h);
        f.setLocation(Math.min(110 + off, Math.max(0, dw - w)), Math.min(16 + off, Math.max(0, dh - h)));
        desktop.add(f);
        f.setVisible(true);
        windows.put(a, f);

        JToggleButton b = new JToggleButton(a.title, iconFor(a, 16));
        b.setFocusable(false);
        b.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        b.setPreferredSize(new Dimension(160, 30));
        b.addActionListener(ev -> {
            try {
                if (f.isIcon()) restore(f);
                else if (f.isSelected()) f.setIcon(true);
                else restore(f);
            } catch (PropertyVetoException ignored) { }
            syncButtons();
        });
        buttons.put(f, b);
        windowStrip.add(b);
        windowStrip.revalidate();
        windowStrip.repaint();
        try { f.setSelected(true); } catch (PropertyVetoException ignored) { }
        syncButtons();
    }

    private void restore(JInternalFrame f) {
        try {
            if (f.isIcon()) f.setIcon(false);
            f.setVisible(true);
            f.toFront();
            f.setSelected(true);
        } catch (PropertyVetoException ignored) { }
        syncButtons();
    }

    private void syncButtons() {
        for (Map.Entry<JInternalFrame, JToggleButton> e : buttons.entrySet())
            e.getValue().setSelected(e.getKey().isSelected() && !e.getKey().isIcon());
    }

    /** Minimised windows simply hide; the taskbar button brings them back. */
    private final class TaskbarDesktopManager extends DefaultDesktopManager {
        @Override public void iconifyFrame(JInternalFrame f) {
            f.setVisible(false);
        }
        @Override public void deiconifyFrame(JInternalFrame f) {
            f.setVisible(true);
            f.toFront();
        }
    }

    // ------------------------------------------------------------- wallpaper
    private void loadWallpaper() {
        String path = null;
        try { path = PREFS.get("wallpaper", null); } catch (Exception ignored) { }
        if (path == null || !new File(path).isFile()) {
            path = null;
            for (String n : new String[]{"wallpaper.jpg", "wallpaper.jpeg", "wallpaper.png"})
                if (new File(n).isFile()) { path = n; break; }
        }
        if (path != null) setWallpaper(new File(path), false);
    }

    private void setWallpaper(File f, boolean save) {
        try {
            BufferedImage img = ImageIO.read(f);
            if (img == null) throw new IOException("not an image file");
            desktop.setWallpaper(img);
            if (save) { try { PREFS.put("wallpaper", f.getAbsolutePath()); } catch (Exception ignored) { } }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not load wallpaper: " + ex.getMessage(), "Wallpaper", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void chooseWallpaper() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Choose wallpaper");
        fc.setFileFilter(new FileNameExtensionFilter("Images (jpg, png, gif, bmp)", "jpg", "jpeg", "png", "gif", "bmp"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) setWallpaper(fc.getSelectedFile(), true);
    }

    private void resetWallpaper() {
        desktop.setWallpaper(null);
        try { PREFS.remove("wallpaper"); } catch (Exception ignored) { }
    }

    private void desktopMenu(MouseEvent e) {
        JPopupMenu m = new JPopupMenu();
        JMenuItem c = new JMenuItem("Change wallpaper...");
        c.addActionListener(ev -> chooseWallpaper());
        JMenuItem r = new JMenuItem("Reset wallpaper");
        r.addActionListener(ev -> resetWallpaper());
        m.add(c);
        m.add(r);
        m.show(e.getComponent(), e.getX(), e.getY());
    }

    /** Desktop background: wallpaper (cover-scaled) or a default gradient. */
    private static final class Desktop extends JDesktopPane {
        private BufferedImage wallpaper, scaled;

        void setWallpaper(BufferedImage img) {
            wallpaper = img;
            scaled = null;
            repaint();
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            if (wallpaper != null && w > 0 && h > 0) {
                if (scaled == null || scaled.getWidth() != w || scaled.getHeight() != h) {
                    scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                    Graphics2D sg = scaled.createGraphics();
                    sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    double s = Math.max((double) w / wallpaper.getWidth(), (double) h / wallpaper.getHeight());
                    int dw = (int) Math.ceil(wallpaper.getWidth() * s), dh = (int) Math.ceil(wallpaper.getHeight() * s);
                    sg.drawImage(wallpaper, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
                    sg.dispose();
                }
                g.drawImage(scaled, 0, 0, null);
            } else {
                g.setPaint(new GradientPaint(0, 0, new Color(0x0a2a6b), w, h, new Color(0x1ea1c9)));
                g.fillRect(0, 0, w, h);
                g.setColor(new Color(255, 255, 255, 18));
                g.fill(new Ellipse2D.Double(w * 0.35, -h * 0.3, h * 1.2, h * 1.2));
                g.setColor(new Color(255, 255, 255, 14));
                g.fill(new Ellipse2D.Double(w * 0.1, h * 0.45, h * 0.9, h * 0.9));
                g.setColor(new Color(255, 255, 255, 38));
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
                String t = "VirtualOS";
                g.drawString(t, w - g.getFontMetrics().stringWidth(t) - 24, h - 22);
            }
            g.dispose();
        }
    }

    // --------------------------------------------------------------- widgets
    private final class DesktopIcon extends JComponent {
        private final App app;
        private boolean selected;

        DesktopIcon(App app) {
            this.app = app;
            setOpaque(false);
            setToolTipText(app.title);
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    select(DesktopIcon.this);
                    if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) open(app);
                    else if (e.isPopupTrigger()) menu(e);
                }
                @Override public void mouseReleased(MouseEvent e) { if (e.isPopupTrigger()) menu(e); }
            });
        }

        private void menu(MouseEvent e) {
            JPopupMenu m = new JPopupMenu();
            JMenuItem o = new JMenuItem("Open");
            o.addActionListener(ev -> open(app));
            m.add(o);
            m.show(this, e.getX(), e.getY());
        }

        void setSelected(boolean s) {
            if (selected != s) { selected = s; repaint(); }
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            if (selected) {
                g.setColor(new Color(255, 255, 255, 60));
                g.fill(new RoundRectangle2D.Float(4, 2, w - 9, h - 5, 10, 10));
                g.setColor(new Color(255, 255, 255, 120));
                g.draw(new RoundRectangle2D.Float(4, 2, w - 9, h - 5, 10, 10));
            }
            paintGlyph(g, app, (w - 44) / 2f, 4, 44);
            int size = 12;
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, size));
            FontMetrics fm = g.getFontMetrics();
            String[] lines = wrap(app.title, fm, w - 8);
            // a single long word (e.g. "Multiprogramming") shrinks the font until it fits
            while (lines.length == 1 && fm.stringWidth(lines[0]) > w - 8 && size > 9) {
                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, --size));
                fm = g.getFontMetrics();
            }
            int y = 4 + 44 + 14;
            for (String line : lines) {
                int x = (w - fm.stringWidth(line)) / 2;
                g.setColor(new Color(0, 0, 0, 170));
                g.drawString(line, x + 1, y + 1);
                g.setColor(Color.WHITE);
                g.drawString(line, x, y);
                y += fm.getHeight() - 1;
            }
            g.dispose();
        }

        private String[] wrap(String s, FontMetrics fm, int max) {
            if (fm.stringWidth(s) <= max) return new String[]{s};
            int best = -1, mid = s.length() / 2;
            for (int i = 0; i < s.length(); i++)
                if (s.charAt(i) == ' ' && (best < 0 || Math.abs(i - mid) < Math.abs(best - mid))) best = i;
            if (best < 0) return new String[]{s};
            return new String[]{s.substring(0, best), s.substring(best + 1)};
        }
    }

    /** Translucent rounded card used by the desktop widgets. */
    private abstract static class Card extends JComponent {
        Card(int w, int h) {
            setPreferredSize(new Dimension(w, h));
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            RoundRectangle2D.Float shape = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g.setColor(new Color(16, 26, 54, 185));
            g.fill(shape);
            g.setColor(new Color(255, 255, 255, 70));
            g.draw(shape);
            paintCard(g);
            g.dispose();
        }

        abstract void paintCard(Graphics2D g);
    }

    private static final class CalendarWidget extends Card {
        CalendarWidget() { super(230, 196); }

        @Override void paintCard(Graphics2D g) {
            LocalDate today = LocalDate.now();
            YearMonth ym = YearMonth.from(today);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            g.drawString(ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + ym.getYear(), 14, 26);
            String[] days = {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};
            double cw = (getWidth() - 20) / 7.0;
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            FontMetrics fm = g.getFontMetrics();
            g.setColor(new Color(255, 255, 255, 170));
            for (int i = 0; i < 7; i++)
                g.drawString(days[i], (float) (10 + i * cw + cw / 2 - fm.stringWidth(days[i]) / 2.0), 50);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            fm = g.getFontMetrics();
            int offset = ym.atDay(1).getDayOfWeek().getValue() % 7;
            for (int d = 1; d <= ym.lengthOfMonth(); d++) {
                int idx = offset + d - 1, col = idx % 7, row = idx / 7;
                double cx = 10 + col * cw + cw / 2;
                int cy = 74 + row * 21;
                String s = String.valueOf(d);
                if (d == today.getDayOfMonth()) {
                    g.setColor(new Color(0x3b82f6));
                    g.fill(new Ellipse2D.Double(cx - 10, cy - 14, 20, 20));
                    g.setColor(new Color(255, 255, 255, 200));
                    g.draw(new Ellipse2D.Double(cx - 10, cy - 14, 20, 20));
                }
                g.setColor(Color.WHITE);
                g.drawString(s, (float) (cx - fm.stringWidth(s) / 2.0), cy);
            }
        }
    }

    private static final class SystemWidget extends Card {
        private double cpu, ram, usedGb, totalGb;

        SystemWidget() {
            super(230, 104);
            update();
            new javax.swing.Timer(1500, e -> { update(); repaint(); }).start();
        }

        @SuppressWarnings("deprecation")
        private void update() {
            try {
                OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
                double c = os.getSystemCpuLoad();
                cpu = c < 0 ? 0 : c;
                long total = os.getTotalPhysicalMemorySize(), free = os.getFreePhysicalMemorySize();
                totalGb = total / 1073741824.0;
                usedGb = (total - free) / 1073741824.0;
                ram = total > 0 ? (double) (total - free) / total : 0;
            } catch (Throwable ignored) { }
        }

        private void bar(Graphics2D g, String label, double frac, int y, Color c) {
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            g.drawString(label, 14, y + 9);
            int x = 48, w = getWidth() - x - 52;
            g.setColor(new Color(255, 255, 255, 70));
            g.fill(new RoundRectangle2D.Float(x, y, w, 9, 9, 9));
            g.setColor(c);
            g.fill(new RoundRectangle2D.Float(x, y, (float) Math.max(6, w * Math.min(1, frac)), 9, 9, 9));
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            String p = Math.round(frac * 100) + "%";
            g.drawString(p, getWidth() - 14 - g.getFontMetrics().stringWidth(p), y + 9);
        }

        @Override void paintCard(Graphics2D g) {
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            g.drawString("System", 14, 26);
            bar(g, "CPU", cpu, 38, new Color(0x4cc764));
            bar(g, "RAM", ram, 58, new Color(0xf2c230));
            g.setColor(new Color(255, 255, 255, 170));
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            g.drawString(String.format("%.1f of %.1f GB in use", usedGb, totalGb), 14, 90);
        }
    }

    // --------------------------------------------------------------- taskbar
    private JPanel buildTaskbar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0)) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setPaint(new GradientPaint(0, 0, new Color(0x2f6fe0), 0, getHeight(), new Color(0x0d3fa6)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(new Color(255, 255, 255, 90));
                g.drawLine(0, 0, getWidth(), 0);
                g.dispose();
            }
        };
        bar.setPreferredSize(new Dimension(10, 46));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 7));
        left.setOpaque(false);
        StartButton start = new StartButton();
        JPopupMenu startMenu = buildStartMenu();
        start.addActionListener(e -> startMenu.show(start, 0, -startMenu.getPreferredSize().height));
        left.add(start);
        left.add(search);
        search.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    List<App> m = matches(search.getText());
                    hideSuggestions();
                    if (!m.isEmpty()) { open(m.get(0)); search.setText(""); }
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    search.setText("");
                    hideSuggestions();
                } else {
                    showSuggestions();
                }
            }
        });
        search.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { hideSuggestions(); }
        });

        windowStrip.setOpaque(false);

        JPanel tray = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        tray.setOpaque(false);
        tray.add(battery);
        tray.add(clock);

        bar.add(left, BorderLayout.WEST);
        bar.add(windowStrip, BorderLayout.CENTER);
        bar.add(tray, BorderLayout.EAST);
        return bar;
    }

    private JMenuItem appItem(App a, Runnable after) {
        JMenuItem it = new JMenuItem(a.title, iconFor(a, 22));
        it.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        it.addActionListener(e -> { open(a); if (after != null) after.run(); });
        return it;
    }

    private JPopupMenu buildStartMenu() {
        JPopupMenu m = new JPopupMenu();
        JMenuItem head = new JMenuItem("VirtualOS");
        head.setEnabled(false);
        head.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        m.add(head);
        m.addSeparator();
        for (App a : APPS) m.add(appItem(a, null));
        m.addSeparator();
        JMenuItem wp = new JMenuItem("Change wallpaper...");
        wp.addActionListener(e -> chooseWallpaper());
        m.add(wp);
        JMenuItem off = new JMenuItem("Shut down");
        off.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "Close VirtualOS?", "Shut down", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                System.exit(0);
        });
        m.add(off);
        return m;
    }

    private List<App> matches(String q) {
        String s = q == null ? "" : q.trim().toLowerCase();
        List<App> out = new ArrayList<>();
        if (s.isEmpty()) return out;
        for (App a : APPS) if (a.title.toLowerCase().contains(s)) out.add(a);
        return out;
    }

    private void hideSuggestions() {
        if (suggestions != null) { suggestions.setVisible(false); suggestions = null; }
    }

    private void showSuggestions() {
        hideSuggestions();
        String q = search.getText().trim();
        if (q.isEmpty()) return;
        JPopupMenu pm = new JPopupMenu();
        pm.setFocusable(false);
        List<App> m = matches(q);
        if (m.isEmpty()) {
            JMenuItem none = new JMenuItem("No programs found");
            none.setEnabled(false);
            pm.add(none);
        } else {
            for (App a : m) pm.add(appItem(a, () -> search.setText("")));
        }
        suggestions = pm;
        pm.show(search, 0, -pm.getPreferredSize().height);
    }

    private static final class StartButton extends JButton {
        private boolean hover;

        StartButton() {
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFocusable(false);
            setPreferredSize(new Dimension(100, 32));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            Color top = hover ? new Color(0x58c862) : new Color(0x3fae49);
            Color bot = hover ? new Color(0x37a042) : new Color(0x278a33);
            g.setPaint(new GradientPaint(0, 0, top, 0, h, bot));
            g.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 16, 16));
            g.setColor(new Color(0, 0, 0, 70));
            g.draw(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 16, 16));
            g.setColor(Color.WHITE);
            for (int r = 0; r < 2; r++)
                for (int c = 0; c < 2; c++) g.fillRect(14 + c * 8, 8 + r * 8, 7, 7);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
            g.drawString("Start", 40, h / 2 + 5);
            g.dispose();
        }
    }

    private static final class SearchField extends JTextField {
        SearchField() {
            setOpaque(false);
            setBorder(new EmptyBorder(2, 12, 2, 30));
            setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            setPreferredSize(new Dimension(220, 30));
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));
            g.setColor(new Color(0, 0, 0, 70));
            g.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));
            g.dispose();
            super.paintComponent(g0);
            Graphics2D h = (Graphics2D) g0.create();
            h.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            h.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (getText().isEmpty() && !isFocusOwner()) {
                h.setColor(new Color(0x888888));
                h.setFont(getFont());
                FontMetrics fm = h.getFontMetrics();
                h.drawString("Search programs", 12, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
            h.setColor(new Color(0x2f6fe0));
            h.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = getWidth() - 22, cy = getHeight() / 2 - 2;
            h.draw(new Ellipse2D.Float(cx - 5, cy - 5, 10, 10));
            h.draw(new Line2D.Float(cx + 3, cy + 3, cx + 8, cy + 8));
            h.dispose();
        }
    }

    private static final class BatteryLabel extends JComponent {
        private int pct = -1;

        BatteryLabel() {
            setPreferredSize(new Dimension(0, 36));
            setVisible(false);
        }

        void set(int p) {
            pct = p;
            boolean show = p >= 0;
            setPreferredSize(new Dimension(show ? 72 : 0, 36));
            setVisible(show);
            revalidate();
            repaint();
        }

        @Override protected void paintComponent(Graphics g0) {
            if (pct < 0) return;
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int y = getHeight() / 2 - 6;
            g.setColor(Color.WHITE);
            g.drawRoundRect(2, y, 24, 12, 3, 3);
            g.fillRect(27, y + 3, 2, 6);
            g.setColor(pct < 20 ? new Color(0xe5484d) : new Color(0x4cc764));
            g.fillRect(4, y + 2, Math.max(1, 21 * pct / 100), 9);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            g.drawString(pct + "%", 34, getHeight() / 2 + 5);
            g.dispose();
        }
    }

    private static final class ClockLabel extends JComponent {
        private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
        private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US);

        ClockLabel() {
            setPreferredSize(new Dimension(96, 36));
            new javax.swing.Timer(1000, e -> repaint()).start();
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            LocalDateTime now = LocalDateTime.now();
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            String t = now.format(TIME);
            g.drawString(t, getWidth() - g.getFontMetrics().stringWidth(t), 15);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            String d = now.format(DATE);
            g.drawString(d, getWidth() - g.getFontMetrics().stringWidth(d), 30);
            g.dispose();
        }
    }

    // --------------------------------------------------------------- battery
    private void startBatteryPoller() {
        Thread t = new Thread(() -> {
            while (true) {
                final int p = readBattery();
                SwingUtilities.invokeLater(() -> battery.set(p));
                if (p < 0) return; // no battery (or unreadable): stop polling
                try { Thread.sleep(60000); } catch (InterruptedException e) { return; }
            }
        }, "battery-poller");
        t.setDaemon(true);
        t.start();
    }

    static int readBattery() {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("mac")) {
                Matcher m = Pattern.compile("(\\d+)%").matcher(run("pmset", "-g", "batt"));
                if (m.find()) return Integer.parseInt(m.group(1));
            } else if (os.contains("win")) {
                String out = run("powershell", "-NoProfile", "-Command",
                        "(Get-CimInstance Win32_Battery).EstimatedChargeRemaining").trim();
                Matcher m = Pattern.compile("(\\d+)").matcher(out);
                if (m.find()) return Math.min(100, Integer.parseInt(m.group(1)));
            } else {
                File f = new File("/sys/class/power_supply/BAT0/capacity");
                if (f.isFile()) return Integer.parseInt(new String(java.nio.file.Files.readAllBytes(f.toPath())).trim());
            }
        } catch (Exception ignored) { }
        return -1;
    }

    private static String run(String... cmd) throws Exception {
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        p.waitFor(5, TimeUnit.SECONDS);
        return out;
    }
}
