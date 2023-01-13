package gopher1.robots;

import battlecode.common.*;

public class Launcher extends Robot {

    private final RobotType[] attackOrderPreference = new RobotType[]{
            RobotType.DESTABILIZER,
            RobotType.BOOSTER,
            RobotType.LAUNCHER,
            RobotType.CARRIER,
            RobotType.AMPLIFIER
    };

    public Launcher(RobotController rc) {
        super(rc);
    }

    public void run() throws GameActionException {
        super.run();

        findTargetsAndAttack();
        performMove();
        findTargetsAndAttack();
    }

    private void findTargetsAndAttack() throws GameActionException {
        // Attack in order of preference
        for (RobotInfo enemy : sensedEnemyDestabilizers) {
            if (rc.canAttack(enemy.getLocation())) {
                rc.attack(enemy.getLocation());
                return;
            }
        }
        for (RobotInfo enemy : sensedEnemyBoosters) {
            if (rc.canAttack(enemy.getLocation())) {
                rc.attack(enemy.getLocation());
                return;
            }
        }
        for (RobotInfo enemy : sensedEnemyLaunchers) {
            if (rc.canAttack(enemy.getLocation())) {
                rc.attack(enemy.getLocation());
                return;
            }
        }
        for (RobotInfo enemy : sensedEnemyCarriers) {
            if (rc.canAttack(enemy.getLocation())) {
                rc.attack(enemy.getLocation());
                return;
            }
        }
        for (RobotInfo enemy : sensedEnemyAmplifiers) {
            if (rc.canAttack(enemy.getLocation())) {
                rc.attack(enemy.getLocation());
                return;
            }
        }
    }

    private RobotInfo getLauncherToFollow() {
        int lowestID = Integer.MAX_VALUE;
        RobotInfo toFollow = null;
        for (RobotInfo friendly : sensedFriendlyRobots) {
            if (friendly.getType() == RobotType.LAUNCHER && friendly.getID() < lowestID) {
                lowestID = friendly.getID();
                toFollow = friendly;
            }
        }
        return toFollow;
    }

    private void performMove() throws GameActionException {
        RobotInfo toFollow = getLauncherToFollow();
        if (toFollow != null) {
            rc.setIndicatorLine(location, toFollow.getLocation(), 255, 0, 0);
            pathing.moveTo(toFollow.getLocation());
        } else {
            pathing.wander();
        }
    }

}
