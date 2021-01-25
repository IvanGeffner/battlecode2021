package twentythreeb;

import battlecode.common.*;

public class Communication {

    final int MAX_MAP_SIZE = GameConstants.MAP_MAX_HEIGHT;
    final int MAX_MAP_SIZE2 = 2 * MAX_MAP_SIZE;
    final int MIN_ID = 10000;
    final int BYTECODE_REMAINING = 6000;

    final int BYTECODE_USED_UNITS = 3500;

    RobotController rc;
    int senseRadius;
    Team myTeam;
    boolean meEC;
    boolean meSlanderer;

    int myTeamIndex, enemyTeamIndex;

    RInfo myEC = null;
    int currentFlag = 0;

    final int messageTries = 12 * 4 + 2;

    Communication(RobotController rc) {
        this.rc = rc;
        senseRadius = rc.getType().sensorRadiusSquared;
        myTeam = rc.getTeam();
        meEC = rc.getType() == RobotType.ENLIGHTENMENT_CENTER;
        meSlanderer = rc.getType() == RobotType.SLANDERER;
        myTeamIndex = myTeam.ordinal();
        enemyTeamIndex = myTeam.opponent().ordinal();
    }

    enum MType {
        NEUTRAL_INF,
        NON_EC_ID,
        EC_ID,
        EC_X,
        EC_Y,
        X,
        Y,
        ENEMY_MUCKRAKER,
        H_SYM,
        V_SYM,
        R_SYM
    }

    static boolean H_SYM = true;
    static boolean V_SYM = true;
    static boolean R_SYM = true;

    static int H_MSG = 0xFFFFF9;
    static int V_MSG = 0xFFFFF1;
    static int R_MSG = 0xFFFFE9;

    //INFO
    MType[] messageTypes = MType.values();
    ECTracker ecMapLocations = new ECTracker();
    IDTracker idTracker = new IDTracker();
    MessageIndex mIndex = new MessageIndex();

    RInfo firstEC = null, lastEC = null;
    RInfo firstNonEC = null, lastNonEC = null;
    RInfo nonECRead = null;
    BoundInfo lbX = null, lbY = null, ubX = null, ubY = null;

    Integer emergencyMessage = null;

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
            if (r.team == myTeamIndex) rc.setIndicatorLine(rc.getLocation(), loc, 255, 0, 0);
            else if (r.team == enemyTeamIndex) rc.setIndicatorLine(rc.getLocation(), loc, 0, 255, 0);
            else{
                rc.setIndicatorLine(rc.getLocation(), loc, 0, 255, 255);
                if (r.influence != null) System.err.println("ID " + r.id + " influence " + r.influence);
            }
        }
    }

    void readMessages(){
        try {
            //if (running()) return;
            if (!meEC){
                firstNonEC = null;
                lastNonEC = null;
            }
            //visible robots
            RobotInfo[] visibleRobots = rc.senseNearbyRobots(senseRadius, myTeam);
            for (RobotInfo r : visibleRobots) {
                if (Clock.getBytecodeNum() > BYTECODE_USED_UNITS) break;
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
            //if (running()) return;
            currentFlag = 0;
            mIndex.set();
            if (!meEC && !meSlanderer && myEC != null) setFlagUnknown(messageTries);
            if (currentFlag == 0){
                if (emergencyMessage != null){
                    currentFlag = emergencyMessage;
                    //if (meEC) System.err.println("Echoing " + emergencyMessage + " of type " + getType(emergencyMessage));
                    //else System.err.println("Emergency " + emergencyMessage + " of type " + getType(emergencyMessage));
                    emergencyMessage = null;
                }
            }
            if (currentFlag == 0) {
                if (!meEC && !meSlanderer) setFlagNonEC(messageTries);
                else setFlagEC(messageTries);
            }
            rc.setFlag(currentFlag + 1);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    void exploredNorth(int value){
        //if (running()) return;
        if (ubY == null){
            ubY = new BoundInfo(value, false);
            return;
        }
        ubY.value = value;
    }

    void exploredSouth(int value){
        //if (running()) return;
        if (lbY == null){
            lbY = new BoundInfo(value, false);
            return;
        }
        lbY.value = value;
    }

    void exploredEast(int value){
        //if (running()) return;
        if (ubX == null){
            ubX = new BoundInfo(value, false);
            return;
        }
        ubX.value = value;
    }

    void exploredWest(int value){
        //if (running()) return;
        if (lbX == null){
            lbX = new BoundInfo(value, false);
            return;
        }
        lbX.value = value;
    }

    void exploredEC(RobotInfo r){ //todo add to loctracker?
        //if (running()) return;
        RInfo ri = getEC(r.getID());
        if (ri == null){
            ri = new RInfo(r.getID(), r.getTeam().ordinal(), true);
            addEC(ri);
        }
        ri.locX = r.getLocation().x;
        ri.locY = r.getLocation().y;
        if (r.getTeam() == Team.NEUTRAL) {
            if (ri.influence == null || r.getInfluence() < ri.influence){
                ri.influence = r.getInfluence();
                ri.knownInfluence = false;
            }
        }
        if (myEC == null && r.getTeam() == rc.getTeam()){
            myEC = ri;
        }
    }

    void exploredECSelf(){
        //if (running()) return;
        RInfo ri = getEC(rc.getID());
        if (ri == null){
            ri = new RInfo(rc.getID(), rc.getTeam().ordinal(), true);
            addEC(ri);
        }
        ri.locX = rc.getLocation().x;
        ri.locY = rc.getLocation().y;
    }

    void exploredNonEC(RobotInfo r){
        //if (running()) return;
        if (meEC){
            if (idTracker.checkID(r.getID())) return;
            idTracker.addID(r.getID());
            RInfo ri = new RInfo(r.getID(), r.getTeam().ordinal(), false);
            addNonEC(ri);
        } else{
            if (emergencyMessage != null) return;
            if (idTracker.checkID(r.getID())) return;
            idTracker.addID(r.getID());
            emergencyMessage = encodeMessage(MType.NON_EC_ID, getMessageNonEC(r.getID()));
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
            case 3:
                if (r.team != Team.NEUTRAL.ordinal()) return null;
                if (unknown && (r.knownInfluence || !r.knownID)) return null;
                code =  r.getContentInfluence();
                if (code == null) return null;
                return encodeMessage(MType.NEUTRAL_INF, code);
        }
        return null;
    }

    boolean checkECID(int id){
        if (myEC == null) return false;
        return myEC.id == id;
    }

    void setFlagEC(int remainingTries){
        if (remainingTries-- <= 0) return;
        Integer mes = mIndex.getFlag(false);
        if (mes == null){
            if (!mIndex.advance()) return;
            setFlagEC(remainingTries);
        }
        else currentFlag = mes;
    }

    void setFlagUnknown(int remainingTries){
        if (remainingTries-- <= 0) return;
        Integer mes = mIndex.getFlag(true);
        if (mes == null){
            if (!mIndex.advance()) return;
            setFlagUnknown(remainingTries);
        }
        else{
            //System.err.println("Got unknown message " + mes);
            currentFlag = mes;
        }
    }

    void setFlagNonEC(int remainingTries){
        //System.err.println("Trying index " + mIndex.currentType);
        if (remainingTries-- <= 0) return;
        while (!mIndex.forNonEC()) if (!mIndex.advance()) return;
        Integer mes = mIndex.getFlag(false);
        if (mes == null){
            if (!mIndex.advance()) return;
            setFlagNonEC(remainingTries);
        }
        else{
            //System.err.println("Got known message " + mes + " at index " + mIndex.currentType);
            currentFlag = mes;
        }
    }

    void processMessage(int code, boolean sentByMyEC){
        //if (sentByMyEC) System.err.println("Got message from HQ: "+ code);
        if (--code < 0) return;
        switch(code){
            case 0xFFFFF9:
                H_SYM = false;
                return;
            case 0xFFFFF1:
                V_SYM = false;
                return;
            case 0xFFFFE9:
                R_SYM = false;
                return;
        }
        MType mType = getType(code);
        int content = getContent(code);
        switch(mType){
            case ENEMY_MUCKRAKER:
                if (meEC) return;
                MapLocation loc = getMuckrakerLoc(content);
                int conv = getMuckrakerConv(content);
                RInfo muck = new RInfo(0, enemyTeamIndex, false);
                muck.loc = loc;
                muck.influence = conv;
                //note that there is no locX or locY
                addNonEC(muck);
                break;
            case NON_EC_ID:
                if (!meEC) return;
                int id = getID(content);
                if (idTracker.checkID(id)) return;
                idTracker.addID(id);
                RInfo ec = new RInfo(id, myTeamIndex, false);
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
                    if (meEC) emergencyMessage = code;
                }
                ec.knownID |= sentByMyEC;
                return;
            case EC_X:
                id = getID(content);
                //System.err.println("Got info about x coordinate of " + id);
                ec = getEC(id);
                if (ec == null) return;
                //System.err.println("Got info about x coordinate of " + id + " and it is " + getExtraElement(content));
                if (meEC && ec.locX == null) emergencyMessage = code;
                ec.locX = getActualValue(rc.getLocation().x, getExtraElement(content));
                ec.knownLocX |= sentByMyEC;
                //ecMapLocations.add(ec.getMapLocation());
                return;
            case EC_Y:
                id = getID(content);
                ec = getEC(id);
                if (ec == null) return;
                //System.err.println("Got info about y coordinate of " + id + " and it is " + getExtraElement(content));
                if (meEC && ec.locY == null) emergencyMessage = code;
                ec.locY = getActualValue(rc.getLocation().y, getExtraElement(content));
                ec.knownLocY |= sentByMyEC;
                //ecMapLocations.add(ec.getMapLocation());
                return;
            case NEUTRAL_INF:
                id = getNeutralID(content);
                ec = getEC(id);
                if (ec == null){
                    //ec = new RInfo(id, Team.NEUTRAL.ordinal(), true);
                    //addEC(ec);
                    return;
                }
                int inf = getInfluence(content);
                if (ec.influence == null || ec.influence >= inf){
                    ec.influence = inf;
                    ec.knownInfluence |= sentByMyEC;
                }
                return;
            case X:
                processBoundX(content, rc.getLocation().x, sentByMyEC);
                return;
            case Y:
                processBoundY(content, rc.getLocation().y, sentByMyEC);
                return;
        }
    }

    void reportMuckraker(MapLocation loc, int conviction){
        if (meEC) return;
        int x = loc.x%MAX_MAP_SIZE2;
        int y = loc.y%MAX_MAP_SIZE2;
        int c = Math.min(conviction, 0x7F);
        emergencyMessage = encodeMessage(MType.ENEMY_MUCKRAKER, (((c << 7) | x) << 7) | y);
    }

    int getMuckrakerConv(int content){
        return (content >>> 14);
    }

    MapLocation getMuckrakerLoc(int content){
        int x = getActualValue(rc.getLocation().x, (content >>> 7)&0x7F);
        int y = getActualValue(rc.getLocation().y, content&0x7F);
        return new MapLocation(x,y);
    }

    int getActualValue(int reference, int coordinate){
        int dif = coordinate - (reference%MAX_MAP_SIZE2);
        if (dif >= MAX_MAP_SIZE) dif -= MAX_MAP_SIZE2;
        else if (dif <= -MAX_MAP_SIZE) dif += MAX_MAP_SIZE2;
        return reference + dif;
    }


    void processBoundX(int content, int reference, boolean known){
        Integer l = getLow(content, reference), u = getHigh(content, reference);
        //boolean echo = false;
        //System.out.println("Processing X " + content + " " +  l + " " + u);
        if (l != null) {
            if (lbX == null) {
                lbX = new BoundInfo(l, known);
                //echo = true;
            }
            lbX.known |= known;
        }
        if (u != null) {
            if (ubX == null) {
                ubX = new BoundInfo(u, known);
                //echo = true;
            }
            ubX.known |= known;
        }
        //if (meEC && echo) emergencyMessage = getMessage(MType.X, lbX, ubX, false);
    }

    void processBoundY(int content, int reference, boolean known){
        Integer l = getLow(content, reference), u = getHigh(content, reference);
        //boolean echo = false;
        //System.out.println("Processing Y " + content + " " +  l + " " + u);
        if (l != null) {
            if (lbY == null) {
                lbY = new BoundInfo(l, known);
                //echo = true;
            }
            lbY.known |= known;
        }
        if (u != null) {
            if (ubY == null) {
                ubY = new BoundInfo(u, known);
                //echo = true;
            }
            ubY.known |= known;
        }
        //if (meEC && echo) emergencyMessage = getMessage(MType.Y, lbY, ubY, false);
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

    int getMessageNonEC(int id){
        return ((id - MIN_ID) << 7) | myTeam.ordinal();
    }

    /*boolean running(){
        return
                //ecMapLocations.isRunning ||
                        idTracker.isRunning;
    }*/

    /*void run(){
        //ecMapLocations.run();
        //idTracker.run();
    }*/

    int getID(int content){
        return (content >>> 7) + MIN_ID;
    }

    int getNeutralID(int content){
        return (content >>> 9) + MIN_ID;
    }

    int getExtraElement (int content){
        return (content & 0x7F);
    }

    int getInfluence(int content){
        return (content&0x1FF);
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
            //System.err.println("Advancing from type " + currentType);
            if (currentType != MType.EC_ID.ordinal()) advanceType();
            else{
                if (currentListElement == null) advanceType();
                else{
                    if (subIndex >= 3){
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
            advance();
            currentTypeCopy = currentType;
            currentListElementCopy = currentListElement;
            subIndexCopy = subIndex;
        }

        boolean forNonEC(){
            switch (messageTypes[currentType]) {
                case NEUTRAL_INF:
                case EC_X:
                case EC_Y:
                case ENEMY_MUCKRAKER:
                case NON_EC_ID:
                    return false;
                case X:
                case Y:
                case H_SYM:
                case V_SYM:
                case R_SYM:
                    return true;
            }
            return subIndex == 0;
        }

        Integer getFlag(boolean unknown) {
            switch (messageTypes[currentType]) {
                case ENEMY_MUCKRAKER:
                case EC_X:
                case EC_Y:
                case NEUTRAL_INF:
                case NON_EC_ID:
                    return null;
                case X:
                    return getMessage(MType.X, lbX, ubX, unknown);
                case Y:
                    return getMessage(MType.Y, lbY, ubY, unknown);
                case EC_ID:
                    return getMessageEC(currentListElement, subIndex, unknown);
                case H_SYM:
                    if (!unknown && !H_SYM) return H_MSG;
                    return null;
                case V_SYM:
                    if (!unknown && !V_SYM) return V_MSG;
                    return null;
                case R_SYM:
                    if (!unknown && !R_SYM) return R_MSG;
                    return null;
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
        Integer influence = null;
        boolean knownID = false, knownLocX = false, knownLocY = false, knownInfluence = false;
        private MapLocation loc = null;
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
            if (EC) ecMapLocations.add(loc);
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

        Integer getContentInfluence(){
            if (influence == null) return null;
            return ((id - MIN_ID) << 9) | influence;
        }

    }

    class IDTracker {

        final int IDNumber = 1024;
        final int INT_BITS = 32;
        final int MAX_ID = IDNumber*INT_BITS;

        int[] IDs = new int[IDNumber];

        IDTracker(){
        }

        void addID(int id){
            if (id >= MAX_ID) return;
            IDs[id/INT_BITS] |= (1 << (id%INT_BITS));
        }

        boolean checkID(int id){
            return (IDs[id/INT_BITS] & (1 << (id%INT_BITS))) > 0;
        }

    }

    //other

    Integer getECDistDiff(MapLocation loc){
        int minDistAlly = -1, minDistEnemy = -1;
        for (RInfo r = firstEC; r != null; r = r.nextInfo){
            if (r.getMapLocation() == null) continue;
            if (r.team == myTeamIndex) {
                int dist = r.getMapLocation().distanceSquaredTo(loc);
                if (minDistAlly < 0 || minDistAlly > dist) minDistAlly = dist;
            } else if (r.team == enemyTeamIndex){
                int dist = r.getMapLocation().distanceSquaredTo(loc);
                if (minDistEnemy < 0 || minDistEnemy > dist) minDistEnemy = dist;
            }
        }
        if (minDistAlly < 0 || minDistEnemy < 0) return null;
        return minDistEnemy - minDistAlly;
    }

    MapLocation getClosestEnemyEC(){
        MapLocation ans = null;
        int minDist = -1;
        for (RInfo r = firstEC; r != null; r = r.nextInfo){
            if (r.getMapLocation() == null) continue;
            if (r.team != rc.getTeam().opponent().ordinal()) continue;
            int dist = r.getMapLocation().distanceSquaredTo(rc.getLocation());
            if (minDist < 0 || minDist > dist){
                minDist = dist;
                ans = r.getMapLocation();
            }
        }
        return ans;
    }

    MapLocation getClosestEC(){
        MapLocation ans = null;
        int minDist = -1;
        for (RInfo r = firstEC; r != null; r = r.nextInfo){
            if (r.getMapLocation() == null) continue;
            if (r.team != rc.getTeam().ordinal()) continue;
            int dist = r.getMapLocation().distanceSquaredTo(rc.getLocation());
            if (minDist < 0 || minDist > dist){
                minDist = dist;
                ans = r.getMapLocation();
            }
        }
        return ans;
    }


    RInfo getClosestNeutralEC(int conviction){
        RInfo ans = null;
        int minDist = -1;
        for (RInfo r = firstEC; r != null; r = r.nextInfo){
            if (r.getMapLocation() == null) continue;
            if (r.team != Team.NEUTRAL.ordinal()) continue;
            if (r.influence == null) continue;
            if (r.influence >= conviction) continue;
            int dist = r.getMapLocation().distanceSquaredTo(rc.getLocation());
            if (minDist < 0 || minDist > dist){
                minDist = dist;
                ans = r;
            }
        }
        return ans;
    }

    RInfo getBestNeutralEC(){
        RInfo ans = null;
        int minValue = 0;
        int x = rc.getLocation().x;
        int y = rc.getLocation().y;
        for (RInfo r = firstEC; r != null; r = r.nextInfo){
            if (r.getMapLocation() == null) continue;
            if (r.team != Team.NEUTRAL.ordinal()) continue;
            if (r.influence == null) continue;
            int dx = Math.abs(x - r.getMapLocation().x), dy = Math.abs(y - r.getMapLocation().y);
            int v = (Math.max(dx, dy) + 5)*r.influence;
            if (ans == null || v < minValue){
                minValue = v;
                ans = r;
            }
        }
        return ans;
    }

    /*boolean everythingCaptured(){
        int cont = 0;
        for (RInfo r = firstEC; r != null; r = r.nextInfo){
            ++cont;
            if (r.team ==enemyTeamIndex) return false;
        }
        return cont == ecMapLocations.lastArrayElement;
    }*/

    Integer getArea(){
        if (ubX == null) return null;
        if (ubY == null) return null;
        if (lbX == null) return null;
        if (lbY == null) return null;
        int dX = ubX.value - lbX.value - 1;
        int dY = ubY.value - lbY.value - 1;
        if (dX > MAX_MAP_SIZE) return null;
        if (dY > MAX_MAP_SIZE) return null;
        return dX*dY;
    }

    boolean dominating(){
        Integer a = getArea();
        if (a == null) return false;
        return rc.getRobotCount()*4 >= a;
    }

    MapLocation getECLoc(){
        if (myEC == null) return null;
        return myEC.getMapLocation();
    }

    boolean criticalZone(){
        RobotInfo[] allies = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, myTeam);
        for (RobotInfo r : allies) if (r.getType() == RobotType.SLANDERER) return true;
        return false;
        /*MapLocation ec = getClosestEC();
        if (ec == null) return false;
        MapLocation enemyEC = getClosestEnemyEC();
        if (enemyEC != null && rc.getLocation().distanceSquaredTo(ec) > rc.getLocation().distanceSquaredTo(enemyEC)) return false;
        if (ec == null) return false;
        int dx = rc.getLocation().x - ec.x;
        int dy = rc.getLocation().y - ec.y;
        int dist = Math.max(Math.abs(dx),Math.abs(dy));
        return (dist <= 11);*/
    }

    boolean hBounds(){
        if (lbX != null && ubX != null) return true;
        return false;
    }

    boolean vBounds(){
        if (lbY != null && ubY != null) return true;
        return false;
    }

    MapLocation getHSym(MapLocation loc){
        if (lbX == null) return null;
        if (ubX == null) return null;
        return new MapLocation(lbX.value + ubX.value - loc.x, loc.y);
    }

    MapLocation getVSym(MapLocation loc){
        if (lbY == null) return null;
        if (ubY == null) return null;
        return new MapLocation(loc.x, lbY.value + ubY.value - loc.y);
    }

    MapLocation getRSym(MapLocation loc){
        if (lbX == null) return null;
        if (ubX == null) return null;
        if (lbY == null) return null;
        if (ubY == null) return null;
        return new MapLocation(lbX.value + ubX.value - loc.x, lbY.value + ubY.value - loc.y);
    }

    void checkLocations(){
        for (RInfo r = firstEC; r != null; r = r.nextInfo){
            if (Clock.getBytecodesLeft() < 500) return;
            r.getMapLocation();
        }
    }

    void checkSymmetry(){
        try {
            if (meSlanderer) return;
            checkLocations();
            for (int i = ecMapLocations.lastArrayElement; i-- > 0; ) {
                if (Clock.getBytecodesLeft() < 500) return;
                MapLocation loc = ecMapLocations.locArray[i];
                //rc.setIndicatorDot(loc, 0, 0, 0);
                MapLocation hLoc = getHSym(loc);
                //if (hLoc != null) rc.setIndicatorDot(hLoc, 255, 0, 0);
                if (hLoc != null && rc.canSenseLocation(hLoc)) {
                    RobotInfo r = rc.senseRobotAtLocation(hLoc);
                    if (r == null || r.getType() != RobotType.ENLIGHTENMENT_CENTER) H_SYM = false;
                }
                MapLocation vLoc = getVSym(loc);
                //if (vLoc != null) rc.setIndicatorDot(vLoc, 0, 255, 0);
                if (vLoc != null && rc.canSenseLocation(vLoc)) {
                    RobotInfo r = rc.senseRobotAtLocation(vLoc);
                    if (r == null || r.getType() != RobotType.ENLIGHTENMENT_CENTER) V_SYM = false;
                }
                MapLocation rLoc = getRSym(loc);
                //if (rLoc != null) rc.setIndicatorDot(rLoc, 0, 0, 255);
                if (rLoc != null && rc.canSenseLocation(rLoc)) {
                    RobotInfo r = rc.senseRobotAtLocation(rLoc);
                    if (r == null || r.getType() != RobotType.ENLIGHTENMENT_CENTER) R_SYM = false;
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    MapTracker enemyChecked = new MapTracker();
    final int MAX_BYTECODE_LEFT = 6500;

    /*MapLocation getClosestEnemyEC(){
        int btc = Clock.getBytecodeNum();
        enemyChecked.reset();
        System.err.println("Reset costs: " + (Clock.getBytecodeNum() - btc));
        MapLocation ans = null;
        int minDist = -1;
        for (RInfo r = firstEC; r != null; r = r.nextInfo){
            if (r.getMapLocation() == null) continue;
            enemyChecked.add(r.getMapLocation());
            if (r.team != rc.getTeam().opponent().ordinal()) continue;
            int dist = r.getMapLocation().distanceSquaredTo(rc.getLocation());
            if (minDist < 0 || minDist > dist){
                minDist = dist;
                ans = r.getMapLocation();
            }
        }

        if (ans != null) return ans;
        for (int i = ecMapLocations.lastArrayElement; i-- > 0; ){
            MapLocation loc = ecMapLocations.locArray[i];
            if (Clock.getBytecodesLeft() < MAX_BYTECODE_LEFT) return ans;
            if (enemyChecked.check(loc)) continue;
            int dist = loc.distanceSquaredTo(rc.getLocation());
            if (minDist < 0 || minDist > dist){
                minDist = dist;
                ans = loc;
            }
        }

        if (ans != null) return ans;

        for (int i = ecMapLocations.lastArrayElement; i-- > 0; ){
            MapLocation loc = ecMapLocations.locArray[i];
            if (Clock.getBytecodesLeft() < MAX_BYTECODE_LEFT) return ans;
            if (H_SYM) {
                MapLocation newLoc = getHSym(loc);
                if (newLoc != null && !enemyChecked.check(newLoc)) {
                    int dist = loc.distanceSquaredTo(rc.getLocation());
                    if (minDist < 0 || minDist > dist) {
                        minDist = dist;
                        ans = loc;
                    }
                }
            }
            if (V_SYM) {
                MapLocation newLoc = getVSym(loc);
                if (newLoc != null && !enemyChecked.check(newLoc)) {
                    int dist = loc.distanceSquaredTo(rc.getLocation());
                    if (minDist < 0 || minDist > dist) {
                        minDist = dist;
                        ans = loc;
                    }
                }
            }
            if (R_SYM){
                MapLocation newLoc = getRSym(loc);
                if (newLoc != null && !enemyChecked.check(newLoc)){
                    int dist = loc.distanceSquaredTo(rc.getLocation());
                    if (minDist < 0 || minDist > dist){
                        minDist = dist;
                        ans = loc;
                    }
                }
            }
        }
        return ans;
    }*/

    void debugSymmetry(){
        System.err.println("Horizontal: " + H_SYM);
        System.err.println("Vertical: " + V_SYM);
        System.err.println("Rotational: " + R_SYM);
    }

}
