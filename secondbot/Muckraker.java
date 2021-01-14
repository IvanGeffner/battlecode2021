package secondbot;

import battlecode.common.*;
public class Muckraker extends MyRobot {


    final int EXPLORER_1_TYPE = 0;
    final int EXPLORER_2_TYPE = 1;
    final int ATTACKER_TYPE = 2;
    final int EXPLORE_2_BYTECODE_REMAINING = 2000;

    int myType;
    boolean moved = false;

    Team myTeam, enemyTeam;

    public Muckraker(RobotController rc){
        super(rc);
        myType = (int)(Math.random()*3.0);
        myTeam = rc.getTeam();
        enemyTeam = myTeam.opponent();
    }

    public void play(){
        moved = false;
        tryKill();
        if (moved) return;
        MapLocation loc = getTarget();
        if (loc != null) path.move(loc);
    }

    void tryKill(){
        try {
            RobotInfo[] robots = rc.senseNearbyRobots(RobotType.MUCKRAKER.actionRadiusSquared, enemyTeam);
            RobotInfo bestSlanderer = null;
            for (RobotInfo r : robots){
                if (r.getType() == RobotType.SLANDERER){
                    if (rc.canExpose(r.getID())){
                       if (bestSlanderer == null || bestSlanderer.getConviction() < r.getConviction()) bestSlanderer = r;
                    }
                }
            }
            if (bestSlanderer != null){
                if (rc.canExpose(bestSlanderer.getID())) rc.expose(bestSlanderer.getID());
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    MapLocation getTarget(){
        MapLocation loc = getClosestSlanderer();
        if (loc != null) return loc;

        if (myType == ATTACKER_TYPE){
            loc = comm.getClosestEnemyEC();
            if (loc != null) return loc;
        }

        if (myType == EXPLORER_2_TYPE){
            loc = getExplore2Target();
            if (loc != null) return loc;
        }

        loc = explore.getExploreTarget();
        if (loc != null) return loc;

        if (myType != ATTACKER_TYPE){
            loc = comm.getClosestEnemyEC();
            if (loc != null) return loc;
        }

        loc = getExplore2Target();

        return loc;
    }

    MapLocation getExplore2Target(){
        MapLocation myLoc = rc.getLocation();
        RobotInfo[] robots = rc.senseNearbyRobots(RobotType.MUCKRAKER.sensorRadiusSquared, myTeam);
        int[] minDists = new int[directions.length];
        for (RobotInfo r : robots){
            if (Clock.getBytecodesLeft() < EXPLORE_2_BYTECODE_REMAINING) break;
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

    MapLocation getClosestSlanderer(){
        try {
            MapLocation myLoc = rc.getLocation();
            RobotInfo[] robots = rc.senseNearbyRobots(RobotType.MUCKRAKER.sensorRadiusSquared, enemyTeam);
            RobotInfo bestSlanderer = null;
            int slandererDist = 0;
            for (RobotInfo r : robots) {
                if (r.getType() != RobotType.SLANDERER) continue;
                if (bestSlanderer == null){
                    bestSlanderer = r;
                    slandererDist = myLoc.distanceSquaredTo(r.getLocation());
                    continue;
                }
                int d = myLoc.distanceSquaredTo(r.getLocation());
                if (d < slandererDist){
                    bestSlanderer = r;
                    slandererDist = d;
                    continue;
                }
                if (d > slandererDist) continue;
                if (r.getConviction() < bestSlanderer.getConviction()){
                    bestSlanderer = r;
                    slandererDist = d;
                }
            }
            if (bestSlanderer != null) {
                return bestSlanderer.getLocation();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

}
