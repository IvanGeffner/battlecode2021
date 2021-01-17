package elevenb;

import battlecode.common.*;

public class Muckraker extends MyRobot {


    final int EXPLORER_1_TYPE = 0;
    final int EXPLORER_2_TYPE = 1;
    final int ATTACKER_TYPE = 2;
    final int EXPLORE_2_BYTECODE_REMAINING = 2000;

    int myType;
    boolean moved = false;

    Team myTeam, enemyTeam;

    int birthday;

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
    }

    public void play(){
        moved = false;
        tryKill();
        tryMove();
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
            path.move(loc);
            return;
        }
        /*
        if (myType == ATTACKER_TYPE){
            if (surroundEnemyHQ()) return;
        }
        if (myType == EXPLORER_2_TYPE){
            loc = explore.getExplore2Target(EXPLORE_2_BYTECODE_REMAINING);
            if (loc != null){
                path.move(loc);
                return;
            }
        }

        loc = explore.getExploreTarget();
        if (loc != null){
            path.move(loc);
            return;
        }

        if (myType != ATTACKER_TYPE){
            if (surroundEnemyHQ()) return;
        }
        explore2();
        */
        if (rc.getRoundNum() - birthday > 150){
            if (surroundEnemyHQ()) return;
        }
        loc = explore.getExplore3Target();
        path.move(loc);
        return;
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
