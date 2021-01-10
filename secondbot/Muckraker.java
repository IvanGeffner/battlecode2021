package secondbot;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Muckraker extends MyRobot {

    public Muckraker(RobotController rc){
        super(rc);
    }

    public void play(){
        MapLocation loc = explore.getExploreTarget();
        path.move(loc);
        if (loc != null){
        }
    }

}
