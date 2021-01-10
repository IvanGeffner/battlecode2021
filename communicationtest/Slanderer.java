package communicationtest;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Slanderer extends MyRobot {


    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
            Direction.CENTER
    };

    public Slanderer(RobotController rc){
        super(rc);
    }

    public void play(){
        MapLocation loc = explore.getExploreTarget();
        path.move(loc);
        if (loc != null){
            //System.err.println("My target is " + loc);
            rc.setIndicatorLine(loc, rc.getLocation(),0, 255, 0);
        }
    }

}
