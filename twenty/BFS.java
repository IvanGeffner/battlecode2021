package twenty;

import battlecode.common.*;

import java.nio.file.Path;

public abstract class BFS {

    final int BYTECODE_REMAINING = 2000;

    Pathfinding path;
    Explore explore;
    static RobotController rc;




    BFS(RobotController rc, Explore explore){
        this.rc = rc;
        this.explore = explore;
        this.path = new Pathfinding(rc, explore);
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

        if (greedy){
            path.move(target);
            return;
        }

        Direction dir = getBestDir(target);
        if (dir != null){
            explore.move(dir);
        }
    }

    abstract Direction getBestDir(MapLocation target);


}
