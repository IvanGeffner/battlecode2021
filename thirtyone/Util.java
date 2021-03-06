package thirtyone;

import battlecode.common.MapLocation;

public class Util {

    static int SAFETY_DISTANCE_ENEMY_EC = 4;

    static int SAFETY_DISTANCE_OUR_HQ = 5;

    static int MUCKRAKER_DIST_EC = 13;

    static int BFS_DIST_ENEMY_EC = 13;

    static int distance(MapLocation A, MapLocation B){
        return Math.max(Math.abs(A.x - B.x), Math.abs(A.y - B.y));
    }

    static int TURNS_WAIT_CONQUEROR = 50;

    static int MUCK_ATTACK_THRESHOLD = 10;

}
