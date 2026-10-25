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
        double totalUsage = 0;
        for (int i = 0; i < cpuInfo.getCount(); i++) {
            System.out.println("CPU " + i + " Usage: " + cpuInfo.getUsage(i) + "%");
            totalUsage = totalUsage + cpuInfo.getUsage(i);
        }
        double avgCpuUsage = Math.round((totalUsage / cpuInfo.getCount()) * 100.0) / 100.0;
        System.out.println("Average CPU Usage: " + avgCpuUsage + "%");

        idLookup pci = idLookup.pci();
        System.out.println("PCI devices: " + pciInfo.getCount());
        for (int i = 0; i < pciInfo.getCount(); i++) {
            int vendor = pciInfo.getVendorId(i);
            int device = pciInfo.getDeviceId(i);
            System.out.printf("  %s  %04x:%04x  %s %s%n", pciInfo.getAddress(i), vendor, device,
                              pci.vendorName(vendor), pci.deviceName(vendor, device));
        }

        idLookup usb = idLookup.usb();
        System.out.println("USB devices: " + usbInfo.getCount());
        for (int i = 0; i < usbInfo.getCount(); i++) {
            int vendor = usbInfo.getVendorId(i);
            int product = usbInfo.getProductId(i);
            System.out.printf("  Bus %03d Device %03d: ID %04x:%04x  %s %s%n", usbInfo.getBusNumber(i),
                              usbInfo.getDeviceNumber(i), vendor, product,
                              usb.vendorName(vendor), usb.deviceName(vendor, product));
        }

        // Cache information for CPU 0
        System.out.println("CPU 0 caches: " + cacheInfo.getCount());
        for (int i = 0; i < cacheInfo.getCount(); i++) {
            System.out.println(cacheInfo.getLevel(i) + " - " + cacheInfo.getType(i) + " cache, size: " + cacheInfo.getSize(i) + " bytes, line size: " + cacheInfo.getLineSize(i) + " bytes");
        }
        System.out.println();
        PciAnalysis.printPciSection(DeviceScanner.scanPci());
    }
}
