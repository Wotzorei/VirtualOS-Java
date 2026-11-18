package minios;

import javax.swing.*;

/** The original tabbed launcher.  Run:  java -cp out minios.ClassicMain */
public class ClassicMain {
    public static void main(String[] args) {
        Theme.install();
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Mini OS Simulator");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1150, 860);
            JTabbedPane tabs = new JTabbedPane();
            tabs.setFont(Theme.BOLD);
            tabs.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4));
            tabs.addTab("1. CPU Scheduling", new CpuScheduling());
            tabs.addTab("2. Banker's Algorithm", new Bankers());
            tabs.addTab("3. Paging", new Paging());
            tabs.addTab("4. Page Replacement", new PageReplacement());
            tabs.addTab("5. Segmentation", new Segmentation());
            tabs.addTab("6. Fragmentation", new Fragmentation());
            tabs.addTab("7. Disk Scheduling", new DiskScheduling());
            tabs.addTab("8. Multiprogramming", new Multiprogramming());
            frame.add(tabs);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
