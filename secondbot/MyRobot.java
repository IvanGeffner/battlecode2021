package secondbot;

import battlecode.common.*;

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
            Direction.CENTER
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
        if (rc.getType() == RobotType.SLANDERER) System.err.println("Bytecode after explore " + Clock.getBytecodeNum());
        comm.readMessages();
        if (rc.getType() == RobotType.SLANDERER) System.err.println("Bytecode after reading " + Clock.getBytecodeNum());
        comm.setFlag();
        if (rc.getType() == RobotType.SLANDERER) System.err.println("Bytecode after flag " + Clock.getBytecodeNum());
        comm.debugDraw();
    }

    void endTurn(){
        comm.run();
        explore.initialize();
        explore.markSeen(); //maybe end of turn?
    }

    void explore(){
        path.move(explore.getExploreTarget());
    }

    void suicide(){
        double a = Math.sqrt(-1);
    }


}
