package gopher4.robots;

import battlecode.common.*;
import gopher4.util.Comms;
import gopher4.util.Pathfinding;
import gopher4.util.SharedArrayStack;

import java.util.Stack;

public abstract class Robot {

    protected RobotController rc;
    protected Pathfinding pathing;
    protected Comms comms;

    protected MapLocation location;
    protected Team team;
    protected RobotType type;

    protected WellInfo[] sensedWells;

    protected Stack<MapLocation> knownFriendlyHQs;

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
    protected Stack<RobotInfo> sensedFriendlyLaunchers;

    protected int width;
    protected int height;
    protected byte[][] knownWellsMap; // 0 = no well, 1 = ada well, 2 = mana well, 3 = elixir well
    // 4 = ada well reported, 5 = mana well reported, 6 = elixir well reported
    protected MapLocation[] knownWellsStack;
    protected int knownWellsStackPointer;


    protected final boolean DEBUG_MODE = true;
    protected final boolean RESIGN_MODE = true;
    protected final int DISTANCE_SQUARED_TO_NOT_REPORT_NEW_WELL = 25;


    public Robot(RobotController rc) throws GameActionException {
        this.rc = rc;
        pathing = new Pathfinding(rc);
        comms = new Comms(rc);

        team = rc.getTeam();
        type = rc.getType();

        knownFriendlyHQs = new Stack<MapLocation>();

        width = rc.getMapWidth();
        height = rc.getMapHeight();
        knownWellsMap = new byte[width][height];
        knownWellsStack = new MapLocation[150];
        knownWellsStackPointer = 0;
    }

    public void run() throws GameActionException, IllegalAccessException {
        if (RESIGN_MODE && rc.getRoundNum() > 100) {
            rc.resign();
        }

        location = rc.getLocation();

        // Update sensing variables
        sensedWells = rc.senseNearbyWells();

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
                case LAUNCHER:
                    sensedFriendlyLaunchers.push(robot);
                    break;
            }
        }

        // dealing with reporting known wells and reading about wells from the shared array.
        for (int intWellLocation : comms.getWellLocationsStack()) {
            MapLocation location = comms.getMapLocationFromBits(intWellLocation);
            if (knownWellsMap[location.x][location.y] < 4) { // if the bot hasn't gotten this information from the shared array before
                ResourceType type = comms.getResourceTypeFromBits(intWellLocation);
                byte wellType = -1;
                switch (type) {
                    case ADAMANTIUM:            wellType = 4;       break;
                    case MANA:                  wellType = 5;       break;
                    case ELIXIR:                wellType = 6;       break;
                    case NO_RESOURCE:   throw new GameActionException(GameActionExceptionType.INTERNAL_ERROR, "Sensed well should have a type.");
                }
                knownWellsMap[location.x][location.y] = wellType;
                knownWellsStack[knownWellsStackPointer++] = location;
            }
        }

        for (WellInfo well : sensedWells) {
            if (knownWellsMap[well.getMapLocation().x][well.getMapLocation().y] == 0) { // if this well was not already known.
                byte wellType = -1;
                switch (well.getResourceType()) {
                    case ADAMANTIUM:            wellType = 1;       break;
                    case MANA:                  wellType = 2;       break;
                    case ELIXIR:                wellType = 3;       break;
                    case NO_RESOURCE:   throw new GameActionException(GameActionExceptionType.INTERNAL_ERROR, "Sensed well should have a type.");
                }
                knownWellsMap[well.getMapLocation().x][well.getMapLocation().y] = wellType;
                knownWellsStack[knownWellsStackPointer++] = well.getMapLocation();
            }
        }

        if (rc.canWriteSharedArray(0, 0) && comms.canReportWellLocation()) {
            for (int wellIndex = 0 ; wellIndex < knownWellsStackPointer ; wellIndex++) {
                MapLocation location = knownWellsStack[wellIndex];
                byte mapValue = knownWellsMap[location.x][location.y];
                boolean needToReport = mapValue < 4 && mapValue > 0;
                if (needToReport) { // to determine if the well should be added to the shared array, check if it is too close to an already known well.
                    boolean isTooCloseToKnownWell = false;
                    for (int intWellLocation : comms.getWellLocationsStack()) {
                        MapLocation sharedArrayLocation = comms.getMapLocationFromBits(intWellLocation);
                        if (sharedArrayLocation.isWithinDistanceSquared(location, DISTANCE_SQUARED_TO_NOT_REPORT_NEW_WELL)) {
                            isTooCloseToKnownWell = true;
                            break;
                        }
                    }
                    if (!isTooCloseToKnownWell) {
                        ResourceType type = ResourceType.NO_RESOURCE;
                        switch (mapValue) {
                            case 1: type = ResourceType.ADAMANTIUM; break;
                            case 2: type = ResourceType.MANA; break;
                            case 3: type = ResourceType.ELIXIR; break;
                        }
                        rc.setIndicatorString("Reported Well at: " + (location.x) + ", " + location.y);
                        comms.reportWellLocation(location, type);
                        knownWellsMap[location.x][location.y] = (byte) (mapValue + 3);
                    }
                }
            }
        }

        if (DEBUG_MODE) {
//            for (int i = 0 ; i < knownWellsStackPointer ; i++) {
//                MapLocation location = knownWellsStack[i];
//                rc.setIndicatorLine(location, rc.getLocation(), 255, 0, 0);
//            }
        }

    }

    public void endTurn() throws GameActionException {
        comms.onTurnEnd();
    }

    protected MapLocation[] getSharedWellLocations() throws GameActionException {
        SharedArrayStack sharedWells = comms.getWellLocationsStack();
        MapLocation[] wellLocations = new MapLocation[sharedWells.getSize()];
        int arrayIndex = 0;
        for (int intLocation : sharedWells) {
            MapLocation location = comms.getMapLocationFromBits(intLocation);
            wellLocations[arrayIndex++] = location;
        }
        return wellLocations;
    }

    protected MapLocation getNearestKnownWell(Stack<MapLocation> wellsToExclude) throws GameActionException {
        MapLocation nearestWell = null;
        MapLocation thisLocation = location;
        int closestDistance = 10000;
        if (wellsToExclude != null) {
            for (int i = 0 ; i < knownWellsStackPointer ; i++) {
                MapLocation location = knownWellsStack[i];
                if (location.distanceSquaredTo(thisLocation) < closestDistance) {
                    boolean isExcluded = false;
                    for (MapLocation exclude : wellsToExclude) {
                        if (exclude.x == location.x && exclude.y == location.y) {
                            isExcluded = true;
                        }
                    }
                    if (!isExcluded) {
                        nearestWell = location;
                        closestDistance = location.distanceSquaredTo(thisLocation);
                    }
                }
            }
        } else {
            for (int i = 0 ; i < knownWellsStackPointer ; i++) {
                MapLocation location = knownWellsStack[i];
                if (location.distanceSquaredTo(thisLocation) < closestDistance) {
                    nearestWell = location;
                    closestDistance = location.distanceSquaredTo(thisLocation);
                }
            }
        }
        return nearestWell;
    }


}
