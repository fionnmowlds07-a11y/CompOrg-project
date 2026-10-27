import java.util.Arrays;

// Measures the CPU caches from inside Java and compares the result with what the
// operating system reports (see cacheInfo).

//  stride test: read every 1st, 2nd, 4th ... element of a huge int array and time the
// average read. The cost per read rises while several reads share one cache line, and
// stops rising once every read needs a new line. That "knee" estimates the line size.

class MemoryHierarchyExplorer {

    private static final int MAX_STRIDE = 128;      // in ints: 128 * 4 = 512 bytes
    private static int sink;                        // using the result stops the JIT deleting the loop


    // creating the large array - a 256MiB int array
    // or a smaller one if the Java heap is too small

    private static int[] createLargeArray() {

        int[] sizesMiB = {256, 128, 64};
        for (int mib : sizesMiB) {
            try {
                int[] data = new int[mib * ((1024 * 1024) / 4)];
                    for (int i = 0; i < data.length; i++)
                    data[i] = i;
            return data;

        } catch (OutOfMemoryError e) {
            }
        }
        return null;
    }

    // This times one pass over the array, touching every stride-th int
    // it returns the ns per read

    private static double strideTestTimer(int[] data, int stride) {

        int reads = 0;
        int sum = 0;
        long t0 = System.nanoTime();

        for (int i = 0; i < data.length; i = i + stride) {
            sum = sum + data[i];
            reads++;
        }

        long t1 = System.nanoTime();
        sink = sink + sum;
        return (t1 -t0) / (double) reads;
    }

    // ------------------------------------------------------------
    // The actual stride test operation
    // ------------------------------------------------------------

    public static double[] strideTest(int[] data) {

        int count = 0;

        for (int stride = 1; stride <= MAX_STRIDE; stride *= 2) {
            count++;
        }

        double[] ns = new double[count];
        int k = 0;

        for (int stride = 1; stride <= MAX_STRIDE; stride *= 2) {
            strideTestTimer(data, stride);                             // we do five different runs so that any outliers are discarded
            double[] runs = new double[5];

            for (int r = 0; r < 5; r++) {
                runs[r] = strideTestTimer(data, stride);
            }
            Arrays.sort(runs);                                        // sorts the five different runs from smallest to largest and we pick the median at index 2
            ns[k++] = runs[2];
        }
        return ns;
    }

    // does the math to estimate the size of the cache line

    public static int estimateLineSize(double[] ns) {
    int n = ns.length;
    double[] tail = { ns[n - 3], ns[n - 2], ns[n - 1] };
    Arrays.sort(tail);
    double plateau = tail[1];

    int currentStrideBytes = 4; // Starts at 4 bytes (1 element * 4 bytes)
    
    for (int k = 0; k < n; k++) {
        if (ns[k] >= 0.8 * plateau) {
            return currentStrideBytes; // Returns the exact byte size
        }
        currentStrideBytes *= 2; // doubles the byte size each step, mirroring the stride loop
    }
    return -1;
}

    // returns size of the biggest cache in cpu0 in bytes

    private static long biggestCache() {
        long best = -1;
        int count = cacheInfo.getCount(0);
        for (int i = 0; i < count; i++)
            best = Math.max(best, cacheInfo.getSize(0, i));
        return best;
    }

    // compares to see if the estimate is accurate

     private static String compare(int estimate, int reported) {

        if (estimate == reported) {
            return "\n\nThe measurement matches.";
        } if (estimate == 2 * reported) {
            return "\n\nThe measurement is twice the line size. \nA likely cause is that the CPU fetches two neighbouring lines together \n(adjacent-line prefetch, common on Intel chips), \nso the cost keeps rising until reads are two lines apart.";
        }
        return "The measurement does not match; repeat the run, since background load adds noise.";
    }

    // ------------------------------------------------------
    // REPORT
    // ------------------------------------------------------

    public static void printStrideReport() {

        System.out.println("== CACHE LINE SIZE (stride test) ==");
        int[] data = createLargeArray();

        if (data == null) {
            System.out.println("Not enough memory for the test.");
            return;
        }

        long arrayBytes = data.length * 4L;
        System.out.println("Array size: " + arrayBytes / (1024 * 1024) + " MiB");

        // The array must be much bigger than the biggest cache, or the result is not trustworthy.

        long biggest = biggestCache();
        if (biggest > arrayBytes / 8) {
            System.out.println("Warning: the biggest cache (" + biggest / 1024 + " KiB) is large compared with the array, so results may be unreliable.");
        }

        double[] ns = strideTest(data);
        double max = ns[0];

        for (int i = 1; i < ns.length; i++) {
            if (ns[i] > max) {
                max = ns[i];
            }
        }

        System.out.println("Stride (bytes)   ns per read");
        int currentStrideBytes = 4;

        for (int k = 0; k < ns.length; k++) {
            int bars = (int) Math.round(30 * ns[k] / max);  // this formula finds the fraction or percentage of 
                                                            // how slow the current stride is compared to the absolute slowest stride (max)

            System.out.printf("%12d   %10.2f  %s%n", currentStrideBytes, ns[k], "#".repeat(bars));  // 12d = byte size as int in column 12 characters wide
                                                                                                    // 10.2f = ns per read as float, rounded to 2 d.p. in column 10 char wide
                                                                                                    // s= string, n = newline
        currentStrideBytes *= 2;
        }

        int estimate = estimateLineSize(ns);
        int reported = cacheInfo.getCount(0) > 0 ? cacheInfo.getLineSize(0, 0) : -1;

        System.out.println();
        System.out.println("Measured: the read time stops rising at a stride of " + estimate + " bytes");

        if (reported < 0) {
            System.out.println("Reported: not available (no cache information)");
    
        } else {
            System.out.println("Reported: line size " + reported + " bytes. " + compare(estimate, reported));
        }
    }
}
