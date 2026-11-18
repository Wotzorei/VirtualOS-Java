# VirtualOS - Mini OS Simulator (Java)

A desktop-style OS simulator in Java (Swing, standard library only - nothing to install).
Double-click a desktop icon to open a module in its own window.

## Run
    mkdir out
    javac -d out minios/*.java
    java -cp out minios.Main          # VirtualOS desktop (needs Java 11+)
    java -cp out minios.ClassicMain   # original tabbed layout
    java -cp out minios.TestLogic     # headless tests: small + large datasets for every module

## Desktop features
- Desktop icons for all 8 modules (double-click to open, right-click > Open)
- Each module opens in its own resizable window; minimise / maximise / close work, and the taskbar
  shows one button per open window (click to minimise or restore)
- Start menu (all programs, change wallpaper, shut down) and a "Search programs" box
  (type a name, press Enter to open the first match)
- Calendar widget (current month, today highlighted) and System widget (live CPU / RAM)
- Taskbar clock and date; battery % when the computer reports one
- Wallpaper: right-click the desktop (or Start > Change wallpaper...) to pick your own image.
  The choice is remembered. A file named wallpaper.jpg / wallpaper.png in the folder you launch
  from is also picked up automatically.

## Files (package `minios`)
| File | Purpose |
|---|---|
| Main.java | Launches the VirtualOS desktop |
| ClassicMain.java | Original tabbed launcher |
| VirtualOS.java | Desktop shell: icons, windows, widgets, taskbar, Start menu, wallpaper |
| Common.java | Shared widgets (dynamic tables, Gantt chart, output box) + input validation |
| CpuScheduling.java | FCFS, SJF, SRTF, Priority (preemptive/non), Round Robin, compare-all |
| Bankers.java | Need matrix, safety check, safe sequence, resource-request test |
| Paging.java | Page table, logical -> physical translation, page-fault/range errors |
| PageReplacement.java | FIFO, LRU, Optimal: frame changes, hits, faults |
| Segmentation.java | Segment table, translation, limit/out-of-range errors, overlap warning |
| Fragmentation.java | First/Best/Worst/Next Fit, internal + external fragmentation, memory map |
| DiskScheduling.java | FCFS, SSTF, SCAN, C-SCAN, LOOK, C-LOOK, head-path plot |
| Multiprogramming.java | Programs sharing memory/CPU/I-O, status timeline, utilisation |
| TestLogic.java | Headless tests for all of the above |

## Dynamic input / validation
Every table accepts any number of rows (add one by one, or "Generate random" for large tests);
the only limit is memory. Blank, non-numeric, negative, duplicate or inconsistent values
(e.g. Allocation > Max, frame mapped twice, process larger than RAM) show an error dialog
instead of crashing.
