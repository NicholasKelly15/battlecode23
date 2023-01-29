package gopher6.util;

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

    public Pathfinding(RobotController rc) {
        this.rc = rc;
    }

    public void moveTo(MapLocation location) throws GameActionException {
        Direction optimalDirection = rc.getLocation().directionTo(location);
        moveTowards(optimalDirection);
    }

    public void moveTowards(Direction direction) throws GameActionException {
//        Direction[] optimalTryOrder = new Direction[]{
//                direction,
//                direction.rotateLeft(),
//                direction.rotateRight(),
//                direction.rotateLeft().rotateLeft(),
//                direction.rotateRight().rotateRight(),
//                direction.rotateRight().rotateRight().rotateRight(),
//                direction.rotateLeft().rotateLeft().rotateLeft()
//        };
//        for (Direction d : optimalTryOrder) {
//            if (d != null && rc.canMove(d)) {
//                rc.move(d);
//                break;
//            }
//        }
        badBFS(direction);
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

    public void badBFS(Direction direction) throws GameActionException {
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
            rc.move(bestInitialDirection);
        } else if (bestInitialDirection == null) {
            System.out.println("No Direction Found");
        }


    }


    public void moveTowardsBFS(Direction direction) throws GameActionException {
        MapLocation currentLocation = rc.getLocation();
        MapLocation[] goalLocations = {
                new MapLocation(currentLocation.x + directionOrdinalToGoalPoints[direction.ordinal()][0][0],
                        currentLocation.y + directionOrdinalToGoalPoints[direction.ordinal()][0][1]),
                new MapLocation(currentLocation.x + directionOrdinalToGoalPoints[direction.ordinal()][1][0],
                        currentLocation.y + directionOrdinalToGoalPoints[direction.ordinal()][1][1]),
                new MapLocation(currentLocation.x + directionOrdinalToGoalPoints[direction.ordinal()][2][0],
                        currentLocation.y + directionOrdinalToGoalPoints[direction.ordinal()][2][1])
        };
        int adjX = currentLocation.x - 4;
        int adjY = currentLocation.y - 4;
        boolean[][] notVisited = new boolean[9][9];
        MapLocation[][] parentLocation = new MapLocation[9][9];
        MapLocation[] locationsQueue = new MapLocation[80];
        int front = 0;
        int back = -1;
        int currentSize = 0;


        switch (back) {
            case 0: back = 1;
            case 1: back = 2;
            case 79: back = 0;
        }
        locationsQueue[back] = currentLocation;
        currentSize++;
        /*
        insert
        if rear == maxSize - 1
            rear = -1
        locationsQueue[++back] = insert
        currentSize++

        delete
        currentSize--
        int temp = locationsQueue[front++]
        if front == maxSize
            front = 0
        return temp

        getSize
            return currentSize
         */



    }

}
