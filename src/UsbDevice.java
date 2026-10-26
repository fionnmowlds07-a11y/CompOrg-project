/**
 * One USB device: where it sits on the bus, its ID numbers and the names looked up for them.
 *
 * A UsbDevice only holds data. It never reads files; DeviceScanner reads sysfs
 * and usb.ids and passes the results in. All fields are final, so a device
 * cannot change once it has been made.
 */
public class UsbDevice
{
    // Position, as lsusb shows it: "Bus 001 Device 003"
    private final String name;          // sysfs folder name, e.g. "1-2" or "usb1"
    private final int busNumber;        // e.g. 1
    private final int deviceNumber;     // e.g. 3

    // Raw numbers reported by the hardware
    private final int vendorId;         // e.g. 0x046d = Logitech
    private final int productId;        // e.g. 0xc52b

    // Human-readable names, looked up in usb.ids
    private final String vendorName;
    private final String productName;

    public UsbDevice(String name, int busNumber, int deviceNumber,
     int vendorId, int productId,
     String vendorName, String productName)
    {
        this.name = name;
        this.busNumber = busNumber;
        this.deviceNumber = deviceNumber;
        this.vendorId = vendorId;
        this.productId = productId;
        this.vendorName = vendorName;
        this.productName = productName;
    }

    //Getter functions for other classes to read since the fields are private
    public String getName()        { return name; }
    public int getBusNumber()      { return busNumber; }
    public int getDeviceNumber()   { return deviceNumber; }
    public int getVendorId()       { return vendorId; }
    public int getProductId()      { return productId; }
    public String getVendorName()  { return vendorName; }
    public String getProductName() { return productName; }

    /** In the style of lsusb */
    @Override
    public String toString()
    {
        return String.format("Bus %03d Device %03d: ID %04x:%04x %s %s",
                             busNumber, deviceNumber, vendorId, productId, vendorName, productName);
    }
}

