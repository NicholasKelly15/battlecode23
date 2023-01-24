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
    protected int tilesInVision;

    protected WellInfo[] sensedWells;

    protected Stack<MapLocation> knownFriendlyHQs;
    protected MapLocation homeHQ;

    protected RobotInfo[] allSensedRobots;
    protected RobotInfo[] allSensedEnemyRobots;

    protected RobotInfo[] sensedEnemyLaunchers;
    protected int sensedEnemyLaunchersStackPointer;
    protected RobotInfo[] sensedEnemyCarriers;
    protected int sensedEnemyCarriersStackPointer;
    protected RobotInfo[] sensedEnemyHeadquarters;
    protected int sensedEnemyHeadquartersStackPointer;
    protected RobotInfo[] sensedEnemyDestabilizers;
    protected int sensedEnemyDestabilizersStackPointer;
    protected RobotInfo[] sensedEnemyBoosters;
    protected int sensedEnemyBoostersStackPointer;
    protected RobotInfo[] sensedEnemyAmplifiers;
    protected int sensedEnemyAmplifiersStackPointer;

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
        switch (type) {
            case DESTABILIZER:
            case LAUNCHER:
            case BOOSTER:
            case CARRIER:
                tilesInVision = 68;         break;
            case AMPLIFIER:
            case HEADQUARTERS:
                tilesInVision = 108;        break;

        }

        sensedEnemyLaunchers = new RobotInfo[tilesInVision];
        sensedEnemyLaunchersStackPointer = 0;
        sensedEnemyCarriers = new RobotInfo[tilesInVision];
        sensedEnemyCarriersStackPointer = 0;
        sensedEnemyHeadquarters = new RobotInfo[4];
        sensedEnemyHeadquartersStackPointer = 0;
        sensedEnemyDestabilizers = new RobotInfo[tilesInVision];
        sensedEnemyDestabilizersStackPointer = 0;
        sensedEnemyBoosters = new RobotInfo[tilesInVision];
        sensedEnemyBoostersStackPointer = 0;
        sensedEnemyAmplifiers = new RobotInfo[tilesInVision];
        sensedEnemyAmplifiersStackPointer = 0;

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

        if (knownFriendlyHQs.isEmpty() && rc.getRoundNum() > 1) {
            for (int intLocation : comms.getHqLocationsStack()) {
                knownFriendlyHQs.push(comms.getMapLocationFromBits(intLocation));
            }
            int closestHQDistance = 10000;
            MapLocation closestHQ = null;
            for (MapLocation hqLocation : knownFriendlyHQs) {
                if (hqLocation != null && hqLocation.distanceSquaredTo(location) < closestHQDistance) {
                    closestHQDistance = hqLocation.distanceSquaredTo(location);
                    closestHQ = hqLocation;
                }
            }
            homeHQ = closestHQ;
        }

        // Update sensing variables
        sensedWells = rc.senseNearbyWells();

        allSensedRobots = rc.senseNearbyRobots();
        allSensedEnemyRobots = rc.senseNearbyRobots(-1, team.opponent());

        sensedEnemyLaunchersStackPointer = 0;
        sensedEnemyCarriersStackPointer = 0;
        sensedEnemyHeadquartersStackPointer = 0;
        sensedEnemyDestabilizersStackPointer = 0;
        sensedEnemyBoostersStackPointer = 0;
        sensedEnemyAmplifiersStackPointer = 0;

        // put the enemies into buckets
        if (allSensedEnemyRobots != null) {
            for (RobotInfo robot : allSensedEnemyRobots) {
                switch (robot.getType()) {
                    case LAUNCHER:
                        sensedEnemyLaunchers[sensedEnemyLaunchersStackPointer++] = robot;
                        break;
                    case CARRIER:
                        sensedEnemyCarriers[sensedEnemyCarriersStackPointer++] = robot;
                        break;
                    case HEADQUARTERS:
                        sensedEnemyHeadquarters[sensedEnemyHeadquartersStackPointer++] = robot;
                        break;
                    case DESTABILIZER:
                        sensedEnemyDestabilizers[sensedEnemyDestabilizersStackPointer++] = robot;
                        break;
                    case BOOSTER:
                        sensedEnemyBoosters[sensedEnemyBoostersStackPointer++] = robot;
                        break;
                    case AMPLIFIER:
                        sensedEnemyAmplifiers[sensedEnemyAmplifiersStackPointer++] = robot;
                        break;
                }
            }
        }

        updateWellInformation();

        rc.setIndicatorString("Home: " + homeHQ);

    }

    private void updateWellInformation() throws GameActionException {
        // dealing with reporting known wells and reading about wells from the shared array.

        // first locally record wells not known from the shared array
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

        // locally record wells that were sensed if they were not already known somehow
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

        // record any wells that this robot knows to the shared array if the shared array does not know about the well
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
                        if (exclude.equals(location)) {
                            isExcluded = true;
                        }
                    }
                    MapLocation assignedHQ = null;
                    int closestHQDistance = 10000;
                    for (MapLocation hq : knownFriendlyHQs) {
                        if (hq.distanceSquaredTo(location) < closestHQDistance) {
                            assignedHQ = hq;
                            closestHQDistance = hq.distanceSquaredTo(location);
                        }
                    }
                    if (!assignedHQ.equals(homeHQ)) {
                        isExcluded = true;
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
                boolean isExcluded = false;
                MapLocation assignedHQ = null;
                int closestHQDistance = 10000;
                for (MapLocation hq : knownFriendlyHQs) {
                    if (hq.distanceSquaredTo(location) < closestHQDistance) {
                        assignedHQ = hq;
                        closestHQDistance = hq.distanceSquaredTo(location);
                    }
                }
                if (!assignedHQ.equals(homeHQ)) {
                    isExcluded = true;
                }
                if (!isExcluded && location.distanceSquaredTo(thisLocation) < closestDistance) {
                    nearestWell = location;
                    closestDistance = location.distanceSquaredTo(thisLocation);
                }
            }
        }
        return nearestWell;
    }


}
