import java.util.List;

/**
 * Build:  javac *.java
 * Run:    java HardwareInspector        (run it from the folder that contains the ids folder)
 *         java HardwareInspector --html (also saves the results as report.html)
 */
public class HardwareInspector
{
    public static void main(String[] args) throws Exception
    {
        System.out.println("CPU model:  " + cpuInfo.getModel());
        System.out.println("CPU cores:  " + cpuInfo.getCount());

        //CPU usage: for loop for per core, and overall average.
        double[] usages = cpuInfo.getUsages();
        double totalUsage = 0;
        for (int i = 0; i < usages.length; i++) {
            double usage = usages[i];
            System.out.println("CPU " + i + " Usage: " + usage + "%");
            totalUsage = totalUsage + usage;
        }
        double avgCpuUsage = Math.round((totalUsage / usages.length) * 100.0) / 100.0;
        System.out.println("Average CPU Usage: " + avgCpuUsage + "%");

        // Print cache information
        System.out.println();
        System.out.println("==================================================================");
        System.out.println("Cache Information\n");
        cacheInfo.printGroupedCacheInfo();
        System.out.println("==================================================================");
        
        // Stride Report and Pointer Chase
        System.out.println("");
        MemoryHierarchyExplorer.printStrideReport();
        System.out.println("");
        MemoryHierarchyExplorer.printCacheSizeReport();

        System.out.println();
        List<PciDevice> pci = DeviceScanner.scanPci();
        List<UsbDevice> usb = DeviceScanner.scanUsb();
        PciAnalysis.printPciSection(pci);   // Fionn: PCI, sorted two ways + summary
        System.out.println();
        UsbReports.printUsbSection(usb);     // Joseph: USB, root hubs labelled
        System.out.println();
        MemoryReport.report(); // Joseph: Memory Report

        // Extension: with --html, also save everything above as a web page
        if (args.length > 0 && args[0].equals("--html")) {
            System.out.println();
            HtmlExport.write("report.html", usages, pci, usb);
        }
    }
}
