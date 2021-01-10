package ecobot;

import battlecode.common.*;

public class Explore {

    static Direction[] dirPath;

    static final int MAX_MAP_SIZE = 64;
    static final int MAX_MAP_SIZE_SQ = MAX_MAP_SIZE*MAX_MAP_SIZE;
    static final int MAX_MAP_SIZE2 = 128;
    boolean[][] visited = new boolean[MAX_MAP_SIZE][];
    RobotController rc;
    int senseRadius;
    boolean initialized = false;
    int initRow = 0;
    final int initBytecodeLeft = 200;
    final int visitedBytecodeLeft = 100;

    MapLocation exploreTarget = null;

    Communication comm;

    Explore (RobotController rc, Communication comm){
        this.rc = rc;
        this.comm = comm;
        senseRadius = rc.getType().sensorRadiusSquared;
        fillDirPath();
        Math.random(); //for some reason the first entry is buggy...
    }

    void initTurn(){
        checkBounds();
        checkRobots();
    }

    void checkRobots(){
        RobotInfo[] robots = rc.senseNearbyRobots();
        for (RobotInfo r : robots){
            if (r.getType() == RobotType.ENLIGHTENMENT_CENTER){
                comm.exploredEC(r);
                continue;
            }
            if (r.getTeam() == rc.getTeam()){
                comm.exploredNonEC(r);
                continue;
            }
        }
        if (rc.getType() == RobotType.ENLIGHTENMENT_CENTER){
            comm.exploredECSelf();
        }
    }

    boolean inTheMap(MapLocation loc){
        if (comm.getBound(comm.ubX) != null && loc.x >= comm.getBound(comm.ubX)) return false;
        if (comm.getBound(comm.lbX) != null && loc.x <= comm.getBound(comm.lbX)) return false;
        if (comm.getBound(comm.ubY) != null && loc.y >= comm.getBound(comm.ubY)) return false;
        if (comm.getBound(comm.lbY) != null && loc.y <= comm.getBound(comm.lbY)) return false;
        return true;
    }

    void initialize(){
        if (initialized){
            rc.setIndicatorDot(rc.getLocation(), 255, 0, 0);
            return;
        }
        while(initRow < MAX_MAP_SIZE){
            if (Clock.getBytecodesLeft() < initBytecodeLeft) return;
            visited[initRow++] = new boolean[MAX_MAP_SIZE];
        }
        initialized = true;
    }

    void emergencyTarget(int tries){
        MapLocation myLoc = rc.getLocation();
        if (exploreTarget != null && myLoc.distanceSquaredTo(exploreTarget) > senseRadius) return;
        int X = rc.getLocation().x;
        int Y = rc.getLocation().y;
        for (int i = tries; i-- > 0; ){
            int dx = (int)(Math.random()*MAX_MAP_SIZE2 - MAX_MAP_SIZE);
            int dy = (int)(Math.random()*MAX_MAP_SIZE2 - MAX_MAP_SIZE);
            exploreTarget = new MapLocation(X+dx,Y+dy);
            if (myLoc.distanceSquaredTo(exploreTarget) > senseRadius) return;
        }
        exploreTarget = null;
    }

    MapLocation getExploreTarget(){
        if (!initialized) emergencyTarget(10);
        else getNewTarget(10);
        return exploreTarget;
    }

    boolean hasVisited (MapLocation loc){
        if (!initialized) return false;
        return visited[loc.x%MAX_MAP_SIZE][loc.y%MAX_MAP_SIZE];
    }

    void getNewTarget(int tries){
        if (exploreTarget != null && !hasVisited(exploreTarget) && inTheMap(exploreTarget)) return;
        int X = rc.getLocation().x;
        int Y = rc.getLocation().y;
        for (int i = tries; i-- > 0; ){
            int dx = (int)(Math.random()*MAX_MAP_SIZE2 - MAX_MAP_SIZE);
            int dy = (int)(Math.random()*MAX_MAP_SIZE2 - MAX_MAP_SIZE);
            exploreTarget = new MapLocation(X+dx,Y+dy);
            if (!hasVisited(exploreTarget) && inTheMap(exploreTarget)) return;
        }
        exploreTarget = null;
    }

    void checkBounds(){
        checkNorth();
        checkSouth();
        checkEast();
        checkWest();
    }

    void markSeen(){
        if (!initialized) return;
        try{
            MapLocation loc = rc.getLocation();
            for (int i = dirPath.length; i-- > 0; ) {
                if (Clock.getBytecodesLeft() < visitedBytecodeLeft) return;
                loc = loc.add(dirPath[i]);
                if (rc.onTheMap(loc)) visited[loc.x%MAX_MAP_SIZE][loc.y%MAX_MAP_SIZE] = true; //encoded
            }
        } catch (Throwable e){
            e.printStackTrace();
        }
    }

    int encode (MapLocation loc){
        return (loc.x*MAX_MAP_SIZE + loc.y)%MAX_MAP_SIZE_SQ;
    }

    void fillDirPath() {
        switch (senseRadius) {
            case 20:
                dirPath = new Direction[]{Direction.NORTHWEST, Direction.NORTHWEST, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.NORTHEAST, Direction.NORTHEAST, Direction.EAST, Direction.EAST, Direction.EAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTHWEST, Direction.SOUTHWEST, Direction.WEST, Direction.WEST, Direction.WEST, Direction.NORTHWEST, Direction.NORTHWEST, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.EAST, Direction.EAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.WEST, Direction.WEST, Direction.NORTHWEST, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.EAST, Direction.EAST, Direction.EAST, Direction.EAST, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.WEST, Direction.WEST, Direction.WEST, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.EAST, Direction.EAST, Direction.SOUTH, Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.CENTER};
                break;
            case 25:
                dirPath = new Direction[]{Direction.NORTHWEST, Direction.WEST, Direction.WEST, Direction.NORTHWEST, Direction.NORTH, Direction.NORTH, Direction.NORTHWEST, Direction.NORTHEAST, Direction.NORTH, Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.EAST, Direction.NORTHEAST, Direction.SOUTHEAST, Direction.EAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTH, Direction.SOUTHEAST, Direction.SOUTHWEST, Direction.SOUTH, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.WEST, Direction.WEST, Direction.NORTHWEST, Direction.WEST, Direction.WEST, Direction.NORTH, Direction.NORTH, Direction.NORTHWEST, Direction.NORTHEAST, Direction.NORTH, Direction.NORTH, Direction.EAST, Direction.EAST, Direction.NORTHEAST, Direction.SOUTHEAST, Direction.EAST, Direction.EAST, Direction.SOUTH, Direction.SOUTH, Direction.SOUTHEAST, Direction.SOUTHWEST, Direction.SOUTH, Direction.SOUTH, Direction.WEST, Direction.WEST, Direction.WEST, Direction.NORTHWEST, Direction.WEST, Direction.NORTH, Direction.NORTHWEST, Direction.NORTHEAST, Direction.NORTH, Direction.EAST, Direction.NORTHEAST, Direction.SOUTHEAST, Direction.EAST, Direction.SOUTH, Direction.SOUTHEAST, Direction.SOUTHWEST, Direction.SOUTH, Direction.WEST, Direction.WEST, Direction.NORTHWEST, Direction.NORTHWEST, Direction.NORTHEAST, Direction.NORTHEAST, Direction.SOUTHEAST, Direction.SOUTHEAST, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST, Direction.NORTHEAST, Direction.SOUTHEAST, Direction.WEST, Direction.CENTER};
                break;
            case 30:
                dirPath = new Direction[]{Direction.NORTHWEST, Direction.NORTHWEST, Direction.NORTHWEST, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.NORTHEAST, Direction.NORTHEAST, Direction.NORTHEAST, Direction.EAST, Direction.EAST, Direction.EAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTHEAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTHWEST, Direction.SOUTHWEST, Direction.SOUTHWEST, Direction.WEST, Direction.WEST, Direction.WEST, Direction.NORTHWEST, Direction.NORTHWEST, Direction.NORTHWEST, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.NORTHEAST, Direction.NORTHEAST, Direction.EAST, Direction.EAST, Direction.EAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTHWEST, Direction.SOUTHWEST, Direction.WEST, Direction.WEST, Direction.WEST, Direction.NORTHWEST, Direction.NORTHWEST, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.EAST, Direction.EAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.WEST, Direction.WEST, Direction.NORTHWEST, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.EAST, Direction.EAST, Direction.EAST, Direction.EAST, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.WEST, Direction.WEST, Direction.WEST, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.EAST, Direction.EAST, Direction.SOUTH, Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.CENTER};
                break;
            default:
                dirPath = new Direction[0];
        }
    }


    void checkNorth(){
        try {
            if (comm.getBound(comm.ubY) != null) return;
            MapLocation loc = rc.getLocation().add(Direction.NORTH);
            if (!rc.onTheMap(loc)){
                comm.exploredNorth(loc.y);
                return;
            }
            loc = loc.add(Direction.NORTH);
            if (!rc.onTheMap(loc)){
                comm.exploredNorth(loc.y);
                return;
            }
            loc = loc.add(Direction.NORTH);
            if (!rc.onTheMap(loc)){
                comm.exploredNorth(loc.y);
                return;
            }
            loc = loc.add(Direction.NORTH);
            if (!rc.onTheMap(loc)){
                comm.exploredNorth(loc.y);
                return;
            }
            loc = loc.add(Direction.NORTH);
            if (senseRadius < 25) return;
            if (!rc.onTheMap(loc)){
                comm.exploredNorth(loc.y);
                return;
            }
            loc = loc.add(Direction.NORTH);
            if (senseRadius < 36) return;
            if (!rc.onTheMap(loc)){
                comm.exploredNorth(loc.y);
                return;
            }
        } catch (Throwable e){
            e.printStackTrace();
        }

    }

    void checkSouth(){
        try {
            if (comm.getBound(comm.lbY) != null) return;
            MapLocation loc = rc.getLocation().add(Direction.SOUTH);
            if (!rc.onTheMap(loc)){
                comm.exploredSouth(loc.y);
                return;
            }
            loc = loc.add(Direction.SOUTH);
            if (!rc.onTheMap(loc)){
                comm.exploredSouth(loc.y);
                return;
            }
            loc = loc.add(Direction.SOUTH);
            if (!rc.onTheMap(loc)){
                comm.exploredSouth(loc.y);
                return;
            }
            loc = loc.add(Direction.SOUTH);
            if (!rc.onTheMap(loc)){
                comm.exploredSouth(loc.y);
                return;
            }
            loc = loc.add(Direction.SOUTH);
            if (senseRadius < 25) return;
            if (!rc.onTheMap(loc)){
                comm.exploredSouth(loc.y);
                return;
            }
            loc = loc.add(Direction.SOUTH);
            if (senseRadius < 36) return;
            if (!rc.onTheMap(loc)){
                comm.exploredSouth(loc.y);
                return;
            }
        } catch (Throwable e){
            e.printStackTrace();
        }

    }

    void checkEast(){
        try {
            if (comm.getBound(comm.ubX) != null) return;
            MapLocation loc = rc.getLocation().add(Direction.EAST);
            if (!rc.onTheMap(loc)){
                comm.exploredEast(loc.x);
                return;
            }
            loc = loc.add(Direction.EAST);
            if (!rc.onTheMap(loc)){
                comm.exploredEast(loc.x);
                return;
            }
            loc = loc.add(Direction.EAST);
            if (!rc.onTheMap(loc)){
                comm.exploredEast(loc.x);
                return;
            }
            loc = loc.add(Direction.EAST);
            if (!rc.onTheMap(loc)){
                comm.exploredEast(loc.x);
                return;
            }
            loc = loc.add(Direction.EAST);
            if (senseRadius < 25) return;
            if (!rc.onTheMap(loc)){
                comm.exploredEast(loc.x);
                return;
            }
            loc = loc.add(Direction.EAST);
            if (senseRadius < 36) return;
            if (!rc.onTheMap(loc)){
                comm.exploredEast(loc.x);
                return;
            }
        } catch (Throwable e){
            e.printStackTrace();
        }

    }

    void checkWest(){
        try {
            if (comm.getBound(comm.lbX) != null) return;
            MapLocation loc = rc.getLocation().add(Direction.WEST);
            if (!rc.onTheMap(loc)){
                comm.exploredWest(loc.x);
                return;
            }
            loc = loc.add(Direction.WEST);
            if (!rc.onTheMap(loc)){
                comm.exploredWest(loc.x);
                return;
            }
            loc = loc.add(Direction.WEST);
            if (!rc.onTheMap(loc)){
                comm.exploredWest(loc.x);
                return;
            }
            loc = loc.add(Direction.WEST);
            if (!rc.onTheMap(loc)){
                comm.exploredWest(loc.x);
                return;
            }
            loc = loc.add(Direction.WEST);
            if (senseRadius < 25) return;
            if (!rc.onTheMap(loc)){
                comm.exploredWest(loc.x);
                return;
            }
            loc = loc.add(Direction.WEST);
            if (senseRadius < 36) return;
            if (!rc.onTheMap(loc)){
                comm.exploredWest(loc.x);
                return;
            }
        } catch (Throwable e){
            e.printStackTrace();
        }

    }

}
