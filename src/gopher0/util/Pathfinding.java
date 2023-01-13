package gopher0.util;

import battlecode.common.*;

import java.util.ArrayList;

public class Pathfinding {

    private RobotController rc;

    public final Direction[] DIRECTIONS = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    public Pathfinding(RobotController rc) {
        this.rc = rc;
    }

    public void moveTo(MapLocation location) throws GameActionException {
        Direction optimalDirection = rc.getLocation().directionTo(location);
        Direction[] optimalTryOrder = new Direction[]{
                optimalDirection,
                optimalDirection.rotateLeft(),
                optimalDirection.rotateRight(),
                optimalDirection.rotateLeft().rotateLeft(),
                optimalDirection.rotateRight().rotateRight(),
                optimalDirection.rotateLeft().rotateLeft().rotateLeft(),
                optimalDirection.rotateRight().rotateRight().rotateRight(),
                optimalDirection.opposite()
        };
        for (Direction direction : optimalTryOrder) {
            if (direction != null && rc.canMove(direction)) {
                rc.move(direction);
            }
        }
    }

    public void moveTowards(Direction direction) {

    }

    public void wander() throws GameActionException {
        ArrayList<Direction> possibleDirections = new ArrayList<Direction>();
        for (Direction dir : DIRECTIONS) {
            if (dir != null && rc.canMove(dir)) {
                possibleDirections.add(dir);
            }
        }
        Direction direction = ArrayListUtils.chooseRandom(possibleDirections);
        if (direction != null) {
            rc.move(direction);
        }
    }

    // Just a little too expensive lmao, like 30 bytecode
//    public boolean canWalkInDirection(Direction direction) throws GameActionException {
//        MapLocation newLoc = rc.getLocation().add(direction);
//        return rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc) && rc.sensePassability(newLoc));
//    }

}
