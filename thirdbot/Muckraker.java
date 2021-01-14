package thirdbot;

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
            loc = explore.getExplore2Target(EXPLORE_2_BYTECODE_REMAINING);
            if (loc != null) return loc;
        }

        loc = explore.getExploreTarget();
        if (loc != null) return loc;

        if (myType != ATTACKER_TYPE){
            loc = comm.getClosestEnemyEC();
            if (loc != null) return loc;
        }

        loc = explore.getExplore2Target(EXPLORE_2_BYTECODE_REMAINING);

        return loc;
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
