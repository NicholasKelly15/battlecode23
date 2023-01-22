package gopher3.util;

import battlecode.common.*;
import java.util.Stack;

public class Comms {

    private RobotController rc;

    private final RobotType[] robotTypeIntToType = new RobotType[]{
            RobotType.HEADQUARTERS,
            RobotType.AMPLIFIER,
            RobotType.LAUNCHER,
            RobotType.CARRIER,
            RobotType.DESTABILIZER,
            RobotType.BOOSTER
    };
    private final ResourceType[] resourceIntToType = new ResourceType[]{
            ResourceType.ADAMANTIUM,
            ResourceType.MANA,
            ResourceType.ELIXIR
    };

    // Breakdown of the 60 Array Entries
    private final int START_OF_LOCATIONS_POINTER_ENTRY = 0;
    private final int END_OF_LOCATIONS_POINTER_ENTRY = 1;
    private final int STARTING_HQ_ENTRY = 2;
    private final int STARTING_WELL_ENTRY = 6;
    private final int STARTING_INT_OF_REPORTING_ENTRIES = 16;

    // Breakdown of the 16 bits in each entry
//    private final int TURN_ADDED_BITS = 1;
//    private final int WELL_REPORTED_HERE_BITS = 1;
//    private final int TYPE_BITS = 3;
//    private final int LOCATION_X_BITS = 6;
//    private final int LOCATION_Y_BITS = 6;

//    private final int TURN_ADDED_BITS_SHIFT = 15;
//    private final int WELL_REPORTED_HERE_BITS_SHIFT = 15;
    private final int TYPE_BITS_SHIFT = 12;
    private final int LOCATION_X_BITS_SHIFT = 6;
    private final int LOCATION_Y_BITS_SHIFT = 0;

    private final int FIRST_BIT_MASK = 0b1000_0000_0000_0000;
    private final int TYPE_BITS_MASK = 0b0111_0000_0000_0000;
    private final int LOCATION_X_BITS_MASK = 0b0000_1111_1100_0000;
    private final int LOCATION_Y_BITS_MASK = 0b0000_0000_0011_1111;

    private Stack<Integer> thisRobotsPosts = new Stack<>();


    /*
    Every robot will run postLocation upon seeing something of interest. The robot will
    then call maintainPosts every turn to remove its post a turn after creating it. Exactly
    one headquarters will runFullPostMaintenance to remove old posts in case the poster died.
     */
    public Comms(RobotController rc) throws GameActionException {
        this.rc = rc;

        rc.writeSharedArray(START_OF_LOCATIONS_POINTER_ENTRY, STARTING_INT_OF_REPORTING_ENTRIES);
        rc.writeSharedArray(END_OF_LOCATIONS_POINTER_ENTRY, STARTING_INT_OF_REPORTING_ENTRIES);
    }

    public void postEnemyLocation(MapLocation location, RobotType enemyType) throws GameActionException {
        int entry = generate16BitEnemyArrayEntry(location, enemyType);
        int nextEntryLocation = rc.readSharedArray(END_OF_LOCATIONS_POINTER_ENTRY) + 1;
        nextEntryLocation = nextEntryLocation > 60 ? START_OF_LOCATIONS_POINTER_ENTRY : nextEntryLocation;
        rc.writeSharedArray(nextEntryLocation, entry);
        rc.writeSharedArray(END_OF_LOCATIONS_POINTER_ENTRY, nextEntryLocation);
        thisRobotsPosts.push(nextEntryLocation);
    }

    public void postWellLocation(MapLocation location, ResourceType wellType) {

    }

    public void maintainPosts() throws GameActionException {
        int lastPost = thisRobotsPosts.peek();
        rc.writeSharedArray(START_OF_LOCATIONS_POINTER_ENTRY, lastPost);
        thisRobotsPosts = new Stack<>();
    }

    public void runFullPostMaintenance() throws GameActionException {
        int startingPost = rc.readSharedArray(START_OF_LOCATIONS_POINTER_ENTRY);
        int endingPost = rc.readSharedArray(END_OF_LOCATIONS_POINTER_ENTRY);
        int newStartingPostNumber = startingPost;
        // Loops through the entries circularly starting at starting post and ending at ending post.
        for (int i = startingPost ; i != endingPost ; i++, i = i > 60 ? STARTING_INT_OF_REPORTING_ENTRIES : i) {
            int valueAtArray = rc.readSharedArray(i);
            if (isFirstBit1(valueAtArray)) {
                newStartingPostNumber = i;
            } else {
                setFirstBitTo1(i);
            }
        }
        rc.writeSharedArray(START_OF_LOCATIONS_POINTER_ENTRY, newStartingPostNumber);
    }

    public boolean canAddPost() throws GameActionException {
        int start = rc.readSharedArray(START_OF_LOCATIONS_POINTER_ENTRY);
        int end = rc.readSharedArray(END_OF_LOCATIONS_POINTER_ENTRY) + 1;
        end = end > 60 ? STARTING_INT_OF_REPORTING_ENTRIES : end;
        return start != end;
    }

//    private int getStartOfLocationsPointer() throws GameActionException {
//        return rc.readSharedArray(START_OF_LOCATIONS_POINTER_ENTRY);
//    }
//
//    private int getEndOfLocationsPointer() throws GameActionException {
//        return rc.readSharedArray(END_OF_LOCATIONS_POINTER_ENTRY);
//    }
//
//    private void setStartOfLocationsPointer(int index) throws GameActionException {
//        rc.writeSharedArray(START_OF_LOCATIONS_POINTER_ENTRY, index);
//    }
//
//    private void setEndOfLocationsPointer(int index) throws GameActionException {
//        rc.writeSharedArray(END_OF_LOCATIONS_POINTER_ENTRY, index);
//    }

    public int generate16BitEnemyArrayEntry(MapLocation location, RobotType type) {
        int turnAddedBit = 0;
        int robotType = -1;
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
        return (robotType << TYPE_BITS_SHIFT)
                | (x << LOCATION_X_BITS_SHIFT)
                | (y << LOCATION_Y_BITS_SHIFT);
//        return (turnAddedBit << TURN_ADDED_BITS_SHIFT)
//                | (robotType << TYPE_BITS_SHIFT)
//                | (x << LOCATION_X_BITS_SHIFT)
//                | (y << LOCATION_Y_BITS_SHIFT);
    }

    private int generate16BitWellArrayEntry(MapLocation location, ResourceType type) {
        int turnAddedBit = 0;
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
        return (wellType << TYPE_BITS_SHIFT)
                | (x << LOCATION_X_BITS_SHIFT)
                | (y << LOCATION_Y_BITS_SHIFT);
//        return (turnAddedBit << TURN_ADDED_BITS_SHIFT)
//                | (wellType << TYPE_BITS_SHIFT)
//                | (x << LOCATION_X_BITS_SHIFT)
//                | (y << LOCATION_Y_BITS_SHIFT);
    }

    public MapLocation getMapLocationFromBits(int bits) {
         int x = (bits & LOCATION_X_BITS_MASK) >> LOCATION_X_BITS_SHIFT;
         int y = (bits & LOCATION_Y_BITS_MASK) >> LOCATION_Y_BITS_SHIFT;
         return new MapLocation(x, y);
    }

    public RobotType getEnemyTypeFromBits(int bits) {
        int intEnemyType = (bits & TYPE_BITS_MASK) >> TYPE_BITS_SHIFT;
        return robotTypeIntToType[intEnemyType];
    }

    public ResourceType getWellTypeFromBits(int bits) {
        int intEnemyType = (bits & TYPE_BITS_MASK) >> TYPE_BITS_SHIFT;
        return resourceIntToType[intEnemyType];
    }

    private MapLocation getMapLocationAtIndex(int index) throws GameActionException {
        return getMapLocationFromBits(rc.readSharedArray(index));
    }

    private RobotType getEnemyTypeAtIndex(int index) throws GameActionException {
        return getEnemyTypeFromBits(rc.readSharedArray(index));
    }

    private ResourceType getWellTypeAtIndex(int index) throws GameActionException {
        return getWellTypeFromBits(rc.readSharedArray(index));
    }

    private boolean isFirstBit1(int bits) {
        return (bits & FIRST_BIT_MASK) > 0;
    }

    private void setFirstBitTo1(int index) throws GameActionException {
        int contents = rc.readSharedArray(index);
        contents = contents | FIRST_BIT_MASK;
        rc.writeSharedArray(index, contents);
    }

    private boolean isWellReportedAtIndex(int index) throws GameActionException {
        return isFirstBit1(rc.readSharedArray(index));
    }

    private boolean wasEntryAddedThisTurnAtIndex(int index) throws GameActionException {
        return isFirstBit1(rc.readSharedArray(index));
    }



}
