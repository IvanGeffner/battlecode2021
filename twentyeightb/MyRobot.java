package twentyeightb;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

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
    BFS bfs;
    Explore explore;
    Communication comm;
    int creationRound;


    public MyRobot(RobotController rc){
        this.rc = rc;
        comm = new Communication(rc);
        explore = new Explore(rc, comm);
        creationRound = rc.getRoundNum();
        switch(rc.getType()){
            case MUCKRAKER:
                bfs = new BFSMuckraker(rc, explore);
                break;
            default:
                bfs = new BFSPolitician(rc, explore);
                break;
        }
    }

    abstract void play();

    void initTurn(){
        comm.init();
        bfs.initTurn();
        explore.initTurn();
        //if (rc.getType() == RobotType.SLANDERER) System.err.println("Bytecode after explore " + Clock.getBytecodeNum());
        comm.readMessages();
        //if (rc.getType() == RobotType.SLANDERER) System.err.println("Bytecode after reading " + Clock.getBytecodeNum());
        comm.setFlag();
        //if (rc.getType() == RobotType.SLANDERER) System.err.println("Bytecode after flag " + Clock.getBytecodeNum());
        //comm.debugDraw();
    }

    void endTurn(){
        //comm.run();
        explore.initialize();
        if (rc.getType() != RobotType.SLANDERER) explore.markSeen();
    }

    boolean surroundEnemyHQ(){
        MapLocation loc = comm.getClosestEnemyEC();
        return moveSafely(loc, Util.SAFETY_DISTANCE_ENEMY_EC);
    }

    boolean surroundOurHQ(int rad){
        MapLocation loc = comm.getClosestEC();
        return moveSafely(loc, rad);
    }

    boolean moveSafely(MapLocation loc, int rad){
        if (loc == null) return false;
        int d = rc.getLocation().distanceSquaredTo(loc);
        d = Math.min(d, rad);
        boolean[] imp = new boolean[directions.length];
        boolean greedy = rc.getType() == RobotType.SLANDERER;
        for (int i = directions.length; i-- > 0; ){
            MapLocation newLoc = rc.getLocation().add(directions[i]);
            if (newLoc.distanceSquaredTo(loc) <= d){
                imp[i] = true;
                greedy = true;
            }
        }
        bfs.path.setImpassable(imp);
        bfs.move(loc, greedy);
        return true;
    }


}
