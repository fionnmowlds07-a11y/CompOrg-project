import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Extension: saves everything the program finds as one web page (report.html).
 *
 * How it works:
 *   - every section of the page has its own method that returns a String of HTML
 *   - write() glues all the Strings together and saves them to a file
 *   - open the file in any web browser to see it
 */
public class HtmlExport
{
    // The look of the page (colours, boxes, tables). AI was used for this styling.
    private static final String STYLE = """
        :root { --bg:#f4f6fb; --card:#ffffff; --text:#1d2433; --muted:#6b7385; --line:#e3e7ef;
                --accent:#3b6cf6; --ok:#2fa36b; --warn:#e0a020; --high:#d9534f; }
        @media (prefers-color-scheme: dark) {
          :root { --bg:#12151c; --card:#1b1f2a; --text:#e6e9f0; --muted:#9aa3b5; --line:#2b3140; }
        }
        * { box-sizing: border-box; }
        body { margin:0; background:var(--bg); color:var(--text);
               font-family: system-ui, -apple-system, "Segoe UI", sans-serif; line-height:1.5; }
        main { max-width:1000px; margin:0 auto; padding:32px 16px; }
        h1 { margin:0; font-size:28px; }
        h2 { margin:0 0 16px; font-size:18px; }
        h3 { margin:16px 0 8px; font-size:15px; }
        .sub { color:var(--muted); margin:4px 0 16px; }
        nav { display:flex; flex-wrap:wrap; gap:8px; margin-bottom:24px; }
        nav a { color:var(--accent); text-decoration:none; border:1px solid var(--line);
                background:var(--card); border-radius:20px; padding:4px 12px; font-size:13px; }
        .card { background:var(--card); border:1px solid var(--line); border-radius:12px;
                padding:20px; margin-bottom:20px; overflow-x:auto; }
        .stats { display:grid; grid-template-columns:repeat(auto-fit, minmax(160px, 1fr)); gap:12px; margin-bottom:16px; }
        .stat { border:1px solid var(--line); border-radius:10px; padding:12px; }
        .stat b { display:block; font-size:22px; }
        .stat span { color:var(--muted); font-size:13px; }
        .bar { background:var(--line); border-radius:6px; height:10px; overflow:hidden; min-width:80px; }
        .bar div { height:100%; border-radius:6px; }
        table { width:100%; border-collapse:collapse; font-size:14px; }
        th { text-align:left; color:var(--muted); font-weight:600; border-bottom:2px solid var(--line); padding:8px; }
        td { border-bottom:1px solid var(--line); padding:8px; vertical-align:middle; }
        .mono { font-family: ui-monospace, Consolas, monospace; }
        .tag { background:var(--accent); color:white; border-radius:20px; padding:2px 8px; font-size:12px; }
        .note { color:var(--muted); font-size:13px; margin:12px 0 0; }
        """;

    /** Build the whole page and save it as fileName. */
    public static void write(String fileName, double[] usages, List<PciDevice> pci, List<UsbDevice> usb)
    {
        String html = "";

        // Top of the page
        html = html + "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n";
        html = html + "<meta charset=\"utf-8\">\n";
        html = html + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n";
        html = html + "<title>Hardware Report</title>\n";
        html = html + "<style>" + STYLE + "</style>\n";
        html = html + "</head>\n<body>\n<main>\n";

        html = html + "<h1>Hardware Report</h1>\n";
        html = html + "<p class=\"sub\">Generated " + java.time.LocalDateTime.now().withNano(0) + "</p>\n";

        // Links that jump to each section
        html = html + "<nav>";
        html = html + "<a href=\"#cpu\">Processor</a>";
        html = html + "<a href=\"#memory\">Memory</a>";
        html = html + "<a href=\"#caches\">Caches</a>";
        html = html + "<a href=\"#pci\">PCI</a>";
        html = html + "<a href=\"#usb\">USB</a>";
        html = html + "</nav>\n";

        // Each section, one after another
        html = html + cpuSection(usages);
        html = html + memorySection();
        html = html + cacheSection();
        html = html + pciSection(pci);
        html = html + usbSection(usb);

        // Bottom of the page
        html = html + "</main>\n</body>\n</html>\n";

        try {
            Files.writeString(Path.of(fileName), html);
            System.out.println("HTML report saved to " + fileName);
        } catch (IOException e) {
            System.err.println("warning: could not write " + fileName + ": " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // PROCESSOR: model, number of cores, average load, a bar per core
    // ---------------------------------------------------------------
    private static String cpuSection(double[] usages)
    {
        // Add up every core's load to get the average
        double total = 0;
        for (int i = 0; i < usages.length; i++) {
            total = total + usages[i];
        }
        double average = 0;
        if (usages.length > 0) {
            average = total / usages.length;
        }

        String html = "<section class=\"card\" id=\"cpu\">\n<h2>Processor</h2>\n";
        html = html + "<div class=\"stats\">\n";
        html = html + stat(escape(cpuInfo.getModel()), "Model");
        html = html + stat("" + cpuInfo.getCount(), "Cores");
        html = html + stat(String.format("%.1f%%", average), "Average load");
        html = html + "</div>\n";

        html = html + "<table>\n<tr><th>Core</th><th>Load</th><th style=\"width:60%\"></th></tr>\n";
        for (int i = 0; i < usages.length; i++) {
            html = html + "<tr>";
            html = html + "<td>CPU " + i + "</td>";
            html = html + "<td>" + String.format("%.1f%%", usages[i]) + "</td>";
            html = html + "<td>" + loadBar(usages[i]) + "</td>";
            html = html + "</tr>\n";
        }
        html = html + "</table>\n</section>\n";
        return html;
    }

    // ---------------------------------------------------------------
    // MEMORY: total, used, available, swap, and a bar of how full it is
    // ---------------------------------------------------------------
    private static String memorySection()
    {
        long total = memInfo.getTotal();          // all in KiB
        long used = memInfo.getUsed();
        long available = memInfo.getAvailable();

        String html = "<section class=\"card\" id=\"memory\">\n<h2>Memory</h2>\n";

        // -1 means /proc/meminfo could not be read
        if (total <= 0 || available < 0) {
            html = html + "<p>Memory information not available (/proc/meminfo could not be read).</p>\n</section>\n";
            return html;
        }
        double percent = (double) used / total * 100;

        html = html + "<div class=\"stats\">\n";
        html = html + stat(MemoryReport.formatBytes(total), "Total");
        html = html + stat(MemoryReport.formatBytes(used), "Used");
        html = html + stat(MemoryReport.formatBytes(available), "Available");
        html = html + stat(String.format("%.1f%%", percent), "In use");

        // Swap is only shown if the machine has some
        long swapTotal = memInfo.field("SwapTotal");
        long swapFree = memInfo.field("SwapFree");
        if (swapTotal > 0) {
            html = html + stat(MemoryReport.formatBytes(swapTotal), "Swap total");
            html = html + stat(MemoryReport.formatBytes(swapTotal - swapFree), "Swap used");
        }
        html = html + "</div>\n";

        html = html + loadBar(percent);
        html = html + "<p class=\"note\">This machine has " + MemoryReport.formatBytes(total) + " of RAM, of which "
                    + MemoryReport.formatBytes(used) + " (" + Math.round(percent) + "%) is in use.</p>\n";
        html = html + "</section>\n";
        return html;
    }

    // ---------------------------------------------------------------
    // CACHES: cores with the same caches are grouped together,
    // the same way cacheInfo.printGroupedCacheInfo() does it
    // ---------------------------------------------------------------
    private static String cacheSection()
    {
        String html = "<section class=\"card\" id=\"caches\">\n<h2>Caches</h2>\n";

        int cores = cpuInfo.getCount();
        if (cores == 0) {
            html = html + "<p>Cache information is not available.</p>\n</section>\n";
            return html;
        }

        // Two cores with the same signature have the same caches.
        // signatures holds each different signature once, groups holds the cores that have it.
        ArrayList<String> signatures = new ArrayList<>();
        ArrayList<ArrayList<Integer>> groups = new ArrayList<>();

        for (int cpu = 0; cpu < cores; cpu++) {
            String sig = cacheInfo.signature(cpu);
            int place = signatures.indexOf(sig);   // -1 means we have not seen it yet
            if (place == -1) {
                signatures.add(sig);
                ArrayList<Integer> newGroup = new ArrayList<>();
                newGroup.add(cpu);
                groups.add(newGroup);
            } else {
                groups.get(place).add(cpu);
            }
        }

        // One small table per group. The first core of the group is used for the numbers.
        for (int g = 0; g < groups.size(); g++) {
            ArrayList<Integer> group = groups.get(g);
            int cpu = group.get(0);

            html = html + "<h3>Cores " + group + "</h3>\n";

            if (cacheInfo.getCount(cpu) == 0) {
                html = html + "<p>No cache information for these cores (common on virtual machines).</p>\n";
                continue;
            }

            html = html + "<table>\n<tr><th>Level</th><th>Type</th><th>Size</th><th>Line size</th></tr>\n";
            for (int i = 0; i < cacheInfo.getCount(cpu); i++) {
                html = html + "<tr>";
                html = html + "<td>L" + cacheInfo.getLevel(cpu, i) + "</td>";
                html = html + "<td>" + escape(cacheInfo.getType(cpu, i)) + "</td>";
                html = html + "<td>" + bytesText(cacheInfo.getSize(cpu, i)) + "</td>";
                html = html + "<td>" + cacheInfo.getLineSize(cpu, i) + " bytes</td>";
                html = html + "</tr>\n";
            }
            html = html + "</table>\n";
        }
        html = html + "</section>\n";
        return html;
    }

    // ---------------------------------------------------------------
    // PCI: every device sorted by address, then counts by vendor and class
    // ---------------------------------------------------------------
    private static String pciSection(List<PciDevice> devices)
    {
        List<PciDevice> sorted = PciAnalysis.sortByAddress(devices);

        String html = "<section class=\"card\" id=\"pci\">\n<h2>PCI devices (" + devices.size() + ")</h2>\n";
        html = html + "<table>\n<tr><th>Address</th><th>ID</th><th>Class</th><th>Vendor</th><th>Device</th></tr>\n";
        for (int i = 0; i < sorted.size(); i++) {
            PciDevice d = sorted.get(i);
            html = html + "<tr>";
            html = html + "<td class=\"mono\">" + String.format("%02x:%02x.%x", d.getBus(), d.getDevice(), d.getFunction()) + "</td>";
            html = html + "<td class=\"mono\">" + String.format("%04x:%04x", d.getVendorId(), d.getDeviceId()) + "</td>";
            html = html + "<td>" + escape(d.getClassName()) + "</td>";
            html = html + "<td>" + escape(d.getVendorName()) + "</td>";
            html = html + "<td>" + escape(d.getDeviceName()) + "</td>";
            html = html + "</tr>\n";
        }
        html = html + "</table>\n</section>\n";

        html = html + countSection("PCI devices by vendor", PciAnalysis.countByVendor(devices), devices.size());
        html = html + countSection("PCI devices by class", PciAnalysis.countByClass(devices), devices.size());
        return html;
    }

    // A table of name / count, with a bar showing each count's share of the total
    private static String countSection(String title, Map<String, Integer> counts, int total)
    {
        List<String> names = new ArrayList<>(counts.keySet());

        String html = "<section class=\"card\">\n<h2>" + title + "</h2>\n";
        html = html + "<table>\n<tr><th>Name</th><th>Count</th><th style=\"width:40%\"></th></tr>\n";
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            int count = counts.get(name);
            html = html + "<tr>";
            html = html + "<td>" + escape(name) + "</td>";
            html = html + "<td>" + count + "</td>";
            html = html + "<td>" + plainBar(100.0 * count / total) + "</td>";
            html = html + "</tr>\n";
        }
        html = html + "</table>\n</section>\n";
        return html;
    }

    // ---------------------------------------------------------------
    // USB: every device, with root hubs labelled
    // ---------------------------------------------------------------
    private static String usbSection(List<UsbDevice> devices)
    {
        int hubs = 0;
        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i).isRootHub()) {
                hubs++;
            }
        }
        int attached = devices.size() - hubs;

        String html = "<section class=\"card\" id=\"usb\">\n<h2>USB devices (" + devices.size() + ": "
                      + hubs + " root hubs, " + attached + " attached)</h2>\n";
        if (devices.isEmpty()) {
            html = html + "<p>No USB devices found.</p>\n</section>\n";
            return html;
        }
        html = html + "<table>\n<tr><th>Bus</th><th>Device</th><th>ID</th><th>Vendor</th><th>Product</th><th></th></tr>\n";
        for (int i = 0; i < devices.size(); i++) {
            UsbDevice d = devices.get(i);
            String tag = "";
            if (d.isRootHub()) {
                tag = "<span class=\"tag\">root hub</span>";
            }
            html = html + "<tr>";
            html = html + "<td>" + String.format("%03d", d.getBusNumber()) + "</td>";
            html = html + "<td>" + String.format("%03d", d.getDeviceNumber()) + "</td>";
            html = html + "<td class=\"mono\">" + String.format("%04x:%04x", d.getVendorId(), d.getProductId()) + "</td>";
            html = html + "<td>" + escape(d.getVendorName()) + "</td>";
            html = html + "<td>" + escape(d.getProductName()) + "</td>";
            html = html + "<td>" + tag + "</td>";
            html = html + "</tr>\n";
        }
        html = html + "</table>\n</section>\n";
        return html;
    }

    // ---------------------------------------------------------------
    // Small helpers used by the sections above
    // ---------------------------------------------------------------

    // A box with a big value and a label under it, e.g. "8" / "Cores"
    private static String stat(String value, String label)
    {
        return "<div class=\"stat\"><b>" + value + "</b><span>" + label + "</span></div>\n";
    }

    // A load bar: green under 50%, amber under 80%, red above that
    private static String loadBar(double percent)
    {
        String colour = "var(--ok)";
        if (percent >= 80) {
            colour = "var(--high)";
        } else if (percent >= 50) {
            colour = "var(--warn)";
        }
        return bar(percent, colour);
    }

    // A blue bar, for numbers where high is not "bad"
    private static String plainBar(double percent)
    {
        return bar(percent, "var(--accent)");
    }

    // Draws the bar itself. Kept between 0 and 100 so it never spills out of its box.
    private static String bar(double percent, String colour)
    {
        if (percent > 100) {
            percent = 100;
        }
        if (percent < 0) {
            percent = 0;
        }
        return "<div class=\"bar\"><div style=\"width:" + String.format("%.1f", percent) + "%; background:" + colour + "\"></div></div>";
    }

    // Cache sizes come in bytes, but formatBytes wants KiB. -1 means it could not be read.
    private static String bytesText(long bytes)
    {
        if (bytes < 0) {
            return "unknown";
        }
        return MemoryReport.formatBytes(bytes / 1024);
    }

    // Names can contain & < > which mean something in HTML, so swap them for safe codes
    private static String escape(String text)
    {
        text = text.replace("&", "&amp;");
        text = text.replace("<", "&lt;");
        text = text.replace(">", "&gt;");
        return text;
    }
}
