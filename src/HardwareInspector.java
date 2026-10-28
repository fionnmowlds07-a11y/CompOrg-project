/**
 * A short example that shows how to call each provided class. It prints raw
 * numbers and a few names. It is NOT a solution to the assignment: you still
 * have to measure load, store the devices, sort them, analyse them and present
 * the results.
 *
 * Build:  javac *.java
 * Run:    java HardwareInspector        (run it from the folder that contains the ids folder)
 */
public class HardwareInspector
{
    public static void main(String[] args) throws Exception
    {
        System.out.println("cores:  " + cpuInfo.getCount());
        System.out.println("model:  " + cpuInfo.getModel());
        System.out.println("core 0: busy=" + cpuInfo.getBusy(0) + " idle=" + cpuInfo.getIdle(0));
        System.out.println("memory: total=" + memInfo.getTotal() + " KiB, used=" + memInfo.getUsed() + " KiB");

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

        // Cache information for CPU 0
        System.out.println("CPU 0 caches: " + cacheInfo.getCount());
        for (int i = 0; i < cacheInfo.getCount(); i++) {
            System.out.println(cacheInfo.getLevel(i) + " - " + cacheInfo.getType(i) + " cache, size: " + cacheInfo.getSize(i) + " bytes, line size: " + cacheInfo.getLineSize(i) + " bytes");
        }
        System.out.println();
        PciAnalysis.printPciSection(DeviceScanner.scanPci());   // Fionn: PCI, sorted two ways + summary
        System.out.println();
        UsbReports.printUsbSection(DeviceScanner.scanUsb());     // Joseph: USB, root hubs labelled
    }
}
