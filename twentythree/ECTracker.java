package twentythree;

import battlecode.common.MapLocation;

public class ECTracker extends MapTracker {

    int REASONABLE_MAX_EC = 12;
    MapLocation[] locArray = new MapLocation[REASONABLE_MAX_EC];
    int lastArrayElement = 0;

    ECTracker(){
        super();
    }

    @Override
    void add(MapLocation loc){
        int arrayPos = (loc.x%MAX_MAP_SIZE)*(1 + (loc.y%MAX_MAP_SIZE)/INT_BITS);
        int bitPos = loc.y%INT_BITS;
        visitedLocations[arrayPos] |= (1 << bitPos);
        if (lastArrayElement >= REASONABLE_MAX_EC) return;
        locArray[lastArrayElement++] = loc;
    }


}
