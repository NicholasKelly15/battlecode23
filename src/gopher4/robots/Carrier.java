package gopher4.robots;

import battlecode.common.*;
import gopher4.util.Pathfinding;

import java.util.Stack;

public class Carrier extends Robot {

    private Pathfinding pathing;

    private int adamantiumHeld;
    private int manaHeld;
    private int elixirHeld;
    private int totalResources;


    private int mode = -1;  // 0 = collectResources, 1 = runAway, 2 = returnToBase, 3 = explore
    private String[] modeStrings = new String[]{"Collection", "Running", "Returning", "Exploring"};
    private MapLocation currentTargetWell = null;
    private MapLocation permanentAssignedWell = null;
    private Stack<MapLocation> wellsSearchedLookingForTarget;



    public Carrier(RobotController rc) throws GameActionException {
        super(rc);

        pathing = new Pathfinding(rc);

        wellsSearchedLookingForTarget = new Stack<MapLocation>();

        MapLocation nearestKnownWell = getNearestKnownWell(null);
        if (nearestKnownWell != null) {
            currentTargetWell = nearestKnownWell;
            mode = 0; // collectResources
        } else {
            mode = 3; // explore
        }
    }

    public void run() throws GameActionException, IllegalAccessException {
        super.run();
        updateInternalVariables();

        rc.setIndicatorString("Mode: " + modeStrings[mode]);

        switch (mode) {
            case 0:     collectResources();
            case 1:     runAway();
            case 2:     returnToBase();
            case 3:     explore();
        }

        endTurn();
    }

    private void updateInternalVariables() {
        adamantiumHeld = rc.getResourceAmount(ResourceType.ADAMANTIUM);
        manaHeld = rc.getResourceAmount(ResourceType.MANA);
        elixirHeld = rc.getResourceAmount(ResourceType.ELIXIR);
        totalResources = adamantiumHeld + manaHeld + elixirHeld;
    }

    // travels to and collects from well.
    private void collectResources() throws GameActionException {
        if (sensedEnemyLaunchers.size() > 0 || sensedEnemyDestabilizers.size() > 0) {
            mode = 1; // runAway
            runAway();
            return;
        } else {
            if (totalResources == GameConstants.CARRIER_CAPACITY) {
                mode = 2; // returnToBase
                returnToBase();
                return;
            }
            // consider thinking about adding a case where the carrier passes by an unknown well on
            // its way to the assigned well.

            // 10 is the distance at which the carrier can see the well and every tile around it.
            if (currentTargetWell.distanceSquaredTo(location) <= 10) {
                boolean isSpotOnTargetWellOpen = isSpotOnWellOpen(currentTargetWell);
                if (isSpotOnTargetWellOpen) {
                    if (rc.canCollectResource(currentTargetWell, -1)) {
                        rc.collectResource(currentTargetWell, -1);
                        if (rc.canMove(location.directionTo(currentTargetWell))) {
                            rc.move(location.directionTo(currentTargetWell));
                        }
                    } else {
                        pathing.moveTo(currentTargetWell);
                    }
                } else {
                    wellsSearchedLookingForTarget.push(currentTargetWell);
                    MapLocation nearestKnownWell = getNearestKnownWell(wellsSearchedLookingForTarget);
                    if (nearestKnownWell != null) {
                        currentTargetWell = nearestKnownWell;
                        mode = 0; // collectResources
                        collectResources();
                    } else {
                        mode = 3; // explore
                        explore();
                    }
                }
            }

        }
    }

    // runs away from enemies in sight or previously seen.
    private void runAway() {

    }

    // go back to the hq that spawned this carrier
    private void returnToBase() {

    }

    // explore the map for undiscovered wells
    private void explore() {

    }

    // must be able to see the target well and every tile within range squared of 2 to call this.
    private boolean isSpotOnWellOpen(MapLocation wellLocation) throws GameActionException {
        for (Direction direction : pathing.DIRECTIONS) {
            if (rc.senseRobotAtLocation(wellLocation.add(direction)) == null) {
                return true;
            }
        }
        return rc.senseRobotAtLocation(wellLocation) == null;
    }



}
