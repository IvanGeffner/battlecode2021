package thirtyone;

import battlecode.common.*;

public class Muckraker extends MyRobot {


    final int EXPLORER_1_TYPE = 0;
    final int EXPLORER_2_TYPE = 1;
    final int ATTACKER_TYPE = 2;
    final int EXPLORE_2_BYTECODE_REMAINING = 2000;

    int myType;
    boolean moved = false;

    int exploreRounds;

    Team myTeam, enemyTeam;

    int birthday;

    final static int EC_DELAY = 100;

    public Muckraker(RobotController rc){
        super(rc);
        myType = (int)(Math.random()*3.0);
        if (rc.getConviction() > 1) {
            myType = (int)(Math.random()*2.0);
            if (myType == EXPLORER_2_TYPE) ++myType;
        }
        myType = EXPLORER_1_TYPE;
        myTeam = rc.getTeam();
        enemyTeam = myTeam.opponent();
        birthday = rc.getRoundNum();
        Math.random();
        exploreRounds = 150;
        if (rc.getConviction() > Util.MUCK_ATTACK_THRESHOLD) exploreRounds = 0;
    }

    public void play(){
        moved = false;
        tryKill();
        tryMove();
        updateECs();
    }

    void tryKill(){
        try {
            if (rc.getCooldownTurns() >= 1) return;
            RobotInfo[] robots = rc.senseNearbyRobots(RobotType.MUCKRAKER.actionRadiusSquared, enemyTeam);
            RobotInfo bestSlanderer = null;
            for (RobotInfo r : robots){
                if (r.getType() == RobotType.SLANDERER){
                    if (rc.canExpose(r.getID())){
                       if (bestSlanderer == null || bestSlanderer.getInfluence() < r.getInfluence()) bestSlanderer = r;
                    }
                }
            }
            if (bestSlanderer != null){
                if (rc.canExpose(bestSlanderer.getID())){
                    rc.expose(bestSlanderer.getID());
                    moved = true;
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    void tryMove(){
        if (moved) return;
        MapLocation loc = getClosestSlanderer();
        if (loc != null){
            bfs.move(loc);
            return;
        }
        if (rc.getRoundNum() - birthday > exploreRounds){
            if (goToEnemyHQ()) return;
        }
        loc = explore.getExplore3Target();
        //rc.setIndicatorDot(loc, 0, 0, 255);
        bfs.move(loc);
        return;
    }

    boolean goToEnemyHQ(){
        MapLocation ans = null;
        int minDist = -1;
        for (Communication.RInfo r = comm.firstEC; r != null; r = r.nextInfo){
            if (r.getMapLocation() == null) continue;
            if (r.team != rc.getTeam().opponent().ordinal()) continue;
            if (rc.getRoundNum() - r.turnExplored <= EC_DELAY) continue;
            int dist = r.getMapLocation().distanceSquaredTo(rc.getLocation());
            if (minDist < 0 || minDist > dist){
                minDist = dist;
                ans = r.getMapLocation();
            }
        }
        return moveSafely(ans, Util.SAFETY_DISTANCE_ENEMY_EC);
    }

    void updateECs(){
        for (Communication.RInfo r = comm.firstEC; r != null; r = r.nextInfo){
            if (r.getMapLocation() == null) continue;
            if (r.team != rc.getTeam().opponent().ordinal()) continue;
            int dist = r.getMapLocation().distanceSquaredTo(rc.getLocation());
            if (dist > Util.MUCKRAKER_DIST_EC) continue;
            r.turnExplored = rc.getRoundNum();
        }
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
