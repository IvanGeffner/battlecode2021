package eight;

import battlecode.common.MapLocation;

public class Util {

    static int SAFETY_DISTANCE_EC = 9;

    static int distance(MapLocation A, MapLocation B){
        return Math.max(Math.abs(A.x - B.x), Math.abs(A.y - B.y));
    }


}
