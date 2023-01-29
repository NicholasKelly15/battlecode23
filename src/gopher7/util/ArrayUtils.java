package gopher7.util;

import battlecode.common.MapLocation;

public class ArrayUtils {

    public static MapLocation chooseRandom(MapLocation[] locations) {
        if (locations != null && locations.length > 0) {
            return locations[(int)(Math.random() * locations.length)];
        } else {
            return null;
        }
    }

}
