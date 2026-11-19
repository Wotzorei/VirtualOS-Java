package minios;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

/** Headless checks for every theme x mode: readable text, visible accents, no two surfaces sharing a colour.
 *  Run:  java -cp out minios.TestTheme */
public class TestTheme {
    static int fails;

    static void check(boolean ok, String what) {
        if (!ok) { fails++; System.out.println("FAIL " + what); }
    }

    public static void main(String[] args) {
        for (Theme.Scheme s : Theme.SCHEMES) {
            for (Theme.Mode m : Theme.Mode.values()) {
                Theme.change(s, m);
                String id = s.name + " / " + m;
                for (Color bg : new Color[]{Theme.INSET, Theme.BG, Theme.CARD, Theme.FIELD, Theme.STRIPE}) {
                    check(Theme.contrast(Theme.TEXT, bg) >= 7.0, id + ": TEXT readable on surfaces");
                    check(Theme.contrast(Theme.MUTED, bg) >= 4.5, id + ": MUTED readable on surfaces");
                }
                check(Theme.contrast(Color.WHITE, Theme.PRIMARY) >= 3.0, id + ": white text on PRIMARY buttons");
                check(Theme.contrast(Theme.PRIMARY_D, Theme.PRIMARY_L) >= 4.5, id + ": soft-button text");
                check(Theme.contrast(Color.WHITE, Theme.TASK_A) >= 3.0, id + ": white text on taskbar");
                Color[] surfaces = {Theme.INSET, Theme.BG, Theme.CARD, Theme.FIELD, Theme.STRIPE,
                        Theme.BORDER, Theme.FRAME, Theme.GRID, Theme.PRIMARY_L, Theme.THUMB, Theme.THUMB_H};
                Set<Integer> seen = new HashSet<>();
                for (Color c : surfaces) check(seen.add(c.getRGB()), id + ": surface colours are distinct (live re-colouring relies on it)");
            }
        }
        Theme.change(Theme.SCHEMES[0], Theme.Mode.DARK);
        System.out.println(fails == 0 ? "Theme checks: all OK (" + Theme.SCHEMES.length * 2 + " palettes)" : fails + " FAILURES");
        System.exit(fails == 0 ? 0 : 1);
    }
}
