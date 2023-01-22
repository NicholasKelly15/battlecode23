package gopher2.util;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

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
        moveTowards(optimalDirection);
    }

    public void moveTowards(Direction direction) throws GameActionException {
        Direction[] optimalTryOrder = new Direction[]{
                direction,
                direction.rotateLeft(),
                direction.rotateRight(),
                direction.rotateLeft().rotateLeft(),
                direction.rotateRight().rotateRight(),
                direction.rotateLeft().rotateLeft().rotateLeft(),
                direction.rotateRight().rotateRight().rotateRight(),
                direction.opposite()
        };
        for (Direction d : optimalTryOrder) {
            if (d != null && rc.canMove(d)) {
                rc.move(d);
            }
        }
    }

    public void wander() throws GameActionException {
        ArrayList<Direction> possibleDirections = new ArrayList<Direction>();
        for (Direction dir : DIRECTIONS) {
            if (dir != null && rc.canMove(dir)) {
                possibleDirections.add(dir);
            }
        }
        Direction direction = ArrayListUtils.chooseRandom(possibleDirections);
        if (direction != null && rc.canMove(direction)) {
            rc.move(direction);
        }
    }

    // Just a little too expensive lmao, like 30 bytecode
//    public boolean canWalkInDirection(Direction direction) throws GameActionException {
//        MapLocation newLoc = rc.getLocation().add(direction);
//        return rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc) && rc.sensePassability(newLoc));
//    }

}
