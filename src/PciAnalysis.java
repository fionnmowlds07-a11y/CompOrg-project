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

    // A copy of the list sorted by vendor name (A to Z), then by address within a vendor.
    public static List<PciDevice> sortByVendor(List<PciDevice> devices)
    {
        List<PciDevice> sorted = new ArrayList<>(devices);
        sorted.sort(Comparator.comparing(PciDevice::getVendorName, String.CASE_INSENSITIVE_ORDER)
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



}

