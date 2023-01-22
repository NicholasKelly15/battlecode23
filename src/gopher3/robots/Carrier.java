package gopher3.robots;

import battlecode.common.*;

public class Carrier extends Robot {

    private int adamantiumHeld;
    private int manaHeld;
    private int elixirHeld;



    public Carrier(RobotController rc) throws GameActionException {
        super(rc);
    }

    public void run() throws GameActionException, IllegalAccessException {
        super.run();
        updateInternalVariables();

        retreatFromEnemies();
        if (adamantiumHeld == GameConstants.CARRIER_CAPACITY
                || manaHeld == GameConstants.CARRIER_CAPACITY
                || elixirHeld == GameConstants.CARRIER_CAPACITY) {
            dumpCarriageAtHQ();
        } else {
            searchAndDrawFromWell();
        }
    }

    private void updateInternalVariables() {
        adamantiumHeld = rc.getResourceAmount(ResourceType.ADAMANTIUM);
        manaHeld = rc.getResourceAmount(ResourceType.MANA);
        elixirHeld = rc.getResourceAmount(ResourceType.ELIXIR);
    }

    private void searchAndDrawFromWell() throws GameActionException {
        WellInfo nearestWell = getNearestKnownWell();
        if (nearestWell != null) {
            if (rc.canCollectResource(nearestWell.getMapLocation(), -1)) {
                rc.collectResource(nearestWell.getMapLocation(), -1);
            } else {
                pathing.moveTo(nearestWell.getMapLocation());
            }
        } else {
            pathing.wander();
        }
    }

    private void dumpCarriageAtHQ() throws GameActionException {
        RobotInfo hq = getNearestFriendlyHQ();
        boolean didTransfer = false;
        if (adamantiumHeld > 0) {
            if (rc.canTransferResource(hq.getLocation(), ResourceType.ADAMANTIUM, adamantiumHeld)) {
                didTransfer = true;
                rc.transferResource(hq.getLocation(), ResourceType.ADAMANTIUM, adamantiumHeld);
            }
        } else if (manaHeld > 0) {
            if (rc.canTransferResource(hq.getLocation(), ResourceType.MANA, manaHeld)) {
                didTransfer = true;
                rc.transferResource(hq.getLocation(), ResourceType.MANA, manaHeld);
            }
        } else if (elixirHeld > 0) {
            didTransfer = true;
            if (rc.canTransferResource(hq.getLocation(), ResourceType.ELIXIR, elixirHeld)) {
                rc.transferResource(hq.getLocation(), ResourceType.ELIXIR, elixirHeld);
            }
        }

        if (!didTransfer) {
            pathing.moveTo(hq.getLocation());
        }
    }

    private WellInfo getNearestKnownWell() {
        WellInfo[] wellsInVision = rc.senseNearbyWells();
        WellInfo nearestWell = null;
        int smallestDistanceSquared = -1;
        for (WellInfo well : wellsInVision) {
            if (nearestWell == null || well.getMapLocation().distanceSquaredTo(location) < smallestDistanceSquared) {
                smallestDistanceSquared = well.getMapLocation().distanceSquaredTo(location);
                nearestWell = well;
            }
        }
        return nearestWell;
    }

}
