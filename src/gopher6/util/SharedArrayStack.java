package gopher6.util;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

import java.util.Iterator;

public class SharedArrayStack implements Iterable<Integer> {

    private RobotController rc;

    private final int TOTAL_SIZE;
//    private final int STACK_MEMORY_SIZE;
    private final int START_OF_ALLOCATED_MEMORY;

    private final int INDEX_OF_STORED_CURRENT_STACK_INDEX;
    private final int INDEX_OF_STACK_MEMORY;

    public SharedArrayStack(RobotController rc, int startingIndex, int stackSize) throws GameActionException {
        this.rc = rc;

        TOTAL_SIZE = stackSize;
//        STACK_MEMORY_SIZE = TOTAL_SIZE - 1;
        START_OF_ALLOCATED_MEMORY = startingIndex;

        INDEX_OF_STORED_CURRENT_STACK_INDEX = START_OF_ALLOCATED_MEMORY;
        INDEX_OF_STACK_MEMORY = INDEX_OF_STORED_CURRENT_STACK_INDEX + 1;

        if (!isSharedArrayMemoryInitialized()) {
            setCurrentStackPointer(INDEX_OF_STACK_MEMORY);
        }
    }

    public boolean isSharedArrayMemoryInitialized() throws GameActionException {
        return getCurrentStackPointer() != 0;
    }

    public void printInfo() throws GameActionException {
        System.out.println("-------------------------");
        System.out.println("Shared Array Stack: (" + START_OF_ALLOCATED_MEMORY + ", " + (START_OF_ALLOCATED_MEMORY + TOTAL_SIZE) + ")");
        System.out.println("Current Pointer: " + getCurrentStackPointer());
        System.out.println("Current # Of Elems: " + (getCurrentStackPointer() - INDEX_OF_STACK_MEMORY));
        System.out.println("-------------------------");
    }

    public boolean isFull() throws GameActionException {
        return getCurrentStackPointer() == START_OF_ALLOCATED_MEMORY + TOTAL_SIZE;
    }

    public boolean isEmpty() throws GameActionException {
        return getCurrentStackPointer() == INDEX_OF_STACK_MEMORY;
    }

    public int getSize() throws GameActionException {
        return getCurrentStackPointer() - INDEX_OF_STACK_MEMORY;
    }

    public void push(int item) throws GameActionException {
        int currentIndex = getCurrentStackPointer();
        rc.writeSharedArray(currentIndex, item);
        setCurrentStackPointer(currentIndex + 1);
    }

    public int pop() throws GameActionException {
        int currentIndex = getCurrentStackPointer();
        int value = rc.readSharedArray(currentIndex);
        setCurrentStackPointer(currentIndex - 1);
        return value;
    }

    public int peek() throws GameActionException {
        return rc.readSharedArray(getCurrentStackPointer());
    }

    private int getCurrentStackPointer() throws GameActionException {
        return rc.readSharedArray(INDEX_OF_STORED_CURRENT_STACK_INDEX);
    }

    private void setCurrentStackPointer(int index) throws GameActionException {
        rc.writeSharedArray(INDEX_OF_STORED_CURRENT_STACK_INDEX, index);
    }

    public Iterator<Integer> iterator() {
        try {
            return new SharedArrayStackIterator(rc, INDEX_OF_STACK_MEMORY, getCurrentStackPointer() - INDEX_OF_STACK_MEMORY);
        } catch (GameActionException e) {
            e.printStackTrace();
            rc.resign();
        }
        rc.resign();
        return null;
    }

}

class SharedArrayStackIterator implements Iterator<Integer> {

    RobotController rc;

    int currentIndex;
    int elementsToTraverse;
    int elementsTraversed;

    public SharedArrayStackIterator(RobotController rc, int stackMemoryStart, int stackCurrentSize) {
        this.rc = rc;

        currentIndex = stackMemoryStart;
        elementsToTraverse = stackCurrentSize;
        elementsTraversed = 0;
    }

    public boolean hasNext() {
        return elementsTraversed < elementsToTraverse;
    }

    public Integer next() {
        try {
            int value = rc.readSharedArray(currentIndex++);
            elementsTraversed++;
            return value;
        } catch (GameActionException e) {
            e.printStackTrace();
            rc.resign();
        }
        return null;
    }

}
