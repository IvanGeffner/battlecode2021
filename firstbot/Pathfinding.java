package firstbot;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Pathfinding {

    RobotController rc;

    Pathfinding(RobotController rc){
        this.rc = rc;
    }

    public void move(MapLocation loc){
        try {
            if (loc == null) return;
            //TODO this is dumb
            MapLocation myLoc = rc.getLocation();
            if (myLoc.distanceSquaredTo(loc) <= 0) return;
            Direction dir = myLoc.directionTo(loc);
            for (int i = 0; i < 8; ++i) {
                if (rc.canMove(dir)) {
                    rc.move(dir);
                    return;
                }
                dir = dir.rotateLeft();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }



}
