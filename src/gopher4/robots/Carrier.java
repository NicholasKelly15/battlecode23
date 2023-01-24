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



    public Carrier(RobotController rc) throws GameActionException {
        super(rc);

        pathing = new Pathfinding(rc);

        wellsSearchedLookingForTarget = new Stack<MapLocation>();
    }

    public void run() throws GameActionException, IllegalAccessException {
        super.run();

        mode = getBestMode();

        switch (mode) {
            case 0:     collectResources(); break;
            case 1:     runAway(); break;
            case 2:     returnToBase(); break;
            case 3:     explore(); break;
        }

        rc.setIndicatorString("Mode: " + modeStrings[mode]);

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
        if (sensedEnemyLaunchersStackPointer > 0 || sensedEnemyDestabilizersStackPointer > 0) {
            return 1;
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
        if (currentTargetWell.distanceSquaredTo(rc.getLocation()) <= 10) {
            boolean isSpotOnTargetWellOpen = isSpotOnWellOpen(currentTargetWell);
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
    private void runAway() {

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
            pathing.wander();
        }
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

    private void throwResourcesToRun() {

    }



}
