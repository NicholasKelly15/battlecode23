package gopher4.robots;

import battlecode.common.*;


public class Headquarters extends Robot {
    public Direction[] DirPref=new Direction[8];
    public Headquarters(RobotController rc) throws Exception {
        super(rc);

        comms.reportSelfHQLocation();

        int setup=0;
        if(rc.getMapHeight()/2>rc.getLocation().y){
            setup++;
        }
        if(rc.getMapWidth()/2>rc.getLocation().x){
            setup+=2;
        }
        switch (setup){
            case 0:
                DirPref[0]= Direction.SOUTHWEST;
                DirPref[1]= Direction.WEST;
                DirPref[2]= Direction.SOUTH;
                DirPref[3]= Direction.NORTHWEST;
                DirPref[4]= Direction.SOUTHEAST;
                DirPref[5]= Direction.NORTH;
                DirPref[6]= Direction.EAST;
                DirPref[7]= Direction.NORTHEAST;
                break;
            case 1:
                DirPref[0]= Direction.NORTHWEST;
                DirPref[1]= Direction.WEST;
                DirPref[2]= Direction.NORTH;
                DirPref[3]= Direction.SOUTHWEST;
                DirPref[4]= Direction.NORTHEAST;
                DirPref[5]= Direction.SOUTH;
                DirPref[6]= Direction.EAST;
                DirPref[7]= Direction.SOUTHEAST;
                break;
            case 2:
                DirPref[0]= Direction.SOUTHEAST;
                DirPref[1]= Direction.EAST;
                DirPref[2]= Direction.SOUTH;
                DirPref[3]= Direction.NORTHEAST;
                DirPref[4]= Direction.SOUTHWEST;
                DirPref[5]= Direction.NORTH;
                DirPref[6]= Direction.WEST;
                DirPref[7]= Direction.NORTHWEST;
                break;
            default:
                DirPref[0]= Direction.NORTHEAST;
                DirPref[1]= Direction.EAST;
                DirPref[2]= Direction.NORTH;
                DirPref[3]= Direction.SOUTHEAST;
                DirPref[4]= Direction.NORTHWEST;
                DirPref[5]= Direction.SOUTH;
                DirPref[6]= Direction.WEST;
                DirPref[7]= Direction.SOUTHWEST;
        }
    }

    private MapLocation getOpenSpawnLocation() throws GameActionException {
        MapLocation spawnLoc = rc.getLocation();
        for (Direction dir : DirPref) {
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
