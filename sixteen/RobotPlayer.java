package sixteen;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public strictfp class RobotPlayer {
    static RobotController rc;

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        MyRobot robot;
        switch(rc.getType()) {
            case ENLIGHTENMENT_CENTER:
                robot = new Enlightment(rc);
                break;
            case SLANDERER:
            case POLITICIAN:
                robot = new Politician(rc);
                break;
            case MUCKRAKER:
            default:
                robot = new Muckraker(rc);
                break;
        }

        while(true){
            robot.initTurn();
            robot.play();
            robot.endTurn();
            Clock.yield();
        }
    }
}
