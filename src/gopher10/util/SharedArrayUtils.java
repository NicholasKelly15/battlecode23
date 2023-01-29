package gopher10.util;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;

public class SharedArrayUtils {

    public static void printSharedArray(RobotController rc, boolean writeBinary) throws GameActionException {
        System.out.println("Start of Array");
        System.out.println("---------------------------------");
        for (int i = 0; i < GameConstants.SHARED_ARRAY_LENGTH ; i++) {
            int value = rc.readSharedArray(i);
            if (writeBinary) {
                System.out.println(i + ": " + Integer.toBinaryString(value));
            } else {
                System.out.println(i + ": " + value);
            }
        }
        System.out.println("---------------------------------");
    }

}
