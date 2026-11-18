package minios;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.*;
import java.awt.*;
import java.util.Collections;

/** One place for the look of the whole app: "Midnight Aurora" dark palette, fonts and Swing defaults. */
public final class Theme {
    private Theme() {}

    // ---- surfaces (dark navy, three depths)
    public static final Color INSET     = new Color(0x0F1120);   // deepest: output boxes, canvases
    public static final Color BG        = new Color(0x161929);   // window background
    public static final Color CARD      = new Color(0x1E2236);   // tables, cards
    public static final Color FIELD     = new Color(0x272C47);   // text boxes, combo boxes
    public static final Color STRIPE    = new Color(0x242843);   // alternate table rows
    public static final Color BORDER    = new Color(0x333958);
    public static final Color FRAME     = new Color(0x2F3458);   // window frame

    // ---- text
    public static final Color TEXT      = new Color(0xE9ECFA);
    public static final Color MUTED     = new Color(0x8F98BD);

    // ---- accents
    public static final Color PRIMARY   = new Color(0x7C6CFF);   // violet
    public static final Color PRIMARY_H = new Color(0x9A8CFF);   // hover
    public static final Color PRIMARY_D = new Color(0xBDB4FF);   // light violet: titles / soft-button text
    public static final Color PRIMARY_L = new Color(0x2E3357);   // soft-button fill / selection
    public static final Color TEAL      = new Color(0x2DE2C5);
    public static final Color CORAL     = new Color(0xFF5C9A);   // pink
    public static final Color AMBER     = new Color(0xFFC857);

    public static final Font BASE  = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    public static final Font BOLD  = BASE.deriveFont(Font.BOLD);
    public static final Font SMALL = BASE.deriveFont(11f);
    public static final Font MONO  = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    /** Rounded 1px outline with a title in light violet (replaces the default etched border). */
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

    /** Slim flat scroll bar: no arrow buttons, rounded translucent thumb. */
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
            g.setColor(isThumbRollover() || isDragging ? new Color(0x6A72A8) : new Color(0x464D78));
            g.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8);
            g.dispose();
        }
    }

    private static ColorUIResource r(Color c) { return new ColorUIResource(c); }

    public static void install() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) { }

        // every font the L&F defines -> our compact sans font (keeping bold/italic)
        for (Object k : Collections.list(UIManager.getDefaults().keys())) {
            if (UIManager.get(k) instanceof FontUIResource) {
                Font f = (Font) UIManager.get(k);
                UIManager.put(k, new FontUIResource(BASE.deriveFont(f.getStyle(), 12f)));
            }
        }
        for (String k : new String[]{"Panel.background", "Viewport.background", "ScrollPane.background",
                "OptionPane.background", "TabbedPane.background", "TabbedPane.tabAreaBackground", "control",
                "CheckBox.background", "RadioButton.background", "Label.background", "SplitPane.background",
                "TabbedPane.contentAreaColor", "TabbedPane.selected", "TabbedPane.light", "TabbedPane.highlight",
                "TabbedPane.shadow", "TabbedPane.darkShadow", "TabbedPane.focus", "Separator.background",
                "ToolTip.background"})
            UIManager.put(k, r(BG));
        for (String k : new String[]{"PopupMenu.background", "MenuItem.background", "Menu.background"})
            UIManager.put(k, r(CARD));
        for (String k : new String[]{"Label.foreground", "TextField.foreground", "TextArea.foreground",
                "Table.foreground", "ComboBox.foreground", "TitledBorder.titleColor", "OptionPane.messageForeground",
                "Button.foreground", "MenuItem.foreground", "Menu.foreground", "TabbedPane.foreground",
                "List.foreground", "CheckBox.foreground", "ToolTip.foreground", "OptionPane.foreground", "Panel.foreground"})
            UIManager.put(k, r(TEXT));
        UIManager.put("MenuItem.disabledForeground", r(MUTED));
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
        UIManager.put("Table.gridColor", r(new Color(0x2A2F4C)));
        UIManager.put("ComboBox.selectionBackground", r(PRIMARY));
        UIManager.put("ComboBox.selectionForeground", r(Color.WHITE));
        UIManager.put("List.selectionBackground", r(PRIMARY));
        UIManager.put("List.selectionForeground", r(Color.WHITE));
        UIManager.put("TextField.caretForeground", r(TEAL));
        UIManager.put("TextArea.caretForeground", r(TEAL));

        // scroll bars: slim, flat, dark
        UIManager.put("ScrollBarUI", DarkScrollBarUI.class.getName());
        UIManager.put("ScrollBar.width", 11);
        UIManager.put("ScrollBar.gradient", null);
        UIManager.put("ScrollBar.track", r(INSET));
        UIManager.put("ScrollBar.background", r(INSET));
        UIManager.put("ScrollBar.thumb", r(new Color(0x464D78)));
        UIManager.put("ScrollBar.thumbShadow", r(new Color(0x464D78)));
        UIManager.put("ScrollBar.thumbHighlight", r(new Color(0x5A6290)));
        UIManager.put("ScrollBar.thumbDarkShadow", r(new Color(0x464D78)));
        UIManager.put("ScrollBar.trackHighlight", r(INSET));

        // dialog / plain buttons (JOptionPane)
        UIManager.put("Button.background", r(PRIMARY_L));
        UIManager.put("Button.gradient", null);
        UIManager.put("Button.select", r(PRIMARY));
        UIManager.put("Button.focus", r(PRIMARY));
        UIManager.put("OptionPane.buttonFont", new FontUIResource(BOLD));

        // popup menus (start menu, search suggestions, right-click)
        UIManager.put("MenuItem.selectionBackground", r(PRIMARY));
        UIManager.put("MenuItem.selectionForeground", r(Color.WHITE));
        UIManager.put("PopupMenu.border", new LineBorder(BORDER, 1));
        UIManager.put("ToolTip.border", new LineBorder(BORDER, 1));
    }
}