package gopher4.robots;
import java.lang.Math;
import java.util.ArrayList;

import battlecode.common.*;


public class Headquarters extends Robot {
    public Direction[] DirPref=new Direction[8];

    int Launchers;
    int symmetryType;
    boolean inCloud;
    ArrayList<MapLocation> spawnradius;
    public Headquarters(RobotController rc) throws Exception {
        super(rc);
        spawnradius=new ArrayList<>();
        inCloud=rc.senseNearbyMapInfos(0)[0].hasCloud();
        visionRange();
//        if(!inCloud) {
//            spawnradius.add(new int[]{0, 3});
//            spawnradius.add(new int[]{-2, 2});
//            spawnradius.add(new int[]{-1, 2});
//            spawnradius.add(new int[]{0, 2});
//            spawnradius.add(new int[]{1, 2});
//            spawnradius.add(new int[]{2, 2});
//            spawnradius.add(new int[]{-2, 1});
//            spawnradius.add(new int[]{-1, 1});
//            spawnradius.add(new int[]{0, 1});
//            spawnradius.add(new int[]{1, 1});
//            spawnradius.add(new int[]{2, 1});
//            spawnradius.add(new int[]{-3, 0});
//            spawnradius.add(new int[]{-2, 0});
//            spawnradius.add(new int[]{-1, 0});
//            spawnradius.add(new int[]{1, 0});
//            spawnradius.add(new int[]{2, 0});
//            spawnradius.add(new int[]{-3, 0});
//            spawnradius.add(new int[]{0, -3});
//            spawnradius.add(new int[]{-2, -2});
//            spawnradius.add(new int[]{-1, -2});
//            spawnradius.add(new int[]{0, -2});
//            spawnradius.add(new int[]{1, -2});
//            spawnradius.add(new int[]{2, -2});
//            spawnradius.add(new int[]{-2, -1});
//            spawnradius.add(new int[]{-1, -1});
//            spawnradius.add(new int[]{0, -1});
//            spawnradius.add(new int[]{1, -1});
//            spawnradius.add(new int[]{2, -1});
//            visionRange();
//        }else{
//            spawnradius.add(new int[]{0, 2});
//            spawnradius.add(new int[]{-1, 1});
//            spawnradius.add(new int[]{0, 1});
//            spawnradius.add(new int[]{1, 1});
//            spawnradius.add(new int[]{-2, 0});
//            spawnradius.add(new int[]{-1, 0});
//            spawnradius.add(new int[]{1, 0});
//            spawnradius.add(new int[]{2, 0});
//            spawnradius.add(new int[]{0, -2});
//            spawnradius.add(new int[]{-1, -1});
//            spawnradius.add(new int[]{0, -1});
//            spawnradius.add(new int[]{1, -1});
//        }
        Launchers=3;
        symmetryType=0;
        comms.reportSelfHQLocation();
    }

    private MapLocation getOpenSpawnLocation() throws GameActionException {
        MapLocation spawnLoc = rc.getLocation();
        MapLocation minLoc=null;
        int minDist=100;
        int heuro=0;
        MapLocation thePoint= new MapLocation (rc.getMapWidth()/2, rc.getMapHeight()/2);
        if(getNearestKnownWell(null)!=null) {
            thePoint= getNearestKnownWell(null);
        }
        for (MapLocation adj : spawnradius) {
            heuro=Math.max(Math.abs(thePoint.x-adj.x),Math.abs(thePoint.y-adj.y));
            if ((minDist>heuro) && !rc.isLocationOccupied(adj) && rc.sensePassability(adj)) {
                minDist=heuro;
                minLoc=adj;
            }
        }
        return minLoc;
    }

    public void run() throws GameActionException, IllegalAccessException {

        super.run();
        symmetryType=rc.readSharedArray(63)/16384;

        int spawnTries = 0;
        while (rc.isActionReady() && spawnTries++ < 15) {
            MapLocation freeLoc = getOpenSpawnLocation();
            if (freeLoc != null && rc.canBuildRobot(RobotType.CARRIER, freeLoc)) {
                rc.buildRobot(RobotType.CARRIER, freeLoc);
            } else if (freeLoc != null && rc.canBuildRobot(RobotType.LAUNCHER, freeLoc)) {
                Launchers++;
                rc.writeSharedArray(63,Launchers+symmetryType*16384);
                rc.setIndicatorString(Launchers+"");
                rc.buildRobot(RobotType.LAUNCHER, freeLoc);
            }
        }

        endTurn();

    }

    public void visionRange() throws GameActionException {
        MapInfo[] cloud=rc.senseNearbyMapInfos(9);
        for(MapInfo loc:cloud){
            if(!loc.getMapLocation().equals(rc.getLocation())) {
                spawnradius.add(loc.getMapLocation());
            }
        }
    }
}
