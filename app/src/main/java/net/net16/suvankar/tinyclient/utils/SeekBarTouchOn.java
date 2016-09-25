package net.net16.suvankar.tinyclient.utils;

/**
 * Utility class to stop seekbar jumping when we are manually setting it by touch
 */
public class SeekBarTouchOn {
    private static boolean TouchOn = false;

    public static boolean isTouchOn() {
        return TouchOn;
    }

    public static void setTouchOn(boolean touchOn) {
        TouchOn = touchOn;
    }
}
