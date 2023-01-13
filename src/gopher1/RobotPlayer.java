package gopher1;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import gopher1.gopher0.robots.*;
import gopher1.robots.*;

public class RobotPlayer {

    public static void run(RobotController rc) throws GameActionException {

        Robot robot = getRobotInstance(rc);
        if (robot != null) {
            while (true) {
                doTurnActions(rc, robot);
                Clock.yield();
            }
        }

    }

    private static void doTurnActions(RobotController rc, Robot robot) {

        try {
            robot.run();
        } catch (Exception e) {
            System.out.println(rc.getType() + " Exception");
            e.printStackTrace();
        } finally {
            Clock.yield();
        }

    }

    private static Robot getRobotInstance(RobotController rc) {
        switch (rc.getType()) {
            case HEADQUARTERS:
                return new Headquarters(rc);

            case CARRIER:
                return new Carrier(rc);

            case LAUNCHER:
                return new Launcher(rc);

            case AMPLIFIER:
                return new Amplifier(rc);
        }
        return null;
    }

}
