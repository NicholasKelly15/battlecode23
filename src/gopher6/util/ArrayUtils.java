package gopher6.util;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

import java.util.ArrayList;

public class ArrayUtils {

    public static MapLocation chooseRandom(MapLocation[] locations) {
        if (locations != null && locations.length > 0) {
            return locations[(int)(Math.random() * locations.length)];
        } else {
            return null;
        }
    }

}
