package gopher4.robots;
import java.lang.Math;
import battlecode.common.*;

public class Headquarters extends Robot {
    public Direction[] DirPref=new Direction[8];
    int [][] spawnradius= {
            {0,3},
            {-2,2}, {-1,2}, {0,2}, {1,2}, {2,2},
            {-2,1}, {-1,1}, {0,1}, {1,1}, {2,1},
            {-3,0}, {-2,0}, {-1,0},{1,0}, {2,0}, {3,0},
            {-2,-2}, {-1,-2}, {0,-2}, {1,-2}, {2,-2},
            {-2,-1}, {-1,-1}, {0,-1}, {1,-1}, {2,-1},
            {0,-3}
    };
    public Headquarters(RobotController rc) throws Exception {
        super(rc);

        comms.reportSelfHQLocation();
    }

    private MapLocation getOpenSpawnLocation() throws GameActionException {
        MapLocation spawnLoc = rc.getLocation();
        MapLocation minLoc=null;
        int minDist=100;
        int heuro=0;
        MapLocation thePoint= new MapLocation (rc.getMapHeight()/2, rc.getMapWidth()/2);
        if(getNearestKnownWell(null)!=null) {
            thePoint= getNearestKnownWell(null);
        }
        for (int[] adj : spawnradius) {
            MapLocation newLoc = spawnLoc.translate(adj[0],adj[1]);
            heuro=Math.max(Math.abs(thePoint.x-newLoc.x),Math.abs(thePoint.y-newLoc.y));
            if ((minDist>heuro) && !rc.isLocationOccupied(newLoc) && rc.sensePassability(newLoc)) {
                minDist=heuro;
                minLoc=newLoc;
            }
        }
        return minLoc;
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
