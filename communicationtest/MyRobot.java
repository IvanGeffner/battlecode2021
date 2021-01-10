package communicationtest;

import battlecode.common.Direction;
import battlecode.common.RobotController;

public abstract class MyRobot {

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
