import java.util.Map;
import java.util.TreeMap;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sorting and summaries for a list of PCI devices. Every method leaves the list
 * it is given unchanged and returns a new result.
 */
public class PciAnalysis
{
    // A copy of the list sorted by bus, then device, then function.
    public static List<PciDevice> sortByAddress(List<PciDevice> devices)
    {
        List<PciDevice> sorted = new ArrayList<>(devices);
        Collections.sort(sorted);
        return sorted;
    }

    // A copy of the list sorted by vendor name (A to Z), then device name, then address.
    public static List<PciDevice> sortByVendor(List<PciDevice> devices)
    {
        List<PciDevice> sorted = new ArrayList<>(devices);
        sorted.sort(Comparator.comparing(PciDevice::getVendorName, String.CASE_INSENSITIVE_ORDER)
                              .thenComparing(PciDevice::getDeviceName, String.CASE_INSENSITIVE_ORDER)
                              .thenComparing(Comparator.naturalOrder()));
        return sorted;
    }


    //How many devices each vendor has, with vendor names in alphabetical order.
    public static Map<String, Integer> countByVendor(List<PciDevice> devices)
    {
        Map<String, Integer> counts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (PciDevice d : devices) {
            String vendor = d.getVendorName();
            counts.put(vendor, counts.getOrDefault(vendor, 0) + 1);
        }
        return counts;
    }

    // How many devices each class has, with class names in alphabetical order.
    public static Map<String, Integer> countByClass(List<PciDevice> devices)
    {
        Map<String, Integer> counts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (PciDevice d : devices) {
            String cls = d.getClassName();
            counts.put(cls, counts.getOrDefault(cls, 0) + 1);
        }
        return counts;
    }

    // The key or keys with the highest count. More than one if there is a tie; empty if there are no counts.
    public static List<String> mostCommon(Map<String, Integer> counts)
    {
        int max = 0;
        for (int n : counts.values()) {
            if (n > max) max = n;
        }

        List<String> top = new ArrayList<>();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (e.getValue() == max) top.add(e.getKey());
        }
        return top;
    }

    // Print the PCI devices sorted by address, then sorted by vendor, then a summary.
    public static void printPciSection(List<PciDevice> devices)
    {
        printTable("by bus:device.function", sortByAddress(devices));
        System.out.println();
        printTable("by vendor", sortByVendor(devices));
        System.out.println();

        
        Map<String, Integer> vendorCounts = countByVendor(devices);
        Map<String, Integer> classCounts = countByClass(devices);

        System.out.println("== PCI SUMMARY ==");
       	System.out.println("Total: " + devices.size() + " devices (vendor counts add to " + total(vendorCounts) + ", class counts add to " + total(classCounts) + ")");
        System.out.println("Vendors: " + joinCounts(vendorCounts));
        System.out.println("Classes: " + joinCounts(classCounts));

        List<String> top = mostCommon(classCounts);
        if (top.isEmpty()) {
            System.out.println("Most common class: none");
        } else {
            int n = classCounts.get(top.get(0));
            String each = top.size() > 1 ? " each" : "";
            System.out.println("Most common class: " + String.join(", ", top) + " (" + n + each + ")");
        }
    }

    // Print one table of devices, in the order the list is in.
    private static void printTable(String title, List<PciDevice> devices)
    {
        System.out.printf("== PCI DEVICES (%d), %s ==%n", devices.size(), title);
        System.out.printf(" BUS DEV FN  ID         %-30s  %s%n", "CLASS", "VENDOR / DEVICE");
        for (PciDevice d : devices) {
            System.out.printf("  %02x  %02x  %x  %04x:%04x  %-30s  %s %s%n",
                              d.getBus(), d.getDevice(), d.getFunction(),
                              d.getVendorId(), d.getDeviceId(),
                              d.getClassName(), d.getVendorName(), d.getDeviceName());
        }
    }

    // Turn counts like {Intel=25, Realtek=1} into "Intel 25, Realtek 1".
    private static String joinCounts(Map<String, Integer> counts)
    {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            parts.add(e.getKey() + " " + e.getValue());
        }
        return String.join(", ", parts);
    }

    // Add up all the counts in a map, e.g. {Intel=25, Realtek=1, Sandisk=1} gives 27.
    private static int total(Map<String, Integer> counts)
    {
        int sum = 0;
        for (int n : counts.values()) {
            sum += n;
        }
        return sum;
    }

}

