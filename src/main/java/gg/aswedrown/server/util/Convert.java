package gg.aswedrown.server.util;

public final class Convert {

    private static final long BITS_IN_KILOBIT = (long) 1E+03;
    private static final long BITS_IN_MEGABIT = (long) 1E+06;
    private static final long BITS_IN_GIGABIT = (long) 1E+09;
    private static final long BITS_IN_TERABIT = (long) 1E+12;

    private Convert() {}

    public static String toHumanReadableBandwidth(long bitsPerSecond) {
        if (bitsPerSecond < 0L)
            throw new IllegalArgumentException("bitsPerSecond cannot be negative");

        if (bitsPerSecond < BITS_IN_KILOBIT)
            return bitsPerSecond + " bit/s";
        else if (bitsPerSecond < BITS_IN_MEGABIT)
            return Math.floorDiv(bitsPerSecond, BITS_IN_KILOBIT) + " kbit/s";
        else if (bitsPerSecond < BITS_IN_GIGABIT)
            return Math.floorDiv(bitsPerSecond, BITS_IN_MEGABIT) + " Mbit/s";
        else if (bitsPerSecond < BITS_IN_TERABIT)
            return Math.floorDiv(bitsPerSecond, BITS_IN_GIGABIT) + " Gbit/s";
        else
            return Math.floorDiv(bitsPerSecond, BITS_IN_TERABIT) + " Tbit/s";
    }

}
