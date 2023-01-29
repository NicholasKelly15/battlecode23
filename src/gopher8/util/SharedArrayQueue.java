package gopher8.util;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

import java.util.Iterator;

public class SharedArrayQueue implements Iterable<Integer> {

    private RobotController rc;

    private final boolean IS_MAINTAINER;
    private int lastTurnQueueSize;
    private int lastTurnElementsAdded;

    private final int TOTAL_SIZE;
    private final int QUEUE_MEMORY_SIZE;
    private final int START_OF_ALLOCATED_MEMORY;

    private final int INDEX_OF_STORED_START;
    private final int INDEX_OF_STORED_CURRENT_SIZE;
    private final int INDEX_OF_STORED_END;
    private final int INDEX_OF_QUEUE_MEMORY;

    private int start;
    private int currentSize;
    private int end;

    /*
    Creates a queue in the shared array using the memory starting at startingIndex
    and with size queueSize.
     */
    public SharedArrayQueue(RobotController rc, int startingIndex, int queueSize) throws GameActionException {
        this.rc = rc;

        if (queueSize < 4) {
            throw new AssertionError("A SharedArrayQueue must have at least 4 indices allocated to it.");
        }

        START_OF_ALLOCATED_MEMORY = startingIndex;
        TOTAL_SIZE = queueSize;
        QUEUE_MEMORY_SIZE = TOTAL_SIZE - 3;

        INDEX_OF_STORED_START = START_OF_ALLOCATED_MEMORY;
        INDEX_OF_STORED_CURRENT_SIZE = START_OF_ALLOCATED_MEMORY + 1;
        INDEX_OF_STORED_END = START_OF_ALLOCATED_MEMORY + 2;
        INDEX_OF_QUEUE_MEMORY = START_OF_ALLOCATED_MEMORY + 3;

        if (!isSharedArrayMemoryInitialized()) {
            IS_MAINTAINER = true;
            lastTurnQueueSize = 0;
            lastTurnElementsAdded = 0;
            initializeSharedArrayMemory();
        } else {
            IS_MAINTAINER = false;
        }
    }

    public void printInfo() {
        System.out.println("-------------------------");
        System.out.println("Shared Array Queue: (" + START_OF_ALLOCATED_MEMORY + ", " + (START_OF_ALLOCATED_MEMORY + TOTAL_SIZE) + ")");
        System.out.println("Current Size: " + currentSize);
        System.out.println("Start: " + start);
        System.out.println("End: " + end);
        System.out.println("-------------------------");
    }

    /*
    Call this before using the shared array.
     */
    public void open() throws GameActionException {
        start = getStart();
        currentSize = getCurrentSize();
        end = getEnd();
    }

    /*
    Call this after using shared array but while still able to write to the shared array.
     */
    public void close() throws GameActionException {
        if (rc.canWriteSharedArray(0, 0)) {
            setStart();
            setCurrentSize();
            setEnd();
        }
    }

    /*
    Call this at the end of every turn to load the updated variables back to the shared array.
     */
    public void turnEnd() throws GameActionException {
        if (IS_MAINTAINER) {
            runMaintenance();
        }
    }

    private void runMaintenance() {
        int elementsToRemove = lastTurnElementsAdded;
        lastTurnElementsAdded = currentSize - lastTurnQueueSize;
        for (int i = 0 ; i < elementsToRemove ; i++) {
            remove();
        }
        lastTurnQueueSize = currentSize;
    }

    public boolean isSharedArrayMemoryInitialized() throws GameActionException {
        return getStart() != 0;
    }

    public void initializeSharedArrayMemory() throws GameActionException {
        setStart(INDEX_OF_QUEUE_MEMORY);
        setEnd(INDEX_OF_QUEUE_MEMORY - 1);
        setCurrentSize(0);
    }

    public boolean isFull() {
        return currentSize == QUEUE_MEMORY_SIZE;
    }

    public boolean isEmpty() {
        return currentSize == 0;
    }

    public int getSize() {
        return currentSize;
    }

    public void insert(int item) throws GameActionException {
        if (isFull()) {
            throw new AssertionError("Cannot insert to a full array queue.");
        }
        if (end == START_OF_ALLOCATED_MEMORY + TOTAL_SIZE - 1) {
            end = INDEX_OF_QUEUE_MEMORY - 1;
        }
        rc.writeSharedArray(++end, item);
        currentSize++;
    }

    public void remove() {
        if (isEmpty()) {
            throw new AssertionError("Cannot remove from an empty array queue.");
        }
        start++;
        if (start == START_OF_ALLOCATED_MEMORY + TOTAL_SIZE) {
            start = INDEX_OF_QUEUE_MEMORY;
        }
        currentSize--;
    }

    private int getStart() throws GameActionException {
        return rc.readSharedArray(INDEX_OF_STORED_START);
    }

    private void setStart() throws GameActionException {
        rc.writeSharedArray(INDEX_OF_STORED_START, start);
    }

    private void setStart(int start) throws GameActionException {
        rc.writeSharedArray(INDEX_OF_STORED_START, start);
    }

    private int getCurrentSize() throws GameActionException {
        return rc.readSharedArray(INDEX_OF_STORED_CURRENT_SIZE);
    }

    private void setCurrentSize() throws GameActionException {
        rc.writeSharedArray(INDEX_OF_STORED_CURRENT_SIZE, currentSize);
    }

    private void setCurrentSize(int currentSize) throws GameActionException {
        rc.writeSharedArray(INDEX_OF_STORED_CURRENT_SIZE, currentSize);
    }

    private int getEnd() throws GameActionException {
        return rc.readSharedArray(INDEX_OF_STORED_END);
    }

    private void setEnd() throws GameActionException {
        rc.writeSharedArray(INDEX_OF_STORED_END, end);
    }

    private void setEnd(int end) throws GameActionException {
        rc.writeSharedArray(INDEX_OF_STORED_END, end);
    }

    public Iterator<Integer> iterator() {
        return new SharedArrayQueueIterator(rc, INDEX_OF_QUEUE_MEMORY, QUEUE_MEMORY_SIZE, start, currentSize);
    }

}

class SharedArrayQueueIterator implements Iterator<Integer> {

    RobotController rc;

    int queueMemoryStart;
    int queueMemorySize;
    int currentIndex;
    int currentSize;
    int elementsTraversed;

    public SharedArrayQueueIterator(RobotController rc, int queueMemoryStart, int queueMemorySize, int start, int currentSize) {
        this.rc = rc;

        this.queueMemoryStart = queueMemoryStart;
        this.queueMemorySize = queueMemorySize;
        this.currentIndex = start;
        this.currentSize = currentSize;
        elementsTraversed = 0;
    }

    public boolean hasNext() {
        return elementsTraversed < currentSize;
    }

    public Integer next() {
        try {
            int value = rc.readSharedArray(currentIndex++);
            if (currentIndex == queueMemoryStart + queueMemorySize) {
                currentIndex = queueMemoryStart;
            }
            elementsTraversed++;
            return value;
        } catch (GameActionException e) {
            e.printStackTrace();
            rc.resign();
        }
        return null;
    }

}
