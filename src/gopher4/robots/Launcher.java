package gopher4.robots;

import battlecode.common.*;

public class Launcher extends Robot {
    MapLocation target;
    private int[] attackPreference; // low preference attacked first
    private final int TURNSTOSTANDBY;
    public Launcher(RobotController rc) throws GameActionException {
        super(rc);
        TURNSTOSTANDBY=50;
        attackPreference = new int[]{
                5, // HQ Preference
                4, // Carrier
                2, // Launcher
                0, // Destabilizer
                1, // Booster
                3  // Amplifier
        };
    }

    public void run() throws GameActionException, IllegalAccessException {
        super.run();

        pathing.wander();

        endTurn();
    }

    public int travel() throws GameActionException {
        while(true) {
            if (SeenEnemies() != null) {
                return attack();
            }
            pathing.moveTo(target);
            if (SeenEnemies() != null) {
                return attack();
            }
            if (rc.getLocation().compareTo(target) == 0) {
                return defend();
            }
            endTurn();
        }
    }

    public int attack(){
        while(true) {

        }
    }

    public int defend() throws GameActionException {
        int turns=0;
        while(true) {
            if (SeenEnemies() != null) {
                return attack();
            }else{
                turns++;
                if(turns>TURNSTOSTANDBY)
                endTurn();
            }
        }
    }

    public RobotInfo SeenEnemies() throws GameActionException {
        RobotInfo[] attackableEnemies = rc.senseNearbyRobots(-1, team.opponent());
        int bestPreference = 10;
        RobotInfo bestEnemy = null;
        for (RobotInfo enemy : attackableEnemies) {
            if (attackPreference[enemy.getType().ordinal()] < bestPreference) {
                bestEnemy = enemy;
                bestPreference = enemy.getType().ordinal();
            }
        }
        return bestEnemy;
    }

}
