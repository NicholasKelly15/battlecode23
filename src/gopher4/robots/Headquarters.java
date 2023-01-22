package gopher4.robots;

import battlecode.common.*;

import java.util.Stack;


public class Headquarters extends Robot {

    public Headquarters(RobotController rc) throws Exception {
        super(rc);
    }

    private MapLocation getOpenSpawnLocation() throws GameActionException {
        MapLocation spawnLoc = rc.getLocation();
        for (Direction dir : pathing.DIRECTIONS) {
            MapLocation newLoc = spawnLoc.add(dir);
            if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc) && rc.sensePassability(newLoc)) {
                return newLoc;
            }
        }
        return null;
    }

    public void run() throws GameActionException, IllegalAccessException {

        super.run();

        MapLocation freeLoc = getOpenSpawnLocation();
        if (freeLoc != null && rc.canBuildRobot(RobotType.CARRIER, freeLoc)) {
            rc.buildRobot(RobotType.CARRIER, freeLoc);
        } else if (freeLoc != null && rc.canBuildRobot(RobotType.LAUNCHER, freeLoc)) {
            rc.buildRobot(RobotType.LAUNCHER, freeLoc);
        }

        endTurn();

    }

}
