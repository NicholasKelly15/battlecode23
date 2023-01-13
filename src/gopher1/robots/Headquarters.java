package gopher1.robots;

import battlecode.common.*;

public class Headquarters extends Robot {

    public Headquarters(RobotController rc) {
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

    public void run() throws GameActionException {

        MapLocation freeLoc = getOpenSpawnLocation();

        if (freeLoc != null && rc.canBuildRobot(RobotType.CARRIER, freeLoc)) {
            rc.buildRobot(RobotType.CARRIER, freeLoc);
        } else if (freeLoc != null && rc.canBuildRobot(RobotType.LAUNCHER, freeLoc)) {
            rc.buildRobot(RobotType.LAUNCHER, freeLoc);
        }

    }

}
