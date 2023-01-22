package gopher2.robots;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class Amplifier extends Robot {

    public Amplifier(RobotController rc) {
        super(rc);
    }

    public void run() throws GameActionException {
        super.run();

        pathing.wander();
    }

}
