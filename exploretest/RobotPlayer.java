package exploretest;

import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        MyRobot robot;
        switch(rc.getType()) {
            case ENLIGHTENMENT_CENTER:
                robot = new Enlightment(rc);
                break;
            case POLITICIAN:
                robot = new Politician(rc);
                break;
            case SLANDERER:
                robot = new Slanderer(rc);
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