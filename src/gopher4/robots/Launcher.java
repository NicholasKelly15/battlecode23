package gopher4.robots;

import battlecode.common.*;
import gopher4.util.SharedArrayStack;

public class Launcher extends Robot {
    MapLocation target;
    private int[] attackPreference; // low preference attacked first
    private final int TURNSTOSTANDBY;
    private final int TURNSTOSTANDBYSHORT;
    private final int GAURDRADIUS;
    private final int FOLLOWRADIUS;
    private boolean[] AllThings;
    private boolean[] CanAttack;
    private boolean[] CantAttack;
    private int symmetryType;
    private int val;

    public Launcher(RobotController rc) throws GameActionException {
        super(rc);
        int temp=rc.readSharedArray(63);
        symmetryType=temp/16384;
        val=temp%16384;
        TURNSTOSTANDBY=50;
        GAURDRADIUS=20;
        FOLLOWRADIUS=16;
        TURNSTOSTANDBYSHORT=10;
        attackPreference = new int[]{
                5, // HQ Preference
                4, // Carrier
                1, // Launcher
                0, // Destabilizer
                2, // Booster
                3  // Amplifier
        };
        AllThings=new boolean[]{false,true,true,true,true,true};
        CanAttack=new boolean[]{false,false,true,true,false,false};
        CantAttack=new boolean[]{false,true,false,false,true,true};
    }

    public void run() throws GameActionException, IllegalAccessException {
        super.run();
        MapLocation thePoint= new MapLocation (rc.getMapWidth()/2, rc.getMapHeight()/2);
        target=thePoint;
        int x= travel();

        endTurn();
    }

    //TODO implement attacking into clouds
    public int travel() throws GameActionException {

        while(true) {
            rc.setIndicatorString("Travel");
            if (SeenEnemies(AllThings,-1) != null) {
                return attack();
            }
            pathing.moveTo(target);
            if (SeenEnemies(AllThings,-1) != null) {
                return attack();
            }
            if (rc.getLocation().isWithinDistanceSquared(target,GAURDRADIUS)) {
                return defend();
            }
            Clock.yield();
        }
    }

    public int attack() throws GameActionException {
        int turns = 0;
        RobotInfo AttackSeen;
        RobotInfo NotAttackSeen;
        Direction Dir;

        while (true) {
            rc.setIndicatorString("Attack");
            AttackSeen = SeenEnemies(CanAttack, -1);
            NotAttackSeen = SeenEnemies(CantAttack, -1);
            if (turns > TURNSTOSTANDBY) {
                return travel();
            }
            if (AttackSeen == null) {
                if (NotAttackSeen != null) {
                    turns = 0;
                    if (rc.canAttack(NotAttackSeen.getLocation())) {
                        rc.attack(NotAttackSeen.getLocation());
                        if (NotAttackSeen.health > RobotType.LAUNCHER.damage && rc.isMovementReady()) {
                            Dir = rc.getLocation().directionTo(NotAttackSeen.getLocation());
                            MoveInDirSomewhat(Dir);
                        }
                    } else {
                        moveToAndAttack(NotAttackSeen.getLocation());
                    }
                } else {
                    turns++;
                }
            } else {
                turns = 0;
                    if (rc.canAttack(AttackSeen.getLocation())) {
                        rc.attack(AttackSeen.getLocation());
                    } else {
                    //    if (rc.getHealth() <= RobotType.LAUNCHER.damage) {
                            moveToAndAttack(AttackSeen.getLocation());
                    //    }
                    }

                if (rc.isMovementReady()) {
                    Dir = rc.getLocation().directionTo(AttackSeen.getLocation()).opposite();
                    MoveInDirSomewhat(Dir);
                        if (rc.canMove(Dir.rotateLeft().rotateLeft())) {
                            rc.move(Dir.rotateLeft().rotateLeft());
                        } else {
                            if (rc.canMove(Dir.rotateRight().rotateRight())) {
                                rc.move(Dir.rotateRight().rotateRight());
                            }
                        }
                }
                Clock.yield();
            }
        }
    }

    public int defend() throws GameActionException {
        MapLocation Center= new  MapLocation(rc.getMapWidth()/2, rc.getMapHeight());
            int turns = 0;
            int turns2= 0;
            while (true) {
                rc.setIndicatorString("Defend");
                if (SeenEnemies(AllThings, -1) != null) {
                    return attack();
                } else {
                    turns++;
                    if (turns > TURNSTOSTANDBY) {
                        target = GetNewTarget();
                        return travel();
                    }
                    if(!rc.getLocation().isWithinDistanceSquared(target,GAURDRADIUS)) {
                        if (turns2 > TURNSTOSTANDBYSHORT) {
                            return travel();
                        }
                        turns2++;
                    }else{
                        turns2=0;
                    }
                    if(rc.isMovementReady()){
                        RobotInfo toFollow = getLauncherToFollow();
                        if (toFollow != null && turns>10) {
                            rc.setIndicatorString("Follower");
                            pathing.moveTo(toFollow.getLocation().add(toFollow.getLocation().directionTo(target)));
                        } else {
                            if(!rc.getLocation().isWithinDistanceSquared(target,GAURDRADIUS)){
                                travel();
                            }else {
                                MoveInDirSomewhat(rc.getLocation().directionTo(Center), GAURDRADIUS);
                            }
                        }
                    }
                    if (SeenEnemies(AllThings, -1) != null) {
                        return attack();
                    }
                    Clock.yield();
                }
            }
        }

    public RobotInfo SeenEnemies (boolean[] ScanedThings, int range) throws GameActionException {
        RobotInfo[] attackableEnemies = rc.senseNearbyRobots(range, team.opponent());
        int bestPreference = 10;
        RobotInfo bestEnemy = null;
        for (RobotInfo enemy : attackableEnemies) {
            if (ScanedThings[enemy.getType().ordinal()] && attackPreference[enemy.getType().ordinal()] < bestPreference) {
                bestEnemy = enemy;
                bestPreference = enemy.getType().ordinal();
            }
        }
        return bestEnemy;
    }

    public void moveToAndAttack(MapLocation Loc) throws GameActionException {
        int difference;
        Direction Dir;
        if (rc.isMovementReady()) {
            if (rc.getLocation().distanceSquaredTo(Loc) < 19) {
                Dir = rc.getLocation().directionTo(Loc);
                if (rc.canMove(Dir)) {
                    rc.move(Dir);
                } else {
                    if (rc.canMove(Dir.rotateLeft())) {
                        rc.move(Dir.rotateLeft());
                    } else {
                        if (rc.canMove(Dir.rotateRight())) {
                            rc.move(Dir.rotateRight());
                        }
                    }
                }
                if (rc.canAttack(Loc)) {
                    rc.attack(Loc);
                }
            } else {
                Dir = rc.getLocation().directionTo(Loc);
                if (rc.canMove(Dir)) {
                    rc.move(Dir);
                } else {
                    difference = rc.getLocation().x - Loc.x;
                    if (difference == 4) {
                        if (rc.canMove(Direction.WEST)) {
                            rc.move(Direction.WEST);
                        }
                    }
                    if (difference == -4) {
                        if (rc.canMove(Direction.EAST)) {
                            rc.move(Direction.EAST);
                        }
                    }
                    difference = rc.getLocation().y - Loc.y;
                    if (difference == 4) {
                        if (rc.canMove(Direction.SOUTH)) {
                            rc.move(Direction.SOUTH);
                        }
                    }
                    if (difference == -4) {
                        if (rc.canMove(Direction.NORTH)) {
                            rc.move(Direction.NORTH);
                        }
                    }
                }
                if (rc.canAttack(Loc) && rc.isActionReady()) {
                    rc.attack(Loc);
                }
            }
        }
    }

    public void MoveInDirSomewhat(Direction Dir) throws GameActionException {
        if (rc.canMove(Dir)) {
            rc.move(Dir);
        } else {
        if (rc.canMove(Dir.rotateLeft())) {
            rc.move(Dir.rotateLeft());
        } else {
            if (rc.canMove(Dir.rotateRight())) {
                rc.move(Dir.rotateRight());
            }
        }
        }
    }
    public void MoveInDirSomewhat(Direction Dir, int r) throws GameActionException {
        if (rc.canMove(Dir)&&  (rc.senseMapInfo(rc.getLocation().add(Dir)).getCurrentDirection()==Direction.CENTER) &&(2<target.distanceSquaredTo(rc.getLocation().add(Dir))&&target.distanceSquaredTo(rc.getLocation().add(Dir))<r)) {
            rc.move(Dir);
        } else {
            if (rc.canMove(Dir.rotateLeft())&&  (rc.senseMapInfo(rc.getLocation().add(Dir.rotateLeft())).getCurrentDirection()==Direction.CENTER) &&2<target.distanceSquaredTo(rc.getLocation().add(Dir.rotateLeft()))&&target.distanceSquaredTo(rc.getLocation().add(Dir.rotateLeft()))<r) {
                rc.move(Dir.rotateLeft());
            } else {
                if (rc.canMove(Dir.rotateRight())&&  (rc.senseMapInfo(rc.getLocation().add(Dir.rotateRight())).getCurrentDirection()==Direction.CENTER) &&2<target.distanceSquaredTo(rc.getLocation().add(Dir.rotateRight()))&&target.distanceSquaredTo(rc.getLocation().add(Dir.rotateRight()))<r) {
                    rc.move(Dir.rotateRight());
                }else {
                    if (rc.canMove(Dir.rotateLeft().rotateLeft())&&  (rc.senseMapInfo(rc.getLocation().add(Dir.rotateLeft().rotateLeft())).getCurrentDirection()==Direction.CENTER) && 2<target.distanceSquaredTo(rc.getLocation().add(Dir.rotateLeft().rotateLeft()))&&target.distanceSquaredTo(rc.getLocation().add(Dir.rotateLeft().rotateLeft()))<r) {
                        rc.move(Dir.rotateLeft().rotateLeft());
                    } else {
                        if (rc.canMove(Dir.rotateRight().rotateRight())&&  (rc.senseMapInfo(rc.getLocation().add(Dir.rotateRight().rotateRight())).getCurrentDirection()==Direction.CENTER) && 2<target.distanceSquaredTo(rc.getLocation().add(Dir.rotateRight().rotateRight()))&&target.distanceSquaredTo(rc.getLocation().add(Dir.rotateRight().rotateRight()))<r) {
                            rc.move(Dir.rotateRight().rotateRight());
                        }
                    }
                }
            }
        }
    }

    private RobotInfo getLauncherToFollow() throws GameActionException {
        int longestDist = 0;
        RobotInfo toFollow = null;
        for (RobotInfo friendly : rc.senseNearbyRobots(FOLLOWRADIUS, team)) {
            if (friendly.getType() == RobotType.LAUNCHER &&friendly.getLocation().distanceSquaredTo(target)>longestDist && friendly.getLocation().distanceSquaredTo(target)>GAURDRADIUS) {
                longestDist=friendly.getLocation().distanceSquaredTo(target);
                toFollow = friendly;
            }
        }
        return toFollow;
    }

    //TODO this is some placeholder code I put in
    public MapLocation GetNewTarget(){
        SharedArrayStack WellLocations =comms.getWellLocationsStack();

        if((val/3)%2==0){
            return target;
        }else{
             return target;
        }

    }
}
