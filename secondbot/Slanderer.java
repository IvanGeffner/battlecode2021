package secondbot;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Slanderer extends MyRobot {

    public Slanderer(RobotController rc){
        super(rc);
    }

    public void play(){
        MapLocation loc = explore.getExploreTarget();
        path.move(loc);
        if (loc != null){
            //System.err.println("My target is " + loc);
            rc.setIndicatorLine(loc, rc.getLocation(),0, 0, 0);
        }
    }

}
