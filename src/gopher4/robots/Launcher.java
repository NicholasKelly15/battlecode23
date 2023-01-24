package gopher4.robots;

import battlecode.common.*;

public class Launcher extends Robot {

    public Launcher(RobotController rc) throws GameActionException {
        super(rc);
    }

    public void run() throws GameActionException, IllegalAccessException {
        super.run();

        pathing.wander();

        endTurn();
    }

}
