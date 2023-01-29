package gopher5;

import battlecode.common.Clock;
import battlecode.common.RobotController;
import gopher5.robots.*;

public class RobotPlayer {

    public static void run(RobotController rc) throws Exception {

        Robot robot = getRobotInstance(rc);
        if (robot != null) {
            while (true) {
                doTurnActions(rc, robot);
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

    private static Robot getRobotInstance(RobotController rc) throws Exception {
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
