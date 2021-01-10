package ecobot;

import battlecode.common.*;

public class Slanderer extends MyRobot {

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

    MapLocation farLocation = null;
    double farValue = 0;

    static final int MAX_MAP_SIZE = GameConstants.MAP_MAX_HEIGHT;
    static final int MAX_MAP_SIZE2 = 2*MAX_MAP_SIZE;

    public Slanderer(RobotController rc){
        super(rc);
    }

    public void play(){
        flee();
        moveFar();
        moveExplore();
        computeFarLocation();
    }

    void flee(){
        try {
            if (rc.getCooldownTurns() >= 1) return;
            int minDist[] = new int[directions.length];
            MapLocation myLoc = rc.getLocation();
            RobotInfo[] robots = rc.senseNearbyRobots(RobotType.SLANDERER.sensorRadiusSquared, rc.getTeam().opponent());
            for (RobotInfo r : robots) {
                if (r.getType() != RobotType.MUCKRAKER) continue;
                MapLocation loc = r.getLocation();
                for (int i = directions.length; i-- > 0; ) {
                    int d = myLoc.add(directions[i]).distanceSquaredTo(loc) + 1;
                    int md = minDist[i];
                    if (md == 0 || md > d) {
                        minDist[i] = d;
                    }
                }
            }

            Direction toMove = null;
            int maxDist = 0;
            for (int i = directions.length; i-- > 0; ) {
                Direction dir = directions[i];
                if (!rc.canMove(dir)) continue;
                int d = minDist[i];
                if (d == 0) return;
                if (toMove == null || d > maxDist) {
                    maxDist = d;
                    toMove = dir;
                }
            }
            if (toMove != null) rc.move(toMove);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    void moveFar(){
        if (rc.getCooldownTurns() >= 1) return;
        if (farLocation != null) path.move(farLocation);
    }

    void moveExplore(){
        MapLocation loc = explore.getExploreTarget();
        if (loc != null) path.move(loc);
    }

    void computeFarLocation(){
        int X = rc.getLocation().x;
        int Y = rc.getLocation().y;
        int cont = 5;
        while (Clock.getBytecodesLeft() > 2000){
            if (--cont <= 0) return;
            int dx = (int)(Math.random()*MAX_MAP_SIZE2 - MAX_MAP_SIZE);
            int dy = (int)(Math.random()*MAX_MAP_SIZE2 - MAX_MAP_SIZE);
            MapLocation loc = new MapLocation(X + dx, Y + dy);
            checkLoc(loc);
        }
    }

    void checkLoc(MapLocation loc){
        int dAlly = comm.getMinDistToEC(loc, rc.getTeam().ordinal());
        if (dAlly < 0) return;
        int dEnemy = comm.getMinDistToEC(loc, rc.getTeam().opponent().ordinal());
        if (dEnemy < 0) return;
        int fValue = dEnemy - dAlly;
        if (fValue > farValue){
            farValue = fValue;
            farLocation = loc;
        }
    }

}
