package gopher3.util;

import battlecode.common.*;

public class Comms2 {

    private RobotController rc;

    private final RobotType[] intToRobotType = new RobotType[]{
            RobotType.HEADQUARTERS,
            RobotType.AMPLIFIER,
            RobotType.LAUNCHER,
            RobotType.CARRIER,
            RobotType.DESTABILIZER,
            RobotType.BOOSTER
    };
    private final ResourceType[] intToWellType = new ResourceType[]{
            ResourceType.ADAMANTIUM,
            ResourceType.MANA,
            ResourceType.ELIXIR
    };

    private final int INDEX_OF_FIRST_INDEX_OF_REPORTED_ROBOTS_QUEUE = 0;
    private final int INDEX_OF_END_INDEX_OF_REPORTED_ROBOTS_QUEUE = 1;
    private final int INDEX_OF_REPORTED_ROBOTS_QUEUE_CURRENT_SIZE = 2;
    private final int INDEX_OF_FIRST_HQ_LOCATION = 3;
    private final int INDEX_OF_START_OF_REPORTED_WELLS = 7;
    private final int INDEX_OF_START_OF_REPORTED_ROBOTS_QUEUE = 17;

    private final int REPORTED_ROBOTS_QUEUE_SIZE = 47;

    private final int BIT_SHIFT_ROBOT_TYPE = 12;
    private final int BIT_SHIFT_X_LOCATION = 6;
    private final int BIT_SHIFT_Y_LOCATION = 0;

    private final int BIT_MASK_FIRST_BIT =  0b1000_0000_0000_0000;
    private final int BIT_MASK_ROBOT_TYPE = 0b0111_0000_0000_0000;
    private final int BIT_MASK_X_LOCATION = 0b0000_1111_1100_0000;
    private final int BIT_MASK_Y_LOCATION = 0b0000_0000_0011_1111;

    public Comms2(RobotController rc) throws GameActionException {
        this.rc = rc;

//        setReportedRobotsQueueFront(INDEX_OF_START_OF_REPORTED_ROBOTS_QUEUE);
//        setReportedRobotsQueueEnd(INDEX_OF_START_OF_REPORTED_ROBOTS_QUEUE - 1);
    }

    public void runSharedArrayMaintenance() throws GameActionException, IllegalAccessException {
        if (rc.getType() != RobotType.HEADQUARTERS) {
            throw new IllegalAccessException("Only the headquarters should run shared array maintenance.");
        } else {
            boolean reachedKeepPartOfArray = false;
            int current = getReportedRobotsQueueFront();
            int front = current;
            int queueSize = getReportedRobotsQueueCurrentSize();
            for (int i = 0 ; i < queueSize ; i++) {
                if (current == INDEX_OF_START_OF_REPORTED_ROBOTS_QUEUE + REPORTED_ROBOTS_QUEUE_SIZE) {
                    current = INDEX_OF_START_OF_REPORTED_ROBOTS_QUEUE;
                }
                int entry = rc.readSharedArray(current);
                if ((entry & BIT_MASK_FIRST_BIT) > 0 && !reachedKeepPartOfArray) {
//                    removeLastFromReportedRobots();
                } else if ((entry & BIT_MASK_FIRST_BIT) == 0) {
                    rc.writeSharedArray(current, entry | BIT_MASK_FIRST_BIT);
                    if (!reachedKeepPartOfArray) {
                        int newFront = current;
                        setReportedRobotsQueueFront(newFront);
                        int sizeChange;
                        if (newFront < front) {
                            sizeChange = (GameConstants.SHARED_ARRAY_LENGTH - front) + (newFront - INDEX_OF_START_OF_REPORTED_ROBOTS_QUEUE);
                        } else {
                            sizeChange = newFront - front;
                        }
                        System.out.println(sizeChange);
                        rc.writeSharedArray(INDEX_OF_REPORTED_ROBOTS_QUEUE_CURRENT_SIZE, getReportedRobotsQueueCurrentSize() - sizeChange);
                    }
                    reachedKeepPartOfArray = true;
                } else {
                    SharedArrayUtils.printSharedArray(rc, true);
                    System.out.println("Can't do that.");
                    rc.resign();
                }
                current++;
            }
        }
    }



    public void reportSelfHeadquartersLocation() throws IllegalAccessException, GameActionException {
        if (rc.getType() != RobotType.HEADQUARTERS) {
            throw new IllegalAccessException("Only the headquarters should report its own location.");
        } else {
            if (!isSelfHeadquartersReported()) {
                appendSelfHeadquartersToArray();
            } else {
                throw new IllegalAccessException("Headquarters should only try to report itself once.");
            }
        }
    }

    public boolean canReportRobotLocation() throws GameActionException {
        return !isReportedRobotsQueueFull();
    }

    public void reportRobotLocation(MapLocation location, RobotType robotType) throws GameActionException {
        appendEnemyLocationToReportedRobotsQueue(location, robotType);
    }

    public void removeLastFromReportedRobots() throws GameActionException {
        dequeueFromReportedRobotsQueue();
    }

    public boolean canReportWellLocation() {
        return isWellLocationsListFull();
    }

    public void reportWellLocation(MapLocation location, ResourceType type) {
        appendWellLocationToWellLocationsList(location, type);
    }

    /*************************************************************************************
     *
     * HELPERS FOR REPORTING SELF HEADQUARTERS LOCATION
     *
     */

    private int getFreeIndexToReportSelfHeadquartersLocation() {
        return -1;
    }

    private int generateEntryToReportSelfHeadquartersLocation() {
        return -1;
    }

    private boolean isSelfHeadquartersReported() {
        return false;
    }

    // Returns the order of hq's, so 0 if this is the first HQ to report.
    private int appendSelfHeadquartersToArray() throws GameActionException {
        int entry = generateEntryToReportSelfHeadquartersLocation();
        int index = getFreeIndexToReportSelfHeadquartersLocation();
        rc.writeSharedArray(index, entry);
        return index - INDEX_OF_FIRST_HQ_LOCATION;
    }

    /*************************************************************************************
     *
     * HELPERS FOR REPORTING ENEMY ROBOT LOCATIONS
     *
     */

    private int generateEntryToReportEnemyLocation(MapLocation location, RobotType type) {
        int robotType;
        switch (type) {
            case HEADQUARTERS:
                robotType = 0;
                break;
            case AMPLIFIER:
                robotType = 1;
                break;
            case LAUNCHER:
                robotType = 2;
                break;
            case CARRIER:
                robotType = 3;
                break;
            case DESTABILIZER:
                robotType = 4;
                break;
            case BOOSTER:
                robotType = 5;
                break;
            default:
                throw new IllegalArgumentException("RobotType must be a valid robot type.");
        }
        int x = location.x;
        int y = location.y;
        return (robotType << BIT_SHIFT_ROBOT_TYPE)
                | (x << BIT_SHIFT_X_LOCATION)
                | (y << BIT_SHIFT_Y_LOCATION);
    }

    private boolean isReportedRobotsQueueFull() throws GameActionException {
        return getReportedRobotsQueueCurrentSize() == REPORTED_ROBOTS_QUEUE_SIZE;
    }

    private int getReportedRobotsQueueFront() throws GameActionException {
        return rc.readSharedArray(INDEX_OF_FIRST_INDEX_OF_REPORTED_ROBOTS_QUEUE);
    }

    private void setReportedRobotsQueueFront(int value) throws GameActionException {
        rc.writeSharedArray(INDEX_OF_FIRST_INDEX_OF_REPORTED_ROBOTS_QUEUE, value);
    }

    private int getReportedRobotsQueueEnd() throws GameActionException {
        return rc.readSharedArray(INDEX_OF_END_INDEX_OF_REPORTED_ROBOTS_QUEUE);
    }

    private void setReportedRobotsQueueEnd(int value) throws GameActionException {
        rc.writeSharedArray(INDEX_OF_END_INDEX_OF_REPORTED_ROBOTS_QUEUE, value);
    }

    private int getReportedRobotsQueueCurrentSize() throws GameActionException {
        return rc.readSharedArray(INDEX_OF_REPORTED_ROBOTS_QUEUE_CURRENT_SIZE);
    }

    private void setReportedRobotsQueueCurrentSize(int value) throws GameActionException {
        rc.writeSharedArray(INDEX_OF_REPORTED_ROBOTS_QUEUE_CURRENT_SIZE, value);
    }

    private void appendEnemyLocationToReportedRobotsQueue(MapLocation location, RobotType type) throws GameActionException {
        int entry = generateEntryToReportEnemyLocation(location, type);
        appendEntryToReportedRobotsQueue(entry);
    }

    private void appendEntryToReportedRobotsQueue(int entry) throws GameActionException {
        int end = getReportedRobotsQueueEnd();
        if (end == GameConstants.SHARED_ARRAY_LENGTH - 1) {
            end = INDEX_OF_START_OF_REPORTED_ROBOTS_QUEUE - 1;
        }
        rc.writeSharedArray(++end, entry);
        setReportedRobotsQueueCurrentSize(getReportedRobotsQueueCurrentSize() + 1);
        setReportedRobotsQueueEnd(end);
    }

    private void dequeueFromReportedRobotsQueue() throws GameActionException {
        int front = getReportedRobotsQueueFront();
        front = front == GameConstants.SHARED_ARRAY_LENGTH - 1 ? INDEX_OF_START_OF_REPORTED_ROBOTS_QUEUE - 1 : front;
        setReportedRobotsQueueFront(front + 1);
        setReportedRobotsQueueCurrentSize(getReportedRobotsQueueCurrentSize() - 1);
    }

    /*************************************************************************************
     *
     * HELPERS FOR REPORTING WELL LOCATIONS
     *
     */

    private int generateEntryToReportWellLocation(MapLocation location, ResourceType type) {
        return -1;
    }

    private boolean isWellLocationsListFull() {
        return false;
    }

    private void appendWellLocationToWellLocationsList(MapLocation location, ResourceType type) {
        int entry = generateEntryToReportWellLocation(location, type);
        appendEntryToWellLocationsList(entry);
    }

    private void appendEntryToWellLocationsList(int entry) {

    }


}
