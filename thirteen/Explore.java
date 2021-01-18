package thirteen;

import battlecode.common.*;

public class Explore {

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

    static Direction[] dirPath;

    static final int MAX_MAP_SIZE = 64;
    static final int MAX_MAP_SIZE_SQ = MAX_MAP_SIZE*MAX_MAP_SIZE;
    static final int MAX_MAP_SIZE2 = 128;
    boolean[][] visited = new boolean[MAX_MAP_SIZE][];
    //int[][] muckrakerLocations = new int[MAX_MAP_SIZE][];
    RobotController rc;
    int senseRadius;
    boolean initialized = false;
    int initRow = 0;
    final int initBytecodeLeft = 300;
    final int visitedBytecodeLeft = 100;

    MapLocation exploreTarget = null;

    Direction exploreDir = Direction.CENTER;
    double angle = 0;
    final double angleOffset = 0.5;
    Boolean rotateLeft = null;

    int conquerorTurns = 0;

    Communication comm;

    final int bytecodeUsed = 2500;

    MapLocation closestMuckraker = null;
    int closestMuckrakerSeenRound = 0;

    Explore (RobotController rc, Communication comm){
        this.rc = rc;
        this.comm = comm;
        senseRadius = rc.getType().sensorRadiusSquared;
        fillDirPath();
        Math.random(); //for some reason the first entry is buggy...
        getExploreDir();
    }

    void initTurn(){
        checkBounds();
        checkRobots();
        computeConquerorTurns();
    }


    void getExploreDir(){
        if (rc.getType() == RobotType.ENLIGHTENMENT_CENTER) return;
        RobotInfo closestEC = null;
        MapLocation myLoc = rc.getLocation();
        RobotInfo[] robots = rc.senseNearbyRobots(RobotType.ENLIGHTENMENT_CENTER.actionRadiusSquared, rc.getTeam());
        for (RobotInfo r : robots){
            if (r.getType() != RobotType.ENLIGHTENMENT_CENTER) continue;
            int d = myLoc.distanceSquaredTo(r.getLocation());
            if (closestEC == null || d < myLoc.distanceSquaredTo(closestEC.getLocation())){
                closestEC = r;
            }
        }
        if (closestEC != null) exploreDir = closestEC.getLocation().directionTo(myLoc);
        if (exploreDir != Direction.CENTER){
            angle = Math.atan2(exploreDir.dy, exploreDir.dx);
            angle += (Math.random()*2.0 - 1)*angleOffset;
        }
    }

    void computeConquerorTurns(){
        if (comm.everythingCaptured()) {
            conquerorTurns++;
        }
        else conquerorTurns = 0;
    }

    void checkRobots(){
        if (rc.getType() == RobotType.ENLIGHTENMENT_CENTER){
            comm.exploredECSelf();
        }
        RobotInfo[] robots = rc.senseNearbyRobots();
        for (RobotInfo r : robots){
            if (Clock.getBytecodeNum() > bytecodeUsed) return;
            if (r.getType() == RobotType.ENLIGHTENMENT_CENTER){
                comm.exploredEC(r);
                continue;
            }
            if (r.getTeam() == rc.getTeam()){
                comm.exploredNonEC(r);
                continue;
            } else{
                if (r.getType() == RobotType.MUCKRAKER){
                    if (closestMuckraker == null || closestMuckrakerSeenRound < rc.getRoundNum()){
                        closestMuckrakerSeenRound = rc.getRoundNum();
                        closestMuckraker = r.getLocation();
                    } else{
                        if (closestMuckraker.distanceSquaredTo(rc.getLocation()) > r.getLocation().distanceSquaredTo(rc.getLocation())){
                            closestMuckrakerSeenRound = rc.getRoundNum();
                            closestMuckraker = r.getLocation();
                        }
                    }
                }
            }
        }
        if (closestMuckraker != null && closestMuckrakerSeenRound == rc.getRoundNum()){
            comm.reportMuckraker(closestMuckraker);
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
            //rc.setIndicatorDot(rc.getLocation(), 255, 0, 0);
            return;
        }
        while(initRow < MAX_MAP_SIZE){
            if (Clock.getBytecodesLeft() < initBytecodeLeft) return;
            visited[initRow] = new boolean[MAX_MAP_SIZE];
            //muckrakerLocations[initRow] = new int[MAX_MAP_SIZE];
            initRow++;
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

    MapLocation getExplore2Target(int minBytecodeRemaining){
        MapLocation myLoc = rc.getLocation();
        RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, rc.getTeam());
        int[] minDists = new int[directions.length];
        for (RobotInfo r : robots){
            if (Clock.getBytecodesLeft() < minBytecodeRemaining) break;
            for (int i = directions.length; i-- > 0; ){
                int dist = r.getLocation().distanceSquaredTo(myLoc.add(directions[i]));
                int mindist = minDists[i];
                if (mindist == 0 || mindist > dist) minDists[i] = dist;
            }
        }
        Direction dir = Direction.CENTER;
        int maxDist = minDists[Direction.CENTER.ordinal()];
        for (int i = directions.length; i-- > 0; ){
            if (!rc.canMove(directions[i])) continue;
            if (maxDist < minDists[i]){
                dir = directions[i];
                maxDist = minDists[i];
            }
        }
        return rc.getLocation().add(dir);
    }

    MapLocation getExplore3Target(){
        checkDirection();
        double x = rc.getLocation().x, y = rc.getLocation().y;
        x += Math.cos(angle)*100.0;
        y += Math.sin(angle)*100.0;
        return new MapLocation((int)x, (int)y);
    }

    void checkDirection(){
        if (!movingOutOfMap(exploreDir)) return;
        System.err.println("Checking new direction!");
        double minCos = 0;
        Direction newDir = null;
        for (Direction dir : directions){
            if (dir == Direction.CENTER) continue;
            if (dir == exploreDir) continue;
            if (movingOutOfMap(dir)) continue;
            double cos = cosAngle(dir, exploreDir);
            if (newDir == null || cos < minCos){
                minCos = cos;
                newDir = dir;
            }
        }
        if (newDir != null){
            System.err.println("Setting from direction " + exploreDir + " to direction " + newDir);
            exploreDir = newDir;
            angle = Math.atan2(exploreDir.dy, exploreDir.dx);
        }
    }

    //todo this may be buggy
    double cosAngle(Direction A, Direction B){
        int a = A.ordinal(), b = B.ordinal();
        if (a > b){
            int aux = b;
            b = a;
            a = aux;
        }
        return Math.min(b - a, a + 8 - b);
    }

    boolean movingOutOfMap(Direction dir){
        try {
            MapLocation loc = rc.getLocation().add(dir);
            if (!rc.onTheMap(loc)) {
                return true;
            }
            loc = loc.add(dir);
            if (!rc.onTheMap(loc)) {
                return true;
            }
            loc = loc.add(dir);
            if (!rc.onTheMap(loc)) {
                return true;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
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
