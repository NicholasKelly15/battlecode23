package gopher4.robots;

import battlecode.common.*;
import gopher4.util.Pathfinding;

import java.util.Arrays;
import java.util.Stack;

public class Carrier extends Robot {

    private Pathfinding pathing;

    //    private int mode = -1;  // 0 = collectResources, 1 = runAway, 2 = returnToBase, 3 = explore, 4 = deployAnchor
    private int mode;
    private final int TURNS_TO_RUN = 5;

    private String[] modeStrings = new String[]{"Collection", "Running", "Returning", "Exploring", "Setting Anchor"};
    private MapLocation currentTargetWell = null;
    private MapLocation permanentAssignedWell = null;
    private Stack<MapLocation> wellsSearchedLookingForTarget;
    private int turnsSinceSeenEnemy;
    private MapLocation lastSeenEnemyPos = null;
    private Direction currentExploreDirection;

    private int[] attackPreference; // low preference attacked first

    public Carrier(RobotController rc) throws GameActionException {
        super(rc);

        pathing = new Pathfinding(rc);

        wellsSearchedLookingForTarget = new Stack<MapLocation>();

        attackPreference = new int[]{
                5, // HQ Preference
                4, // Carrier
                2, // Launcher
                0, // Destabilizer
                1, // Booster
                3  // Amplifier
        };
        mode = 3;
        turnsSinceSeenEnemy = 0;
        currentExploreDirection = rc.getLocation().directionTo(new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2));
    }

    public void run() throws GameActionException, IllegalAccessException {
        super.run();

        switch (mode) {
            case 0:
                mode = collectResources();
                break;
            case 1:
                mode = runAway();
                break;
            case 2:
                mode = returnToBase();
                break;
            case 3:
                mode = explore();
                break;
            case 4:
                mode = deployAnchor();
                break;
        }
        rc.setIndicatorString("Mode: " + modeStrings[mode]);

        endTurn();
    }

    private int testAndRunIfNeeded() throws GameActionException, IllegalAccessException {
        if (sensedEnemyLaunchersStackPointer > 0) {
            lastSeenEnemyPos = sensedEnemyLaunchers[0].getLocation();
            return enterRunAway();
        } else if (sensedEnemyDestabilizersStackPointer > 0) {
            lastSeenEnemyPos = sensedEnemyDestabilizers[0].getLocation();
            return enterRunAway();
        }

        return -1;
    }

    private int getTotalResourceCount() {
        return rc.getResourceAmount(ResourceType.ADAMANTIUM)
                + rc.getResourceAmount(ResourceType.MANA)
                + rc.getResourceAmount(ResourceType.ELIXIR);
    }

    // travels to and collects from well.
    private int collectResources() throws GameActionException, IllegalAccessException {
        // consider thinking about adding a case where the carrier passes by an unknown well on
        // its way to the assigned well.

        int returnNum = testAndRunIfNeeded();
        if (returnNum != -1) {
            return returnNum;
        }

        MapLocation nearestKnownWell = getNearestKnownWell(wellsSearchedLookingForTarget);
        if (nearestKnownWell != null) {
            currentTargetWell = nearestKnownWell;
        } else {
            return explore();
        }

        // 10 is the distance at which the carrier can see the well and every tile around it.
        if (canSeeWellAndSurroundings(currentTargetWell)) {
            boolean isSpotOnTargetWellOpen = canWellUseMoreCarriers(currentTargetWell);
            if (isSpotOnTargetWellOpen) {
                int moveTries = 0;
                while (rc.isMovementReady() && currentTargetWell.distanceSquaredTo(rc.getLocation()) > 2 && moveTries++ < 5) {
                    pathing.moveTo(currentTargetWell);
                }
                if (rc.isMovementReady() && currentTargetWell.distanceSquaredTo(rc.getLocation()) <= 2 && rc.canMove(rc.getLocation().directionTo(currentTargetWell))) {
                    rc.move(rc.getLocation().directionTo(currentTargetWell));
                }

                while (rc.isActionReady() && getTotalResourceCount() < GameConstants.CARRIER_CAPACITY && rc.canCollectResource(currentTargetWell, -1)) {
                    rc.collectResource(currentTargetWell, -1);
                }

                int resourcesHeld = getTotalResourceCount();
                if (rc.isMovementReady() && resourcesHeld == GameConstants.CARRIER_CAPACITY) {
                    return returnToBase();
                }
            } else {
                wellsSearchedLookingForTarget.push(currentTargetWell);
                return collectResources();
            }
        } else {
            int moveTries = 0;
            while (rc.isMovementReady() && moveTries++ < 5) {
                pathing.moveTo(currentTargetWell);
            }
        }

        return 0;

    }

    private int enterRunAway() throws GameActionException, IllegalAccessException {
        turnsSinceSeenEnemy = 0;
        return runAway();
    }

    // runs away from enemies in sight or previously seen.
    private int runAway() throws GameActionException, IllegalAccessException {
        turnsSinceSeenEnemy++;
        if (turnsSinceSeenEnemy > TURNS_TO_RUN) {
            return collectResources();
        }

        if (getTotalResourceCount() > 0 && rc.isActionReady()) {
            throwResourcesToRun();
        }
        int moveTries = 0;
        while (rc.isMovementReady() && moveTries++ < 3) {
            pathing.moveTowards(lastSeenEnemyPos.directionTo(rc.getLocation()));
        }

        return 1;
    }

    // go back to the hq that spawned this carrier
    private int returnToBase() throws GameActionException, IllegalAccessException {
        int returnNum = testAndRunIfNeeded();
        if (returnNum != -1) {
            return returnNum;
        }

        int moveTries = 0;
        while (rc.isMovementReady() && moveTries++ < 3) {
            pathing.moveTo(homeHQ);
        }
        if (rc.getLocation().distanceSquaredTo(homeHQ) <= 2) {
            if (rc.canTransferResource(homeHQ, ResourceType.ELIXIR, rc.getResourceAmount(ResourceType.ELIXIR))) {
                rc.transferResource(homeHQ, ResourceType.ELIXIR, rc.getResourceAmount(ResourceType.ELIXIR));
            }
            if (rc.canTransferResource(homeHQ, ResourceType.MANA, rc.getResourceAmount(ResourceType.MANA))) {
                rc.transferResource(homeHQ, ResourceType.MANA, rc.getResourceAmount(ResourceType.MANA));
            }
            if (rc.canTransferResource(homeHQ, ResourceType.ADAMANTIUM, rc.getResourceAmount(ResourceType.ADAMANTIUM))) {
                rc.transferResource(homeHQ, ResourceType.ADAMANTIUM, rc.getResourceAmount(ResourceType.ADAMANTIUM));
            }

            RobotInfo homeHQInfo = rc.senseRobotAtLocation(homeHQ);
            if (homeHQInfo.getTotalAnchors() > 0) {
                if (rc.canTakeAnchor(homeHQ, Anchor.STANDARD)) {
                    rc.takeAnchor(homeHQ, Anchor.STANDARD);
                    return deployAnchor();
                } else if (rc.canTakeAnchor(homeHQ, Anchor.ACCELERATING)) {
                    rc.takeAnchor(homeHQ, Anchor.ACCELERATING);
                    return deployAnchor();
                }
            } else {
                return collectResources();
            }

        }

        return 2;
    }

    // explore the map for undiscovered wells
    private int explore() throws GameActionException, IllegalAccessException {
        int returnNum = testAndRunIfNeeded();
        if (returnNum != -1) {
            return returnNum;
        }

        int movesTried = 0;
        while (rc.isMovementReady() && movesTried++ < 4) {
            pathing.moveTowards(currentExploreDirection);
        }

        return 3;
    }

    private int deployAnchor() throws GameActionException, IllegalAccessException {
        if (rc.getAnchor() == null) {
            return collectResources();
        }

        int[] nearbyIslands = rc.senseNearbyIslands();
        MapLocation closestPoint = null;
        if (nearbyIslands != null) {

            MapLocation currentLocation = rc.getLocation();
            int closest = 10000;
            for (int i = nearbyIslands.length; i-- > 0 ; ) {
                if (rc.senseTeamOccupyingIsland(nearbyIslands[i]) == Team.NEUTRAL) {
                    MapLocation[] sensedLocations = rc.senseNearbyIslandLocations(nearbyIslands[i]);
                    for (int j = sensedLocations.length ; j-- > 0 ; ) {
                        if (sensedLocations[j].distanceSquaredTo(currentLocation) < closest) {
                            closest = sensedLocations[j].distanceSquaredTo(currentLocation);
                            closestPoint = sensedLocations[j];
                        }
                    }
                }
            }
        }

        System.out.println(closestPoint);

        if (closestPoint != null) {
            int moveTries = 0;
            while (rc.isMovementReady() && moveTries++ < 3) {
                pathing.moveTo(closestPoint);
            }
            if (rc.canPlaceAnchor()) {
                rc.placeAnchor();
                return collectResources();
            }
        } else {
            explore();
        }

        return 4;
    }

    private boolean canSeeWellAndSurroundings(MapLocation wellLocation) {
        for (Direction direction : pathing.DIRECTIONS) {
            if (!rc.canSenseLocation(wellLocation.add(direction))) {
                return false;
            }
        }
        return true;
    }

    // must be able to see the target well and every tile within range squared of 2 to call this.
    private boolean isSpotOnWellOpen(MapLocation wellLocation) throws GameActionException {
        for (Direction direction : pathing.DIRECTIONS) {
            if (rc.senseRobotAtLocation(wellLocation.add(direction)) == null && rc.sensePassability(wellLocation.add(direction))) {
                return true;
            }
        }
        return rc.senseRobotAtLocation(wellLocation) == null;
    }

    private boolean canWellUseMoreCarriers(MapLocation wellLocation) throws GameActionException {
        int friendlyCarriersCloserToWell = 0;
        int availableGatherLocationsOnWell = 0;
        MapLocation location = rc.getLocation();

        for (Direction direction : pathing.DIRECTIONS) {
            if (rc.canSenseLocation(location.add(direction))) {
                MapInfo info = rc.senseMapInfo(location.add(direction));
                if (info.isPassable() && info.getCurrentDirection() == Direction.CENTER) {
                    availableGatherLocationsOnWell++;
                }
            }
        }

        int thisRobotsDistanceToWell = location.distanceSquaredTo(wellLocation);
        RobotInfo[] friendlies = rc.senseNearbyRobots(20, team);
        for (RobotInfo friendly : friendlies) {
            if (friendly.getType() == RobotType.CARRIER
                    && friendly.getLocation().distanceSquaredTo(wellLocation) < thisRobotsDistanceToWell) {
                friendlyCarriersCloserToWell++;
            }
        }

        return friendlyCarriersCloserToWell < availableGatherLocationsOnWell * 1.5;
    }

    private void throwResourcesToRun() throws GameActionException {
        if (getTotalResourceCount() == 0) {
            return;
        }

        RobotInfo[] attackableEnemies = rc.senseNearbyRobots(9, team.opponent());
        int bestPreference = 10;
        RobotInfo bestEnemy = null;
        for (RobotInfo enemy : attackableEnemies) {
            if (attackPreference[enemy.getType().ordinal()] < bestPreference) {
                bestEnemy = enemy;
                bestPreference = enemy.getType().ordinal();
            }
        }

        if (bestEnemy != null && rc.canAttack(bestEnemy.getLocation())) {
            rc.attack(bestEnemy.getLocation());
        } else {
            rc.attack(rc.getLocation().add(Direction.CENTER));
        }
    }



}
