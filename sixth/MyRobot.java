package sixth;

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
        //if (rc.getType() == RobotType.SLANDERER) System.err.println("Bytecode after explore " + Clock.getBytecodeNum());
        comm.readMessages();
        //if (rc.getType() == RobotType.SLANDERER) System.err.println("Bytecode after reading " + Clock.getBytecodeNum());
        comm.setFlag();
        //if (rc.getType() == RobotType.SLANDERER) System.err.println("Bytecode after flag " + Clock.getBytecodeNum());
        //comm.debugDraw();
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

    boolean berserk(){
        return explore.conquerorTurns >= 100 && rc.getRoundNum() > 700;
    }

    boolean surroundEnemyHQ(){
        MapLocation loc = comm.getClosestEnemyEC();
        if (loc == null) return false;
        int d = rc.getLocation().distanceSquaredTo(loc);
        d = Math.min(d, Util.SAFETY_DISTANCE_EC);
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
        d = Math.min(d, Util.SAFETY_DISTANCE_EC);
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
