package thirdbot;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Pathfinding {

    RobotController rc;
    final double minPassabilityInv = 10;
    final double sqrt2 = Math.sqrt(2.0);
    MapLocation target = null;
    double avgImpassabilityInv = 10;

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


    Pathfinding(RobotController rc){
        this.rc = rc;
    }

    double getEstimation (MapLocation loc){
        try {
            if (loc.distanceSquaredTo(target) == 0) return 0;
            int d = Util.distance(target, loc);
            double p = rc.sensePassability(loc);
            return 1.0/p + (d - 1)*avgImpassabilityInv;
        } catch (Throwable e){
            e.printStackTrace();
        }
        return 1e9;
    }

    public void move(MapLocation loc){
        try {

            if (rc.getCooldownTurns() >= 1) return;
            target = loc;
            if (target == null) return;
            MapLocation myLoc = rc.getLocation();
            if (target.distanceSquaredTo(myLoc) <= 0) return;
            rc.setIndicatorLine(myLoc, target, 0, 0, 255);
            Direction bestDir = null;
            double bestEstimation = 0;
            double firstStep = 1.0/rc.sensePassability(myLoc);
            int contPassability = 0;
            double avgP = 0;
            for (Direction dir : directions){
                MapLocation newLoc = myLoc.add(dir);
                if (!rc.onTheMap(newLoc)) continue;

                //pass
                avgP += 1.0/rc.sensePassability(newLoc);
                ++contPassability;


                if (!rc.canMove(dir)) continue;
                if (!strictlyCloser(newLoc, myLoc, target)) continue;

                double estimation = firstStep + getEstimation(newLoc);
                if (bestDir == null || estimation < bestEstimation){
                    bestEstimation = estimation;
                    bestDir = dir;
                }
            }
            if (contPassability != 0){
                avgImpassabilityInv = avgP/contPassability;
            }
            if (bestDir != null) rc.move(bestDir);
        } catch (Throwable e){
            e.printStackTrace();
        }
    }

    boolean strictlyCloser(MapLocation newLoc, MapLocation oldLoc, MapLocation target){
        int dOld = Util.distance(target, oldLoc), dNew = Util.distance(target, newLoc);
        if (dOld < dNew) return false;
        if (dNew < dOld) return true;
        return target.distanceSquaredTo(newLoc) < target.distanceSquaredTo(oldLoc);

    }
}
