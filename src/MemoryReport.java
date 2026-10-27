public class MemoryReport {
    public static String formatBytes(long KiB){
        if (KiB < 0){ 
            return "unknown"; // returns unknown if the value if the KiB is negative
        }
        double MiB = KiB / 1024.0; //formats KiB into MiB
        double GiB = MiB / 1024.0; //formats MiB into GiB
        if (GiB >= 1.0){
            return String.format("%.2f GiB", GiB); //returns the value in GiB if it is big enough to be formatted into GiB
        } else if (MiB >= 1.0){
            return String.format("%.2f MiB", MiB); //returns the value in MiB if it is big enough to be formatted into MiB
        } else {
            return String.format("%d KiB", KiB); //if the value is too small to be formatted into MiB or GiB, it will return the value in KiB
        }
    }
    public static void report(){
        long total = memInfo.getTotal(); //gets the total memory in KiB from the starter code
        long available = memInfo.getAvailable(); //gets the available memory in KiB from the starter code
        long used = memInfo.getUsed(); //gets the used memory in KiB from the starter code

        System.out.println("== MEMORY ==");
        if (total <= 0 || available < 0) {
            // the starter returns -1 when /proc/meminfo can't be read, so there is nothing to work out
            System.out.println("Memory information not available (/proc/meminfo could not be read).");
            return;
        }

        double percentUsed = (double) used / total * 100; //calculates the percentage of memory used

        System.out.println("Total Memory: " + formatBytes(total));
        System.out.println("Used Memory: " + formatBytes(used));
        System.out.println("Available Memory: " + formatBytes(available));
        System.out.println("Percentage of the Memory Used: " + String.format("%.2f%%", percentUsed));

        long swapTotal = memInfo.field("SwapTotal"); //gets the total swap memory from the starter code
        long swapFree = memInfo.field("SwapFree"); //gets the free swap memory from the starter code
        if (swapTotal > 0){
            long swapUsed = swapTotal - swapFree;
            System.out.println("Total Swap Memory: " + formatBytes(swapTotal)); //prints the swap totals
            System.out.println("Used Swap Memory: " + formatBytes(swapUsed));
        }

        System.out.println("In plain English: this machine has " + formatBytes(total) + " of RAM, of which "
                           + formatBytes(used) + " (" + Math.round(percentUsed) + "%) is in use.");
    }
}