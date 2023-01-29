package gopher10.robots;

import battlecode.common.*;
import gopher10.util.Pathfinding;

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
    private ResourceType resourceGoal;

    private MapLocation[] exploreTargets;
    private int exploreTargetsPointer;
    private int exploreTargetsTraversalPointer;
    private int turnOfLastExploredTarget;

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

        if (rc.getID() % 10 > 6) {
            resourceGoal = ResourceType.ADAMANTIUM;
        } else {
            resourceGoal = ResourceType.MANA;
        }

        exploreTargets = new MapLocation[32];
        exploreTargetsPointer = 0;
        exploreTargetsTraversalPointer = 0;
        turnOfLastExploredTarget = 0;
        setExploreTargets();

    }

    private void setExploreTargets() {
        Direction[] directionsSearched = new Direction[8];
        int directionsSearchedPointer = 0;

        MapLocation currentLocation = rc.getLocation();
        for (Direction initialDirection : pathing.DIRECTIONS) {
            MapLocation newLocation = add100UnitsSquaredInDirection(currentLocation, initialDirection);
            if (rc.onTheMap(newLocation)) {
                exploreTargets[exploreTargetsPointer++] = newLocation;
                directionsSearched[directionsSearchedPointer++] = initialDirection;
            }
        }

        for (int i = exploreTargetsPointer ; i-- > 0 ; ) {
            MapLocation location = exploreTargets[i];
            MapLocation further1 = add100UnitsSquaredInDirection(location, directionsSearched[i]);
            MapLocation further2 = add100UnitsSquaredInDirection(location, directionsSearched[i].rotateLeft());
            if (rc.onTheMap(further1)) {
                exploreTargets[exploreTargetsPointer++] = further1;
            }
            if (rc.onTheMap(further2)) {
                exploreTargets[exploreTargetsPointer++] = further2;
            }
        }
    }

    private MapLocation add100UnitsSquaredInDirection(MapLocation location, Direction direction) {
        switch (direction) {
            case NORTH:
                return new MapLocation(location.x, location.y + 10);
            case EAST:
                return new MapLocation(location.x + 10, location.y);
            case SOUTH:
                return new MapLocation(location.x, location.y - 10);
            case WEST:
                return new MapLocation(location.x - 10, location.y);
            case NORTHEAST:
                return new MapLocation(location.x + 7, location.y + 7);
            case NORTHWEST:
                return new MapLocation(location.x - 7, location.y + 7);
            case SOUTHEAST:
                return new MapLocation(location.x + 7, location.y - 7);
            case SOUTHWEST:
                return new MapLocation(location.x - 7, location.y - 7);
            default:
                throw new AssertionError("There should not be any other directions possible.");
        }
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
        RobotInfo[] friendlies = rc.senseNearbyRobots(RobotType.CARRIER.visionRadiusSquared, team);
        RobotInfo[] enemies = rc.senseNearbyRobots(RobotType.LAUNCHER.actionRadiusSquared, team.opponent());
        int nonCarrierFriendlyPointer = 0;
        int attackingEnemiesPointer = 0;
        for (int i = friendlies.length ; i-- > 0 ; ) {
            switch (friendlies[i].getType()) {
                case CARRIER:
                    break;
                default:
                    friendlies[nonCarrierFriendlyPointer++] = friendlies[i];
                    break;
            }
        }
        for (int i = enemies.length ; i-- > 0 ; ) {
            switch (enemies[i].getType()) {
                case DESTABILIZER:
                case LAUNCHER:
                    enemies[attackingEnemiesPointer++] = enemies[i];
                    break;
                default:
                    break;
            }
        }

        inner: for (int i = attackingEnemiesPointer ; i-- > 0 ; ) {
            MapLocation position = enemies[i].getLocation();
            for (int j = nonCarrierFriendlyPointer ; j-- > 0 ; ) {
                if (position.distanceSquaredTo(friendlies[j].getLocation()) <= 16) {
                    continue inner;
                }
            }
            lastSeenEnemyPos = position;
            return runAway();
        }

        return -1;
    }

    private int getTotalResourceCount() {
        return rc.getResourceAmount(ResourceType.ADAMANTIUM)
                + rc.getResourceAmount(ResourceType.MANA)
                + rc.getResourceAmount(ResourceType.ELIXIR);
    }

    private int enterCollectResources() throws GameActionException, IllegalAccessException {
        wellsSearchedLookingForTarget = new Stack<>();
        return collectResources();
    }

    // travels to and collects from well.
    private int collectResources() throws GameActionException, IllegalAccessException {
        // consider thinking about adding a case where the carrier passes by an unknown well on
        // its way to the assigned well.

        int returnNum = testAndRunIfNeeded();
        if (returnNum != -1) {
            return returnNum;
        }

        if (currentTargetWell == null) {
            MapLocation nearestKnownWell = getNearestKnownWell(wellsSearchedLookingForTarget, resourceGoal);
            if (nearestKnownWell != null) {
                currentTargetWell = nearestKnownWell;
            } else {
                return explore();
            }
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
                    currentTargetWell = null;
                    return returnToBase();
                }
            } else {
                wellsSearchedLookingForTarget.push(currentTargetWell);
                MapLocation nearestKnownWell = getNearestKnownWell(wellsSearchedLookingForTarget, resourceGoal);
                if (nearestKnownWell == null) {
                    explore();
                } else {
                    currentTargetWell = nearestKnownWell;
                }
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
        if (sensedEnemyLaunchersStackPointer > 0) {
            lastSeenEnemyPos = sensedEnemyLaunchers[0].getLocation();
            turnsSinceSeenEnemy = 0;
        } else if (sensedEnemyDestabilizersStackPointer > 0) {
            lastSeenEnemyPos = sensedEnemyDestabilizers[0].getLocation();
            turnsSinceSeenEnemy = 0;
        }

        turnsSinceSeenEnemy++;
        if (turnsSinceSeenEnemy > TURNS_TO_RUN) {
            if (getTotalResourceCount() > 5) {
                return returnToBase();
            } else {
                return collectResources();
            }
        }

//        if (getTotalResourceCount() > 0 && rc.isActionReady()) {
//            throwResourcesToRun();
//        }
        int moveTries = 0;
        while (rc.isMovementReady() && moveTries++ < 3) {
            if (rc.getLocation().distanceSquaredTo(homeHQ) <= 25
                    && sensedEnemyLaunchersStackPointer == 0 && sensedEnemyDestabilizersStackPointer == 0) {
                return returnToBase();
            } else {
                pathing.moveTowards(lastSeenEnemyPos.directionTo(rc.getLocation()));
            }
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
                return enterCollectResources();
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

        MapLocation nearestKnownWell = getNearestKnownWell(wellsSearchedLookingForTarget, resourceGoal);
        if (nearestKnownWell != null) {
            return enterCollectResources();
        }

        if (rc.getRoundNum() - turnOfLastExploredTarget > 20) {
            exploreTargetsTraversalPointer++;
        }

        if (exploreTargetsTraversalPointer < exploreTargetsPointer) {

            MapLocation target = exploreTargets[exploreTargetsTraversalPointer];
            int moveTries = 0;
            while (exploreTargetsTraversalPointer < exploreTargetsPointer && rc.isMovementReady() && moveTries++ < 4) {
                if (target.distanceSquaredTo(rc.getLocation()) <= RobotType.CARRIER.visionRadiusSquared) {
                    target = exploreTargets[exploreTargetsTraversalPointer++];
                    turnOfLastExploredTarget = rc.getRoundNum();
                } else {
                    pathing.moveTo(target);
                }
            }

        } else {
            MapLocation going = rc.getLocation().add(currentExploreDirection).add(currentExploreDirection).add(currentExploreDirection);
            if (going.x < 0 || going.x > rc.getMapWidth()
                    || going.y < 0 || going.y > rc.getMapHeight()) {
                int random = rng.nextInt() % 4;
                if (random == 0) {
                    currentExploreDirection = currentExploreDirection.opposite().rotateLeft();
                } else if (random == 1) {
                    currentExploreDirection = currentExploreDirection.opposite().rotateRight();
                } else if (random == 2) {
                    currentExploreDirection = currentExploreDirection.opposite().rotateRight().rotateRight();
                } else if (random == 3) {
                    currentExploreDirection = currentExploreDirection.opposite().rotateLeft().rotateLeft();
                }
            }

            int movesTried = 0;
            while (rc.isMovementReady() && movesTried++ < 4) {
                pathing.moveTowards(currentExploreDirection);
            }
        }



        return 3;
    }

    private int deployAnchor() throws GameActionException, IllegalAccessException {
        if (rc.getAnchor() == null) {
            return enterCollectResources();
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


        if (closestPoint != null) {
            int moveTries = 0;
            while (rc.isMovementReady() && moveTries++ < 3) {
                pathing.moveTo(closestPoint);
            }
            if (rc.canPlaceAnchor()) {
                rc.placeAnchor();
                return enterCollectResources();
            }
        } else {
            int movesTried = 0;
            while (rc.isMovementReady() && movesTried++ < 4) {
                pathing.moveTowards(currentExploreDirection);
            }
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
