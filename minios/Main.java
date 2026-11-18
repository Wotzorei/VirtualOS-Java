package minios;

import javax.swing.*;

/** VirtualOS launcher (desktop style).  Run:  java -cp out minios.Main */
public class Main {
    public static void main(String[] args) {
        Theme.install();
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("VirtualOS");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1280, 800);
            frame.setContentPane(new VirtualOS());
            frame.setLocationRelativeTo(null);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setVisible(true);
        });
    }
}