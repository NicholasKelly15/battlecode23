package gopher1.robots;

import battlecode.common.*;
import gopher1.util.Pathfinding;

import java.util.ArrayList;
import java.util.Stack;

public abstract class Robot {

    protected RobotController rc;
    protected Pathfinding pathing;

    protected MapLocation location;
    protected Team team;
    protected RobotType type;

    protected ArrayList<RobotInfo> knownFriendlyHQs;

    protected RobotInfo[] fullRangeSensedRobots;
    protected RobotInfo[] sensedFriendlyRobots;
    protected RobotInfo[] sensedEnemyRobots;

    protected Stack<RobotInfo> sensedEnemyAmplifiers;
    protected Stack<RobotInfo> sensedEnemyBoosters;
    protected Stack<RobotInfo> sensedEnemyCarriers;
    protected Stack<RobotInfo> sensedEnemyDestabilizers;
    protected Stack<RobotInfo> sensedEnemyHeadquarters;
    protected Stack<RobotInfo> sensedEnemyLaunchers;

//    protected Stack<RobotInfo> sensedFriendlyAmplifiers;
//    protected Stack<RobotInfo> sensedFriendlyBoosters;
//    protected Stack<RobotInfo> sensedFriendlyCarriers;
//    protected Stack<RobotInfo> sensedFriendlyDestabilizers;
//    protected Stack<RobotInfo> sensedFriendlyHeadquarters;
//    protected Stack<RobotInfo> sensedFriendlyLaunchers;


    public Robot(RobotController rc) {
        this.rc = rc;
        pathing = new Pathfinding(rc);

        location = rc.getLocation();
        team = rc.getTeam();
        type = rc.getType();

        knownFriendlyHQs = new ArrayList<RobotInfo>();
    }

    public void run() throws GameActionException {
        // Update sensing variables
        fullRangeSensedRobots = rc.senseNearbyRobots();
        sensedFriendlyRobots = rc.senseNearbyRobots(type.visionRadiusSquared, team);
        sensedEnemyRobots = rc.senseNearbyRobots(type.visionRadiusSquared, team.opponent());

        sensedEnemyAmplifiers = new Stack<RobotInfo>();
        sensedEnemyBoosters = new Stack<RobotInfo>();
        sensedEnemyCarriers = new Stack<RobotInfo>();
        sensedEnemyDestabilizers = new Stack<RobotInfo>();
        sensedEnemyHeadquarters = new Stack<RobotInfo>();
        sensedEnemyLaunchers = new Stack<RobotInfo>();

//        sensedFriendlyAmplifiers = new Stack<RobotInfo>();
//        sensedFriendlyBoosters = new Stack<RobotInfo>();
//        sensedFriendlyCarriers = new Stack<RobotInfo>();
//        sensedFriendlyDestabilizers = new Stack<RobotInfo>();
//        sensedFriendlyHeadquarters = new Stack<RobotInfo>();
//        sensedFriendlyLaunchers = new Stack<RobotInfo>();

        for (RobotInfo robot : sensedEnemyRobots) {
            switch (robot.getType()) {
                case AMPLIFIER:
                    sensedEnemyAmplifiers.push(robot);
                    break;
                case BOOSTER:
                    sensedEnemyBoosters.push(robot);
                    break;
                case CARRIER:
                    sensedEnemyCarriers.push(robot);
                    break;
                case DESTABILIZER:
                    sensedEnemyDestabilizers.push(robot);
                    break;
                case HEADQUARTERS:
                    sensedEnemyHeadquarters.push(robot);
                    break;
                case LAUNCHER:
                    sensedEnemyLaunchers.push(robot);
                    break;
            }
        }

//        for (RobotInfo robot : sensedFriendlyRobots) {
//            switch (robot.getType()) {
//                case AMPLIFIER:
//                    sensedFriendlyAmplifiers.push(robot);
//                    break;
//                case BOOSTER:
//                    sensedFriendlyBoosters.push(robot);
//                    break;
//                case CARRIER:
//                    sensedFriendlyCarriers.push(robot);
//                    break;
//                case DESTABILIZER:
//                    sensedFriendlyDestabilizers.push(robot);
//                    break;
//                case HEADQUARTERS:
//                    sensedFriendlyHeadquarters.push(robot);
//                    break;
//                case LAUNCHER:
//                    sensedFriendlyLaunchers.push(robot);
//                    break;
//            }
//        }

        for (RobotInfo robot : sensedFriendlyRobots) {
            if (robot.getType() == RobotType.HEADQUARTERS && !knownFriendlyHQs.contains(robot)) {
                knownFriendlyHQs.add(robot);
            }
        }

    }

    public RobotInfo getNearestFriendlyHQ() {
        RobotInfo nearestFriendlyHQ = null;
        int nearestDistSquared = -1;
        for (RobotInfo hq : knownFriendlyHQs) {
            if (nearestFriendlyHQ == null || hq.getLocation().distanceSquaredTo(location) < nearestDistSquared) {
                nearestFriendlyHQ = hq;
                nearestDistSquared = hq.getLocation().distanceSquaredTo(location);
            }
        }
        return nearestFriendlyHQ;
    }

    public MapLocation getAverageEnemyLocation() {
        int averageX = 0;
        int averageY = 0;
        for (RobotInfo enemy : sensedEnemyRobots) {
            averageX += enemy.getLocation().x;
            averageY += enemy.getLocation().y;
        }
        if (sensedEnemyRobots.length > 0) {
            return new MapLocation((int)(averageX / sensedEnemyRobots.length), (int)(averageY / sensedEnemyRobots.length));
        } else {
            return null;
        }
    }

}
