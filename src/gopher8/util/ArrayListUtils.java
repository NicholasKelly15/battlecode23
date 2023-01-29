package gopher8.util;

import battlecode.common.Direction;

import java.util.ArrayList;

public class ArrayListUtils {

    public static Direction chooseRandom(ArrayList<Direction> directions) {
        if (directions != null && directions.size() > 0) {
            return directions.get((int)(Math.random() * directions.size()));
        } else {
            return null;
        }
    }

}
