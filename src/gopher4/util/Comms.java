package gopher4.util;

import battlecode.common.*;

public class Comms {

    private RobotController rc;
    private SharedArrayQueue enemyLocationsQueue;
    private SharedArrayStack wellLocationsStack;
    private SharedArrayStack hqLocationsStack;

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

    private final int INDEX_OF_ENEMY_LOCATIONS_QUEUE_START = 0;
    private final int MEMORY_ALLOCATED_TO_ENEMY_LOCATIONS_QUEUE = 35;

    private final int INDEX_OF_WELL_LOCATIONS_STACK_START = 35;
    private final int MEMORY_ALLOCATED_TO_WELL_LOCATIONS_STACK = 15;

    private final int INDEX_OF_HQ_LOCATIONS_STACK_START = 50;
    private final int MEMORY_ALLOCATED_TO_HQ_LOCATIONS_STACK = 5;

    private final int BIT_SHIFT_TYPE = 12;
    private final int BIT_SHIFT_X_LOCATION = 6;
    private final int BIT_SHIFT_Y_LOCATION = 0;

//    private final int BIT_MASK_FIRST_BIT =  0b1000_0000_0000_0000;
    private final int BIT_MASK_TYPE = 0b0111_0000_0000_0000;
    private final int BIT_MASK_X_LOCATION = 0b0000_1111_1100_0000;
    private final int BIT_MASK_Y_LOCATION = 0b0000_0000_0011_1111;



    public Comms(RobotController rc) throws GameActionException {
        this.rc = rc;
        enemyLocationsQueue = new SharedArrayQueue(rc, INDEX_OF_ENEMY_LOCATIONS_QUEUE_START, MEMORY_ALLOCATED_TO_ENEMY_LOCATIONS_QUEUE);
        wellLocationsStack = new SharedArrayStack(rc, INDEX_OF_WELL_LOCATIONS_STACK_START, MEMORY_ALLOCATED_TO_WELL_LOCATIONS_STACK);
        hqLocationsStack = new SharedArrayStack(rc, INDEX_OF_HQ_LOCATIONS_STACK_START, MEMORY_ALLOCATED_TO_HQ_LOCATIONS_STACK);
    }

    // Call at start of turn.
    public void open() throws GameActionException {
        enemyLocationsQueue.open();
    }

    // Call at end of turn.
    public void close() throws GameActionException {
        enemyLocationsQueue.close();
    }

    public void onTurnEnd() throws GameActionException {
        enemyLocationsQueue.turnEnd();
    }

    /**************************
     * Public methods for interfacing with the comms.
     */

    public boolean canReportEnemyLocation() {
        return !enemyLocationsQueue.isFull();
    }

    public void reportEnemyLocation(MapLocation location, RobotType type) throws GameActionException {
        int entry = generateEntryToReportEnemyLocation(location, type);
        enemyLocationsQueue.insert(entry);
    }

    public SharedArrayQueue getEnemyLocationsQueue() {
        return enemyLocationsQueue;
    }


    public boolean canReportWellLocation() throws GameActionException {
        return !wellLocationsStack.isFull();
    }

    public void reportWellLocation(MapLocation location, ResourceType type) throws GameActionException {
        int entry = generateEntryToReportWell(location, type);
        wellLocationsStack.push(entry);
    }

    public SharedArrayStack getWellLocationsStack() {
        return wellLocationsStack;
    }

    public boolean canReportHQLocation() throws GameActionException {
        return !hqLocationsStack.isFull();
    }

    public void reportSelfHQLocation() throws GameActionException {
        MapLocation loc = rc.getLocation();
        int entry = generateEntryToReportSelfHQLocation(loc);
        hqLocationsStack.push(entry);
    }

    public SharedArrayStack getHqLocationsStack() {
        return hqLocationsStack;
    }


    /**************************
     * Methods for encoding information into 16 bit representations.
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
        return (robotType << BIT_SHIFT_TYPE)
                | (x << BIT_SHIFT_X_LOCATION)
                | (y << BIT_SHIFT_Y_LOCATION);
    }

    private int generateEntryToReportWell(MapLocation location, ResourceType type) {
        int wellType = -1;
        switch (type) {
            case ADAMANTIUM:
                wellType = 0;
                break;
            case MANA:
                wellType = 1;
                break;
            case ELIXIR:
                wellType = 2;
                break;
            default:
                throw new IllegalArgumentException("ResourceType must be a valid resource type.");
        }
        int x = location.x;
        int y = location.y;
        return (wellType << BIT_SHIFT_TYPE)
                | (x << BIT_SHIFT_X_LOCATION)
                | (y << BIT_SHIFT_Y_LOCATION);
    }

    private int generateEntryToReportSelfHQLocation(MapLocation location) {
        int x = location.x;
        int y = location.y;
        return (x << BIT_SHIFT_X_LOCATION)
                | (y << BIT_SHIFT_Y_LOCATION);
    }

    /**************************
     * Methods for decoding 16 bit representations into information.
     */

    public MapLocation getMapLocationFromBits(int bits) {
        int x = (bits & BIT_MASK_X_LOCATION) >> BIT_SHIFT_X_LOCATION;
        int y = (bits & BIT_MASK_Y_LOCATION) >> BIT_SHIFT_Y_LOCATION;
        return new MapLocation(x, y);
    }

    public RobotType getEnemyTypeFromBits(int bits) {
        int intEnemyType = (bits & BIT_MASK_TYPE) >> BIT_SHIFT_TYPE;
        return intToRobotType[intEnemyType];
    }

    public ResourceType getResourceTypeFromBits(int bits) {
        int intResourceType = (bits & BIT_MASK_TYPE) >> BIT_SHIFT_TYPE;
        return intToWellType[intResourceType];
    }

}
