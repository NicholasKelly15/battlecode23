package gopher4.robots;

import battlecode.common.*;
import gopher4.util.Pathfinding;

import java.util.Stack;

public class Carrier extends Robot {

    private Pathfinding pathing;

    private int mode = -1;  // 0 = collectResources, 1 = runAway, 2 = returnToBase, 3 = explore
    private String[] modeStrings = new String[]{"Collection", "Running", "Returning", "Exploring"};
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
        turnsSinceSeenEnemy = 0;
        currentExploreDirection = rc.getLocation().directionTo(new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2));
    }

    public void run() throws GameActionException, IllegalAccessException {
        super.run();

        if (mode == 1) {
            turnsSinceSeenEnemy++;
        }

        changeModeAndRun();

        endTurn();
    }

    public void changeModeAndRun() throws GameActionException, IllegalAccessException {
        mode = getBestMode();

        switch (mode) {
            case 0:     collectResources(); break;
            case 1:     runAway(); break;
            case 2:     returnToBase(); break;
            case 3:     explore(); break;
        }

        rc.setIndicatorString("Mode: " + modeStrings[mode]);
    }

    private int getBestMode() throws GameActionException, IllegalAccessException {
        if (sensedEnemyLaunchersStackPointer > 0) {
            lastSeenEnemyPos = sensedEnemyLaunchers[0].getLocation();
            return 1;
        } else if (sensedEnemyDestabilizersStackPointer > 0) {
            lastSeenEnemyPos = sensedEnemyDestabilizers[0].getLocation();
            return 1;
        } else if (mode == 1 && turnsSinceSeenEnemy < 5) {
            return 1;
        } else if (mode == 1) {
            turnsSinceSeenEnemy = 0;
        }

        if (getTotalResourceCount() == GameConstants.CARRIER_CAPACITY) {
            return 2;
        }

        MapLocation nearestKnownWell = getNearestKnownWell(wellsSearchedLookingForTarget);
        if (nearestKnownWell != null) {
            currentTargetWell = nearestKnownWell;
            return 0;
        } else {
            return 3;
        }
    }

    private int getTotalResourceCount() {
        return rc.getResourceAmount(ResourceType.ADAMANTIUM)
                + rc.getResourceAmount(ResourceType.MANA)
                + rc.getResourceAmount(ResourceType.ELIXIR);
    }

    // travels to and collects from well.
    private void collectResources() throws GameActionException, IllegalAccessException {
        // consider thinking about adding a case where the carrier passes by an unknown well on
        // its way to the assigned well.

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

                int resourcesHeld = rc.getResourceAmount(ResourceType.ADAMANTIUM)
                        + rc.getResourceAmount(ResourceType.MANA)
                        + rc.getResourceAmount(ResourceType.ELIXIR);
                if (rc.isMovementReady() && resourcesHeld == GameConstants.CARRIER_CAPACITY) {
                    changeModeAndRun();
                }
            } else {
                wellsSearchedLookingForTarget.push(currentTargetWell);
                changeModeAndRun();
            }
        } else {
            int moveTries = 0;
            while (rc.isMovementReady() && moveTries++ < 5) {
                pathing.moveTo(currentTargetWell);
            }
        }

    }

    // runs away from enemies in sight or previously seen.
    private void runAway() throws GameActionException, IllegalAccessException {
        if (getTotalResourceCount() > 0 && rc.isActionReady()) {
            throwResourcesToRun();
        }
        int moveTries = 0;
        while (rc.isMovementReady() && moveTries++ < 3) {
            pathing.moveTowards(lastSeenEnemyPos.directionTo(rc.getLocation()));
        }
    }

    // go back to the hq that spawned this carrier
    private void returnToBase() throws GameActionException, IllegalAccessException {
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
            changeModeAndRun();
        }
    }

    // explore the map for undiscovered wells
    private void explore() throws GameActionException, IllegalAccessException {
        int movesTried = 0;
        while (rc.isMovementReady() && movesTried++ < 4) {
            pathing.moveTowards(currentExploreDirection);
        }
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
