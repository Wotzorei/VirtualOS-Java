package minios;

import com.sun.management.OperatingSystemMXBean;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * VirtualOS - a small desktop shell that hosts the Mini OS Simulator modules.
 * Desktop icons (double-click) open each module in its own window; the taskbar has a Start menu,
 * program search, window buttons, show-desktop edge strip, and clickable clock/calendar.
 * Right-click the desktop to change the wallpaper or personalize the shell.
 */
public class VirtualOS extends JPanel {
    private static final String GAME_NAME = "BATTLE-Z";
    private static final String OS_NAME = "Jureve Vantage";

    private static final class Reminder {
        final String time;
        final String note;

        Reminder(String time, String note) {
            this.time = time;
            this.note = note;
        }

        @Override
        public String toString() {
            return "[" + time + "] " + note;
        }
    }

    // ------------------------------------------------------------------ apps
    static final class App {
        final String title;
        final Color color;
        final int glyph;
        final Supplier<JPanel> factory;

        App(String title, Color color, int glyph, Supplier<JPanel> factory) {
            this.title = title;
            this.color = color;
            this.glyph = glyph;
            this.factory = factory;
        }
    }

    private static List<App> loadAvailableApps() {
        List<App> apps = new ArrayList<>();

        // Dedicated OS icon/app
        apps.add(new App(OS_NAME, new Color(0x0ea5e9), 8, null));

        // Core VirtualOS modules
        apps.add(new App("CPU Scheduling", new Color(0xe63946), 0, CpuScheduling::new));
        apps.add(new App("Banker's Algorithm", new Color(0x2f6fdb), 1, Bankers::new));
        apps.add(new App("Paging", new Color(0xf06292), 2, Paging::new));
        apps.add(new App("Page Replacement", new Color(0x2f7fe0), 3, PageReplacement::new));
        apps.add(new App("Segmentation", new Color(0x2bb673), 4, Segmentation::new));
        apps.add(new App("Fragmentation", new Color(0x7e57c2), 5, Fragmentation::new));
        apps.add(new App("Disk Scheduling", new Color(0x1f9d55), 6, DiskScheduling::new));
        apps.add(new App("Multiprogramming", new Color(0x1e88e5), 7, Multiprogramming::new));

        // Optional Game App
        File gameBinDir = new File("C:\\Users\\laptop\\Documents\\Coding Stuff\\Platformer-Game-Learning\\bin");
        File mainClassFile = new File(gameBinDir, "main/MainClass.class");
        File stagedMainClass = new File("cache/battle-z/bin/main/MainClass.class");

        if (mainClassFile.exists() || stagedMainClass.exists()) 
            apps.add(new App(GAME_NAME, new Color(0x8b0000), 9, VirtualOS::createGameApp));

        return apps;
    }

    static final List<App> APPS = loadAvailableApps();

    private static Color lighten(Color c, float f) {
        return new Color(Math.round(c.getRed() + (255 - c.getRed()) * f),
                Math.round(c.getGreen() + (255 - c.getGreen()) * f),
                Math.round(c.getBlue() + (255 - c.getBlue()) * f));
    }

    private static final File DESIGNATED_GAME_DIR = new File("cache/battle-z");

    private static void copyDirectory(File source, File target) throws IOException {
        if (source == null || !source.exists()) return;
        if (source.isDirectory()) {
            if (!target.exists() && !target.mkdirs()) {
                throw new IOException("Failed to create directory: " + target);
            }
            File[] children = source.listFiles();
            if (children != null) {
                for (File child : children) {
                    copyDirectory(child, new File(target, child.getName()));
                }
            }
        } else {
            File parent = target.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("Failed to create parent directory: " + parent);
            }
            // Skip copying if the staged file is already up to date
            if (target.exists()
                    && source.length() == target.length()
                    && source.lastModified() <= target.lastModified()) {
                return;
            }
            try {
                java.nio.file.Files.copy(
                        source.toPath(),
                        target.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
            } catch (java.nio.file.FileSystemException ex) {
                // If Windows still holds a lock on an already-staged JAR/DLL, reuse it
                if (!target.exists()) throw ex;
            }
        }
    }

    private static JPanel createGameApp() {
        JPanel container = new JPanel(new BorderLayout());
        try {
            File sourceBinDir = new File("C:\\Users\\laptop\\Documents\\Coding Stuff\\Platformer-Game-Learning\\bin");
            File sourceRoot = sourceBinDir.getParentFile();
            File sourceLibDir = sourceRoot != null ? new File(sourceRoot, "lib") : null;

            File stagedBinDir = new File(DESIGNATED_GAME_DIR, "bin");
            File stagedLibDir = new File(DESIGNATED_GAME_DIR, "lib");

            if (sourceBinDir.exists()) {
                copyDirectory(sourceBinDir, stagedBinDir);
                if (sourceLibDir != null && sourceLibDir.exists() && sourceLibDir.isDirectory()) {
                    copyDirectory(sourceLibDir, stagedLibDir);
                }
            }

            List<java.net.URL> urlList = new ArrayList<>();
            urlList.add(stagedBinDir.toURI().toURL());

            if (stagedLibDir.exists() && stagedLibDir.isDirectory()) {
                File[] jars = stagedLibDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
                if (jars != null) {
                    for (File jar : jars) {
                        urlList.add(jar.toURI().toURL());
                    }
                }
            }

            java.net.URLClassLoader classLoader = new java.net.URLClassLoader(
                    urlList.toArray(new java.net.URL[0]),
                    VirtualOS.class.getClassLoader()
            );

            Class<?> gameClass = classLoader.loadClass("main.Game");
            java.lang.reflect.Constructor<?> ctor = gameClass.getConstructor(boolean.class);
            Object gameInstance = ctor.newInstance(false);

            java.lang.reflect.Method getPanelMethod = gameClass.getMethod("getGamePanel");
            JPanel gamePanel = (JPanel) getPanelMethod.invoke(gameInstance);

            java.util.concurrent.atomic.AtomicBoolean killed = new java.util.concurrent.atomic.AtomicBoolean(false);
            Runnable killProcess = () -> {
                if (!killed.compareAndSet(false, true)) return;

                // Stop any game loop method or background thread inside main.Game
                for (String methodName : new String[]{"stop", "stopGame", "close", "dispose"}) {
                    try {
                        java.lang.reflect.Method m = gameClass.getMethod(methodName);
                        m.invoke(gameInstance);
                        break;
                    } catch (Exception ignored) { }
                }
                for (java.lang.reflect.Field field : gameClass.getDeclaredFields()) {
                    try {
                        field.setAccessible(true);
                        Object val = field.get(gameInstance);
                        if (val instanceof Thread) {
                            ((Thread) val).interrupt();
                        } else if (field.getType() == boolean.class && field.getName().toLowerCase().contains("run")) {
                            field.setBoolean(gameInstance, false);
                        }
                    } catch (Exception ignored) { }
                }

                try {
                    classLoader.close();
                } catch (IOException ignored) { }

                // Dispose the hosting JInternalFrame so VirtualOS cleans up the process & taskbar
                JInternalFrame frame = (JInternalFrame) SwingUtilities.getAncestorOfClass(JInternalFrame.class, container);
                if (frame != null) {
                    try {
                        frame.setClosed(true);
                    } catch (PropertyVetoException e) {
                        frame.dispose();
                    }
                }
            };

            // Listen directly on gamePanel while it has active game focus
            gamePanel.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                        killProcess.run();
                    }
                }
            });

            // Fallback window-level key binding if container or a child has focus
            container.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "killGameProcess");
            container.getActionMap().put("killGameProcess", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    killProcess.run();
                }
            });

            container.add(gamePanel, BorderLayout.CENTER);

            container.addAncestorListener(new javax.swing.event.AncestorListener() {
                @Override public void ancestorAdded(javax.swing.event.AncestorEvent event) {
                    gamePanel.requestFocusInWindow();
                }
                @Override public void ancestorMoved(javax.swing.event.AncestorEvent event) {}
                @Override public void ancestorRemoved(javax.swing.event.AncestorEvent event) {
                    killProcess.run();
                }
            });

            container.setFocusable(true);
            container.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mousePressed(java.awt.event.MouseEvent e) {
                    gamePanel.requestFocusInWindow();
                }
            });

        } catch (Exception ex) {
            ex.printStackTrace();
            JLabel errLabel = new JLabel("Failed to load game: " + ex.getClass().getSimpleName(), SwingConstants.CENTER);
            errLabel.setForeground(Color.RED);
            container.add(errLabel, BorderLayout.CENTER);
        }
        return container;
    }

    // ------------------------------------------------------------ icon appearance
    enum IconShape {
        ROUNDED("Rounded"), CIRCLE("Circle"), SQUARE("Square");
        final String label;
        IconShape(String l) { label = l; }
    }

    enum IconColors {
        ORIGINAL("Original"), ACCENT("Theme accent"), MONO("Monochrome");
        final String label;
        IconColors(String l) { label = l; }
    }

    /** How icons look everywhere (desktop, windows, taskbar, menus). Saved between runs. */
    static final class IconStyle {
        static final int SMALL = 28, MEDIUM = 36, LARGE = 48;
        static int size = MEDIUM;
        static IconShape shape = IconShape.ROUNDED;
        static boolean gradient = true;
        static IconColors colors = IconColors.ORIGINAL;
        static boolean labels = true;
        static final Map<String, Integer> custom = new HashMap<>();

        static void load(Preferences p) {
            try {
                int sz = p.getInt("icon.size", MEDIUM);
                size = sz == SMALL || sz == LARGE ? sz : MEDIUM;
                shape = IconShape.valueOf(p.get("icon.shape", IconShape.ROUNDED.name()));
                colors = IconColors.valueOf(p.get("icon.colors", IconColors.ORIGINAL.name()));
                gradient = p.getBoolean("icon.gradient", true);
                labels = p.getBoolean("icon.labels", true);
                custom.clear();
                String raw = p.get("icon.custom", "");
                if (!raw.isEmpty()) for (String part : raw.split("\\|")) {
                    int eq = part.lastIndexOf('=');
                    if (eq > 0) custom.put(part.substring(0, eq), Integer.parseInt(part.substring(eq + 1)));
                }
            } catch (Exception ignored) { }
        }

        static void save(Preferences p) {
            try {
                p.putInt("icon.size", size);
                p.put("icon.shape", shape.name());
                p.put("icon.colors", colors.name());
                p.putBoolean("icon.gradient", gradient);
                p.putBoolean("icon.labels", labels);
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, Integer> e : custom.entrySet()) {
                    if (sb.length() > 0) sb.append('|');
                    sb.append(e.getKey()).append('=').append(e.getValue());
                }
                p.put("icon.custom", sb.toString());
            } catch (Exception ignored) { }
        }
    }

    static Color colorOf(App a) {
        Integer c = IconStyle.custom.get(a.title);
        if (c != null) return new Color(c);
        switch (IconStyle.colors) {
            case ACCENT: return Theme.PRIMARY;
            case MONO:   return Theme.isDark() ? new Color(0x5B6477) : new Color(0x6B7385);
            default:     return a.color;
        }
    }

    /** Draws a clean, compact app icon (squircle + crisp rim + geometric glyph). */
    static void paintGlyph(Graphics2D g0, App a, float x, float y, float s) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        final Color col = colorOf(a);
        Shape bg;
        switch (IconStyle.shape) {
            case CIRCLE: bg = new Ellipse2D.Float(x, y, s, s); break;
            case SQUARE: bg = new RoundRectangle2D.Float(x, y, s, s, s * 0.12f, s * 0.12f); break;
            default:     bg = new RoundRectangle2D.Float(x, y, s, s, s * 0.28f, s * 0.28f);
        }
        g.setPaint(IconStyle.gradient ? new GradientPaint(x, y, lighten(col, 0.20f), x, y + s, col) : col);
        g.fill(bg);

        if (s >= 20f) {
            g.setColor(new Color(255, 255, 255, 55));
            g.setStroke(new BasicStroke(1f));
            g.draw(bg);
        }

        float u = s / 10f;
        g.setColor(Color.WHITE);
        switch (a.glyph) {
            case 0: { // Clean CPU microchip
                for (int k = 0; k < 2; k++) {
                    float p = 3.55f + k * 2.25f;
                    g.fill(new RoundRectangle2D.Float(x + p * u, y + 1.9f * u, 0.65f * u, 1.1f * u, 0.3f * u, 0.3f * u));
                    g.fill(new RoundRectangle2D.Float(x + p * u, y + 7.0f * u, 0.65f * u, 1.1f * u, 0.3f * u, 0.3f * u));
                    g.fill(new RoundRectangle2D.Float(x + 1.9f * u, y + p * u, 1.1f * u, 0.65f * u, 0.3f * u, 0.3f * u));
                    g.fill(new RoundRectangle2D.Float(x + 7.0f * u, y + p * u, 1.1f * u, 0.65f * u, 0.3f * u, 0.3f * u));
                }
                g.fill(new RoundRectangle2D.Float(x + 2.9f * u, y + 2.9f * u, 4.2f * u, 4.2f * u, 0.9f * u, 0.9f * u));
                g.setColor(col);
                g.fill(new RoundRectangle2D.Float(x + 4.1f * u, y + 4.1f * u, 1.8f * u, 1.8f * u, 0.4f * u, 0.4f * u));
                break;
            }
            case 1: { // Clean shield with checkmark
                Path2D.Float p = new Path2D.Float();
                p.moveTo(x + 5f * u, y + 2.0f * u);
                p.lineTo(x + 7.7f * u, y + 3.0f * u);
                p.lineTo(x + 7.7f * u, y + 5.2f * u);
                p.curveTo(x + 7.7f * u, y + 6.8f * u, x + 6.4f * u, y + 7.8f * u, x + 5f * u, y + 8.3f * u);
                p.curveTo(x + 3.6f * u, y + 7.8f * u, x + 2.3f * u, y + 6.8f * u, x + 2.3f * u, y + 5.2f * u);
                p.lineTo(x + 2.3f * u, y + 3.0f * u);
                p.closePath();
                g.fill(p);
                g.setColor(col);
                g.setStroke(new BasicStroke(0.85f * u, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D.Float chk = new Path2D.Float();
                chk.moveTo(x + 3.7f * u, y + 5.0f * u);
                chk.lineTo(x + 4.7f * u, y + 6.0f * u);
                chk.lineTo(x + 6.4f * u, y + 4.1f * u);
                g.draw(chk);
                break;
            }
            case 2: { // Compact home / page frame
                Path2D.Float p = new Path2D.Float();
                p.moveTo(x + 5f * u, y + 2.0f * u);
                p.lineTo(x + 8.1f * u, y + 4.8f * u);
                p.lineTo(x + 7.2f * u, y + 4.8f * u);
                p.lineTo(x + 7.2f * u, y + 7.8f * u);
                p.lineTo(x + 2.8f * u, y + 7.8f * u);
                p.lineTo(x + 2.8f * u, y + 4.8f * u);
                p.lineTo(x + 1.9f * u, y + 4.8f * u);
                p.closePath();
                g.fill(p);
                g.setColor(col);
                g.fill(new RoundRectangle2D.Float(x + 4.3f * u, y + 5.6f * u, 1.4f * u, 2.2f * u, 0.3f * u, 0.3f * u));
                break;
            }
            case 3: { // Clean cycle ring with arrow
                g.setStroke(new BasicStroke(1.15f * u, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Arc2D.Float(x + 2.6f * u, y + 2.6f * u, 4.8f * u, 4.8f * u, 45, 275, Arc2D.OPEN));
                Path2D.Float arrow = new Path2D.Float();
                arrow.moveTo(x + 6.5f * u, y + 2.1f * u);
                arrow.lineTo(x + 7.5f * u, y + 3.5f * u);
                arrow.lineTo(x + 5.8f * u, y + 3.6f * u);
                arrow.closePath();
                g.fill(arrow);
                break;
            }
            case 4: { // Stacked memory segments
                g.fill(new RoundRectangle2D.Float(x + 2.4f * u, y + 2.5f * u, 5.2f * u, 2.1f * u, 0.8f * u, 0.8f * u));
                g.fill(new RoundRectangle2D.Float(x + 2.4f * u, y + 5.4f * u, 5.2f * u, 2.1f * u, 0.8f * u, 0.8f * u));
                g.setColor(col);
                g.fill(new Ellipse2D.Float(x + 3.1f * u, y + 3.15f * u, 0.8f * u, 0.8f * u));
                g.fill(new Ellipse2D.Float(x + 3.1f * u, y + 6.05f * u, 0.8f * u, 0.8f * u));
                break;
            }
            case 5: { // Compact 2x2 partition grid
                g.fill(new RoundRectangle2D.Float(x + 2.4f * u, y + 2.4f * u, 2.3f * u, 2.3f * u, 0.55f * u, 0.55f * u));
                g.fill(new RoundRectangle2D.Float(x + 5.3f * u, y + 2.4f * u, 2.3f * u, 2.3f * u, 0.55f * u, 0.55f * u));
                g.fill(new RoundRectangle2D.Float(x + 2.4f * u, y + 5.3f * u, 2.3f * u, 2.3f * u, 0.55f * u, 0.55f * u));
                g.setColor(new Color(255, 255, 255, 130));
                g.fill(new RoundRectangle2D.Float(x + 5.3f * u, y + 5.3f * u, 2.3f * u, 2.3f * u, 0.55f * u, 0.55f * u));
                break;
            }
            case 6: { // Minimalist disk platter
                g.setStroke(new BasicStroke(0.75f * u, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Ellipse2D.Float(x + 2.2f * u, y + 2.2f * u, 5.6f * u, 5.6f * u));
                g.draw(new Ellipse2D.Float(x + 3.8f * u, y + 3.8f * u, 2.4f * u, 2.4f * u));
                g.fill(new Ellipse2D.Float(x + 4.55f * u, y + 4.55f * u, 0.9f * u, 0.9f * u));
                g.draw(new Line2D.Float(x + 5.0f * u, y + 5.0f * u, x + 7.2f * u, y + 2.8f * u));
                break;
            }
            case 8: { // Minimalist OS control sliders
                g.setStroke(new BasicStroke(0.75f * u, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                float[] ys = {3.1f, 5.0f, 6.9f}, kx = {6.4f, 3.6f, 5.5f};
                for (float yy : ys) g.draw(new Line2D.Float(x + 2.3f * u, y + yy * u, x + 7.7f * u, y + yy * u));
                for (int k = 0; k < 3; k++) {
                    g.setColor(Color.WHITE);
                    g.fill(new Ellipse2D.Float(x + (kx[k] - 0.85f) * u, y + (ys[k] - 0.85f) * u, 1.7f * u, 1.7f * u));
                    g.setColor(col);
                    g.fill(new Ellipse2D.Float(x + (kx[k] - 0.35f) * u, y + (ys[k] - 0.35f) * u, 0.7f * u, 0.7f * u));
                }
                break;
            }
            case 9: { // Big Z with "Battle" cutting through the middle
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

                // Bold geometric Z path
                Path2D.Float zPath = new Path2D.Float();
                zPath.moveTo(x + 2.2f * u, y + 1.9f * u);
                zPath.lineTo(x + 7.8f * u, y + 1.9f * u);
                zPath.lineTo(x + 7.8f * u, y + 3.15f * u);
                zPath.lineTo(x + 3.75f * u, y + 6.85f * u);
                zPath.lineTo(x + 7.8f * u, y + 6.85f * u);
                zPath.lineTo(x + 7.8f * u, y + 8.1f * u);
                zPath.lineTo(x + 2.2f * u, y + 8.1f * u);
                zPath.lineTo(x + 2.2f * u, y + 6.85f * u);
                zPath.lineTo(x + 6.25f * u, y + 3.15f * u);
                zPath.lineTo(x + 2.2f * u, y + 3.15f * u);
                zPath.closePath();

                // Cut a clean horizontal channel through the center of the Z
                Area zArea = new Area(zPath);
                float bandH = 2.05f * u;
                zArea.subtract(new Area(new Rectangle2D.Float(
                        x + 1.0f * u, y + 5.0f * u - bandH / 2f, 8.0f * u, bandH)));
                g.fill(zArea);

                // Draw "Battle" inside the horizontal cutout
                String word = "Battle";
                float maxTextW = 7.8f * u;
                float fontSize = 2.3f * u;
                Font font = new Font(Font.SANS_SERIF, Font.BOLD, 10).deriveFont(fontSize);
                g.setFont(font);
                FontMetrics fm = g.getFontMetrics();
                if (fm.stringWidth(word) > maxTextW) {
                    fontSize *= maxTextW / fm.stringWidth(word);
                    font = font.deriveFont(fontSize);
                    g.setFont(font);
                    fm = g.getFontMetrics();
                }

                float tx = x + (s - fm.stringWidth(word)) / 2f;
                float ty = y + (s + fm.getAscent() - fm.getDescent()) / 2f;
                g.setColor(new Color(0, 0, 0, 120));
                g.drawString(word, tx, ty + 0.18f * u);
                g.setColor(Color.WHITE);
                g.drawString(word, tx, ty);
                break;
            }

            default: { // Compact ascending bars
                g.fill(new RoundRectangle2D.Float(x + 2.4f * u, y + 5.3f * u, 1.35f * u, 2.4f * u, 0.45f * u, 0.45f * u));
                g.fill(new RoundRectangle2D.Float(x + 4.32f * u, y + 3.7f * u, 1.35f * u, 4.0f * u, 0.45f * u, 0.45f * u));
                g.fill(new RoundRectangle2D.Float(x + 6.25f * u, y + 2.3f * u, 1.35f * u, 5.4f * u, 0.45f * u, 0.45f * u));
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
    private boolean autoArrange = true;
    private JPanel taskbar;
    private Runnable personalizeSync = () -> { };
    final App personalizeApp = new App("Personalize", new Color(0x7e57c2), 8, () -> new PersonalizePanel());
    private final SystemWidget system = new SystemWidget();
    private final Map<App, JInternalFrame> windows = new LinkedHashMap<>();
    private final Map<JInternalFrame, JToggleButton> buttons = new HashMap<>();
    private final List<JInternalFrame> hiddenByShowDesktop = new ArrayList<>();
    private final JPanel windowStrip = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 7));
    private final BatteryLabel battery = new BatteryLabel();
    private final ClockLabel clock = new ClockLabel();
    private final SearchField search = new SearchField();
    private final Map<LocalDate, List<Reminder>> reminders = new HashMap<>();
    private JPopupMenu calendarPopup;
    private JPopupMenu suggestions;
    private int cascade = 0;

    public VirtualOS() {
        setLayout(new BorderLayout());
        IconStyle.load(PREFS);
        desktop.setDesktopManager(new TaskbarDesktopManager());
        for (App a : APPS) {
            DesktopIcon ic = new DesktopIcon(a);
            icons.add(ic);
            desktop.add(ic, Integer.valueOf(-4));
        }
        loadArrangement();
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
        taskbar = buildTaskbar();
        add(taskbar, BorderLayout.SOUTH);
        loadWallpaper();
        Theme.addListener(this::onThemeChanged);
        updateOpenCount();

        startBatteryPoller();
    }

    // --------------------------------------------------------- process count
    private int openProcessCount() {
        int count = 0;
        for (JInternalFrame f : windows.values()) {
            if (f != null && !f.isClosed()) {
                count++;
            }
        }
        return count;
    }

    private List<String> openProcessNames() {
        List<String> names = new ArrayList<>();
        for (Map.Entry<App, JInternalFrame> entry : windows.entrySet()) {
            JInternalFrame f = entry.getValue();
            if (f != null && !f.isClosed()) {
                String state = (f.isIcon() || !f.isVisible()) ? " [Hidden]" : "";
                names.add(entry.getKey().title + state);
            }
        }
        return names;
    }

    private void updateOpenCount() {
        int count = openProcessCount();
        List<String> names = openProcessNames();
        system.setToolTipText(names.isEmpty()
                ? "Open Processes: 0"
                : "Open Processes (" + count + "): " + String.join(", ", names));
        system.repaint();
    }

    private void toggleShowDesktop() {
        List<JInternalFrame> currentlyVisible = new ArrayList<>();
        for (JInternalFrame f : windows.values()) {
            if (f != null && !f.isClosed() && f.isVisible() && !f.isIcon()) {
                currentlyVisible.add(f);
            }
        }

        if (!currentlyVisible.isEmpty()) {
            hiddenByShowDesktop.clear();
            for (JInternalFrame f : currentlyVisible) {
                f.setVisible(false);
                hiddenByShowDesktop.add(f);
            }
        } else if (!hiddenByShowDesktop.isEmpty()) {
            for (JInternalFrame f : new ArrayList<>(hiddenByShowDesktop)) {
                if (!f.isClosed()) {
                    f.setVisible(true);
                    f.toFront();
                }
            }
            hiddenByShowDesktop.clear();
        }
        syncButtons();
    }

    private JPanel buildJureveVantagePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(12, 14, 12, 14));

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(Theme.MONO);
        area.setBackground(Theme.INSET);
        area.setForeground(Theme.TEXT);

        Runnable refresh = () -> {
            int count = openProcessCount();
            List<String> openNames = openProcessNames();
            Runtime rt = Runtime.getRuntime();
            long totalMb = rt.totalMemory() / (1024 * 1024);
            long freeMb = rt.freeMemory() / (1024 * 1024);
            long usedMb = totalMb - freeMb;

            StringBuilder sb = new StringBuilder();
            sb.append("OS Name         : ").append(OS_NAME).append("\n");
            sb.append("Host System     : ").append(System.getProperty("os.name")).append(" (")
              .append(System.getProperty("os.arch")).append(")\n");
            sb.append("Java Runtime    : ").append(System.getProperty("java.version")).append("\n");
            sb.append("Available Cores : ").append(rt.availableProcessors()).append("\n");
            sb.append("JVM Memory      : ").append(usedMb).append(" MB used / ").append(totalMb).append(" MB allocated\n");
            sb.append("Open Processes  : ").append(count).append("\n");
            sb.append("Running Apps    : ").append(openNames.isEmpty() ? "None" : String.join(", ", openNames)).append("\n");
            area.setText(sb.toString());
        };

        refresh.run();
        javax.swing.Timer timer = new javax.swing.Timer(1500, e -> refresh.run());
        timer.start();
        panel.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0 && !panel.isDisplayable()) {
                timer.stop();
            }
        });

        JButton refreshBtn = Common.softButton("Refresh", refresh);
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.setOpaque(false);
        south.add(refreshBtn);

        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    // ---------------------------------------------------------------- layout
    private static final int MARGIN_X = 8, MARGIN_Y = 8;

    private int cellW() { return Math.max(74, IconStyle.size + 38); }
    private int cellH() { return IconStyle.labels ? IconStyle.size + 32 : IconStyle.size + 12; }
    private int maxRows() { return Math.max(1, (desktop.getHeight() - MARGIN_Y) / cellH()); }
    private int maxCols() { return Math.max(1, (desktop.getWidth() - MARGIN_X) / cellW()); }

    void relayout() {
        int w = desktop.getWidth(), h = desktop.getHeight();
        if (w <= 0 || h <= 0) return;
        int rows = maxRows(), cols = maxCols(), cw = cellW(), ch = cellH();
        if (autoArrange || rows * cols < icons.size()) {
            flowCells(rows);
            for (DesktopIcon ic : icons) ic.setBounds(MARGIN_X + ic.col * cw, MARGIN_Y + ic.row * ch, cw, ch);
        } else {
            boolean[][] used = new boolean[cols][rows];
            for (DesktopIcon ic : icons) {
                int c = Math.max(0, Math.min(cols - 1, ic.col)), r = Math.max(0, Math.min(rows - 1, ic.row));
                if (used[c][r]) { int[] f = nearestFree(used, c, r); c = f[0]; r = f[1]; }
                used[c][r] = true;
                ic.setBounds(MARGIN_X + c * cw, MARGIN_Y + r * ch, cw, ch);
            }
        }
        Dimension sd = system.getPreferredSize();
        system.setBounds(w - sd.width - 14, 12, sd.width, sd.height);
        desktop.repaint();
    }

    private void flowCells(int rows) {
        int i = 0;
        for (DesktopIcon ic : icons) { ic.col = i / rows; ic.row = i % rows; i++; }
    }

    private static int[] nearestFree(boolean[][] used, int c0, int r0) {
        int bestC = -1, bestR = -1, best = Integer.MAX_VALUE;
        for (int c = 0; c < used.length; c++)
            for (int r = 0; r < used[c].length; r++) {
                if (used[c][r]) continue;
                int d = (c - c0) * (c - c0) + (r - r0) * (r - r0);
                if (d < best) { best = d; bestC = c; bestR = r; }
            }
        return new int[]{bestC, bestR};
    }

    private void select(DesktopIcon sel) {
        for (DesktopIcon ic : icons) ic.setSelected(ic == sel);
    }

    private void dropIcon(DesktopIcon ic) {
        int cols = maxCols(), rows = maxRows(), cw = cellW(), ch = cellH();
        int c = Math.max(0, Math.min(cols - 1, Math.round((ic.getX() - MARGIN_X) / (float) cw)));
        int r = Math.max(0, Math.min(rows - 1, Math.round((ic.getY() - MARGIN_Y) / (float) ch)));
        if (autoArrange) {
            int index = Math.max(0, Math.min(icons.size() - 1, c * rows + r));
            icons.remove(ic);
            icons.add(index, ic);
        } else {
            boolean[][] used = new boolean[cols][rows];
            for (DesktopIcon o : icons) {
                if (o == ic) continue;
                int oc = Math.max(0, Math.min(cols - 1, Math.round((o.getX() - MARGIN_X) / (float) cw)));
                int or = Math.max(0, Math.min(rows - 1, Math.round((o.getY() - MARGIN_Y) / (float) ch)));
                used[oc][or] = true;
            }
            if (used[c][r]) { int[] f = nearestFree(used, c, r); c = f[0]; r = f[1]; }
            ic.col = c; ic.row = r;
        }
        relayout();
        saveArrangement();
    }

    void setAutoArrange(boolean on) {
        if (on == autoArrange) return;
        if (!on) { flowCellsFromScreen(); }
        autoArrange = on;
        relayout();
        saveArrangement();
        personalizeSync.run();
    }

    private void flowCellsFromScreen() {
        int cw = cellW(), ch = cellH();
        for (DesktopIcon ic : icons) {
            ic.col = Math.max(0, Math.round((ic.getX() - MARGIN_X) / (float) cw));
            ic.row = Math.max(0, Math.round((ic.getY() - MARGIN_Y) / (float) ch));
        }
    }

    void sortIcons(Comparator<DesktopIcon> cmp) {
        icons.sort(cmp);
        flowCells(maxRows());
        relayout();
        saveArrangement();
    }

    void sortByName(boolean ascending) {
        sortIcons((x, y) -> ascending ? x.app.title.compareToIgnoreCase(y.app.title) : y.app.title.compareToIgnoreCase(x.app.title));
    }

    void sortByColor() {
        sortIcons(Comparator.<DesktopIcon>comparingDouble(ic -> {
            Color c = colorOf(ic.app);
            return Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null)[0];
        }).thenComparing(ic -> ic.app.title));
    }

    void sortDefault() { sortIcons(Comparator.comparingInt(ic -> APPS.indexOf(ic.app))); }

    private void saveArrangement() {
        try {
            StringBuilder order = new StringBuilder(), cells = new StringBuilder();
            for (DesktopIcon ic : icons) {
                if (order.length() > 0) { order.append('|'); cells.append('|'); }
                order.append(ic.app.title);
                cells.append(ic.app.title).append('=').append(ic.col).append(',').append(ic.row);
            }
            PREFS.put("icons.order", order.toString());
            PREFS.put("icons.cells", cells.toString());
            PREFS.putBoolean("icons.auto", autoArrange);
        } catch (Exception ignored) { }
    }

    private void loadArrangement() {
        try {
            autoArrange = PREFS.getBoolean("icons.auto", true);
            String order = PREFS.get("icons.order", "");
            if (!order.isEmpty()) {
                List<DesktopIcon> sorted = new ArrayList<>();
                for (String t : order.split("\\|"))
                    for (DesktopIcon ic : icons) if (ic.app.title.equals(t) && !sorted.contains(ic)) sorted.add(ic);
                for (DesktopIcon ic : icons) if (!sorted.contains(ic)) sorted.add(ic);
                icons.clear();
                icons.addAll(sorted);
            }
            String cells = PREFS.get("icons.cells", "");
            if (!cells.isEmpty()) for (String part : cells.split("\\|")) {
                int eq = part.lastIndexOf('='), cm = part.lastIndexOf(',');
                if (eq < 0 || cm < eq) continue;
                for (DesktopIcon ic : icons) if (ic.app.title.equals(part.substring(0, eq))) {
                    ic.col = Integer.parseInt(part.substring(eq + 1, cm));
                    ic.row = Integer.parseInt(part.substring(cm + 1));
                }
            }
        } catch (Exception ignored) { }
    }

    // ------------------------------------------------------- icon look + theme changes
    void iconStyleChanged() {
        IconStyle.save(PREFS);
        for (Map.Entry<App, JInternalFrame> e : windows.entrySet()) {
            e.getValue().setFrameIcon(iconFor(e.getKey(), 16));
            JToggleButton b = buttons.get(e.getValue());
            if (b != null) b.setIcon(iconFor(e.getKey(), 16));
        }
        relayout();
        desktop.repaint();
        windowStrip.repaint();
        personalizeSync.run();
    }

    void setMode(Theme.Mode m) { if (m != Theme.mode()) Theme.change(Theme.scheme(), m); }
    void setScheme(Theme.Scheme sc) { if (sc != Theme.scheme()) Theme.change(sc, Theme.mode()); }
    void toggleMode() { setMode(Theme.isDark() ? Theme.Mode.LIGHT : Theme.Mode.DARK); }

    private void onThemeChanged() {
        hideSuggestions();
        Theme.recolor(this);
        for (JInternalFrame f : windows.values()) f.repaint();
        relayout();
        desktop.repaint();
        taskbar.repaint();
        repaint();
        personalizeSync.run();
    }

    private final java.util.Set<App> launchingApps = new java.util.HashSet<>();

    private void setLoadingCursor(boolean loading) {
        Cursor cursor = Cursor.getPredefinedCursor(loading ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR);
        setCursor(cursor);
        desktop.setCursor(cursor);

        JRootPane root = SwingUtilities.getRootPane(this);
        if (root != null) {
            Component glass = root.getGlassPane();
            glass.setCursor(cursor);
            glass.setVisible(loading);
        }
    }

    // --------------------------------------------------------------- windows
    void open(App a) {
        JInternalFrame existing = windows.get(a);
        if (existing != null && !existing.isClosed()) {
            restore(existing);
            return;
        }
        if (launchingApps.contains(a)) {
            return;
        }

        launchingApps.add(a);
        setLoadingCursor(true);

        new SwingWorker<JPanel, Void>() {
            @Override
            protected JPanel doInBackground() {
                if (OS_NAME.equals(a.title)) {
                    return buildJureveVantagePanel();
                }
                return a.factory != null ? a.factory.get() : null;
            }

            @Override
            protected void done() {
                try {
                    JPanel content = get();
                    if (content == null) return;

                    final JInternalFrame f = new JInternalFrame(a.title, true, true, true, true);
                    f.setFrameIcon(iconFor(a, 16));
                    f.setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
                    f.setContentPane(content);

                    if (a.title.equals(GAME_NAME)) {
                        f.pack();
                        int x = (desktop.getWidth() - f.getWidth()) / 2;
                        int y = (desktop.getHeight() - f.getHeight()) / 2;
                        f.setLocation(Math.max(0, x), Math.max(0, y));
                    } else {
                        int dw = Math.max(400, desktop.getWidth()), dh = Math.max(300, desktop.getHeight());
                        boolean small = a == personalizeApp || OS_NAME.equals(a.title);
                        int w = Math.min(small ? 560 : 1000, dw - 40), h = Math.min(small ? 520 : 700, dh - 20);
                        f.setSize(w, h);

                        int off = (cascade++ % 6) * 28;
                        f.setLocation(Math.min(110 + off, Math.max(0, dw - w)), Math.min(16 + off, Math.max(0, dh - h)));
                    }

                    desktop.add(f);
                    f.setVisible(true);
                    windows.put(a, f);
                    updateOpenCount();

                    f.addInternalFrameListener(new InternalFrameAdapter() {
                        @Override public void internalFrameClosed(InternalFrameEvent e) {
                            if (windows.get(a) == f) windows.remove(a);
                            hiddenByShowDesktop.remove(f);
                            JToggleButton btn = buttons.remove(f);
                            if (btn != null) { windowStrip.remove(btn); windowStrip.revalidate(); windowStrip.repaint(); }
                            updateOpenCount();
                        }
                        @Override public void internalFrameActivated(InternalFrameEvent e) { syncButtons(); }
                        @Override public void internalFrameDeactivated(InternalFrameEvent e) { syncButtons(); }
                    });

                    JToggleButton b = new TaskButton(a.title, iconFor(a, 16));
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
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    launchingApps.remove(a);
                    if (launchingApps.isEmpty()) {
                        setLoadingCursor(false);
                    }
                }
            }
        }.execute();
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

    private static final class TaskButton extends JToggleButton {
        private boolean hover;

        TaskButton(String text, Icon icon) {
            super(text, icon);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFocusable(false);
            setOpaque(false);
            setHorizontalAlignment(SwingConstants.LEFT);
            setIconTextGap(8);
            setBorder(new EmptyBorder(0, 10, 0, 8));
            setFont(Theme.BASE);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        @Override public Color getForeground() { return Color.WHITE; }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(255, 255, 255, isSelected() ? 72 : hover ? 50 : 28));
            g.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
            if (isSelected()) {
                g.setColor(new Color(255, 255, 255, 110));
                g.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
            }
            g.dispose();
            super.paintComponent(g0);
        }
    }

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
        JMenu sort = new JMenu("Sort icons by");
        addItem(sort, "Name (A to Z)", () -> sortByName(true));
        addItem(sort, "Name (Z to A)", () -> sortByName(false));
        addItem(sort, "Color", this::sortByColor);
        addItem(sort, "Default order", this::sortDefault);
        m.add(sort);
        JCheckBoxMenuItem auto = new JCheckBoxMenuItem("Auto-arrange icons", autoArrange);
        auto.addActionListener(ev -> setAutoArrange(auto.isSelected()));
        m.add(auto);
        JMenu size = new JMenu("Icon size");
        ButtonGroup sg = new ButtonGroup();
        for (int[] o : new int[][]{{IconStyle.SMALL}, {IconStyle.MEDIUM}, {IconStyle.LARGE}}) {
            String name = o[0] == IconStyle.SMALL ? "Small" : o[0] == IconStyle.MEDIUM ? "Medium" : "Large";
            JRadioButtonMenuItem it = new JRadioButtonMenuItem(name, IconStyle.size == o[0]);
            it.addActionListener(ev -> { IconStyle.size = o[0]; iconStyleChanged(); });
            sg.add(it);
            size.add(it);
        }
        m.add(size);
        JCheckBoxMenuItem lbl = new JCheckBoxMenuItem("Show icon names", IconStyle.labels);
        lbl.addActionListener(ev -> { IconStyle.labels = lbl.isSelected(); iconStyleChanged(); });
        m.add(lbl);
        m.addSeparator();
        addItem(m, Theme.isDark() ? "Switch to light mode" : "Switch to dark mode", this::toggleMode);
        JMenu themes = new JMenu("Theme");
        ButtonGroup tg = new ButtonGroup();
        for (Theme.Scheme sc : Theme.SCHEMES) {
            JRadioButtonMenuItem it = new JRadioButtonMenuItem(sc.name, sc == Theme.scheme());
            it.addActionListener(ev -> setScheme(sc));
            tg.add(it);
            themes.add(it);
        }
        m.add(themes);
        addItem(m, "Personalize...", () -> open(personalizeApp));
        m.addSeparator();
        addItem(m, "Change wallpaper...", this::chooseWallpaper);
        addItem(m, "Reset wallpaper", this::resetWallpaper);
        m.show(e.getComponent(), e.getX(), e.getY());
    }

    private static void addItem(Container menu, String text, Runnable action) {
        JMenuItem it = new JMenuItem(text);
        it.addActionListener(ev -> action.run());
        menu.add(it);
    }

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
                g.setPaint(new GradientPaint(0, 0, Theme.DESK_A, w, h, Theme.DESK_B));
                g.fillRect(0, 0, w, h);
                g.setColor(new Color(255, 255, 255, 18));
                g.fill(new Ellipse2D.Double(w * 0.35, -h * 0.3, h * 1.2, h * 1.2));
                g.setColor(new Color(255, 255, 255, 14));
                g.fill(new Ellipse2D.Double(w * 0.1, h * 0.45, h * 0.9, h * 0.9));
                g.setColor(new Color(255, 255, 255, 38));
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
                String t = OS_NAME;
                g.drawString(t, w - g.getFontMetrics().stringWidth(t) - 24, h - 22);
            }
            g.dispose();
        }
    }

    // --------------------------------------------------------------- widgets
    private final class DesktopIcon extends JComponent {
        private final App app;
        private boolean selected;
        int col, row;
        private Point pressAt, startAt;
        private boolean dragging;

        DesktopIcon(App app) {
            this.app = app;
            setOpaque(false);
            setToolTipText(app.title);
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    select(DesktopIcon.this);
                    if (e.isPopupTrigger()) { menu(e); return; }
                    if (!SwingUtilities.isLeftMouseButton(e)) return;
                    if (e.getClickCount() == 2) { pressAt = null; open(app); return; }
                    pressAt = SwingUtilities.convertPoint(DesktopIcon.this, e.getPoint(), desktop);
                    startAt = getLocation();
                    dragging = false;
                }
                @Override public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger()) { menu(e); return; }
                    boolean was = dragging;
                    dragging = false;
                    pressAt = null;
                    if (was) dropIcon(DesktopIcon.this);
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseDragged(MouseEvent e) {
                    if (pressAt == null) return;
                    Point p = SwingUtilities.convertPoint(DesktopIcon.this, e.getPoint(), desktop);
                    int dx = p.x - pressAt.x, dy = p.y - pressAt.y;
                    if (!dragging && Math.abs(dx) + Math.abs(dy) < 6) return;
                    dragging = true;
                    int nx = Math.max(0, Math.min(desktop.getWidth() - getWidth(), startAt.x + dx));
                    int ny = Math.max(0, Math.min(desktop.getHeight() - getHeight(), startAt.y + dy));
                    setLocation(nx, ny);
                }
            });
        }

        private void menu(MouseEvent e) {
            JPopupMenu m = new JPopupMenu();
            addItem(m, "Open", () -> open(app));
            m.addSeparator();
            addItem(m, "Change icon color...", () -> {
                Color c = JColorChooser.showDialog(VirtualOS.this, "Icon color - " + app.title, colorOf(app));
                if (c != null) { IconStyle.custom.put(app.title, c.getRGB() & 0xFFFFFF); iconStyleChanged(); }
            });
            JMenuItem reset = new JMenuItem("Reset icon color");
            reset.setEnabled(IconStyle.custom.containsKey(app.title));
            reset.addActionListener(ev -> { IconStyle.custom.remove(app.title); iconStyleChanged(); });
            m.add(reset);
            addItem(m, "Personalize...", () -> open(personalizeApp));
            m.show(this, e.getX(), e.getY());
        }

        void setSelected(boolean s) {
            if (selected != s) { selected = s; repaint(); }
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight(), sz = IconStyle.size;
            if (selected || dragging) {
                g.setColor(new Color(255, 255, 255, dragging ? 75 : 52));
                g.fill(new RoundRectangle2D.Float(3, 2, w - 6, h - 4, 10, 10));
                g.setColor(new Color(255, 255, 255, 115));
                g.draw(new RoundRectangle2D.Float(3, 2, w - 6, h - 4, 10, 10));
            }
            paintGlyph(g, app, (w - sz) / 2f, 5, sz);
            if (IconStyle.labels) {
                int size = 11;
                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, size));
                FontMetrics fm = g.getFontMetrics();
                String[] lines = wrap(app.title, fm, w - 6);
                while (lines.length == 1 && fm.stringWidth(lines[0]) > w - 6 && size > 9) {
                    g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, --size));
                    fm = g.getFontMetrics();
                }
                int y = 5 + sz + 12;
                for (String line : lines) {
                    int x = (w - fm.stringWidth(line)) / 2;
                    g.setColor(new Color(0, 0, 0, 175));
                    g.drawString(line, x + 1, y + 1);
                    g.setColor(Color.WHITE);
                    g.drawString(line, x, y);
                    y += fm.getHeight() - 1;
                }
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
            g.setColor(Theme.WIDGET_BG);
            g.fill(shape);
            g.setColor(Theme.WIDGET_TRACK);
            g.draw(shape);
            paintCard(g);
            g.dispose();
        }

        abstract void paintCard(Graphics2D g);
    }

    private final class SystemWidget extends Card {
        private double cpu, ram, usedGb, totalGb;

        SystemWidget() {
            super(230, 118);
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
            g.setColor(Theme.WIDGET_TEXT);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            g.drawString(label, 14, y + 9);
            int x = 48, w = getWidth() - x - 52;
            g.setColor(Theme.WIDGET_TRACK);
            g.fill(new RoundRectangle2D.Float(x, y, w, 9, 9, 9));
            g.setColor(c);
            g.fill(new RoundRectangle2D.Float(x, y, (float) Math.max(6, w * Math.min(1, frac)), 9, 9, 9));
            g.setColor(Theme.WIDGET_TEXT);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            String p = Math.round(frac * 100) + "%";
            g.drawString(p, getWidth() - 14 - g.getFontMetrics().stringWidth(p), y + 9);
        }

        @Override void paintCard(Graphics2D g) {
            g.setColor(Theme.WIDGET_TEXT);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            g.drawString("System", 14, 24);
            bar(g, "CPU", cpu, 36, new Color(0x4cc764));
            bar(g, "RAM", ram, 55, new Color(0xf2c230));
            g.setColor(Theme.WIDGET_MUTED);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            g.drawString(String.format("%.1f of %.1f GB in use", usedGb, totalGb), 14, 83);

            g.setColor(Theme.WIDGET_TRACK);
            g.drawLine(14, 91, getWidth() - 14, 91);

            int openCount = openProcessCount();
            g.setColor(openCount > 0 ? Theme.TEAL : Theme.WIDGET_MUTED);
            g.fill(new Ellipse2D.Float(14, 100, 7, 7));

            g.setColor(Theme.WIDGET_TEXT);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            g.drawString("Open Processes", 26, 107);

            String countStr = String.valueOf(openCount);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            g.drawString(countStr, getWidth() - 14 - g.getFontMetrics().stringWidth(countStr), 107);
        }
    }

    // ------------------------------------------------- popup calendar & reminders
    private void toggleCalendarPopup() {
        if (calendarPopup != null && calendarPopup.isVisible()) {
            calendarPopup.setVisible(false);
            return;
        }
        calendarPopup = new JPopupMenu();
        calendarPopup.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        calendarPopup.add(new CalendarPopupPanel());
        calendarPopup.pack();
        int px = Math.max(0, clock.getWidth() - calendarPopup.getPreferredSize().width);
        int py = -calendarPopup.getPreferredSize().height;
        calendarPopup.show(clock, px, py);
    }

    private final class CalendarPopupPanel extends JPanel {
        private LocalDate selectedDate = LocalDate.now();
        private final JLabel selectedHeader = new JLabel();
        private final JTextField timeInput = new JTextField("09:00 AM", 8);
        private final JTextField noteInput = new JTextField(15);
        private final DefaultListModel<Reminder> reminderModel = new DefaultListModel<>();
        private final List<CustomButton> dayButtons = new ArrayList<>();
        private final List<LocalDate> dayDates = new ArrayList<>();

        CalendarPopupPanel() {
            setLayout(new BorderLayout(6, 6));
            setBorder(new EmptyBorder(10, 10, 10, 10));
            setBackground(Theme.CARD);
            setPreferredSize(new Dimension(320, 420));

            YearMonth ym = YearMonth.from(LocalDate.now());
            JLabel title = new JLabel(
                    ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + ym.getYear(),
                    SwingConstants.CENTER
            );
            title.setFont(Theme.BOLD);
            add(title, BorderLayout.NORTH);

            JPanel grid = new JPanel(new GridLayout(0, 7, 3, 3));
            grid.setOpaque(false);
            for (String dayName : new String[]{"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"}) {
                JLabel lbl = new JLabel(dayName, SwingConstants.CENTER);
                lbl.setFont(Theme.SMALL);
                lbl.setForeground(Theme.MUTED);
                grid.add(lbl);
            }

            int offset = ym.atDay(1).getDayOfWeek().getValue() % 7;
            for (int i = 0; i < offset; i++) {
                grid.add(new JLabel(""));
            }

            LocalDate today = LocalDate.now();
            for (int day = 1; day <= ym.lengthOfMonth(); day++) {
                LocalDate date = ym.atDay(day);
                CustomButton btn = new CustomButton(String.valueOf(day), CustomButton.Variant.CALENDAR);
                btn.setToday(date.equals(today));
                btn.addActionListener(e -> selectDate(date));
                dayButtons.add(btn);
                dayDates.add(date);
                grid.add(btn);
            }
            add(grid, BorderLayout.CENTER);

            JPanel bottom = new JPanel(new BorderLayout(4, 4));
            bottom.setOpaque(false);

            selectedHeader.setFont(Theme.BOLD);
            bottom.add(selectedHeader, BorderLayout.NORTH);

            JList<Reminder> list = new JList<>(reminderModel);
            list.setFont(Theme.SMALL);
            list.setVisibleRowCount(4);
            bottom.add(new JScrollPane(list), BorderLayout.CENTER);

            JPanel controls = new JPanel(new BorderLayout(4, 4));
            controls.setOpaque(false);

            JPanel fields = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            fields.setOpaque(false);
            fields.add(new JLabel("Time:"));
            fields.add(timeInput);
            fields.add(new JLabel("Note:"));
            fields.add(noteInput);

            JButton addBtn = Common.button("Add Reminder", () -> {
                String t = timeInput.getText().trim();
                String n = noteInput.getText().trim();
                if (t.isEmpty() || n.isEmpty()) return;
                reminders.computeIfAbsent(selectedDate, k -> new ArrayList<>()).add(new Reminder(t, n));
                noteInput.setText("");
                refreshList();
            });

            controls.add(fields, BorderLayout.CENTER);
            controls.add(addBtn, BorderLayout.SOUTH);
            bottom.add(controls, BorderLayout.SOUTH);

            add(bottom, BorderLayout.SOUTH);
            selectDate(LocalDate.now());
        }

        private void selectDate(LocalDate date) {
            this.selectedDate = date;
            for (int i = 0; i < dayButtons.size(); i++) {
                dayButtons.get(i).setSelectedDay(dayDates.get(i).equals(date));
            }
            selectedHeader.setText("Reminders - " + date);
            refreshList();
        }

        private void refreshList() {
            reminderModel.clear();
            List<Reminder> items = reminders.get(selectedDate);
            if (items != null) {
                for (Reminder r : items) {
                    reminderModel.addElement(r);
                }
            }
        }
    }

    // --------------------------------------------------------------- taskbar
    private JPanel buildTaskbar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0)) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setPaint(new GradientPaint(0, 0, Theme.TASK_A, 0, getHeight(), Theme.TASK_B));
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
        start.addActionListener(e -> {
            JPopupMenu startMenu = buildStartMenu();
            startMenu.show(start, 0, -startMenu.getPreferredSize().height);
        });
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

        clock.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clock.setToolTipText("Click to toggle calendar & reminders");
        clock.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) toggleCalendarPopup();
            }
        });

        CustomButton showDesktopBtn = new CustomButton("", CustomButton.Variant.EDGE);
        showDesktopBtn.setPreferredSize(new Dimension(18, 46));
        showDesktopBtn.setMinimumSize(new Dimension(18, 46));
        showDesktopBtn.setMaximumSize(new Dimension(18, 46));
        showDesktopBtn.setToolTipText("Show Desktop (Hide or restore all open windows)");
        showDesktopBtn.addActionListener(e -> toggleShowDesktop());

        JPanel tray = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        tray.setOpaque(false);
        tray.add(new ModeToggle());
        tray.add(battery);
        tray.add(clock);

        JPanel east = new JPanel(new BorderLayout(4, 0));
        east.setOpaque(false);
        east.add(tray, BorderLayout.CENTER);
        east.add(showDesktopBtn, BorderLayout.EAST);

        bar.add(left, BorderLayout.WEST);
        bar.add(windowStrip, BorderLayout.CENTER);
        bar.add(east, BorderLayout.EAST);
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
        JMenuItem head = new JMenuItem(OS_NAME);
        head.setEnabled(false);
        head.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        m.add(head);
        m.addSeparator();
        for (App a : APPS) m.add(appItem(a, null));
        m.addSeparator();
        addItem(m, Theme.isDark() ? "Switch to light mode" : "Switch to dark mode", this::toggleMode);
        addItem(m, "Personalize...", () -> open(personalizeApp));
        JMenuItem wp = new JMenuItem("Change wallpaper...");
        wp.addActionListener(e -> chooseWallpaper());
        m.add(wp);
        JMenuItem off = new JMenuItem("Shut down");
        off.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "Close " + OS_NAME + "?", "Shut down", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
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

    private static final class StartButton extends CustomButton {
        private boolean hover;

        StartButton() {
            super("Start", Variant.PRIMARY);
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
            Color top = hover ? Theme.mix(Theme.START_A, Color.WHITE, .18f) : Theme.START_A;
            Color bot = hover ? Theme.mix(Theme.START_B, Color.WHITE, .14f) : Theme.START_B;
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
        @Override public Color getForeground() { return Theme.isDark() ? Color.WHITE : Theme.TEXT; }
        @Override public Color getCaretColor() { return Theme.isDark() ? Color.WHITE : Theme.TEXT; }

        SearchField() {
            setOpaque(false);
            setBorder(new EmptyBorder(2, 12, 2, 30));
            setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            setPreferredSize(new Dimension(220, 30));
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Theme.isDark() ? Theme.mix(Theme.TASK_B, Color.BLACK, .45f) : new Color(0xFCFCFE));
            g.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));
            g.setColor(new Color(0, 0, 0, 70));
            g.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));
            g.dispose();
            super.paintComponent(g0);
            Graphics2D h = (Graphics2D) g0.create();
            h.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            h.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (getText().isEmpty() && !isFocusOwner()) {
                h.setColor(Theme.isDark() ? new Color(255, 255, 255, 140) : new Color(0x7A7F8E));
                h.setFont(getFont());
                FontMetrics fm = h.getFontMetrics();
                h.drawString("Search programs", 12, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
            h.setColor(Theme.isDark() ? Color.WHITE : Theme.TASK_A);
            h.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = getWidth() - 22, cy = getHeight() / 2 - 2;
            h.draw(new Ellipse2D.Float(cx - 5, cy - 5, 10, 10));
            h.draw(new Line2D.Float(cx + 3, cy + 3, cx + 8, cy + 8));
            h.dispose();
        }
    }

    private final class ModeToggle extends JComponent {
        private boolean hover;

        ModeToggle() {
            setPreferredSize(new Dimension(34, 36));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Switch mode");
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                @Override public void mouseClicked(MouseEvent e) { if (SwingUtilities.isLeftMouseButton(e)) toggleMode(); }
            });
        }

        @Override public String getToolTipText() {
            return Theme.isDark() ? "Switch to light mode" : "Switch to dark mode";
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            if (hover) {
                g.setColor(new Color(255, 255, 255, 50));
                g.fill(new Ellipse2D.Float(cx - 15, cy - 15, 30, 30));
            }
            g.setColor(Color.WHITE);
            if (Theme.isDark()) {
                Area moon = new Area(new Ellipse2D.Float(cx - 7, cy - 7, 14, 14));
                moon.subtract(new Area(new Ellipse2D.Float(cx - 2, cy - 10, 14, 14)));
                g.fill(moon);
            } else {
                g.fill(new Ellipse2D.Float(cx - 4.5f, cy - 4.5f, 9, 9));
                g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int k = 0; k < 8; k++) {
                    double a = k * Math.PI / 4;
                    g.draw(new Line2D.Double(cx + Math.cos(a) * 7, cy + Math.sin(a) * 7, cx + Math.cos(a) * 9.5, cy + Math.sin(a) * 9.5));
                }
            }
            g.dispose();
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

    // ----------------------------------------------------------- personalize
    private static final class Chips<T> extends JPanel {
        private final List<T> items = new ArrayList<>();
        private final List<String> names = new ArrayList<>();
        private final Consumer<T> onPick;
        private T selected;

        @SafeVarargs
        Chips(T selected, Function<T, String> label, Consumer<T> onPick, T... options) {
            super(new FlowLayout(FlowLayout.LEFT, 5, 2));
            setOpaque(false);
            this.selected = selected;
            this.onPick = onPick;
            for (T o : options) {
                items.add(o);
                names.add(label.apply(o));
                add(new Chip(o, label.apply(o)));
            }
        }

        void set(T t) { selected = t; repaint(); }

        private final class Chip extends JComponent {
            private final T value;
            private final String text;
            private boolean hover;

            Chip(T value, String text) {
                this.value = value;
                this.text = text;
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setFont(Theme.BOLD);
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                    @Override public void mouseClicked(MouseEvent e) {
                        if (!SwingUtilities.isLeftMouseButton(e)) return;
                        selected = value;
                        Chips.this.repaint();
                        onPick.accept(value);
                    }
                });
            }

            @Override public Dimension getPreferredSize() {
                return new Dimension(getFontMetrics(getFont()).stringWidth(text) + 24, 26);
            }

            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                boolean on = value.equals(selected);
                Color fill = on ? Theme.PRIMARY : hover ? Theme.mix(Theme.PRIMARY_L, Theme.PRIMARY, .16f) : Theme.PRIMARY_L;
                g.setColor(fill);
                g.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                g.setFont(getFont());
                g.setColor(on ? Color.WHITE : Theme.PRIMARY_D);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g.dispose();
            }
        }
    }

    private final class SchemeCard extends JComponent {
        private final Theme.Scheme scheme;
        private boolean hover;

        SchemeCard(Theme.Scheme scheme) {
            this.scheme = scheme;
            setPreferredSize(new Dimension(150, 84));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(scheme.name);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                @Override public void mouseClicked(MouseEvent e) { if (SwingUtilities.isLeftMouseButton(e)) setScheme(scheme); }
            });
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Color[] c = Theme.swatch(scheme, Theme.mode());
            int w = getWidth(), h = getHeight();
            boolean on = scheme == Theme.scheme();
            RoundRectangle2D.Float shape = new RoundRectangle2D.Float(1, 1, w - 3, h - 3, 14, 14);
            g.setColor(Theme.CARD);
            g.fill(shape);
            Shape oldClip = g.getClip();
            g.clip(new RoundRectangle2D.Float(5, 5, w - 11, h - 30, 9, 9));
            g.setPaint(new GradientPaint(5, 5, c[3], w - 6, h - 25, c[4]));
            g.fillRect(5, 5, w - 10, h - 28);
            g.setColor(c[1]);
            g.fill(new RoundRectangle2D.Float(w * 0.30f, 14, w * 0.52f, h - 50, 6, 6));
            g.setColor(c[6]);
            g.fill(new RoundRectangle2D.Float(w * 0.30f, 14, w * 0.52f, 9, 6, 6));
            g.setColor(c[2]);
            g.fill(new RoundRectangle2D.Float(w * 0.36f, h - 46, w * 0.22f, 8, 5, 5));
            g.setColor(c[5]);
            g.fillRect((int) (w * 0.36f), 30, (int) (w * 0.34f), 2);
            g.fillRect((int) (w * 0.36f), 36, (int) (w * 0.24f), 2);
            for (int k = 0; k < 3; k++) {
                g.setColor(k == 0 ? c[2] : new Color(255, 255, 255, 190));
                g.fill(new RoundRectangle2D.Float(11, 11 + k * 14, 10, 10, 4, 4));
            }
            g.setClip(oldClip);
            g.setFont(Theme.BOLD);
            g.setColor(Theme.TEXT);
            g.drawString(scheme.name, 10, h - 9);
            g.setColor(c[2]);
            g.fill(new Ellipse2D.Float(w - 20, h - 20, 10, 10));
            g.setStroke(new BasicStroke(on ? 2.4f : 1f));
            g.setColor(on ? Theme.PRIMARY : hover ? Theme.PRIMARY_D : Theme.BORDER);
            g.draw(shape);
            g.dispose();
        }
    }

    private final class IconPreview extends JComponent {
        @Override public Dimension getPreferredSize() { return new Dimension(380, IconStyle.size + 22); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, Theme.DESK_A, getWidth(), getHeight(), Theme.DESK_B));
            g.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));
            int sz = IconStyle.size, gap = sz + 18, x = 14;
            for (int i = 0; i < APPS.size() && x + sz < getWidth() - 6; i++, x += gap)
                paintGlyph(g, APPS.get(i), x, 11, sz);
            g.dispose();
        }
    }

    private final class PersonalizePanel extends JPanel {
        private Chips<Theme.Mode> modeChips;
        private Chips<Integer> sizeChips;
        private Chips<IconShape> shapeChips;
        private Chips<Boolean> fillChips, labelChips, arrangeChips;
        private Chips<IconColors> colorChips;
        private final IconPreview preview = new IconPreview();
        private final List<SchemeCard> cards = new ArrayList<>();

        PersonalizePanel() {
            setLayout(new BorderLayout());
            JPanel body = new JPanel();
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.setBorder(new EmptyBorder(8, 10, 10, 10));
            body.add(appearanceCard());
            body.add(Box.createVerticalStrut(8));
            body.add(iconsCard());
            body.add(Box.createVerticalStrut(8));
            body.add(wallpaperCard());
            JScrollPane sp = new JScrollPane(body);
            sp.setBorder(null);
            sp.getVerticalScrollBar().setUnitIncrement(16);
            add(sp, BorderLayout.CENTER);
            personalizeSync = this::sync;
        }

        private void sync() {
            modeChips.set(Theme.mode());
            sizeChips.set(IconStyle.size);
            shapeChips.set(IconStyle.shape);
            fillChips.set(IconStyle.gradient);
            labelChips.set(IconStyle.labels);
            arrangeChips.set(autoArrange);
            colorChips.set(IconStyle.colors);
            for (SchemeCard c : cards) c.repaint();
            preview.revalidate();
            preview.repaint();
        }

        private JPanel section(String title) {
            JPanel p = new JPanel(new GridBagLayout());
            p.setOpaque(false);
            p.setBorder(Common.titled(title));
            p.setAlignmentX(Component.LEFT_ALIGNMENT);
            return p;
        }

        private void addRow(JPanel p, int row, String label, JComponent c) {
            GridBagConstraints gc = new GridBagConstraints();
            gc.gridy = row; gc.insets = new Insets(4, 6, 4, 6); gc.anchor = GridBagConstraints.WEST;
            gc.gridx = 0; gc.weightx = 0; gc.fill = GridBagConstraints.NONE;
            JLabel l = Common.label(label);
            gc.anchor = GridBagConstraints.NORTHWEST;
            gc.insets = new Insets(8, 6, 4, 10);
            p.add(l, gc);
            gc.gridx = 1; gc.weightx = 1; gc.fill = GridBagConstraints.HORIZONTAL;
            gc.insets = new Insets(4, 0, 4, 6);
            p.add(c, gc);
        }

        private JPanel appearanceCard() {
            JPanel p = section("Appearance");
            modeChips = new Chips<>(Theme.mode(), m -> m == Theme.Mode.DARK ? "Dark" : "Light",
                    m -> setMode(m), Theme.Mode.DARK, Theme.Mode.LIGHT);
            addRow(p, 0, "Mode", modeChips);
            JPanel grid = new JPanel(new GridLayout(0, 3, 8, 8));
            grid.setOpaque(false);
            for (Theme.Scheme sc : Theme.SCHEMES) {
                SchemeCard c = new SchemeCard(sc);
                cards.add(c);
                grid.add(c);
            }
            addRow(p, 1, "Theme", grid);
            return p;
        }

        private JPanel iconsCard() {
            JPanel p = section("Icons");
            int r = 0;
            sizeChips = new Chips<>(IconStyle.size,
                    v -> v == IconStyle.SMALL ? "Small" : v == IconStyle.MEDIUM ? "Medium" : "Large",
                    v -> { IconStyle.size = v; iconStyleChanged(); }, IconStyle.SMALL, IconStyle.MEDIUM, IconStyle.LARGE);
            addRow(p, r++, "Size", sizeChips);
            shapeChips = new Chips<>(IconStyle.shape, v -> v.label,
                    v -> { IconStyle.shape = v; iconStyleChanged(); }, IconShape.values());
            addRow(p, r++, "Shape", shapeChips);
            fillChips = new Chips<>(IconStyle.gradient, v -> v ? "Gradient" : "Flat",
                    v -> { IconStyle.gradient = v; iconStyleChanged(); }, true, false);
            addRow(p, r++, "Fill", fillChips);
            colorChips = new Chips<>(IconStyle.colors, v -> v.label,
                    v -> { IconStyle.colors = v; iconStyleChanged(); }, IconColors.values());
            addRow(p, r++, "Colors", colorChips);
            labelChips = new Chips<>(IconStyle.labels, v -> v ? "Show names" : "Hide names",
                    v -> { IconStyle.labels = v; iconStyleChanged(); }, true, false);
            addRow(p, r++, "Names", labelChips);
            addRow(p, r++, "Preview", preview);

            JComboBox<String> which = Common.combo(titles());
            JPanel pick = Common.row();
            pick.add(which);
            pick.add(Common.softButton("Pick color...", () -> {
                String t = (String) which.getSelectedItem();
                App a = appByTitle(t);
                if (a == null) return;
                Color c = JColorChooser.showDialog(this, "Icon color - " + t, colorOf(a));
                if (c != null) { IconStyle.custom.put(t, c.getRGB() & 0xFFFFFF); iconStyleChanged(); }
            }));
            pick.add(Common.softButton("Reset all colors", () -> { IconStyle.custom.clear(); iconStyleChanged(); }));
            addRow(p, r++, "Single icon", pick);

            arrangeChips = new Chips<>(autoArrange, v -> v ? "Automatic" : "Free (drag anywhere)",
                    v -> setAutoArrange(v), true, false);
            addRow(p, r++, "Arrange", arrangeChips);
            JPanel sort = Common.row();
            sort.add(Common.softButton("Name A-Z", () -> sortByName(true)));
            sort.add(Common.softButton("Name Z-A", () -> sortByName(false)));
            sort.add(Common.softButton("Color", VirtualOS.this::sortByColor));
            sort.add(Common.softButton("Default order", VirtualOS.this::sortDefault));
            addRow(p, r++, "Sort by", sort);
            return p;
        }

        private JPanel wallpaperCard() {
            JPanel p = section("Wallpaper");
            JPanel row = Common.row();
            row.add(Common.softButton("Choose image...", VirtualOS.this::chooseWallpaper));
            row.add(Common.softButton("Use theme background", VirtualOS.this::resetWallpaper));
            addRow(p, 0, "Background", row);
            return p;
        }
    }

    private static String[] titles() {
        String[] t = new String[APPS.size()];
        for (int i = 0; i < t.length; i++) t[i] = APPS.get(i).title;
        return t;
    }

    private static App appByTitle(String t) {
        for (App a : APPS) if (a.title.equals(t)) return a;
        return null;
    }

    // --------------------------------------------------------------- battery
    private void startBatteryPoller() {
        Thread t = new Thread(() -> {
            while (true) {
                final int p = readBattery();
                SwingUtilities.invokeLater(() -> battery.set(p));
                if (p < 0) return;
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