package gopher2.robots;

import battlecode.common.*;

public class Launcher extends Robot {

//    private final RobotType[] attackOrderPreference = new RobotType[]{
//            RobotType.DESTABILIZER,
//            RobotType.BOOSTER,
//            RobotType.LAUNCHER,
//            RobotType.CARRIER,
//            RobotType.AMPLIFIER
//    };

    public Launcher(RobotController rc) {
        super(rc);
    }

    public void run() throws GameActionException {
        super.run();

        if (sensedEnemyLaunchers.size() > 0) {
            combat();
        } else if (sensedEnemyRobots.length > 0) {
            chaseEnemiesDown();
        } else {
            performMove();
        }
    }

    private void makeAttack() throws GameActionException {
        RobotInfo target = getHighestPriorityTarget();
        if (target != null && rc.canAttack(target.getLocation())) {
            rc.attack(target.getLocation());
        }
    }

    private void makeAttack(RobotInfo highestPriority) throws GameActionException {
        if (highestPriority != null && rc.canAttack(highestPriority.getLocation())) {
            rc.attack(highestPriority.getLocation());
        }
    }

    private void combat() throws GameActionException {
//        int knownEnemiesInVision = sensedEnemyLaunchers.size();
//        RobotInfo furthestEnemy = getFurthestEnemyAttackRobot();
//        int knownFriendlyHelpers = getFriendlyAttackersPressuringEnemy(furthestEnemy);
//        if (knownFriendlyHelpers > knownEnemiesInVision) {
//            rc.setIndicatorString("Advantage");
//            pathing.moveTo(furthestEnemy.getLocation());
//            makeAttack();
//        } else {
//            RobotInfo highestPriority = getHighestPriorityTarget();
//            if (highestPriority != null
//                    && highestPriority.getLocation().distanceSquaredTo(location) <= type.actionRadiusSquared) {
////                rc.setIndicatorString("Attacking then retreating.");
//                makeAttack(highestPriority);
//                retreatFromEnemies();
//            } else if (highestPriority != null) {
////                rc.setIndicatorString("Approaching then attacking.");
//                pathing.moveTo(highestPriority.getLocation());
//                makeAttack(highestPriority);
//            }
//        }
        RobotInfo bestTarget = getHighestPriorityTarget();
        if (bestTarget != null) {
            makeAttack(bestTarget);
            retreatFromEnemies();
        } else {
            performMove();
        }
    }

    private void chaseEnemiesDown() throws GameActionException {
        MapLocation averageEnemy = getAverageEnemyLocation();
        pathing.moveTo(averageEnemy);
        makeAttack();
    }

    private RobotInfo getFurthestEnemyAttackRobot() throws GameActionException {
        int furthestDistance = 0;
        RobotInfo furthestEnemy = null;
        for (RobotInfo enemy : sensedEnemyLaunchers) {
            if (enemy.getLocation().distanceSquaredTo(location) > furthestDistance) {
                furthestEnemy = enemy;
                furthestDistance = enemy.getLocation().distanceSquaredTo(location);
            }
        }
        return furthestEnemy;
    }

    private int getFriendlyAttackersPressuringEnemy(RobotInfo enemy) {
        int result = 0;
        for (RobotInfo friendly : sensedFriendlyLaunchers) {
            if (friendly.getLocation().distanceSquaredTo(enemy.getLocation()) <= RobotType.LAUNCHER.visionRadiusSquared) {
                result++;
            }
        }
        return result;
    }

    private RobotInfo getHighestPriorityTarget() {
        // Look through the enemies by attack preference.
        for (RobotInfo enemy : sensedEnemyDestabilizers) {
            if (rc.canAttack(enemy.getLocation())) {
                return enemy;
            }
        }
        for (RobotInfo enemy : sensedEnemyBoosters) {
            if (rc.canAttack(enemy.getLocation())) {
                return enemy;
            }
        }
        for (RobotInfo enemy : sensedEnemyLaunchers) {
            if (rc.canAttack(enemy.getLocation())) {
                return enemy;
            }
        }
        for (RobotInfo enemy : sensedEnemyCarriers) {
            if (rc.canAttack(enemy.getLocation())) {
                return enemy;
            }
        }
        for (RobotInfo enemy : sensedEnemyAmplifiers) {
            if (rc.canAttack(enemy.getLocation())) {
                return enemy;
            }
        }
        return null;
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
            pathing.moveTo(toFollow.getLocation());
        } else {
            pathing.wander();
        }
    }

}
