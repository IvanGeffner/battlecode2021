package rushbot;

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
            int dX = Math.abs(target.x - loc.x);
            int dY = Math.abs(target.y - loc.y);
            int minDif = Math.min(dX, dY), maxDif = Math.max(dX, dY);
            double p = rc.sensePassability(loc);
            if (minDif > 0){
                return (1.0/p)*sqrt2 + ((minDif - 1)*sqrt2 + (maxDif - minDif))*avgImpassabilityInv;
            }
            return (maxDif - 1)*avgImpassabilityInv + (1.0/p);
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
            //computeAverageImpassability();
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
                double estimation = firstStep;
                switch(dir){
                    case NORTHWEST:
                    case NORTHEAST:
                    case SOUTHWEST:
                    case SOUTHEAST:
                        estimation*= sqrt2;
                        break;
                    default:
                        break;
                }
                estimation += getEstimation(newLoc);
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
}
