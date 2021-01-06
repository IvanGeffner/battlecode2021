package exploretest;

import battlecode.common.Direction;
import battlecode.common.RobotController;

public abstract class MyRobot {

    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    RobotController rc;
    Pathfinding path;
    Explore explore;
    int creationRound;


    public MyRobot(RobotController rc){
        this.rc = rc;
        path = new Pathfinding(rc);
        explore = new Explore(rc);
        creationRound = rc.getRoundNum();
    }

    abstract void play();

    void initTurn(){
        explore.checkBounds();
    }

    void endTurn(){
        explore.initialize();
        explore.markSeen(); //maybe end of turn?
    }


}
