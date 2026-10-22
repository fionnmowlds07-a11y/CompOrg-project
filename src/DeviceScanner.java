import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the lists of devices from the starter classes. Call each scan once and
 * keep the list; the rest of the program works from that list.
 */
public class DeviceScanner
{
    /** Read every PCI device once and look up its names. */
    public static List<PciDevice> scanPci()
    {
        idLookup ids;
	
        try {
            ids = idLookup.pci();
        } catch (IOException e) {
            System.err.println("warning: " + e.getMessage() + "; showing raw IDs instead of names");
            ids = null;
        }

        List<PciDevice> list = new ArrayList<>();

        for (int i = 0; i < pciInfo.getCount(); i++) {
            int vendor = pciInfo.getVendorId(i);
            int device = pciInfo.getDeviceId(i);
            int cls = pciInfo.getClassCode(i);

            String vendorName = null, deviceName = null, className = null;
            if (ids != null) {
                vendorName = ids.vendorName(vendor);
                deviceName = ids.deviceName(vendor, device);
                className = ids.className(cls);
            }


            vendorName = orRaw(vendorName, String.format("[vendor %04x]", vendor));
            deviceName = orRaw(deviceName, String.format("[device %04x]", device));
            className = orRaw(className, String.format("[class %06x]", cls));

            list.add(new PciDevice(pciInfo.getAddress(i),
				    pciInfo.getBus(i),         // Bus number
				    pciInfo.getDevice(i),      // Slot number
                                    pciInfo.getFunction(i),    // Function number
				    vendor,
				    device,
				    cls,
                                    vendorName,         // Looks up Vendor ID e.g. Intel Corp
				    deviceName, // Looks up device ID e.g. Wifi
                                    className));           // Looks up class ID e.g. Network controller
        }
        return list;
    }

    private static String orRaw(String name, String fallback){
	if (name != null){
		return name; // Name exists in pci.ids so return it
	 } else {
		return fallback; // Name does not exist in pci.ids, return the raw hex
	 }
    }

}

