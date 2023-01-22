package gopher4.robots;

import battlecode.common.*;

public class Carrier extends Robot {

    private int adamantiumHeld;
    private int manaHeld;
    private int elixirHeld;



    public Carrier(RobotController rc) throws GameActionException {
        super(rc);
    }

    public void run() throws GameActionException, IllegalAccessException {
        super.run();
        updateInternalVariables();


    }

    private void updateInternalVariables() {
        adamantiumHeld = rc.getResourceAmount(ResourceType.ADAMANTIUM);
        manaHeld = rc.getResourceAmount(ResourceType.MANA);
        elixirHeld = rc.getResourceAmount(ResourceType.ELIXIR);
    }

}
