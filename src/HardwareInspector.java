/**
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
        PciAnalysis.printPciSection(DeviceScanner.scanPci());
    }
}
