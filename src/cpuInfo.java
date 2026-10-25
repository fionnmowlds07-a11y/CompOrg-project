import java.nio.file.Path;

/**
 * Information about the processor, read from /proc/cpuinfo and /proc/stat.
 *
 * The busy and idle values are counts of "ticks" since the machine booted
 * (usually 100 ticks per second). To find the load, read them twice, wait about
 * a second, read them again, and work out busy / (busy + idle) from the DIFFERENCES.
 */
public class cpuInfo
{
    private static final Path CPUINFO = sysFile.path("/proc/cpuinfo");
    private static final Path STAT = sysFile.path("/proc/stat");

    /** Number of cores (logical processors) the operating system sees. */
    public static int getCount()
    {
        int count = 0;
        for (String line : sysFile.lines(CPUINFO))
            if (line.startsWith("processor"))
                count++;
        return count;
    }

    /**
     * The processor model, for example "Intel(R) Xeon(R) Processor @ 2.80GHz".
     * On arm64 there is no model name line, so this returns "unknown"; use
     * getField("CPU part") or getField("CPU implementer") instead.
     */
    public static String getModel()
    {
        String model = getField("model name");
        return model.isEmpty() ? "unknown" : model;
    }

    /** The value of the first line in /proc/cpuinfo whose name is exactly 'name', or "". */
    public static String getField(String name)
    {
        for (String line : sysFile.lines(CPUINFO)) {
            int colon = line.indexOf(':');
            if (colon > 0 && line.substring(0, colon).strip().equals(name))
                return line.substring(colon + 1).strip();
        }
        return "";
    }
    /** Ticks since boot that core number 'core' (0, 1, 2 ...) has spent doing work. */
    public static long getBusy(int core)
    {
        long[] f = fields(core);
        // user + nice + system + irq + softirq + steal
        return f == null ? -1 : f[0] + f[1] + f[2] + f[5] + f[6] + f[7];
    }

    /** Ticks since boot that core number 'core' (0, 1, 2 ...) has spent doing work. (ALL) */
    public static long getTotal(int core)
    {
        long[] f = fields(core);
        // user + nice + system + idle + iowait + irq + softirq + steal (idle + iowait must be included for accurate cpu usage)
        return f == null ? -1 : f[0] + f[1] + f[2] + f[3] + f[4] + f[5] + f[6] + f[7];
    }

    

    /** Ticks since boot that core number 'core' has spent idle (including waiting for I/O). */
    public static long getIdle(int core)
    {
        long[] f = fields(core);
        return f == null ? -1 : f[3] + f[4];
    }
/** OLD getUsage code, deprecated and replaced by getUsages()
    public static double getUsage(int core) { //reads time counters since boot of each CPU core, adds them together
        //this only returns for ONE cpu core.
        long oldTotal = getTotal(core);
        long oldIdleTime = getIdle(core);

        //waits one second between old and new reading
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        }

        long newTotal = getTotal(core);
        long newIdleTime = getIdle(core);

        long deltaTotal = newTotal - oldTotal;
        long deltaIdle = newIdleTime - oldIdleTime;

        double usagePercent = 100.0 * (deltaTotal - deltaIdle) / deltaTotal;
        double rounded = Math.round(usagePercent * 100.0) / 100.0;

        return rounded;

    }
    **/

    /**
      returns the usage of every core after one one-second sampling interval.
      both snapshots are taken for all cores before calculating any usage.
      returns all usages in a double[]
     */
    public static double[] getUsages()
    {
        int count = getCount();
        long[] oldTotal = new long[count];
        long[] oldIdle = new long[count];

        for (int core = 0; core < count; core++) {
            oldTotal[core] = getTotal(core);
            oldIdle[core] = getIdle(core);
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new double[count];
        }

        double[] usages = new double[count];
        for (int core = 0; core < count; core++) {
            long newTotal = getTotal(core);
            long newIdle = getIdle(core);
            long deltaTotal = newTotal - oldTotal[core];
            long deltaIdle = newIdle - oldIdle[core];
            double usage = deltaTotal == 0
                    ? 0
                    : 100.0 * (deltaTotal - deltaIdle) / deltaTotal;
            usages[core] = Math.round(usage * 100.0) / 100.0;
        }
        return usages;
    }

    /** The numbers from the "cpuN" line of /proc/stat, or null if there is no such core. */
    private static long[] fields(int core)
    {
        String prefix = "cpu" + core + " ";
        for (String line : sysFile.lines(STAT)) {
            if (line.startsWith(prefix)) {
                String[] part = line.strip().split("\\s+");
                long[] f = new long[8];
                for (int i = 0; i < 8; i++)
                    f[i] = (i + 1 < part.length) ? Long.parseLong(part[i + 1]) : 0;
                return f;
            }
        }
        return null;
    }
}
