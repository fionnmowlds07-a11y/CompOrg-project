public class USBReport {
    public static void report(){
        int count = usbInfo.getCount(); // gets the amount of USB devices
        System.out.println("------USB Report------");
        if (count == 0) {
            System.out.println("No USB devices found."); // output if count is 0
            return;
        }
        else if (count < 0) {
            System.out.println("Error retrieving USB device count."); // output if an error is given
            return;
        }
        System.out.println("Total USB Devices: " + count); // print the count of USB devices

        for (int i = 0; i < count; i++) {
                int busInfo = usbInfo.getBusNumber(i);//{ gets the info from the USB devices
                int deviceInfo = usbInfo.getDeviceNumber(i);
                int vendor = usbInfo.getVendorId(i);
                int productID = usbInfo.getProductId(i);
                String manufacturer = usbInfo.getManufacturer(i);
                String product = usbInfo.getProduct(i);//}
                

                System.out.println("Bus Information: " + busInfo); //{ prints the info from the USB devices
                System.out.println("Device Information: " + deviceInfo);
                System.out.println("Vendor ID: " + String.format("%04x", vendor));
                System.out.println("Product ID: " + String.format("%04x", productID));
                System.out.println("Manufacturer: " + manufacturer);
                System.out.println("Product: " + product);//}
            }
        }
     
}