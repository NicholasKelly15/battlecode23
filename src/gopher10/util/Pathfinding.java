package gopher10.util;

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

    private final int[][][] directionOrdinalToGoalPoints = new int[][][]{
            {{-1, 4}, {0, 4}, {1, 4}},          // North
            {{2, 4}, {3, 3}, {4, 2}},           // Northeast
            {{4, 1}, {4, 0}, {4, -1}},          // East
            {{4, -2}, {3, -3}, {2, -4}},        // Southeast
            {{1, -4}, {0, -4}, {-1, -4}},       // South
            {{-2, -4}, {-3, -3}, {-4, -2}},     // Southwest
            {{-4, -1}, {-4, 0}, {-4, 1}},       // West
            {{-4, 2}, {-3, 3}, {-2, 4}}         // Northwest
    };

    private int pathingMode = 0;
    private Direction currentDirection;

    public Pathfinding(RobotController rc) {
        this.rc = rc;
    }

    public void moveTo(MapLocation location) throws GameActionException {
        Direction optimalDirection = rc.getLocation().directionTo(location);
        moveTowards(optimalDirection);
    }

    public Direction getBestMoveTowards(Direction direction) throws GameActionException {
        if (pathingMode == 0) {
            return badBFS(direction);
        } else {
            return moveTowards(rc.getLocation().add(direction).add(direction).add(direction).add(direction));
        }
    }

    public void moveTowards(Direction direction) throws GameActionException {

        Direction bestMove = getBestMoveTowards(direction);
        if (bestMove != Direction.CENTER && rc.canMove(bestMove)) {
            rc.move(bestMove);
        }

    }

    public Direction moveTowards(MapLocation target) throws GameActionException {
        if (rc.getLocation().equals(target)) {
            return Direction.CENTER;
        }
        if (!rc.isMovementReady()) {
            return Direction.CENTER;
        }
        Direction d = rc.getLocation().directionTo(target);
        if (rc.canMove(d)) {
            rc.move(d);
            currentDirection = null; // there is no obstacle we're going around
            pathingMode = 0;
        } else {
            // Going around some obstacle: can't move towards d because there's an obstacle there
            // Idea: keep the obstacle on our right hand

            if (currentDirection == null) {
                currentDirection = d;
            }
            // Try to move in a way that keeps the obstacle on our right
            for (int i = 0; i < 8; i++) {
                if (rc.canMove(currentDirection)) {
                    Direction returnDir = currentDirection;
                    currentDirection = currentDirection.rotateRight();
                    return returnDir;
                } else {
                    currentDirection = currentDirection.rotateLeft();
                }
            }
        }
        return Direction.CENTER;
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

    public Direction badBFS(Direction direction) throws GameActionException {
        if (!rc.isMovementReady()) {
            return Direction.CENTER;
        }

        MapLocation currentLocation = rc.getLocation();
        MapLocation goal = currentLocation.add(direction).add(direction).add(direction).add(direction).add(direction).add(direction).add(direction).add(direction);
        Direction[] moveableDirections = new Direction[8];
        int moveableDirectionsTop = 0;
        Direction bestInitialDirection = null;
        int currentClosestDistanceReached = 10000;
        int adjX = currentLocation.x - 4;
        int adjY = currentLocation.y - 4;
        boolean[][] visitedTiles = new boolean[9][9];
        visitedTiles[4][4] = true;
        visitedTiles[5][4] = true;
        visitedTiles[3][4] = true;
        visitedTiles[4][3] = true;
        visitedTiles[5][3] = true;
        visitedTiles[3][3] = true;
        visitedTiles[4][5] = true;
        visitedTiles[5][5] = true;
        visitedTiles[3][5] = true;

        for (Direction dir : DIRECTIONS) {
            if (rc.canMove(dir)) {
                moveableDirections[moveableDirectionsTop++] = dir;
            }
        }

        for (int i = moveableDirectionsTop ; i-- > 0 ; ) {
            MapLocation newLocation = currentLocation.add(moveableDirections[i]);
            MapInfo locInfo = rc.senseMapInfo(newLocation);
            if (locInfo.getCurrentDirection() != Direction.CENTER) {
                newLocation = newLocation.add(locInfo.getCurrentDirection());
                if (rc.onTheMap(newLocation) && newLocation.distanceSquaredTo(goal) < currentClosestDistanceReached) {
                    currentClosestDistanceReached = newLocation.distanceSquaredTo(goal);
                    bestInitialDirection = moveableDirections[i];
                }
            }

            for (Direction dir : DIRECTIONS) {
                MapLocation newLoc2 = newLocation.add(dir);
                if (rc.onTheMap(newLoc2) && rc.canSenseLocation(newLoc2) && !visitedTiles[newLoc2.x - adjX][newLoc2.y - adjY]) {
                    visitedTiles[newLoc2.x - adjX][newLoc2.y - adjY] = true;
                    MapInfo locInfo2 = rc.senseMapInfo(newLoc2);
                    if (locInfo2.isPassable()) {
                        newLoc2 = newLoc2.add(locInfo2.getCurrentDirection());
                        if (newLoc2.distanceSquaredTo(goal) < currentClosestDistanceReached) {
                            currentClosestDistanceReached = newLoc2.distanceSquaredTo(goal);
                            bestInitialDirection = moveableDirections[i];
                        }
                    }
                }
            }

        }

        if (bestInitialDirection != null && rc.canMove(bestInitialDirection)) {
            return bestInitialDirection;
        } else if (bestInitialDirection == null) {
            pathingMode = 1;
        }

        return Direction.CENTER;

    }

}
