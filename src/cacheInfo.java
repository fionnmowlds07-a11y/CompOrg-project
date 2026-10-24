import java.nio.file.Files;
import java.nio.file.Path;

// Information about the CPU caches, read from /sys/devices/system/cpu/cpu0/cache.
// For now, this is a single core (CPU 0) information reader.

class cacheInfo {

    // cpu0 represents the first CPU or logical core, and index0 is the first cache (L1).
    private static final Path CACHEINFO = sysFile.path("/sys/devices/system/cpu/cpu0/cache");


    // counts how many cache levels CPU 0 has. This is the number of index folders under /sys/devices/system/cpu/cpu0/cache.
    // it returns the number of index folders, which is the number of cache levels (L1, L2, L3, etc.) for CPU 0.


    public static int getCount() {
        int  count = 0;
        while (Files.isDirectory(CACHEINFO.resolve("index" + count))) {
            count++;    
        }
        return count;
    }

    // returns the cache level of CPU 0. This is the number of index folders under /sys/devices/system/cpu/cpu0/cache.
    public static long getLevel(int index) {
        return sysFile.number(CACHEINFO.resolve("index" + index + "/level"));
    }

    //returns the cache type of each cache level of CPU 0.
    //type can be "Data", "Instruction", or "Unified".
    public static String getType(int index) {
        return sysFile.text(CACHEINFO.resolve("index" + index + "/type"));
    }

    // returns the cache size of each cache level of CPU 0. 
    // sysFile.text is used to read the size from the "size" file under each index folder.
    // This String (after removing the unit using substring) is then parsed to a long value.
    // The size is returned in bytes.
    // The size is read from the "size" file under each index folder, and it can be in kilobytes (K), megabytes (M), or bytes. 
    // The method converts the size to bytes (using appropriate multipliers) and returns it as a long. 
    // If the size cannot be read or parsed, it returns -1.

    public static long getSize(int index) {
        String size = sysFile.text(CACHEINFO.resolve("index" + index + "/size"));

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

    // returns the cache line size of each cache level of CPU 0.
    // The cache line size is read from the "coherency_line_size" file under each index folder, and it is returned as a long value. 
    // If the line size cannot be read or parsed, it returns -1.
    // The value is usually 64 bytes for modern CPUs, but it can vary depending on the architecture and cache level.

    public static long getLineSize(int index) {
        return sysFile.number(CACHEINFO.resolve("index" + index + "/coherency_line_size"));
    }
}
