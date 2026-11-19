# VirtualOS - Mini OS Simulator (Java)

A desktop-style OS simulator in Java (Swing, standard library only - nothing to install).
Double-click a desktop icon to open a module in its own window.

## Run
    mkdir out
    javac -d out minios/*.java
    java -cp out minios.Main          # VirtualOS desktop (needs Java 11+)
    java -cp out minios.ClassicMain   # original tabbed layout
    java -cp out minios.TestLogic     # headless tests: small + large datasets for every module
    java -cp out minios.TestTheme     # headless check of every theme/mode (contrast, distinct colours)

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

## Personalize (themes, light/dark, icons)
Open it from the Start menu, the desktop right-click menu, or an icon's right-click menu.
Every setting applies instantly (open windows included) and is remembered between runs.

1. **Light / dark mode** - the sun/moon button in the taskbar, Start > Switch mode, or Personalize > Mode.
2. **Arrange icons** - drag an icon to move it. *Automatic* (default): icons flow top-to-bottom and the dragged
   icon is inserted where you drop it. *Free*: icons snap to a grid cell anywhere on the desktop (never stacking).
   Sort by Name A-Z / Z-A / Color / Default order from the desktop right-click menu or Personalize.
3. **Icon appearance** - size (small/medium/large), shape (rounded/circle/square), gradient or flat, colours
   (original / theme accent / monochrome), show or hide names, and a custom colour for any single icon
   (right-click the icon > Change icon color...).
4. **Overall theme** - Midnight Aurora (default), Ocean, Forest, Sunset, Rose, Graphite; each has a dark and a light
   variant covering windows, tables, buttons, title bars, taskbar, widgets and the default wallpaper.

`ClassicMain` also starts in whichever theme/mode you last chose.

Notes: the taskbar now removes a window's button when the window is closed, and re-opening an already open
program just brings its window forward instead of building a second copy first.

## Files (package `minios`)
| File | Purpose |
|---|---|
| Main.java | Launches the VirtualOS desktop |
| Theme.java | Colour themes, light/dark palettes, window frame, live re-colouring |
| TestTheme.java | Headless theme/mode checks |
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

## Starting state / Clear
Every module opens completely empty (no sample values, no pre-filled tables, no matrices). Type your own data,
use the Generate / Random / Load-example buttons, or the sample buttons where a module has one.
The red **Clear all inputs** button at the top of each module empties all text boxes and tables, wipes the
results, Gantt chart and drawings, and (Banker's) removes the matrices. Drop-down choices are kept.

## Dynamic input / validation
Every table accepts any number of rows (add one by one, or "Generate random" for large tests);
the only limit is memory. Blank, non-numeric, negative, duplicate or inconsistent values
(e.g. Allocation > Max, frame mapped twice, process larger than RAM) show an error dialog
instead of crashing.
