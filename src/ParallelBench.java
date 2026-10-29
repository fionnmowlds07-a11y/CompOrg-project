/**
 * EXTENSION 2: does a job get faster when we split it across more threads?
 *
 * The job: multiply two big matrices (C = A x B). Every row of C can be worked
 * out on its own, so we share the rows out between the threads.
 * Each measured speed-up is printed next to what Amdahl's law predicts.
 *
 * Run:  java -cp src/out ParallelBench
 */
public class ParallelBench
{
    static final int N = 800;   // the matrices are N x N
    static double[][] a = new double[N][N];
    static double[][] b = new double[N][N];
    static double[][] c = new double[N][N];

    public static void main(String[] args) throws InterruptedException
    {
        // Fill A and B with random numbers
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                a[i][j] = Math.random();
                b[i][j] = Math.random();
            }
        }

        int cores = Runtime.getRuntime().availableProcessors();
        System.out.println("== PARALLEL SPEEDUP (" + N + " x " + N + " matrix multiply) ==");
        System.out.println("CPUs on this machine: " + cores);
        System.out.println();
        System.out.println("THREADS   TIME (ms)   MEASURED   AMDAHL   PERFECT");

        multiply(1);   // warm-up run, so Java has compiled the code before we time it
        double oneThread = 0;
        double f = 0;  // the sequential part of the job (0 = none, 1 = all of it)

        for (int threads = 1; threads <= cores * 2; threads = threads * 2) {
            long start = System.nanoTime();
            multiply(threads);
            double ms = (System.nanoTime() - start) / 1_000_000.0;

            if (threads == 1) oneThread = ms;
            double speedup = oneThread / ms;

            // Work out f from the 2-thread run: Amdahl's law with n = 2 is speedup = 2 / (1 + f), so f = 2 / speedup - 1
            if (threads == 2) f = Math.max(0, 2 / speedup - 1);

            // Amdahl's law (from the slides): speed-up = n / (1 + (n - 1) f)
            double amdahl = threads / (1 + (threads - 1) * f);

            System.out.printf("%7d   %9.0f   %7.2fx   %5.2fx   %6dx%n", threads, ms, speedup, amdahl, threads);
        }

        System.out.println();
        System.out.printf("Sequential part f = %.1f%% (worked out from the 2-thread run)%n", f * 100);
    }

    // Works out C = A x B with 'threads' threads. Thread t does rows t, t + threads, t + 2*threads ...
    static void multiply(int threads) throws InterruptedException
    {
        Thread[] workers = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            int first = t;   // each thread needs its own copy of t
            workers[t] = new Thread(() -> {
                for (int i = first; i < N; i += threads) {
                    for (int j = 0; j < N; j++) {
                        double sum = 0;
                        for (int k = 0; k < N; k++) {
                            sum += a[i][k] * b[k][j];
                        }
                        c[i][j] = sum;
                    }
                }
            });
            workers[t].start();
        }
        for (Thread w : workers) {
            w.join();   // wait until every thread has finished
        }
    }
}
