package net.net16.suvankar.tinyclient.utils;

/**
 * This class validates a String if it is a valid IPv4 address or not.
 */
public class IPv4AddressValidator {
    public static boolean isValid(String ipAddress) {
        if(ipAddress.matches("(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)")) {
            String[] parts = ipAddress.split("\\.");
            for (String p : parts) {
                Integer i = Integer.parseInt(p);
                if (i >= 0 && i <= 255) {
                    if(p.length()>1 && p.startsWith("0")){
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
        else
            return false;
        return true;
    }
}