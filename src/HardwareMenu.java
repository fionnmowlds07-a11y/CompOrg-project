import java.util.Scanner;

public class HardwareMenu {
    public static void main(String[] args) throws Exception
    {
    boolean menu = true;
    Scanner Scan = new Scanner(System.in);
        do { 
            System.out.println("=====================================");
            System.out.println("--------------Main Menu--------------");
            System.out.println("=====================================\n");
            System.out.println("Enter the corresponding number to view specific system info.\n");
            System.out.println("1: CPU information");
            System.out.println("2: Memory information");
            System.out.println("3: PCI information");
            System.out.println("4: USB information");
            System.out.println("9: Quit");


            int choice = 0;
            try {
                System.out.print("Select:");
                choice = Integer.parseInt(Scan.nextLine());
            } catch(NumberFormatException e) {
                System.out.println("Invalid input. Please try again:\n");
                continue;
            }

            switch(choice) {
                case 1:
                    choice = 0;

                    System.out.println("CPU cores:  " + cpuInfo.getCount());
                    System.out.println("CPU model:  " + cpuInfo.getModel());

                    //CPU usage: for loop for per core, and overall average.
                    double[] usages = cpuInfo.getUsages();
                    double totalUsage = 0;
                    for (int i = 0; i < usages.length; i++) {
                        double usage = usages[i];
                        System.out.println("CPU " + i + " Usage: " + usage + "%");
                        totalUsage = totalUsage + usage;
                    }
                    double avgCpuUsage = Math.round((totalUsage / usages.length) * 100.0) / 100.0;
                    System.out.println("Average CPU Usage: " + avgCpuUsage + "%");

                    // Cache information for CPU 0
                    // Print cache information
                    System.out.println();
                    System.out.println("==================================================================");
                    System.out.println("Cache Information\n");
                    cacheInfo.printGroupedCacheInfo();
                    System.out.println("==================================================================");
                    
                    // Stride Report and Pointer Chase
                    System.out.println("");
                    MemoryHierarchyExplorer.printStrideReport();
                    System.out.println("");
                    MemoryHierarchyExplorer.printCacheSizeReport();

                    System.out.println("Enter E to exit:");
                    while (!Scan.nextLine().equalsIgnoreCase("e")) {
                        System.out.println("Invalid input. Enter E:");
                    }
                    break;

                case 2:
                    choice = 0;
                    MemoryReport.report();
                    System.out.println("Enter E to exit:");
                    while (!Scan.nextLine().equalsIgnoreCase("e")) {
                        System.out.println("Invalid input. Enter E:");
                    }
                    break;

                case 3:
                    choice = 0;
                    PciAnalysis.printPciSection(DeviceScanner.scanPci());   // Fionn: PCI, sorted two ways + summary
                    System.out.println("Enter E to exit:");
                    while (!Scan.nextLine().equalsIgnoreCase("e")) {
                        System.out.println("Invalid input. Enter E:");
                    }
                    break;

                case 4:
                    choice = 0;
                    UsbReports.printUsbSection(DeviceScanner.scanUsb());     // Joseph: USB, root hubs labelled
                    System.out.println("Enter E to exit:");
                    while (!Scan.nextLine().equalsIgnoreCase("e")) {
                        System.out.println("Invalid input. Enter E:");
                    }
                    break;

                case 9:
                    menu = false;
                    break;

            }

        } while (menu);
        Scan.close();
    }
}
