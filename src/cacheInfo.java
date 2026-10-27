import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

// Information about the CPU caches, read from /sys/devices/system/cpu/cpuN/cache.

class cacheInfo {

    // cpuN represents the Nth CPU or logical core, and index0 is the first cache entry.
    private static final Path CPU_BASE_PATH = sysFile.path("/sys/devices/system/cpu");

    private static Path addCPUandIndex(int cpuNumber, int index) {
        return CPU_BASE_PATH.resolve("cpu" + cpuNumber).resolve("cache")
                            .resolve("index" + index);
    }

    // counts how many cache layers CPU N has. This is equal to the number of index folders under /sys/devices/system/cpu/cpuN/cache.

    public static int getCount(int cpuNumber) {
        int  count = 0;
        while (Files.isDirectory(addCPUandIndex(cpuNumber, count))) {
            count++;    
        }
        return count;
    }

    // returns the cache levels of CPU N.
    public static int getLevel(int cpuNumber, int index) {
        return (int) sysFile.number(addCPUandIndex(cpuNumber, index).resolve("level"));
    }

    //returns the cache type of each cache level of CPU N.
    //type can be "Data", "Instruction", or "Unified".
    public static String getType(int cpuNumber, int index) {
        return sysFile.text(addCPUandIndex(cpuNumber, index).resolve("type"));
    }

    // returns the cache size of each cache level of CPU N. 
    // sysFile.text is used to read the size from the "size" file under each index folder.
    // The size can be in kilobytes (K), megabytes (M), or bytes. 
    // This String (after removing the unit using substring and multiplying by the appropriate factor) is then parsed to a long value. 
    // If the size cannot be read or parsed, it returns -1.


    public static long getSize(int cpuNumber, int index) {
        String size = sysFile.text(addCPUandIndex(cpuNumber, index).resolve("size"));

        try {
            if (size.endsWith("K"))
                return Long.parseLong(size.substring(0, size.length() - 1)) * 1024;
            if (size.endsWith("M"))
                return Long.parseLong(size.substring(0, size.length() - 1)) * 1024 * 1024;
            return Long.parseLong(size);

        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // returns the cache line size of each cache level of CPU N.
    // The cache line size is read from the "coherency_line_size" file under each index folder, and it is returned as a long value. 
    // If the line size cannot be read or parsed, it returns -1.
    // The value is usually 64 bytes for modern CPUs, but it can vary depending on the architecture and cache level.

    public static int getLineSize(int cpuNumber, int index) {
        return (int) sysFile.number(addCPUandIndex(cpuNumber, index).resolve("coherency_line_size"));
    }
    
    // A short piece of text describing all the caches of one core (level, type, size, line size). 
    // Two cores with identical caches give identical text, so we can group cores by comparing this text. 
    // It leaves out the shared-core list on purpose, because that list differs from core to core even when the caches are the same.

    public static String signature(int cpu) {
        String s = "";
        for (int i = 0; i < getCount(cpu); i++) {

            // Format the size that comes from getSize() so that we get commas.
            String formattedSize = String.format("%,d", getSize(cpu, i));
            s += "L" + getLevel(cpu, i) + " - " + getType(cpu, i) + " cache, size: " + formattedSize + " bytes, line size: " + getLineSize(cpu, i) + " bytes\n";
        }
        return s;
    }


    // This method prints the cache information for each CPU, but groups identical CPU cache configurations first.
    // It uses the unique Signatures list to make sure a signature doesn't match an already existing one, before creating a new list of groupedCores.
    // The indexOf() method checks the list for a specific signature and returns -1 if it is not found.

    public static void printGroupedCacheInfo() {

        int count = cpuInfo.getCount();
        
        if (count == 0) {
            System.out.println("Cache information is not available");
            return;
        }

        ArrayList<String> uniqueSignatures = new ArrayList<>();
        ArrayList<ArrayList<Integer>> groupedCores = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String sig = cacheInfo.signature(i);

            int index = uniqueSignatures.indexOf(sig);

            if (index == -1) {
                uniqueSignatures.add(sig);
                ArrayList<Integer> newGroup = new ArrayList<>();
                newGroup.add(i);
                groupedCores.add(newGroup);

            } else {
                groupedCores.get(index).add(i);
            }
        }

        // loop through our list of uniqueSignatures and print each unique signature, along with what cores share it.
        for (int i = 0; i < uniqueSignatures.size(); i++) {
            String sig = uniqueSignatures.get(i);
            ArrayList<Integer> cores = groupedCores.get(i);
            System.out.println("Cores " + cores + " share the following cache configuration:");
            System.out.println(sig);
        }
    }
}