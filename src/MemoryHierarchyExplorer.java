import java.util.Arrays;

// Measures the CPU caches from inside Java and compares the result with what the
// operating system reports (see cacheInfo).

//  stride test: read every 1st, 2nd, 4th ... element of a huge int array and time the
// average read. The cost per read rises while several reads share one cache line, and
// stops rising once every read needs a new line. That "knee" estimates the line size.

class MemoryHierarchyExplorer {

    private static final int MAX_STRIDE = 128;                          // in ints: 128 * 4 = 512 bytes
    private static int sink;                                            // using the result stops the JIT deleting the loop
    private static final long MAX_WORKING_SET = 128L * 1024 * 1024;     // max working set - 128MiB
    private static final double JUMP = 1.4;                             // a rise of 1.4x or more counts as a cache boundary


    // creating the large array - a 256MiB int array
    // or a smaller one if the Java heap is too small
    // -------------------------------------------------------------------------------------

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
    // -------------------------------------------------------------------------------------


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
    // -------------------------------------------------------------------------------------

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
    // -------------------------------------------------------------------------------------

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

    // -------------------------------------------------------------------
    // POINTER CHASE
    // -------------------------------------------------------------------

    // The working-set sizes we test: 4 KiB, 8 KiB, 16 KiB ... up to MAX_SET.
    // creates all the different arrays we need of these sizes
    // -------------------------------------------------------------------------------------

    public static long[] workingSets() {
        int count = 0;                // counts the number of arrays we need before creating them
        for (long bytes = 4L * 1024; bytes <= MAX_WORKING_SET; bytes *= 2) {
            count++;
        }

        long[] sizes = new long[count];
        long currentBytes = 4L * 1024; // Start at 4 KiB
    
        for (int k = 0; k < count; k++) {
            sizes[k] = currentBytes;   // Store the current size
            currentBytes *= 2;         // Double it for the next working set size
        }
    
        return sizes;
    }

    // this method creates an array of integers of length n
    // it loops through it and assigns each index the value of itself, to fill the array
    // random is used to shuffle the array elements without allowing elements to remain where they are
    // next [i] says which index to visit after i, so every slot together forms a big loop or cycle
    // -------------------------------------------------------------------------------------

    private static int[] makeRandomCycle(int n) {

        int[] next = new int[n];
        for (int i = 0; i < n; i++) {
            next[i] = i;
        }

        java.util.Random random = new java.util.Random(42);
        for (int i = n - 1; i > 0; i--) {
            int j = random.nextInt(i);              // 0 to i-1, never i itself, so no short loops
            int t = next[i];
            next[i] = next[j];
            next[j] = t;
        }
        return next;
    }

    // this times how long it takes the CPU to fetch the element at a given array index and store it in our pointer variable
    // the loop is repeated a given numbver of times
    // retuirns the average ns per repeat
    // -------------------------------------------------------------------------------------

    private static double timeChase(int[] next, long steps) {

        int p = 0;
        long t0 = System.nanoTime();
        
        for (long k = 0; k < steps; k++) {
            p = next[p];                            // each read depends on the previous one
        }

        long t1 = System.nanoTime();
        sink += p;
        return (t1 - t0) / (double) steps;
    }

    // takes an array of different working-set sizes, loops through them
    // builds the random cycle / loop for each size
    // returns an array of average nanosecond speeds for each size
    // -------------------------------------------------------------------------------------

    public static double[] pointerChase(long[] sizes) {

        double[] ns = new double[sizes.length]; // creates an empty results container to store the average ns per read for each test size
        int done = 0;                           // tracking counter for how many working-set sizes successfully tested before running out of memory

        for (long workingSetBytes : sizes) {
            int[] next;

            try {
                next = makeRandomCycle( (int) workingSetBytes / 4);

            } catch (OutOfMemoryError e) {
                break;                          // stops here: the sizes so far are still valid
            }
             
            timeChase(next, 2000000);

            double[] runs = new double[5];
            for (int run = 0; run < 5; run++) {
                runs[run] = timeChase(next, 5000000);
            }
            Arrays.sort(runs);
            ns[done++] = runs[2];   // takes the median of the five runs
        }
    return Arrays.copyOf(ns, done); // this returns the array but removes everything from index 'done' onward to remove meaningless data due to unsuccessful runs
    }

    // this method takes the array of working-set sizes and average ns per read for each test size
    // it calculates where the cache boundaries lie
    // -------------------------------------------------------------------------------------

    public static long[] findBoundaries(long[] bytes, double[] ns) {

        long[] found = new long[ns.length];              // Array to store the detected cache boundary sizes
        int count = 0;                                   // Tracks how many boundaries we have successfully found
        int best = -1;                                   // Index of the sharpest jump in the current cache step (-1 means none found yet)
        double bestRatio = 0;                            // Stores the size of the biggest ratio jump found so far

        // Loop through the timing results starting from the second element
        for (int i = 1; i < ns.length; i++) {
            double ratio = ns[i] / ns[i - 1];            // Compare current working set speed to the previous one
        
            if (ratio >= JUMP) {                         // If the time per read jumped by 1.4x or more (a cache boundary)
                if (ratio > bestRatio) {                 // Check if this jump is sharper than previous ones in this step
                   best = i;                            // Save this index as our current best candidate
                    bestRatio = ratio;                   // Update the best ratio record
                }
            } else if (best >= 0) {                      // If the jump has ended (ratio dropped back down below JUMP)
                found[count++] = bytes[best - 1];        // Record the working set size right BEFORE the sharpest jump
                best = -1;                               // Reset best index for the next potential cache step
                bestRatio = 0;                           // Reset best ratio for the next step
            }
        }
    
        // Catch any final boundary if the loop ended while still inside a jump step
        if (best >= 0) {
            found[count++] = bytes[best - 1];            // Record the final boundary size
        }
        return Arrays.copyOf(found, count);              // Trim the array to the exact number of boundaries found and return it
    }

    // -------------------------------------------------------------------------------------
    // ========= Printing Cache Size Details ===============================================
    // -------------------------------------------------------------------------------------

    public static void printCacheSizeReport() {

        System.out.println("== CACHE SIZES (pointer chase) ==");
        long [] workingSetBytes = workingSets();
        double [] ns = pointerChase(workingSetBytes);

        if (ns.length < 3) {
            System.out.println("Not enough memory for the test. Try: java -Xmx1g -cp out CacheExplorer");
            return;
        }
        
        // finding min and max for our graph
        double min = ns[0];
        double max = ns[0];
        for (int i = 0; i < ns.length; i++) {
            if (ns[i] < min) {
                min = ns[i];
            }
            if (ns[i] > max) {
                max = ns[i];
            }
        }
        System.out.println("Working set   ns per read");

        for (int k = 0; k < ns.length; k++) {
            // the times span a huge range, so the bars use a log scale
            int bars = 1 + (int) Math.round(30 * Math.log(ns[k] / min) / Math.log(max / min));
            System.out.printf("%10s   %10.2f  %s%n", label(workingSetBytes[k]), ns[k], "#".repeat(bars)); // 10s = string, width 10. 10.2f = float, 2d.p.
        }

        long[] measured = findBoundaries(Arrays.copyOf(workingSetBytes, ns.length), ns); // resize so both are same size, because the boundaries method assumes they are
        System.out.println();
        System.out.print("Steps found (the working set just before each big rise):");
            for (long measurement : measured) {
                System.out.print(" " + label(measurement));
            }
        System.out.println();

        // the caches the machine reports for core 0, leaving out the instruction cache
        int count = cacheInfo.getCount(0);
        long[] reported = new long[Math.max(count, 0)];
        int[] levels = new int[reported.length];
        int r = 0;

        for (int i = 0; i < count; i++) {
            if (!cacheInfo.getType(0, i).equals("Instruction")) {
                reported[r] = cacheInfo.getSize(0, i);
                levels[r] = cacheInfo.getLevel(0, i);
                r++;
            }
        }

        System.out.println();

        if (r > 0) {
            System.out.printf("%-6s %12s %12s   %s%n", "Level", "Reported", "Measured", "Result");
        }
        long largest = workingSetBytes[ns.length - 1];

        for (int i = 0; i < r; i++) {
            String m = "-";
            String result;

            if (reported[i] > largest) {
                result = "bigger than the largest test (" + label(largest) + ")";

            } else if (measured.length == 0) {
                result = "no step found";

            } else {
                long near = nearest(measured, reported[i]);
                m = label(near);
                result = verdict(near, reported[i]);
            }
            System.out.printf("%-6s %12s %12s   %s%n", "L" + levels[i], label(reported[i]), m, result);
        }

        if (r == 0) {
            System.out.println("Reported cache sizes: not available (no cache information)");
        }
    }

    // -------------------------------------------------------------------------------------
    // Labelling method for bytes to kib or mib
    // 4096 becomes "4 KiB", 1048576 becomes "1 MiB".
    // -------------------------------------------------------------------------------------

    private static String label(long bytes) {
        if (bytes >= 1024 * 1024 && bytes % (1024 * 1024) == 0)
            return (bytes / (1024 * 1024)) + " MiB";
        return (bytes / 1024) + " KiB";
    }

    // The measured boundary closest to a reported size (closest on a doubling scale).
    // -------------------------------------------------------------------------------------

    static long nearest(long[] measured, long reported)
    {
        long best = measured[0];
        double bestDistance = Double.MAX_VALUE;
        for (long m : measured) {
            double distance = Math.abs(Math.log((double) m / reported));
            if (distance < bestDistance) {
                bestDistance = distance;
                best = m;
            }
        }
        return best;
    }

    // -------------------------------------------------------------------------------------

    private static String verdict(long measured, long reported)
    {
        double ratio = (double) measured / reported;
        if (ratio >= 0.5 && ratio <= 2)
            return "matches (within one doubling)";
        if (ratio >= 0.25 && ratio < 0.5)
            return "smaller than reported";
        if (ratio > 2 && ratio <= 4)
            return "larger than reported";
        return "no step found near this size";
    }
}