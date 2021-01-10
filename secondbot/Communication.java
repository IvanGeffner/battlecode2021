package secondbot;

import battlecode.common.*;

public class Communication {

    final int MAX_MAP_SIZE = GameConstants.MAP_MAX_HEIGHT;
    final int MAX_MAP_SIZE2 = 2*MAX_MAP_SIZE;
    final int MIN_ID = 10000;
    final int BYTECODE_REMAINING = 2000;

    RobotController rc;
    int senseRadius;
    Team myTeam;
    boolean meEC;

    RInfo myEC = null;
    int currentFlag = 0;

    Communication(RobotController rc){
        this.rc = rc;
        senseRadius = rc.getType().sensorRadiusSquared;
        myTeam = rc.getTeam();
        meEC = rc.getType() == RobotType.ENLIGHTENMENT_CENTER;
    }

    enum MType{
        NONE,
        NON_EC_ID,
        EC_ID,
        EC_X,
        EC_Y,
        X,
        Y,
    }

    //INFO
    MType[] messageTypes = MType.values();
    LocTracker ecMapLocations = new LocTracker();
    IDTracker idTracker = new IDTracker();
    MessageIndex mIndex = new MessageIndex();

    RInfo firstEC = null, lastEC = null;
    RInfo firstNonEC = null, lastNonEC = null;
    RInfo nonECRead = null;
    BoundInfo lbX = null, lbY = null, ubX = null, ubY = null;

    Integer IDUnit = null;

    //CORE METHODS

    void debugDraw(){
        if (lbX != null){
            rc.setIndicatorLine(rc.getLocation(), new MapLocation(lbX.value, rc.getLocation().y), 0, 0, 0);
        }
        if (ubX != null){
            rc.setIndicatorLine(rc.getLocation(), new MapLocation(ubX.value, rc.getLocation().y), 0, 0, 0);
        }
        if (lbY != null){
            rc.setIndicatorLine(rc.getLocation(), new MapLocation(rc.getLocation().x, lbY.value), 0, 0, 0);
        }
        if (ubY != null){
            rc.setIndicatorLine(rc.getLocation(), new MapLocation(rc.getLocation().x, ubY.value), 0, 0, 0);
        }

        for (RInfo r = firstEC; r != null; r = r.nextInfo){
            MapLocation loc = r.getMapLocation();
            if (loc == null) continue;
            rc.setIndicatorLine(rc.getLocation(), loc, 255, 0, 0);
        }
    }

    void readMessages(){ //TODO: cap bytecode expense per type
        try {
            if (running()) return;

            //visible robots
            RobotInfo[] visibleRobots = rc.senseNearbyRobots(senseRadius, myTeam);
            for (RobotInfo r : visibleRobots) {
                int id = r.getID();
                processMessage(rc.getFlag(id), checkECID(id));
            }

            //known ec ids
            for (RInfo ec = firstEC; ec != null; ec = ec.nextInfo){
                if (!rc.canGetFlag(ec.id)){
                    ec.remove();
                    continue;
                }
                if (ec.team != myTeam.ordinal()) continue;
                processMessage(rc.getFlag(ec.id), checkECID(ec.id));
            }
            if (meEC) {
                if (nonECRead == null) nonECRead = firstNonEC;
                for (; nonECRead != null && Clock.getBytecodesLeft() > BYTECODE_REMAINING; nonECRead = nonECRead.nextInfo){
                    if (!rc.canGetFlag(nonECRead.id)){
                        nonECRead.remove();
                        continue;
                    }
                    processMessage(rc.getFlag(nonECRead.id), false);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    void setFlag(){
        try {
            if (running()) return;
            if (IDUnit != null){
                rc.setFlag(encodeMessage(MType.NON_EC_ID, getMessageNonEC()));
                IDUnit = null;
                return;
            }
            currentFlag = 0;
            mIndex.set();
            if (!meEC){
                if (myEC != null) setFlagUnknown();
                if (currentFlag == 0) setFlagNonEC();
                //else System.err.println("Unknown Message to ID " + myEC.id + ": " + currentFlag);
            }
            else setFlagEC();
            rc.setFlag(currentFlag);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    void exploredNorth(int value){
        if (running()) return;
        if (ubY == null){
            ubY = new BoundInfo(value, false);
            return;
        }
        ubY.value = value;
    }

    void exploredSouth(int value){
        if (running()) return;
        if (lbY == null){
            lbY = new BoundInfo(value, false);
            return;
        }
        lbY.value = value;
    }

    void exploredEast(int value){
        if (running()) return;
        if (ubX == null){
            ubX = new BoundInfo(value, false);
            return;
        }
        ubX.value = value;
    }

    void exploredWest(int value){
        if (running()) return;
        if (lbX == null){
            lbX = new BoundInfo(value, false);
            return;
        }
        lbX.value = value;
    }

    void exploredEC(RobotInfo r){
        if (running()) return;
        RInfo ri = getEC(r.getID());
        if (ri == null){
            ri = new RInfo(r.getID(), r.getTeam().ordinal(), true);
            addEC(ri);
        }
        ri.locX = r.getLocation().x;
        ri.locY = r.getLocation().y;
        if (myEC == null && r.getTeam() == rc.getTeam()){
            myEC = ri;
        }
    }

    void exploredECSelf(){
        if (running()) return;
        RInfo ri = getEC(rc.getID());
        if (ri == null){
            ri = new RInfo(rc.getID(), rc.getTeam().ordinal(), true);
            addEC(ri);
        }
        ri.locX = rc.getLocation().x;
        ri.locY = rc.getLocation().y;
    }

    void exploredNonEC(RobotInfo r){
        if (running()) return;
        if (meEC){
            if (idTracker.checkID(r.getID())) return;
            idTracker.addID(r.getID());
            RInfo ri = new RInfo(r.getID(), r.getTeam().ordinal(), false);
            addNonEC(ri);
        } else{
            if (IDUnit != null) return;
            if (idTracker.checkID(r.getID())) return;
            idTracker.addID(r.getID());
            IDUnit = r.getID();
        }
    }

    Integer getBound(BoundInfo b){
        if (b == null) return null;
        return b.value;
    }

    Integer getMessageEC(RInfo r, int subIndex, boolean unknown){
        if (r == null) return null;
        switch (subIndex){
            case 0:
                if (unknown && r.knownID) return null;
                return encodeMessage(MType.EC_ID, r.getContentID());
            case 1:
                if (unknown && (r.knownLocX || !r.knownID)) return null;
                Integer code =  r.getContentX();
                if (code == null) return null;
                return encodeMessage(MType.EC_X, code);
            case 2:
                if (unknown && (r.knownLocY || !r.knownID)) return null;
                code =  r.getContentY();
                if (code == null) return null;
                return encodeMessage(MType.EC_Y, code);
        }
        return null;
    }

    boolean checkECID(int id){
        if (myEC == null) return false;
        return myEC.id == id;
    }

    void setFlagEC(){
        Integer mes = mIndex.getFlag(false);
        if (mes == null){
            if (!mIndex.advance()) return;
            setFlagEC();
        }
        else currentFlag = mes;
    }

    void setFlagUnknown(){
        Integer mes = mIndex.getFlag(true);
        if (mes == null){
            if (!mIndex.advance()) return;
            setFlagUnknown();
        }
        else{
            //System.err.println("Got here with message " + mes);
            currentFlag = mes;
        }
    }

    void setFlagNonEC(){
        while (!mIndex.forNonEC()) if (!mIndex.advance()) return;
        Integer mes = mIndex.getFlag(false);
        if (mes == null){
            if (!mIndex.advance()) return;
            setFlagNonEC();
        }
        else currentFlag = mes;
    }

    void processMessage(int code, boolean sentByMyEC){
        //if (sentByMyEC) System.err.println("Got message from HQ: "+ code);
        MType mType = getType(code);
        int content = getContent(code);
        switch(mType){
            case NONE: return;
            case NON_EC_ID:
                if (!meEC) return;
                int id = getID(content);
                if (idTracker.checkID(id)) return;
                idTracker.addID(id);
                RInfo ec = new RInfo(id, myTeam.ordinal(), true);
                addNonEC(ec);
                return;
            case EC_ID:
                id = getID(content);
                //System.err.println("Got the ID of an EC: " + id);
                ec = getEC(id);
                if (ec == null) {
                    //System.err.println("Adding " + id);
                    ec = new RInfo(id, getExtraElement(content), true);
                    addEC(ec);
                }
                ec.knownID |= sentByMyEC;
                return;
            case EC_X:
                id = getID(content);
                //System.err.println("Got info about x coordinate of " + id);
                ec = getEC(id);
                if (ec == null) return;
                //System.err.println("Got info about x coordinate of " + id + " and it is " + getExtraElement(content));
                ec.locX = getActualValue(rc.getLocation().x, getExtraElement(content));
                ec.knownLocX |= sentByMyEC;
                ecMapLocations.add(ec.getMapLocation());
                return;
            case EC_Y:
                id = getID(content);
                ec = getEC(id);
                if (ec == null) return;
                //System.err.println("Got info about y coordinate of " + id + " and it is " + getExtraElement(content));
                ec.locY = getActualValue(rc.getLocation().y, getExtraElement(content));
                ec.knownLocY |= sentByMyEC;
                ecMapLocations.add(ec.getMapLocation());
                return;
            case X:
                processBoundX(content, rc.getLocation().x, sentByMyEC);
                return;
            case Y:
                processBoundY(content, rc.getLocation().y, sentByMyEC);
                return;
        }
    }

    int getActualValue(int reference, int coordinate){
        int dif = coordinate - (reference%MAX_MAP_SIZE2);
        if (dif >= MAX_MAP_SIZE) dif -= MAX_MAP_SIZE2;
        else if (dif <= -MAX_MAP_SIZE) dif += MAX_MAP_SIZE2;
        return reference + dif;
    }


    void processBoundX(int content, int reference, boolean known){
        Integer l = getLow(content, reference), u = getHigh(content, reference);
        //System.out.println("Processing X " + content + " " +  l + " " + u);
        if (l != null) {
            if (lbX == null) {
                lbX = new BoundInfo(l, known);
            }
            lbX.known |= known;
        }
        if (u != null) {
            if (ubX == null) {
                ubX = new BoundInfo(u, known);
            }
            ubX.known |= known;
        }
    }

    void processBoundY(int content, int reference, boolean known){
        Integer l = getLow(content, reference), u = getHigh(content, reference);
        //System.out.println("Processing Y " + content + " " +  l + " " + u);
        if (l != null) {
            if (lbY == null) {
                lbY = new BoundInfo(l, known);
            }
            lbY.known |= known;
        }
        if (u != null) {
            if (ubY == null) {
                ubY = new BoundInfo(u, known);
            }
            ubY.known |= known;
        }
    }

    Integer getLow(int boundCode, int reference){
        boundCode = (boundCode >>> 9);
        if ((boundCode&1) == 0) return null;
        return getActualValue(reference, (boundCode >>> 1));
    }

    Integer getHigh(int boundCode, int reference){
        if ((boundCode&1) == 0) return null;
        return getActualValue(reference,(boundCode >>> 1)&0xFF);
    }

    int encodeMessage(MType type, int content){
        return (content << 3) | type.ordinal();
    }

    MType getType(int code){
        return messageTypes[code&7];
    }

    int getContent(int code){
        return code >>> 3;
    }

    RInfo getEC(int id){
        RInfo ans = firstEC;
        while (ans != null){
            if (ans.id == id) return ans;
            ans = ans.nextInfo;
        }
        return null;
    }

    int getMessageNonEC(){
        return ((IDUnit - MIN_ID) << 7) | myTeam.ordinal();
    }

    boolean running(){
        return ecMapLocations.isRunning || idTracker.isRunning;
    }

    void run(){
        ecMapLocations.run();
        idTracker.run();
    }

    int getID(int content){
        return (content >>> 7) + MIN_ID;
    }

    int getExtraElement (int content){
        return (content & 0x7F);
    }


    int getBoundValue(BoundInfo bi){
        if (bi == null) return 0;
        return ((bi.value%MAX_MAP_SIZE2) << 1) | 1;
    }

    int encodeBound(BoundInfo low, BoundInfo high){
        int ans = (getBoundValue(low) << 9) | getBoundValue(high);
        //System.err.println("Encoding " + getBound(low) + " " + getBound(high) + " into " + ans);

        return ans;
    }

    Integer getMessage(MType t, BoundInfo lb, BoundInfo ub, boolean unknown){
        boolean kl = false, ku = false;
        if (lb == null){
            if (ub == null) return null;
            kl = true;
        }
        else if (lb.known) kl = true;
        if (ub == null){
            ku = true;
        } else if (ub.known) ku = true;
        if (unknown && kl && ku) return null;
        return encodeMessage(t, encodeBound(lb, ub));
    }

    void addEC(RInfo ec){
        if (lastEC == null){
            firstEC = ec;
            lastEC = ec;
            return;
        }
        lastEC.add(ec);
    }

    void addNonEC(RInfo ec){
        if (lastNonEC == null){
            firstNonEC = ec;
            lastNonEC = ec;
            return;
        }
        lastNonEC.add(ec);
    }

    //Helper Classes

    class MessageIndex{
        int currentType = 0;
        RInfo currentListElement = null;
        int subIndex = 0;

        int currentTypeCopy = 0;
        RInfo currentListElementCopy = null;
        int subIndexCopy = 0;

        MessageIndex(){}

        void advanceType(){
            currentType = (currentType+1)% messageTypes.length;
            if (currentType == MType.EC_ID.ordinal()){
                currentListElement = firstEC;
                subIndex = 0;
            }
        }

        boolean advance(){
            if (currentType != MType.EC_ID.ordinal()) advanceType();
            else{
                if (currentListElement == null) advanceType();
                else{
                    if (subIndex >= 2){
                        subIndex = 0;
                        currentListElement = currentListElement.nextInfo;
                        if (currentListElement == null) advanceType();
                    } else {
                        ++subIndex;
                    }
                }
            }
            if (currentType != currentTypeCopy) return true;
            if (currentListElement != currentListElementCopy) return true;
            if (subIndex != subIndexCopy) return true;
            return false;
        }

        void set(){
            currentTypeCopy = currentType;
            currentListElementCopy = currentListElement;
            subIndexCopy = subIndex;
            advance();
        }

        boolean forNonEC(){
            switch (messageTypes[currentType]){
                case NONE:
                case EC_X:
                case EC_Y:
                case NON_EC_ID: return false;
                case X:
                case Y: return true;
            }
            return subIndex == 0;
        }

        Integer getFlag(boolean unknown){
            switch(messageTypes[currentType]){
                case NONE:
                case EC_X:
                case EC_Y:
                case NON_EC_ID: return null;
                case X: return getMessage(MType.X, lbX, ubX, unknown);
                case Y: return getMessage(MType.Y, lbY, ubY, unknown);
                case EC_ID: return getMessageEC(currentListElement, subIndex, unknown);
            }
            return null;
        }
    }

     class BoundInfo {
        int value;
        boolean known;
        BoundInfo(int value, boolean known){
            this.value = value;
            this.known = known;
        }
    }

    class RInfo {
        RInfo nextInfo = null;
        RInfo previousInfo = null;
        boolean removed = false;
        int id, team;
        Integer locX = null, locY = null;
        boolean knownID = false, knownLocX = false, knownLocY = false;
        MapLocation loc = null;
        boolean EC;

        RInfo(int id, int team, boolean EC) {
            this.id = id;
            this.team = team;
            this.EC = EC;
        }

        MapLocation getMapLocation(){
            if (loc != null) return loc;
            if (locX == null) return null;
            if (locY == null) return null;
            loc = new MapLocation(locX, locY);
            return loc;
        }

        void add(RInfo e){
            if (this.nextInfo != null){
                this.nextInfo.previousInfo = e;
                e.nextInfo = this.nextInfo;
            }
            else{
                if (EC) lastEC = e;
                else lastNonEC = e;
            }
            this.nextInfo = e;
            e.previousInfo = this;
        }

        void remove(){
            removed = true;
            if (previousInfo != null) previousInfo.nextInfo = this.nextInfo;
            else{
                if (EC) firstEC = this.nextInfo;
                else firstNonEC = this.nextInfo;
            }
            if (nextInfo != null) nextInfo.previousInfo = this.previousInfo;
            else{
                if (EC) lastEC = this.previousInfo;
                else lastNonEC = this.previousInfo;
            }
            if (myEC == this) myEC = null;
        }

        int getContentID(){
            return ((id - MIN_ID) << 7) | team;
        }

        Integer getContentX(){
            if (locX == null) return null;
            return ((id - MIN_ID) << 7) | (locX%MAX_MAP_SIZE2);
        }

        Integer getContentY(){
            if (locY == null) return null;
            return ((id - MIN_ID) << 7) | (locY%MAX_MAP_SIZE2);
        }

    }


    //keeping track of ECs
    class LocTracker {
        final int MIN_BYTECODE = 200;
        int[][] ECLocs = new int[MAX_MAP_SIZE][];

        int runningIndex = 0;
        boolean isRunning = true;

        int REASONABLE_MAX_EC = 12;
        int lastArrayElement = 0;
        MapLocation[] locArray = new MapLocation[REASONABLE_MAX_EC];

        void add(MapLocation loc){
            if (isRunning) return;
            if (loc == null) return;
            if (lastArrayElement >= REASONABLE_MAX_EC) return; //shouldnt happen
            if (ECLocs[loc.x%MAX_MAP_SIZE][loc.y%MAX_MAP_SIZE] == 0){
                ECLocs[loc.x%MAX_MAP_SIZE][loc.y%MAX_MAP_SIZE] = lastArrayElement;
                locArray[lastArrayElement++] = loc;
            }
        }

        void run(){
            if (!isRunning) return;
            while (runningIndex < MAX_MAP_SIZE){
                if (Clock.getBytecodesLeft() <= MIN_BYTECODE) return;
                ECLocs[runningIndex++] = new int[MAX_MAP_SIZE];
            }
            isRunning = false;
        }

        LocTracker(){}
    }

    class IDTracker {

        final int ROOT = 128;
        final int MIN_BYTECODE = 400;

        int runningIndex = 0;
        boolean isRunning = true;
        boolean[][] IDs = new boolean[ROOT][];

        IDTracker(){
        }

        void addID(int id){
            IDs[id/ROOT][id%ROOT] = true;
        }

        boolean checkID(int id){
            return IDs[id/ROOT][id%ROOT];
        }

        void run(){
            if (!isRunning) return;
            while (runningIndex < ROOT){
                if (Clock.getBytecodesLeft() <= MIN_BYTECODE) return;
                IDs[runningIndex++] = new boolean[ROOT];
            }
            isRunning = false;
        }

    }

    //other

    int getMinDistToEC(MapLocation loc, int team){
        int d = -1;
        for (RInfo r = firstEC; r != null; r = r.nextInfo){
            if (r.getMapLocation() == null) continue;
            if (r.team != team) continue;
            int dist = r.getMapLocation().distanceSquaredTo(loc);
            if (d < 0 || d < dist) d = dist;
        }
        return d;
    }




}
