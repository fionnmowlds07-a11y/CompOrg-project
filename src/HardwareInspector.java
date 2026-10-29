/**
 * Build:  javac *.java
 * Run:    java HardwareInspector        (run it from the folder that contains the ids folder)
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
        PciAnalysis.printPciSection(DeviceScanner.scanPci());   // Fionn: PCI, sorted two ways + summary
        System.out.println();
        UsbReports.printUsbSection(DeviceScanner.scanUsb());     // Joseph: USB, root hubs labelled
    }
}
