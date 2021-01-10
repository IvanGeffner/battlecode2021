package ecobot;

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
    Communication comm;
    int creationRound;


    public MyRobot(RobotController rc){
        this.rc = rc;
        path = new Pathfinding(rc);
        comm = new Communication(rc);
        explore = new Explore(rc, comm);
        creationRound = rc.getRoundNum();
    }

    abstract void play();

    void initTurn(){
        explore.initTurn();
        comm.readMessages();
        comm.setFlag();
        comm.debugDraw();
    }

    void endTurn(){
        comm.run();
        explore.initialize();
        explore.markSeen(); //maybe end of turn?
    }


}
