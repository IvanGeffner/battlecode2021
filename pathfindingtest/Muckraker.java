package pathfindingtest;

import battlecode.common.*;

public class Muckraker extends MyRobot {

    MapLocation loc = new MapLocation(10015, 23939);

    public Muckraker(RobotController rc){
        super(rc);
    }

    public void play(){
        path.move(loc);
    }

    public void initTurn(){

    }

    public void endTurn(){

    }

}
