/**
 * One PCI device: its address, its ID numbers and the names looked up for them.
 *
 * A PciDevice only holds data. It never reads files; DeviceScanner reads sysfs
 * and pci.ids and passes the results in. All fields are final, so a device
 * cannot change once it has been made.
 */
public class PciDevice implements Comparable<PciDevice>
{
    // Address, from the folder name in /sys/bus/pci/devices
    private final String address;   // e.g. "0000:00:14.3"
    private final int bus;          // 0-255
    private final int device;       // 0-31
    private final int function;     // 0-7

    // Raw numbers reported by the hardware
    private final int vendorId;     // e.g. 0x8086 = Intel
    private final int deviceId;     // e.g. 0xa840
    private final int classCode;    // 24 bits, e.g. 0x028000

    // Human-readable names, looked up in pci.ids
    private final String vendorName;
    private final String deviceName;
    private final String className;

    public PciDevice(String address, int bus, int device, int function,
     int vendorId, int deviceId, int classCode,
     String vendorName, String deviceName, String className)
    {
        this.address = address;
        this.bus = bus;
        this.device = device;
        this.function = function;
        this.vendorId = vendorId;
        this.deviceId = deviceId;
        this.classCode = classCode;
        this.vendorName = vendorName;
        this.deviceName = deviceName;
        this.className = className;
    }

    @Override
    public int compareTo(PciDevice other){
    //Lower number goes infront of other compared device in the list
    //Lexicographic order: bus decides first; return early if they differ
    int c = Integer.compare(bus, other.bus);
        if (c != 0) return c;

    //If Bus is the same number, go to device
        c = Integer.compare(device, other.device);
        if (c != 0) return c;

    //If Device is the same, compare function
        return Integer.compare(function, other.function);
    }

    //Getter functions for other classes to read since the fields are private
    public String getAddress()    { return address; }
    public int getBus()           { return bus; }
    public int getDevice()        { return device; }
    public int getFunction()      { return function; }
    public int getVendorId()      { return vendorId; }
    public int getDeviceId()      { return deviceId; }
    public int getClassCode()     { return classCode; }
    public String getVendorName() { return vendorName; }
    public String getDeviceName() { return deviceName; }
    public String getClassName()  { return className; }

    // The class code is 24 bits: base class, sub class, programming interface.
    // Shift the byte we want to the bottom, then mask off everything above it.
    public int getBaseClass()  { return (classCode >> 16) & 0xFF; }
    public int getSubClass()   { return (classCode >> 8) & 0xFF; }
    public int getProgIf()     { return classCode & 0xFF; }

    /** In the style of lspci  */
    @Override
    public String toString()
    {
        return String.format("%02x:%02x.%x %04x:%04x %s: %s %s", bus, device, function, vendorId, deviceId, className, vendorName, deviceName);
    }



}
