import java.util.Random;

/**
 * EXTENSION: measures how fast each CPU cache is, and compares it with the
 * cache sizes Linux reports (cacheInfo).
 *
 * Run:  taskset -c 0 java -cp src/out CacheExplorer
 */
public class CacheExplorer
{
    static long sink = 0;   // keeps the result of each loop so Java can't skip running it

    public static void main(String[] args)
    {
        // What Linux says CPU 0 has
        System.out.println("== CACHE EXPLORER ==");
        for (int i = 0; i < cacheInfo.getCount(); i++) {
            System.out.println("L" + cacheInfo.getLevel(i) + " " + cacheInfo.getType(i) + ": " + cacheInfo.getSize(i) / 1024 + " KiB");
        }
        System.out.println();

        // Time the loop for every array size from 4 KiB to 128 MiB, doubling each time
        for (int kib = 4; kib <= 128 * 1024; kib = kib * 2) {
            int[] loop = makeLoop(kib * 1024 / 4);   // int = 4 bytes
            timeLoop(loop);                          // warm-up run, result thrown away
            double ns = timeLoop(loop);
            System.out.printf("%8d KiB  %7.2f ns  %s%n", kib, ns, fitsIn(kib * 1024L));
        }
    }

    // Makes an array where each slot holds the number of the next slot to visit.
    // The order is random, but it is one big loop that goes through every slot.
    static int[] makeLoop(int n)
    {
        int[] next = new int[n];
        for (int i = 0; i < n; i++) {
            next[i] = i;
        }
        Random random = new Random();
        for (int i = n - 1; i > 0; i--) {
            int j = random.nextInt(i);   // pick a slot before i 
            int temp = next[i];
            next[i] = next[j];
            next[j] = temp;
        }
        return next;
    }

    // Follows the loop 5 million times and returns the average time per step in nanoseconds
    static double timeLoop(int[] next)
    {
        int steps = 5_000_000;
        int p = 0;
        long start = System.nanoTime();
        for (int k = 0; k < steps; k++) {
            p = next[p];   // the next step can't start until this one has loaded
        }
        long end = System.nanoTime();
        sink = sink + p;
        return (double) (end - start) / steps;
    }

    // Which cache Linux says an array of this size fits in
    static String fitsIn(long bytes)
    {
        for (int i = 0; i < cacheInfo.getCount(); i++) {
            if (!cacheInfo.getType(i).equals("Instruction") && bytes <= cacheInfo.getSize(i)) {
                return "fits in L" + cacheInfo.getLevel(i);
            }
        }
        return "Too big, goes to RAM";
    }
}

