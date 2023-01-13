package gopher0.robots;

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

    private void performMove() throws GameActionException {
        pathing.wander();
    }

}
