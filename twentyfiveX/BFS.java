package twentyfiveX;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public abstract class BFS {

    final int BYTECODE_REMAINING = 1200;
    final int GREEDY_TURNS = 4;

    Pathfinding path;
    Explore explore;
    static RobotController rc;
    MapTracker mapTracker = new MapTracker();

    int turnsGreedy = 0;

    MapLocation currentTarget = null;




    BFS(RobotController rc, Explore explore){
        this.rc = rc;
        this.explore = explore;
        this.path = new Pathfinding(rc, explore);
    }

    void reset(){
        turnsGreedy = 0;
        mapTracker.reset();
    }

    void update(MapLocation target){
        if (currentTarget == null || target.distanceSquaredTo(currentTarget) > 0){
            reset();
        } else --turnsGreedy;
        currentTarget = target;
        mapTracker.add(rc.getLocation());
    }

    void activateGreedy(){
        turnsGreedy = GREEDY_TURNS;
    }

    void initTurn(){
        path.initTurn();
    }

    void move(MapLocation target){
        move(target, false);
    }

    void move(MapLocation target, boolean greedy){
        if (target == null) return;
        if (rc.getCooldownTurns() >= 1) return;
        if (rc.getLocation().distanceSquaredTo(target) == 0) return;

        update(target);

        if (!greedy && turnsGreedy <= 0){

            //System.err.println("Using bfs");
            Direction dir = getBestDir(target);
            if (dir != null && !mapTracker.check(rc.getLocation().add(dir))){
                explore.move(dir);
                return;
            } else activateGreedy();
        }

        if (Clock.getBytecodesLeft() >= BYTECODE_REMAINING){
            //System.err.println("Using greedy");
            path.move(target);
            --turnsGreedy;
        }

    }

    abstract Direction getBestDir(MapLocation target);


}
