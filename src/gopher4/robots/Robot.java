package gopher4.robots;

import battlecode.common.*;
import gopher4.util.Comms;
import gopher4.util.Pathfinding;

import java.util.ArrayList;
import java.util.Stack;

public abstract class Robot {

    protected RobotController rc;
    protected Pathfinding pathing;
    protected Comms comms;

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

    protected Stack<RobotInfo> sensedFriendlyAmplifiers;
    protected Stack<RobotInfo> sensedFriendlyBoosters;
    protected Stack<RobotInfo> sensedFriendlyCarriers;
    protected Stack<RobotInfo> sensedFriendlyDestabilizers;
    protected Stack<RobotInfo> sensedFriendlyHeadquarters;
    protected Stack<RobotInfo> sensedFriendlyLaunchers;


    public Robot(RobotController rc) throws GameActionException {
        this.rc = rc;
        pathing = new Pathfinding(rc);
        comms = new Comms(rc);

        location = rc.getLocation();
        team = rc.getTeam();
        type = rc.getType();

        knownFriendlyHQs = new ArrayList<>();
    }

    public void run() throws GameActionException, IllegalAccessException {
        // Update sensing variables
        fullRangeSensedRobots = rc.senseNearbyRobots();
        sensedFriendlyRobots = rc.senseNearbyRobots(type.visionRadiusSquared, team);
        sensedEnemyRobots = rc.senseNearbyRobots(type.visionRadiusSquared, team.opponent());

        sensedEnemyAmplifiers = new Stack<>();
        sensedEnemyBoosters = new Stack<>();
        sensedEnemyCarriers = new Stack<>();
        sensedEnemyDestabilizers = new Stack<>();
        sensedEnemyHeadquarters = new Stack<>();
        sensedEnemyLaunchers = new Stack<>();

        sensedFriendlyAmplifiers = new Stack<>();
        sensedFriendlyBoosters = new Stack<>();
        sensedFriendlyCarriers = new Stack<>();
        sensedFriendlyDestabilizers = new Stack<>();
        sensedFriendlyHeadquarters = new Stack<>();
        sensedFriendlyLaunchers = new Stack<>();

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

        for (RobotInfo robot : sensedFriendlyRobots) {
            switch (robot.getType()) {
                case AMPLIFIER:
                    sensedFriendlyAmplifiers.push(robot);
                    break;
                case BOOSTER:
                    sensedFriendlyBoosters.push(robot);
                    break;
                case CARRIER:
                    sensedFriendlyCarriers.push(robot);
                    break;
                case DESTABILIZER:
                    sensedFriendlyDestabilizers.push(robot);
                    break;
                case HEADQUARTERS:
                    sensedFriendlyHeadquarters.push(robot);
                    break;
                case LAUNCHER:
                    sensedFriendlyLaunchers.push(robot);
                    break;
            }
        }

        for (RobotInfo robot : sensedFriendlyRobots) {
            if (robot.getType() == RobotType.HEADQUARTERS && !knownFriendlyHQs.contains(robot)) {
                knownFriendlyHQs.add(robot);
            }
        }

    }

    public void endTurn() throws GameActionException {
        comms.onTurnEnd();
    }


}
