package gopher0.robots;

import battlecode.common.*;
import gopher0.util.Pathfinding;

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
    protected Stack<RobotInfo> sensedEnemyRobots;
    protected Stack<RobotInfo> sensedEnemyAmplifiers;
    protected Stack<RobotInfo> sensedEnemyBoosters;
    protected Stack<RobotInfo> sensedEnemyCarriers;
    protected Stack<RobotInfo> sensedEnemyDestabilizers;
    protected Stack<RobotInfo> sensedEnemyHeadquarters;
    protected Stack<RobotInfo> sensedEnemyLaunchers;


    public Robot(RobotController rc) {
        this.rc = rc;
        pathing = new Pathfinding(rc);

        location = rc.getLocation();
        team = rc.getTeam();
        type = rc.getType();

        knownFriendlyHQs = new ArrayList<RobotInfo>();

        sensedEnemyRobots = new Stack<RobotInfo>();
        sensedEnemyAmplifiers = new Stack<RobotInfo>();
        sensedEnemyBoosters = new Stack<RobotInfo>();
        sensedEnemyCarriers = new Stack<RobotInfo>();
        sensedEnemyDestabilizers = new Stack<RobotInfo>();
        sensedEnemyHeadquarters = new Stack<RobotInfo>();
        sensedEnemyLaunchers = new Stack<RobotInfo>();
    }

    public void run() throws GameActionException {
        // Update sensing variables
        fullRangeSensedRobots = rc.senseNearbyRobots();
        sensedFriendlyRobots = rc.senseNearbyRobots(type.visionRadiusSquared, team);

        for (RobotInfo robot : fullRangeSensedRobots) {
            if (robot.getType() == RobotType.HEADQUARTERS && robot.getTeam() == team && !knownFriendlyHQs.contains(robot)) {
                knownFriendlyHQs.add(robot);
            }

            if (robot.getTeam() == team) {

            } else {
                sensedEnemyRobots.push(robot);
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

}
