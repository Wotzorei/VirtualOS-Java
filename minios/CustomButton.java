package minios;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Custom-designed button replacing standard JButtons across VirtualOS and its modules.
 */
public class CustomButton extends JButton {

    public enum Variant {
        PRIMARY,
        SECONDARY,
        DANGER,
        CALENDAR,
        EDGE
    }

    private final Variant variant;
    private boolean hover;
    private boolean selectedDay;
    private boolean today;

    public CustomButton(String text) {
        this(text, null, Variant.SECONDARY);
    }

    public CustomButton(String text, Variant variant) {
        this(text, null, variant);
    }

    public CustomButton(String text, Icon icon, Variant variant) {
        super(text, icon);
        this.variant = variant;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setFocusable(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        switch (variant) {
            case CALENDAR:
                setFont(Theme.SMALL);
                setBorder(new EmptyBorder(2, 2, 2, 2));
                break;
            case EDGE:
                setFont(Theme.SMALL);
                setBorder(new EmptyBorder(0, 0, 0, 0));
                break;
            default:
                setFont(Theme.BOLD);
                setBorder(new EmptyBorder(4, 14, 4, 14));
                break;
        }

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
        });
    }

    public Variant getVariant() {
        return variant;
    }

    public void setSelectedDay(boolean selectedDay) {
        this.selectedDay = selectedDay;
        repaint();
    }

    public void setToday(boolean today) {
        this.today = today;
        repaint();
    }

    @Override
    public Color getForeground() {
        if (variant == null) return super.getForeground();
        if (!isEnabled()) return Theme.MUTED;
        switch (variant) {
            case PRIMARY:
            case DANGER:
            case EDGE:
                return Color.WHITE;
            case CALENDAR:
                return selectedDay ? Color.WHITE : (today ? Theme.PRIMARY_D : Theme.TEXT);
            default:
                return Theme.PRIMARY_D;
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        if (variant == Variant.EDGE || variant == Variant.CALENDAR) return d;
        return new Dimension(d.width, Math.max(28, d.height));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        boolean pressed = getModel().isPressed();

        if (variant == Variant.EDGE) {
            paintEdgeButton(g, w, h, pressed);
            g.dispose();
            return;
        }

        if (variant == Variant.CALENDAR) {
            paintCalendarButton(g, w, h, pressed);
            g.dispose();
            super.paintComponent(g0);
            return;
        }

        float arc = 12f;
        Color topColor;
        Color botColor;
        Color borderColor;

        if (!isEnabled()) {
            topColor = Theme.BORDER;
            botColor = Theme.BORDER;
            borderColor = Theme.BORDER;
        } else if (variant == Variant.PRIMARY) {
            Color base = pressed ? Theme.mix(Theme.PRIMARY, Color.BLACK, 0.18f)
                    : hover ? Theme.PRIMARY_H : Theme.PRIMARY;
            topColor = Theme.mix(base, Color.WHITE, pressed ? 0.04f : 0.14f);
            botColor = Theme.mix(base, Color.BLACK, 0.14f);
            borderColor = Theme.mix(Theme.PRIMARY, Color.BLACK, 0.28f);
        } else if (variant == Variant.DANGER) {
            Color base = pressed ? Theme.mix(Theme.CORAL, Color.BLACK, 0.18f)
                    : hover ? Theme.mix(Theme.CORAL, Color.WHITE, 0.16f) : Theme.CORAL;
            topColor = Theme.mix(base, Color.WHITE, pressed ? 0.04f : 0.14f);
            botColor = Theme.mix(base, Color.BLACK, 0.14f);
            borderColor = Theme.mix(Theme.CORAL, Color.BLACK, 0.28f);
        } else {
            Color base = pressed ? Theme.mix(Theme.PRIMARY_L, Theme.PRIMARY, 0.26f)
                    : hover ? Theme.mix(Theme.PRIMARY_L, Theme.PRIMARY, 0.14f) : Theme.PRIMARY_L;
            topColor = Theme.mix(base, Color.WHITE, Theme.isDark() ? 0.05f : 0.35f);
            botColor = base;
            borderColor = Theme.mix(Theme.PRIMARY, Theme.BORDER, 0.55f);
        }

        RoundRectangle2D.Float body = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, arc, arc);
        g.setPaint(new GradientPaint(0, 0, topColor, 0, h, botColor));
        g.fill(body);

        if (isEnabled() && !pressed) {
            g.setColor(new Color(255, 255, 255, variant == Variant.SECONDARY && !Theme.isDark() ? 140 : 48));
            g.draw(new RoundRectangle2D.Float(1.5f, 1.5f, w - 3f, h - 3f, arc - 2f, arc - 2f));
        }

        g.setColor(borderColor);
        g.draw(body);
        g.dispose();

        if (pressed) {
            Graphics2D shifted = (Graphics2D) g0.create();
            shifted.translate(0, 1);
            super.paintComponent(shifted);
            shifted.dispose();
        } else {
            super.paintComponent(g0);
        }
    }

    private void paintCalendarButton(Graphics2D g, int w, int h, boolean pressed) {
        RoundRectangle2D.Float tile = new RoundRectangle2D.Float(1f, 1f, w - 2f, h - 2f, 8f, 8f);
        if (selectedDay) {
            g.setColor(Theme.PRIMARY);
            g.fill(tile);
        } else if (pressed) {
            g.setColor(Theme.mix(Theme.PRIMARY_L, Theme.PRIMARY, 0.30f));
            g.fill(tile);
        } else if (hover) {
            g.setColor(Theme.PRIMARY_L);
            g.fill(tile);
        } else if (today) {
            g.setColor(Theme.mix(Theme.PRIMARY_L, Theme.CARD, 0.45f));
            g.fill(tile);
        }

        if (today && !selectedDay) {
            g.setColor(Theme.PRIMARY);
            g.draw(tile);
        }
    }

    private void paintEdgeButton(Graphics2D g, int w, int h, boolean pressed) {
        int alpha = pressed ? 85 : hover ? 55 : 22;
        g.setColor(new Color(255, 255, 255, alpha));
        g.fillRect(0, 0, w, h);

        g.setColor(new Color(255, 255, 255, hover ? 160 : 95));
        g.drawLine(0, 0, 0, h);

        int barH = 14;
        int bx = (w - 2) / 2;
        int by = (h - barH) / 2;
        g.setColor(new Color(255, 255, 255, hover ? 220 : 140));
        g.fillRoundRect(bx, by, 2, barH, 2, 2);
    }
}