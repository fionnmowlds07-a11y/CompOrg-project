import java.util.List;

/**
 * The USB section of the report. Prints every device in lsusb order (usbInfo
 * already sorts by bus, then device number), with the root hubs counted
 * separately from the devices that are actually plugged in.
 */
public class UsbReport
{
    public static void printUsbSection(List<UsbDevice> devices)
    {
        // A VM or the cloud server may have no USB at all
        if (devices.isEmpty()) {
            System.out.println("== USB DEVICES (0) ==");
            System.out.println("No USB devices found (Incase of no devieces on cloud server).");
            return;
        }

        int hubs = 0;
        for (UsbDevice d : devices) {
            if (d.isRootHub()) hubs++;
        }

        System.out.printf("== USB DEVICES (%d: %d root hubs, %d attached) ==%n",
                          devices.size(), hubs, devices.size() - hubs);
        for (UsbDevice d : devices) {
            String tag = d.isRootHub() ? "  (root hub)" : "";
            System.out.println("  " + d + tag);
        }
    }
}

