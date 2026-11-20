package minios;

import javax.accessibility.Accessible;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * One place for the look of the whole app: colour themes, light / dark mode, fonts and Swing defaults.
 *
 * Every colour is a plain static field that is re-computed when the theme or mode changes
 * ({@link #change}); custom-painted components simply read the fields while painting, and
 * components that stored a colour at construction time are re-coloured by {@link #recolor}.
 */
public final class Theme {
    private Theme() {}

    public enum Mode { DARK, LIGHT }

    /** A colour theme: one accent plus the desktop / taskbar colours. Surfaces are derived from the hue. */
    public static final class Scheme {
        public final String name;
        final float hue, sat;
        final int accent, deskA, deskB, taskA, taskB, startA, startB;
        Scheme(String name, float hueDeg, float sat, int accent, int deskA, int deskB,
               int taskA, int taskB, int startA, int startB) {
            this.name = name; this.hue = hueDeg / 360f; this.sat = sat; this.accent = accent;
            this.deskA = deskA; this.deskB = deskB; this.taskA = taskA; this.taskB = taskB;
            this.startA = startA; this.startB = startB;
        }
        @Override public String toString() { return name; }
    }

    public static final Scheme[] SCHEMES = {
        new Scheme("Midnight Aurora", 231, 1.00f, 0x7C6CFF, 0x0A2A6B, 0x1EA1C9, 0x2F6FE0, 0x0D3FA6, 0x3FAE49, 0x278A33),
        new Scheme("Ocean",           205, 1.00f, 0x1E8FEA, 0x053A63, 0x19B3D6, 0x1F8AE0, 0x0B5A9E, 0x2FB6A8, 0x1B8C80),
        new Scheme("Forest",          150, 0.75f, 0x1FA866, 0x0B3D2E, 0x3AA56B, 0x2E9A63, 0x14633C, 0x8BC34A, 0x5A9A1F),
        new Scheme("Sunset",           20, 0.60f, 0xE8582A, 0x5A1E3F, 0xF08A3C, 0xE0662F, 0xA63A14, 0xF2B134, 0xD98B0A),
        new Scheme("Rose",            335, 0.70f, 0xE8458A, 0x4A1442, 0xE0588F, 0xD63F86, 0x8F1F55, 0xFF7AA8, 0xE0457D),
        new Scheme("Graphite",        220, 0.12f, 0x6B7FA8, 0x20242C, 0x5B6577, 0x4A5262, 0x272C36, 0x6B7FA8, 0x4B5D84),
        new Scheme("Amethyst",        272, 0.75f, 0x9B5DE5, 0x240B4C, 0x8E44AD, 0x7B3FE4, 0x4A1D96, 0x0284C7, 0x0369A1),
        new Scheme("Teal Lagoon",     176, 0.80f, 0x0D9488, 0x042F2E, 0x2DD4BF, 0x0F766E, 0x115E59, 0xEA580C, 0x9A3412),
        new Scheme("Crimson",         352, 0.70f, 0xDC2626, 0x450A15, 0xC83246, 0xB91C1C, 0x7F1D1D, 0xD97706, 0x92400E),
        new Scheme("Amber Gold",       38, 0.65f, 0xD97706, 0x3E1F0D, 0xD9822B, 0xC26A12, 0x7C3F08, 0x2B9348, 0x1B6B32),
        new Scheme("Cyberpunk",       290, 0.85f, 0xD946EF, 0x12082E, 0xB5179E, 0x3A0CA3, 0x1F065C, 0x0284C7, 0x035388),
        new Scheme("Nordic Frost",    212, 0.42f, 0x4F86C6, 0x1B2838, 0x5E81AC, 0x3B5978, 0x24384D, 0x2A8C89, 0x1D6664),
        new Scheme("Dark Blue",       222, 0.85f, 0x2563EB, 0x050D24, 0x102A63, 0x173B8A, 0x0A1D4A, 0x0284C7, 0x0369A1),
        new Scheme("Blood Red",       358, 0.80f, 0xB91C1C, 0x1C0306, 0x6E0B14, 0x7F0D18, 0x3B050A, 0x991B1B, 0x5C0A0A),
        new Scheme("Forest Green",    142, 0.80f, 0x15803D, 0x062414, 0x14532D, 0x166534, 0x0B3B1E, 0x2E7D32, 0x1B5E20),
        new Scheme("BlackHole",       230, 0.05f, 0x3B404E, 0x030305, 0x12141C, 0x181A22, 0x08090D, 0xB45309, 0x78350F),
        new Scheme("Divine White",     45, 0.14f, 0xB8860B, 0xFFFFFF, 0xDCE7F7, 0x64748B, 0x475569, 0xC59318, 0x9A6E08),
        new Scheme("Sakura Pink",     340, 0.55f, 0xDB4C77, 0xFAD2E1, 0xE889AB, 0xC24670, 0x8F294D, 0xE05A88, 0xB83764),
        new Scheme("Halloween Orange", 26, 0.78f, 0xEA580C, 0x1E0D2B, 0xD9531E, 0xD9531E, 0x8C2D08, 0x6D28D9, 0x4C1D95),
    };

    // ---- surfaces (three depths)
    public static Color INSET, BG, CARD, FIELD, STRIPE, BORDER, FRAME, GRID;
    // ---- text
    public static Color TEXT, MUTED;
    // ---- accents
    public static Color PRIMARY, PRIMARY_H, PRIMARY_D, PRIMARY_L, TEAL, CORAL, AMBER;
    // ---- scroll bar thumb
    public static Color THUMB, THUMB_H;
    // ---- desktop shell
    public static Color DESK_A, DESK_B, TASK_A, TASK_B, START_A, START_B;
    public static Color WIDGET_BG, WIDGET_TEXT, WIDGET_MUTED, WIDGET_TRACK;

    private static Scheme scheme = SCHEMES[0];
    private static Mode mode = Mode.DARK;

    public static final Font BASE  = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    public static final Font BOLD  = BASE.deriveFont(Font.BOLD);
    public static final Font SMALL = BASE.deriveFont(11f);
    public static final Font MONO  = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    private static final Preferences PREFS = Preferences.userNodeForPackage(Theme.class);
    private static final List<Runnable> LISTENERS = new ArrayList<>();
    private static Map<Integer, Color> lastChange = new HashMap<>();
    private static boolean installed;

    static { compute(scheme, mode); }

    public static Scheme scheme() { return scheme; }
    public static Mode mode() { return mode; }
    public static boolean isDark() { return mode == Mode.DARK; }

    // ================================================================ colour maths
    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
    private static Color hsb(float h, float s, float b) { return new Color(Color.HSBtoRGB(h, clamp(s), clamp(b))); }
    static Color rgb(int v) { return new Color(v); }

    public static Color mix(Color a, Color b, float t) {
        return new Color(Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    private static double chan(int v) {
        double c = v / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
    static double luminance(Color c) { return 0.2126 * chan(c.getRed()) + 0.7152 * chan(c.getGreen()) + 0.0722 * chan(c.getBlue()); }

    /** WCAG contrast ratio, 1 (none) .. 21 (black on white). */
    public static double contrast(Color a, Color b) {
        double x = luminance(a), y = luminance(b);
        return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05);
    }

    private static double worst(Color c, Color... against) {
        double m = 99;
        for (Color a : against) m = Math.min(m, contrast(c, a));
        return m;
    }

    /** Nudges c towards white (or black) until it has at least `min` contrast against every colour in `against`. */
    private static Color fit(Color c, double min, boolean lighten, Color... against) {
        Color t = lighten ? new Color(0xFAFAFA) : new Color(0x050505);
        for (int i = 0; i < 40 && worst(c, against) < min; i++) c = mix(c, t, 0.06f);
        return c;
    }

    // ================================================================ palettes
    private static void compute(Scheme s, Mode m) {
        scheme = s; mode = m;
        float h = s.hue, k = s.sat;
        Color accent = rgb(s.accent);
        if (m == Mode.DARK) {
            INSET  = hsb(h, .52f * k, .125f);
            BG     = hsb(h, .50f * k, .160f);
            CARD   = hsb(h, .44f * k, .210f);
            FIELD  = hsb(h, .45f * k, .280f);
            STRIPE = hsb(h, .46f * k, .260f);
            GRID   = hsb(h, .42f * k, .300f);
            FRAME  = hsb(h, .45f * k, .330f);
            PRIMARY_L = hsb(h, .47f * k, .340f);
            BORDER = hsb(h, .42f * k, .355f);
            THUMB  = hsb(h, .42f * k, .470f);
            THUMB_H = hsb(h, .37f * k, .660f);
            TEXT   = hsb(h, .06f * k, .980f);
            MUTED  = fit(hsb(h, .24f * k, .740f), 4.6, true, INSET, BG, CARD, FIELD, STRIPE);
            PRIMARY = fit(accent, 3.2, false, Color.WHITE);
            PRIMARY_H = mix(PRIMARY, Color.WHITE, .20f);
            PRIMARY_D = fit(mix(accent, Color.WHITE, .55f), 4.6, true, PRIMARY_L, CARD, BG, INSET);
            TEAL = rgb(0x2DE2C5); CORAL = rgb(0xFF5C9A); AMBER = rgb(0xFFC857);
            DESK_A = rgb(s.deskA); DESK_B = rgb(s.deskB);
            TASK_A = rgb(s.taskA); TASK_B = rgb(s.taskB);
            WIDGET_BG = new Color(16, 26, 54, 185);
            WIDGET_TEXT = rgb(0xFEFEFE);
            WIDGET_MUTED = new Color(255, 255, 255, 170);
            WIDGET_TRACK = new Color(255, 255, 255, 70);
        } else {
            INSET  = hsb(h, .07f * k, .940f);
            STRIPE = hsb(h, .05f * k, .955f);
            BG     = hsb(h, .05f * k, .965f);
            FIELD  = hsb(h, .01f * k, .990f);
            CARD   = hsb(h, .02f * k, .995f);
            GRID   = hsb(h, .10f * k, .900f);
            BORDER = hsb(h, .14f * k, .850f);
            FRAME  = hsb(h, .16f * k, .800f);
            PRIMARY_L = hsb(h, .16f * k, .930f);
            THUMB  = hsb(h, .15f * k, .740f);
            THUMB_H = hsb(h, .22f * k, .600f);
            TEXT   = hsb(h, .45f * k, .170f);
            MUTED  = fit(hsb(h, .25f * k, .450f), 4.6, false, INSET, BG, CARD, FIELD, STRIPE, GRID);
            PRIMARY = fit(accent, 3.6, false, Color.WHITE);
            PRIMARY_H = mix(PRIMARY, Color.WHITE, .18f);
            PRIMARY_D = fit(mix(accent, Color.BLACK, .45f), 5.5, false, PRIMARY_L, CARD, BG, INSET);
            TEAL = rgb(0x0E9F8A); CORAL = rgb(0xE0316F); AMBER = rgb(0xC98A00);
            DESK_A = mix(rgb(s.deskA), Color.WHITE, .22f); DESK_B = mix(rgb(s.deskB), Color.WHITE, .32f);
            TASK_A = fit(mix(rgb(s.taskA), Color.WHITE, .16f), 3.2, false, Color.WHITE);
            TASK_B = fit(mix(rgb(s.taskB), Color.WHITE, .12f), 3.6, false, Color.WHITE);
            WIDGET_BG = new Color(255, 255, 255, 205);
            WIDGET_TEXT = rgb(0x1B2342);
            WIDGET_MUTED = new Color(27, 35, 66, 170);
            WIDGET_TRACK = new Color(27, 35, 66, 50);
        }
        START_A = rgb(s.startA); START_B = rgb(s.startB);
    }

    /** Every colour the re-colouring walker knows about, in a fixed order. */
    private static Color[] snapshot() {
        return new Color[]{INSET, BG, CARD, FIELD, STRIPE, BORDER, FRAME, GRID, TEXT, MUTED,
                PRIMARY, PRIMARY_H, PRIMARY_D, PRIMARY_L, TEAL, CORAL, AMBER, THUMB, THUMB_H};
    }

    // ================================================================ public API
    /** Loads the saved theme (if any) and installs the Swing look. Call once at start-up. */
    public static void install() {
        if (!installed) {
            try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) { }
            // every font the L&F defines -> our compact sans font (keeping bold/italic)
            for (Object key : Collections.list(UIManager.getDefaults().keys())) {
                if (UIManager.get(key) instanceof FontUIResource) {
                    Font f = (Font) UIManager.get(key);
                    UIManager.put(key, new FontUIResource(BASE.deriveFont(f.getStyle(), 12f)));
                }
            }
            installed = true;
        }
        try {
            Scheme s = byName(PREFS.get("theme.scheme", SCHEMES[0].name));
            Mode m = "LIGHT".equals(PREFS.get("theme.mode", "DARK")) ? Mode.LIGHT : Mode.DARK;
            compute(s, m);
        } catch (Exception ignored) { }
        applyDefaults();
    }

    public static Scheme byName(String n) {
        for (Scheme s : SCHEMES) if (s.name.equals(n)) return s;
        return SCHEMES[0];
    }

    /** Switches theme and/or mode, remembers the choice and tells every listener to refresh. */
    public static void change(Scheme s, Mode m) {
        Color[] before = snapshot();
        compute(s, m);
        Color[] after = snapshot();
        Map<Integer, Color> map = new HashMap<>();
        for (int i = 0; i < before.length; i++) {
            int key = before[i].getRGB() & 0xFFFFFF;
            if (key == (after[i].getRGB() & 0xFFFFFF) || key == 0xFFFFFF || key == 0) continue;
            map.putIfAbsent(key, after[i]);
        }
        lastChange = map;
        try { PREFS.put("theme.scheme", s.name); PREFS.put("theme.mode", m.name()); } catch (Exception ignored) { }
        applyDefaults();
        for (Runnable r : new ArrayList<>(LISTENERS)) r.run();
    }

    public static void addListener(Runnable r) { LISTENERS.add(r); }

    /**
     * Colours of another theme / mode without switching to it (for preview cards):
     * {BG, CARD, PRIMARY, DESK_A, DESK_B, TEXT, TASK_A, FIELD}.
     */
    public static Color[] swatch(Scheme s, Mode m) {
        Scheme os = scheme;
        Mode om = mode;
        compute(s, m);
        Color[] out = {BG, CARD, PRIMARY, DESK_A, DESK_B, TEXT, TASK_A, FIELD};
        compute(os, om);          // compute() is deterministic, so this restores the current palette exactly
        return out;
    }

    /** Re-colours a component tree that was built under the previous theme (see {@link #change}). */
    public static void recolor(Component root) { recolor(root, lastChange); }

    // ================================================================ borders
    /** Rounded 1px outline with a title in the accent tint (replaces the default etched border). */
    public static Border card(String title) {
        Border line = new LineBorder(BORDER, 1, true);
        TitledBorder t = BorderFactory.createTitledBorder(line, " " + title + " ",
                TitledBorder.LEFT, TitledBorder.TOP, BOLD, PRIMARY_D);
        return BorderFactory.createCompoundBorder(t, new EmptyBorder(0, 4, 2, 4));
    }

    /** Padding + thin outline for text fields and combo boxes. */
    public static Border input() {
        return BorderFactory.createCompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(2, 6, 2, 6));
    }

    /** Slim flat scroll bar: no arrow buttons, rounded thumb. Colours are read at paint time. */
    public static class DarkScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        public static ComponentUI createUI(JComponent c) { return new DarkScrollBarUI(); }

        private static JButton none() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            return b;
        }
        @Override protected JButton createDecreaseButton(int o) { return none(); }
        @Override protected JButton createIncreaseButton(int o) { return none(); }

        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(INSET);
            g.fillRect(r.x, r.y, r.width, r.height);
        }

        @Override protected void paintThumb(Graphics g0, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(isThumbRollover() || isDragging ? THUMB_H : THUMB);
            g.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8);
            g.dispose();
        }
    }

    // ================================================================ Swing defaults
    private static ColorUIResource r(Color c) { return new ColorUIResource(c); }

    private static void applyDefaults() {
        for (String k : new String[]{"Panel.background", "Viewport.background", "ScrollPane.background",
                "OptionPane.background", "TabbedPane.background", "TabbedPane.tabAreaBackground", "control",
                "CheckBox.background", "RadioButton.background", "Label.background", "SplitPane.background",
                "TabbedPane.contentAreaColor", "TabbedPane.selected", "TabbedPane.light", "TabbedPane.highlight",
                "TabbedPane.shadow", "TabbedPane.darkShadow", "TabbedPane.focus", "Separator.background",
                "ToolTip.background"})
            UIManager.put(k, r(BG));
        for (String k : new String[]{"PopupMenu.background", "MenuItem.background", "Menu.background",
                "CheckBoxMenuItem.background", "RadioButtonMenuItem.background"})
            UIManager.put(k, r(CARD));
        for (String k : new String[]{"Label.foreground", "TextField.foreground", "TextArea.foreground",
                "Table.foreground", "ComboBox.foreground", "TitledBorder.titleColor", "OptionPane.messageForeground",
                "Button.foreground", "MenuItem.foreground", "Menu.foreground", "TabbedPane.foreground",
                "List.foreground", "CheckBox.foreground", "ToolTip.foreground", "OptionPane.foreground", "Panel.foreground",
                "CheckBoxMenuItem.foreground", "RadioButtonMenuItem.foreground", "ToggleButton.foreground",
                "RadioButton.foreground", "TableHeader.foreground"})
            UIManager.put(k, r(TEXT));
        UIManager.put("MenuItem.disabledForeground", r(MUTED));
        UIManager.put("CheckBoxMenuItem.disabledForeground", r(MUTED));
        UIManager.put("Label.disabledForeground", r(MUTED));
        UIManager.put("Separator.foreground", r(BORDER));
        for (String k : new String[]{"TextField.background", "ComboBox.background", "ComboBox.listBackground"})
            UIManager.put(k, r(FIELD));
        UIManager.put("TextArea.background", r(INSET));
        for (String k : new String[]{"Table.background", "List.background", "TableHeader.background"})
            UIManager.put(k, r(CARD));

        UIManager.put("TextField.border", input());
        UIManager.put("ComboBox.border", input());
        UIManager.put("ScrollPane.border", new LineBorder(BORDER, 1, true));
        for (String k : new String[]{"TextField.selectionBackground", "TextArea.selectionBackground", "Table.selectionBackground"})
            UIManager.put(k, r(PRIMARY_L));
        for (String k : new String[]{"TextField.selectionForeground", "TextArea.selectionForeground", "Table.selectionForeground"})
            UIManager.put(k, r(TEXT));
        UIManager.put("Table.gridColor", r(GRID));
        UIManager.put("ComboBox.selectionBackground", r(PRIMARY));
        UIManager.put("ComboBox.selectionForeground", r(Color.WHITE));
        UIManager.put("List.selectionBackground", r(PRIMARY));
        UIManager.put("List.selectionForeground", r(Color.WHITE));
        UIManager.put("TextField.caretForeground", r(TEAL));
        UIManager.put("TextArea.caretForeground", r(TEAL));

        // scroll bars: slim and flat
        UIManager.put("ScrollBarUI", DarkScrollBarUI.class.getName());
        UIManager.put("ScrollBar.width", 11);
        UIManager.put("ScrollBar.gradient", null);
        UIManager.put("ScrollBar.track", r(INSET));
        UIManager.put("ScrollBar.background", r(INSET));
        UIManager.put("ScrollBar.thumb", r(THUMB));
        UIManager.put("ScrollBar.thumbShadow", r(THUMB));
        UIManager.put("ScrollBar.thumbHighlight", r(THUMB_H));
        UIManager.put("ScrollBar.thumbDarkShadow", r(THUMB));
        UIManager.put("ScrollBar.trackHighlight", r(INSET));

        // dialog / plain buttons (JOptionPane, taskbar window buttons)
        for (String k : new String[]{"Button.background", "ToggleButton.background"}) UIManager.put(k, r(PRIMARY_L));
        UIManager.put("Button.gradient", null);
        UIManager.put("ToggleButton.gradient", null);
        for (String k : new String[]{"Button.select", "ToggleButton.select"}) UIManager.put(k, r(PRIMARY));
        for (String k : new String[]{"Button.focus", "ToggleButton.focus"}) UIManager.put(k, r(PRIMARY));
        UIManager.put("OptionPane.buttonFont", new FontUIResource(BOLD));

        // popup menus (start menu, search suggestions, right-click)
        UIManager.put("MenuItem.selectionBackground", r(PRIMARY));
        UIManager.put("MenuItem.selectionForeground", r(Color.WHITE));
        UIManager.put("CheckBoxMenuItem.selectionBackground", r(PRIMARY));
        UIManager.put("CheckBoxMenuItem.selectionForeground", r(Color.WHITE));
        UIManager.put("Menu.selectionBackground", r(PRIMARY));
        UIManager.put("Menu.selectionForeground", r(Color.WHITE));
        UIManager.put("PopupMenu.border", new LineBorder(BORDER, 1));
        UIManager.put("ToolTip.border", new LineBorder(BORDER, 1));

        // window frames (internal frames on the desktop): our own title bar + border, painted from the live theme
        UIManager.put("InternalFrameUI", FrameUI.class.getName());
        UIManager.put("InternalFrame.border", new BorderUIResource(new FrameBorder()));
        UIManager.put("InternalFrame.closeIcon", new IconUIResource(new TitleIcon(0)));
        UIManager.put("InternalFrame.maximizeIcon", new IconUIResource(new TitleIcon(1)));
        UIManager.put("InternalFrame.minimizeIcon", new IconUIResource(new TitleIcon(2)));
        UIManager.put("InternalFrame.iconifyIcon", new IconUIResource(new TitleIcon(3)));
        UIManager.put("InternalFrame.titleFont", new FontUIResource(BOLD));
    }

    // ================================================================ window frame
    /** Window frame border: accent colour while the window is active, quiet grey otherwise. */
    static final class FrameBorder extends javax.swing.border.AbstractBorder implements UIResource {
        @Override public Insets getBorderInsets(Component c) { return new Insets(4, 4, 4, 4); }
        @Override public Insets getBorderInsets(Component c, Insets i) { i.set(4, 4, 4, 4); return i; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            boolean sel = c instanceof JInternalFrame && ((JInternalFrame) c).isSelected();
            g.setColor(sel ? TASK_B : FRAME);
            g.fillRect(x, y + 2, 4, h - 2);
            g.fillRect(x + w - 4, y + 2, 4, h - 2);
            g.fillRect(x, y + h - 4, w, 4);
            g.setColor(sel ? mix(TASK_B, Color.BLACK, .35f) : BORDER);
            g.drawRect(x, y, w - 1, h - 1);
        }
    }

    /** White title-bar glyphs: 0 close, 1 maximise, 2 restore, 3 minimise. */
    static final class TitleIcon implements Icon {
        private final int kind;
        TitleIcon(int kind) { this.kind = kind; }
        @Override public int getIconWidth() { return 18; }
        @Override public int getIconHeight() { return 18; }
        @Override public void paintIcon(Component c, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.translate(x, y);
            switch (kind) {
                case 0: g.drawLine(5, 5, 13, 13); g.drawLine(13, 5, 5, 13); break;
                case 1: g.drawRect(5, 5, 8, 8); break;
                case 2: g.drawRect(4, 7, 7, 7); g.drawLine(7, 5, 14, 5); g.drawLine(14, 5, 14, 11); break;
                default: g.drawLine(5, 13, 13, 13);
            }
            g.dispose();
        }
    }

    public static class FrameUI extends javax.swing.plaf.basic.BasicInternalFrameUI {
        private java.beans.PropertyChangeListener selListener;

        public static ComponentUI createUI(JComponent c) { return new FrameUI((JInternalFrame) c); }
        public FrameUI(JInternalFrame f) { super(f); }

        @Override protected JComponent createNorthPane(JInternalFrame w) { return new TitleBar(w); }

        @Override protected void installListeners() {
            super.installListeners();
            selListener = e -> { if ("selected".equals(e.getPropertyName())) frame.repaint(); };
            frame.addPropertyChangeListener(selListener);
        }

        @Override protected void uninstallListeners() {
            frame.removePropertyChangeListener(selListener);
            super.uninstallListeners();
        }
    }

    /** Title bar: accent gradient when active, grey when not; buttons are plain white glyphs. */
    static final class TitleBar extends javax.swing.plaf.basic.BasicInternalFrameTitlePane {
        TitleBar(JInternalFrame f) { super(f); }

        private static void plain(AbstractButton b) {
            if (b == null) return;
            b.setOpaque(false);
            b.setContentAreaFilled(false);
            b.setBorderPainted(false);
            b.setBorder(new EmptyBorder(0, 0, 0, 0));
            b.setFocusable(false);
        }

        @Override protected void createButtons() {
            super.createButtons();
            plain(iconButton); plain(maxButton); plain(closeButton);
        }

        @Override protected void assembleSystemMenu() {
            super.assembleSystemMenu();
            if (menuBar != null) {
                menuBar.setOpaque(false);
                menuBar.setBorder(new EmptyBorder(0, 2, 0, 0));
                for (Component c : menuBar.getComponents()) if (c instanceof JComponent) {
                    ((JComponent) c).setOpaque(false);
                    ((JComponent) c).setBorder(new EmptyBorder(0, 0, 0, 0));
                }
            }
        }

        @Override public void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            boolean sel = frame.isSelected();
            int w = getWidth(), h = getHeight();
            g.setPaint(sel ? new GradientPaint(0, 0, TASK_A, 0, h, TASK_B)
                           : new GradientPaint(0, 0, FRAME, 0, h, mix(FRAME, BG, .35f)));
            g.fillRect(0, 0, w, h);
            String title = frame.getTitle();
            if (title != null) {
                g.setFont(getFont());
                FontMetrics fm = g.getFontMetrics();
                int x = (menuBar != null ? menuBar.getX() + menuBar.getWidth() : 0) + 6, right = w - 4;
                for (AbstractButton b : new AbstractButton[]{iconButton, maxButton, closeButton})
                    if (b != null && b.isVisible() && b.getParent() == this) right = Math.min(right, b.getX() - 4);
                String t = title;
                if (fm.stringWidth(t) > right - x) {
                    while (t.length() > 1 && fm.stringWidth(t + "...") > right - x) t = t.substring(0, t.length() - 1);
                    t += "...";
                }
                g.setColor(sel ? Color.WHITE : TEXT);
                g.drawString(t, x, (h + fm.getAscent() - fm.getDescent()) / 2);
            }
            g.dispose();
        }
    }

    // ================================================================ live re-colouring
    private static Color map(Color c, Map<Integer, Color> m) {
        if (c == null) return null;
        return c.getAlpha() != 255 ? null : m.get(c.getRGB() & 0xFFFFFF);
    }

    private static void recolor(Component c, Map<Integer, Color> m) {
        if (m.isEmpty() || c == null) return;
        recolorOne(c, m);
        if (c instanceof JComboBox) {
            try {
                Accessible popup = ((JComboBox<?>) c).getAccessibleContext().getAccessibleChild(0);
                if (popup instanceof Component) recolor((Component) popup, m);
            } catch (Exception ignored) { }
        }
        if (c instanceof Container)
            for (Component k : ((Container) c).getComponents()) recolor(k, m);
    }

    private static void recolorOne(Component c, Map<Integer, Color> m) {
        Color n;
        if (c.isBackgroundSet() && (n = map(c.getBackground(), m)) != null) c.setBackground(n);
        if (c.isForegroundSet() && (n = map(c.getForeground(), m)) != null) c.setForeground(n);
        if (c instanceof JTable) {
            JTable t = (JTable) c;
            if ((n = map(t.getGridColor(), m)) != null) t.setGridColor(n);
            if ((n = map(t.getSelectionBackground(), m)) != null) t.setSelectionBackground(n);
            if ((n = map(t.getSelectionForeground(), m)) != null) t.setSelectionForeground(n);
        }
        if (c instanceof JList) {
            JList<?> l = (JList<?>) c;
            if ((n = map(l.getSelectionBackground(), m)) != null) l.setSelectionBackground(n);
            if ((n = map(l.getSelectionForeground(), m)) != null) l.setSelectionForeground(n);
        }
        if (c instanceof JTextComponent) {
            JTextComponent t = (JTextComponent) c;
            if ((n = map(t.getCaretColor(), m)) != null) t.setCaretColor(n);
            if ((n = map(t.getSelectionColor(), m)) != null) t.setSelectionColor(n);
            if ((n = map(t.getSelectedTextColor(), m)) != null) t.setSelectedTextColor(n);
            if ((n = map(t.getDisabledTextColor(), m)) != null) t.setDisabledTextColor(n);
        }
        if (c instanceof JComponent) {
            JComponent jc = (JComponent) c;
            Border b = jc.getBorder(), nb = recolorBorder(b, m);
            if (nb != b) jc.setBorder(nb);
        }
        if (c instanceof JMenuItem || c instanceof JPopupMenu) c.repaint();
    }

    private static Border recolorBorder(Border b, Map<Integer, Color> m) {
        if (b == null) return null;
        if (b.getClass() == LineBorder.class) {
            LineBorder lb = (LineBorder) b;
            Color n = map(lb.getLineColor(), m);
            return n == null ? b : new LineBorder(n, lb.getThickness(), lb.getRoundedCorners());
        }
        if (b instanceof TitledBorder) {
            TitledBorder tb = (TitledBorder) b;
            Color n = map(tb.getTitleColor(), m);
            if (n != null) tb.setTitleColor(n);
            Border inner = tb.getBorder(), ni = recolorBorder(inner, m);
            if (ni != inner) tb.setBorder(ni);
            return b;
        }
        if (b.getClass() == CompoundBorder.class) {
            CompoundBorder cb = (CompoundBorder) b;
            Border o = recolorBorder(cb.getOutsideBorder(), m), i = recolorBorder(cb.getInsideBorder(), m);
            return o == cb.getOutsideBorder() && i == cb.getInsideBorder() ? b : new CompoundBorder(o, i);
        }
        return b;
    }
}
