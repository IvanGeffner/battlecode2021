package seventeen;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public abstract class MyRobot {

    final int EXPLORE_2_BYTECODE_REMAINING = 2000;


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
        path.initTurn();
        explore.initTurn();
        comm.readMessages();
        comm.setFlag();
    }

    void endTurn(){
        comm.run();
        explore.initialize();
        explore.markSeen(); //maybe end of turn?
    }

    void explore(){
        path.move(explore.getExploreTarget());
    }
    void explore2(){
        path.move(explore.getExplore2Target(EXPLORE_2_BYTECODE_REMAINING));
    }


    boolean surroundEnemyHQ(){
        MapLocation loc = comm.getClosestEnemyEC();
        if (loc == null) return false;
        int d = rc.getLocation().distanceSquaredTo(loc);
        d = Math.min(d, Util.SAFETY_DISTANCE_ENEMY_EC);
        boolean[] imp = new boolean[directions.length];
        for (int i = directions.length; i-- > 0; ){
            MapLocation newLoc = rc.getLocation().add(directions[i]);
            if (newLoc.distanceSquaredTo(loc) <= d) imp[i] = true;
        }
        path.setImpassable(imp);
        path.move(loc);
        return true;
    }

    boolean surroundOurHQ(){
        MapLocation loc = comm.getClosestEC();
        if (loc == null) return false;
        int d = rc.getLocation().distanceSquaredTo(loc);
        d = Math.min(d, Util.SAFETY_DISTANCE_OUR_HQ);
        boolean[] imp = new boolean[directions.length];
        for (int i = directions.length; i-- > 0; ){
            MapLocation newLoc = rc.getLocation().add(directions[i]);
            if (newLoc.distanceSquaredTo(loc) <= d) imp[i] = true;
        }
        path.setImpassable(imp);
        path.move(loc);
        return true;
    }


}
