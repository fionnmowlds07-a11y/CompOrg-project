# hardware-analysis

Hardware Analysis Java program built for CS4421. It reads the CPU, caches, memory, PCI and USB details of the machine it runs on and prints a report.

## Requirements

- Linux (the program reads from `/proc` and `/sys`)
- Java (JDK) 17 or newer

## Build

From the project root (the folder that contains `src` and `ids`):

```bash
javac -d out src/*.java
```

## Run

Always run from the project root, because the program needs the `ids` folder to look up device names.

| What | Command |
|------|---------|
| Full report | `java -cp out HardwareInspector` |
| Full report + HTML page | `java -cp out HardwareInspector --html` |
| Interactive menu | `java -cp out HardwareMenu` |

### 1. HardwareInspector

Prints everything in one go: CPU model, cores and per-core usage, grouped cache info, the stride and cache-size memory tests, PCI devices, USB devices and a memory summary.

![HardwareInspector output](screenshots%20for%20ReadMe/java%20-cp%20out%20HardwareInspector.png)

### 2. HardwareInspector --html

Same report as above, and also saves it as `report.html` in the project root. Open it in any browser.

![HTML report](screenshots%20for%20ReadMe/Report%20html%20file.png)

### 3. HardwareMenu

Lets you pick one section at a time. Type a number and press Enter; type `E` to return to the menu.

```
1: CPU information
2: Memory information
3: PCI information
4: USB information
9: Quit
```

![HardwareMenu](screenshots%20for%20ReadMe/Harware%20inspector%20menu.png)

## AI declaration

We used AI tools to help write the CSS styling in the HTML report (`src/HtmlExport.java`) and to learn how to structure and format HTML. All other code was written by the team.
