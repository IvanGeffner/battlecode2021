package twentythree;

import battlecode.common.*;

public class Politician extends MyRobot {

    static int[] attackRanges = new int[] {
            1,
            2,
            4,
            5,
            8,
            9
    };

    static int[] damages = null;
    static int[] unbuffedDamage = null;
    static double[] efficiency = null;
    static boolean[] shouldHit = null;

    int PROTECT_TURNS = 30 + 10;
    int turnCreation = 0;

    int MAX_BYTECODE_SEARCH = 9500;

    static final Direction[] dirPathSlanderer = new Direction[]{Direction.NORTHWEST, Direction.NORTHWEST, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.NORTHEAST, Direction.NORTHEAST, Direction.EAST, Direction.EAST, Direction.EAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTHWEST, Direction.SOUTHWEST, Direction.WEST, Direction.WEST, Direction.WEST, Direction.NORTHWEST, Direction.NORTHWEST, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.EAST, Direction.EAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.WEST, Direction.WEST, Direction.NORTHWEST, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.EAST, Direction.EAST, Direction.EAST, Direction.EAST, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.WEST, Direction.WEST, Direction.WEST, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.EAST, Direction.EAST, Direction.SOUTH, Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.CENTER};



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

    static final int MAX_MAP_SIZE = GameConstants.MAP_MAX_HEIGHT;
    static final int MAX_MAP_SIZE2 = 2*MAX_MAP_SIZE;
    Slanderer slanderer = new Slanderer();
    boolean moved = false;

    static Team myTeam, enemyTeam;
    static final double efficiencyMuckraker = 1.5;

    final static int KILL_BONUS = 6;
    final static int STRONG_THRESHOLD = 20;


    boolean explorer = false;
    //boolean shouldBerserk = false;

    boolean criticalZone = false;
    double enemyBonus;
    int myAttackDamage;

    public Politician (RobotController rc){
        super(rc);
        myTeam = rc.getTeam();
        enemyTeam = myTeam.opponent();
        turnCreation = rc.getRoundNum();
        /*if (rc.getInfluence() < 15) {
            if ((int) (Math.random() * 4) == 1) explorer = true;
        }*/
    }

    public void play(){
        //slanderer
        moved = false;
        if (rc.getType() == RobotType.SLANDERER){
            slanderer.play();
            return;
        }
        checkSuicide();
        //politician
        update();
        tryAttack();
        tryMove();
    }

    void checkSuicide(){
        try {
            if (rc.getInfluence() <= GameConstants.EMPOWER_TAX) {
                if (rc.canEmpower(1)) {
                    rc.empower(1);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    void update(){
        criticalZone = comm.criticalZone();
        enemyBonus = rc.getEmpowerFactor(rc.getTeam().opponent(), 0);
        myAttackDamage = getAttackDamage();
    }

    void computeDamages(){
        int ad = getAttackDamage();
        int adb = getAttackDamageUnbuffed();
        if (ad <= 0) return;
        for (int i = attackRanges.length; i-- > 0; ){
            RobotInfo[] robots = rc.senseNearbyRobots(attackRanges[i]);
            int nRobots = robots.length;
            if (nRobots == 0) continue;
            damages[i] = ad/nRobots;
            unbuffedDamage[i] = adb/nRobots;
        }
    }

    void computeEfficiency(){
        MapLocation myLoc = rc.getLocation();
        RobotInfo[] robots = rc.senseNearbyRobots(attackRanges[attackRanges.length-1]);
        boolean ally, neutral;
        boolean str = rc.getConviction() >= STRONG_THRESHOLD;
        double enemyBuff = rc.getEmpowerFactor(rc.getTeam().opponent(), 0);
        for (RobotInfo r : robots){
            int d = r.getLocation().distanceSquaredTo(myLoc);
            int maxInf;
            ally = (r.getTeam() == myTeam);
            neutral = (r.getTeam() == Team.NEUTRAL);
            double e = 1;
            switch (r.getType()) {
                case ENLIGHTENMENT_CENTER:
                    switch (d) {
                        case 1:
                            if (neutral){
                                if (damages[0] > r.getConviction()) {
                                    efficiency[0] += e * damages[0] + GameConstants.EMPOWER_TAX + KILL_BONUS;
                                    shouldHit[0] = true;
                                }
                            }
                            else {
                                if (!ally) {
                                    shouldHit[0] = true;
                                    efficiency[0] += e * damages[0];
                                    if (str) efficiency[0] += GameConstants.EMPOWER_TAX + KILL_BONUS;
                                } else{
                                    efficiency[0] += e * unbuffedDamage[0];
                                }
                            }
                        case 2:
                            if (neutral){
                                if (damages[1] > r.getConviction()) {
                                    efficiency[1] += e * damages[1] + GameConstants.EMPOWER_TAX + KILL_BONUS;
                                    shouldHit[1] = true;
                                }
                            }
                            else {
                                if (!ally) {
                                    shouldHit[1] = true;
                                    efficiency[1] += e * damages[1];
                                    if (str) efficiency[1] += GameConstants.EMPOWER_TAX + KILL_BONUS;
                                } else{
                                    efficiency[1] += e * unbuffedDamage[1];
                                }
                            }
                        case 3:
                        case 4:
                            if (neutral){
                                if (damages[2] > r.getConviction()) {
                                    efficiency[2] += e * damages[2] + GameConstants.EMPOWER_TAX + KILL_BONUS;
                                    shouldHit[2] = true;
                                }
                            }
                            else {
                                if (!ally) {
                                    shouldHit[2] = true;
                                    efficiency[2] += e * damages[2];
                                    if (str) efficiency[2] += GameConstants.EMPOWER_TAX + KILL_BONUS;
                                } else{
                                    efficiency[2] += e * unbuffedDamage[2];
                                }
                            }
                        case 5:
                            if (neutral){
                                if (damages[3] > r.getConviction()) {
                                    efficiency[3] += e * damages[3] + GameConstants.EMPOWER_TAX + KILL_BONUS;
                                    shouldHit[3] = true;
                                }
                            }
                            else {
                                if (!ally) {
                                    shouldHit[3] = true;
                                    efficiency[3] += e * damages[3];
                                    if (str) efficiency[3] += GameConstants.EMPOWER_TAX + KILL_BONUS;
                                } else{
                                    efficiency[3] += e * unbuffedDamage[3];
                                }
                            }
                        case 6:
                        case 7:
                        case 8:
                            if (neutral){
                                if (damages[4] > r.getConviction()) {
                                    efficiency[4] += e * damages[4] + GameConstants.EMPOWER_TAX + KILL_BONUS;
                                    shouldHit[4] = true;
                                }
                            }
                            else {
                                if (!ally) {
                                    shouldHit[4] = true;
                                    efficiency[4] += e * damages[4];
                                    if (str) efficiency[4] += GameConstants.EMPOWER_TAX + KILL_BONUS;
                                } else{
                                    efficiency[4] += e * unbuffedDamage[4];
                                }
                            }
                        case 9:
                            if (neutral){
                                if (damages[5] > r.getConviction()) {
                                    efficiency[5] += e * damages[5] + GameConstants.EMPOWER_TAX + KILL_BONUS;
                                    shouldHit[5] = true;
                                }
                            }
                            else {
                                if (!ally) {
                                    shouldHit[5] = true;
                                    efficiency[5] += e * damages[5];
                                    if (str) efficiency[5] += GameConstants.EMPOWER_TAX + KILL_BONUS;
                                } else{
                                    efficiency[5] += e * unbuffedDamage[5];
                                }
                            }
                    }
                    break;
                case SLANDERER:
                case POLITICIAN:
                case MUCKRAKER:
                    if (ally) maxInf = r.getInfluence() - r.getConviction();
                    else {
                        if (r.getType() == RobotType.MUCKRAKER){
                            maxInf = r.getConviction() + 1;
                            e = efficiencyMuckraker;
                        }
                        else{
                            e = enemyBuff;
                            maxInf = 1 + r.getConviction();
                            switch(r.getInfluence()){
                                case 21:
                                case 41:
                                case 63:
                                case 85:
                                case 107:
                                case 130:
                                case 154:
                                case 178:
                                case 203:
                                case 228:
                                case 255:
                                case 282:
                                case 310:
                                case 339:
                                case 368:
                                case 399:
                                case 431:
                                case 463:
                                case 497:
                                case 532:
                                case 568:
                                case 605:
                                case 643:
                                case 683:
                                case 724:
                                case 766:
                                case 810:
                                case 855:
                                case 902:
                                case 949: break;
                                default:
                                    maxInf += r.getInfluence();
                                    break;
                            }
                            //maxInf = r.getInfluence() + 1 + r.getConviction();
                        }
                    }
                    switch (d) {
                        case 1:
                            if (!ally) shouldHit[0] = true;
                            efficiency[0] += e*Math.min(maxInf, damages[0]);
                            if (!ally && damages[0] > r.getConviction()){
                                efficiency[0] += GameConstants.EMPOWER_TAX + KILL_BONUS;
                            }
                        case 2:
                            if (!ally) shouldHit[1] = true;
                            efficiency[1] += e*Math.min(maxInf, damages[1]);
                            if (!ally && damages[1] > r.getConviction()){
                                efficiency[1] += GameConstants.EMPOWER_TAX + KILL_BONUS;
                            }
                        case 3:
                        case 4:
                            if (!ally) shouldHit[2] = true;
                            efficiency[2] += e*Math.min(maxInf, damages[2]);
                            if (!ally && damages[2] > r.getConviction()){
                                efficiency[2] += GameConstants.EMPOWER_TAX + KILL_BONUS;
                            }
                        case 5:
                            if (!ally) shouldHit[3] = true;
                            efficiency[3] += e*Math.min(maxInf, damages[3]);
                            if (!ally && damages[3] > r.getConviction()){
                                efficiency[3] += GameConstants.EMPOWER_TAX + KILL_BONUS;
                            }
                        case 6:
                        case 7:
                        case 8:
                            if (!ally) shouldHit[4] = true;
                            efficiency[4] += e*Math.min(maxInf, damages[4]);
                            if (!ally && damages[4] > r.getConviction()){
                                efficiency[4] += GameConstants.EMPOWER_TAX + KILL_BONUS;
                            }
                        case 9:
                            if (!ally) shouldHit[5] = true;
                            efficiency[5] += e*Math.min(maxInf, damages[5]);
                            if (!ally && damages[5] > r.getConviction()){
                                efficiency[5] += GameConstants.EMPOWER_TAX + KILL_BONUS;
                            }
                    }
            }
        }
    }

    void tryMove(){
        MapLocation ans = getEfficientEnemy();
        if (ans != null){
            bfs.move(ans);
            return;
        }

        if (tryProtect()) return;


        Communication.RInfo nEC = comm.getClosestNeutralEC(getAttackDamage());
        if (nEC != null && nEC.getMapLocation() != null){
            bfs.move(getBestAdjacent(nEC.getMapLocation()));
            return;
        }
        if (!weak()){
            ans = getGridPosition();
            if (ans != null){
                bfs.move(ans);
                return;
            }
        }

        if (surroundEnemyHQ()) return;


        ans = explore.getExploreTarget();
        if (ans != null){
            bfs.move(ans);
        }
    }

    boolean tryProtect(){
        if (!weak()) return false;
        if (rc.getRoundNum() - turnCreation >= PROTECT_TURNS) return false;
        return surroundOurHQ(25);
    }

    void tryAttack(){
        try {
            damages = new int[attackRanges.length];
            unbuffedDamage = new int[attackRanges.length];
            shouldHit = new boolean[attackRanges.length];
            efficiency = new double[attackRanges.length];

            computeDamages();
            computeEfficiency();

            if (rc.getCooldownTurns() >= 1) return;

            double bestEff = 0;
            int bestRange = -1;
            for (int i = attackRanges.length; i-- > 0;) {
                double e = efficiency[i];
                if (e <= bestEff) continue;
                if (!shouldHit[i]) continue;
                bestRange = attackRanges[i];
                bestEff = e;
            }
            if (bestEff <= 0) return;
            if (bestEff >= rc.getConviction()){
                rc.empower(bestRange);
                moved = true;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    void debugEfficiency(){
        for (int i = attackRanges.length; i-- > 0;) {
            System.err.println("For attack range " + attackRanges[i] + " i have efficiency " + efficiency[i]);
        }
    }

    int getAttackDamage(){
        //int ans = (int)(rc.getEmpowerFactor(myTeam,0)*rc.getConviction()) - GameConstants.EMPOWER_TAX;
        double ans = (rc.getEmpowerFactor(myTeam, 0)*(rc.getConviction() - GameConstants.EMPOWER_TAX));
        if (ans < 0) return 0;
        return (int) ans;
    }

    int getAttackDamageUnbuffed(){
        double ans = (rc.getConviction() - GameConstants.EMPOWER_TAX);
        if (ans < 0) return 0;
        return (int) ans;
    }

    static final int[] EC_X = new int[]{-1, 0, 1, 0};
    static final int[] EC_Y = new int[]{0, 1, 0, -1};

    MapLocation getGridPosition(){
        try {
            MapLocation loc = comm.getClosestEnemyEC();
            if (loc == null) return null;
            if (loc.distanceSquaredTo(rc.getLocation()) == 1) return rc.getLocation();
            for (int i = EC_X.length; i-- > 0; ) {
                MapLocation target = new MapLocation(loc.x + EC_X[i], loc.y + EC_Y[i]);
                if (rc.canSenseLocation(target) && !rc.isLocationOccupied(target)) return target;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    double getEfficiency(RobotType r, int conv, int infl){
        int maxInf = conv+1;
        double ef = 1;
        switch(r) {
            case MUCKRAKER:
                ef = efficiencyMuckraker;
                break;
            case POLITICIAN:
            case SLANDERER:
                switch (infl) {
                    case 21:
                    case 41:
                    case 63:
                    case 85:
                    case 107:
                    case 130:
                    case 154:
                    case 178:
                    case 203:
                    case 228:
                    case 255:
                    case 282:
                    case 310:
                    case 339:
                    case 368:
                    case 399:
                    case 431:
                    case 463:
                    case 497:
                    case 532:
                    case 568:
                    case 605:
                    case 643:
                    case 683:
                    case 724:
                    case 766:
                    case 810:
                    case 855:
                    case 902:
                    case 949:
                        break;
                    default:
                        ef = enemyBonus;
                        maxInf += infl;
                        break;
                }
        }
        int bonusKill = 0;
        if (myAttackDamage > conv) bonusKill = GameConstants.EMPOWER_TAX + KILL_BONUS;
        return ef*Math.min(myAttackDamage, maxInf) + bonusKill;
    }

    MapLocation getEfficientEnemy(){
        MapLocation myLoc = rc.getLocation();
        MapLocation bestLoc = null;
        int bestDist = 0;
        RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, rc.getTeam().opponent());
        for (RobotInfo r : robots){
            if (Clock.getBytecodeNum() > MAX_BYTECODE_SEARCH) break;
            double e = getEfficiency(r.getType(), r.getConviction(), r.getInfluence());
            if (e < rc.getConviction()) continue;
            MapLocation adjLoc = getBestAdjacent(r.getLocation());
            int d = r.getLocation().distanceSquaredTo(adjLoc);
            if (bestLoc == null || bestDist > d){
                bestDist = d;
                bestLoc = adjLoc;
            }
        }

        if (bestLoc != null) return bestLoc;
        //TODO

        for (Communication.RInfo r = comm.firstNonEC; r != null; r = r.nextInfo){
            if (Clock.getBytecodeNum() > MAX_BYTECODE_SEARCH) return bestLoc;
            MapLocation loc = r.getMapLocation();
            if (loc == null) continue;
            rc.setIndicatorDot(loc, 0, 255, 0);
            double e = getEfficiency(RobotType.MUCKRAKER, r.influence, r.influence);
            if (e < rc.getConviction()) continue;
            int d = loc.distanceSquaredTo(myLoc);
            if (bestLoc == null || bestDist > d){
                bestDist = d;
                bestLoc = loc;
            }
        }
        return bestLoc;
    }

    MapLocation getBestAdjacent(MapLocation loc){

        try {

            MapLocation myLoc = rc.getLocation();

            MapLocation ans = loc;
            Integer bestDist = null;

            MapLocation newLoc = loc.add(Direction.NORTH);
            if (rc.canSenseLocation(newLoc)) {
                if (rc.onTheMap(newLoc)) {
                    if (!rc.isLocationOccupied(newLoc)) {
                        int d = myLoc.distanceSquaredTo(newLoc);
                        if (bestDist == null || d < bestDist){
                            bestDist = d;
                            ans = newLoc;
                        }
                    }
                }
            }

            newLoc = loc.add(Direction.WEST);
            if (rc.canSenseLocation(newLoc)) {
                if (rc.onTheMap(newLoc)) {
                    if (!rc.isLocationOccupied(newLoc)) {
                        int d = myLoc.distanceSquaredTo(newLoc);
                        if (bestDist == null || d < bestDist){
                            bestDist = d;
                            ans = newLoc;
                        }
                    }
                }
            }

            newLoc = loc.add(Direction.EAST);
            if (rc.canSenseLocation(newLoc)) {
                if (rc.onTheMap(newLoc)) {
                    if (!rc.isLocationOccupied(newLoc)) {
                        int d = myLoc.distanceSquaredTo(newLoc);
                        if (bestDist == null || d < bestDist){
                            bestDist = d;
                            ans = newLoc;
                        }
                    }
                }
            }

            newLoc = loc.add(Direction.SOUTH);
            if (rc.canSenseLocation(newLoc)) {
                if (rc.onTheMap(newLoc)) {
                    if (!rc.isLocationOccupied(newLoc)) {
                        int d = myLoc.distanceSquaredTo(newLoc);
                        if (bestDist == null || d < bestDist){
                            //bestDist = d;
                            ans = newLoc;
                        }
                    }
                }
            }
            if (ans != null) return ans;
        } catch (Exception e){
            e.printStackTrace();
        }
        return loc;
    }

    boolean weak(){
        return rc.getConviction() < STRONG_THRESHOLD;
    }





    /* Slanderer */
    class Slanderer {

        final int MUCKRAKER_MEMORY = 10;

        final int fleeBytecode = 1500;
        final double muckrakerActionDist = RobotType.MUCKRAKER.actionRadiusSquared;

        MapLocation farLocation = null;
        Integer farValue = 0;
        RobotInfo lastEnemyMuckrakerSeen = null;
        int turnSeen = 0;

        public Slanderer(){}

        public void play(){
            tryMoveS();
        }

        void tryMoveS(){
            if (flee()){
                return;
            }

            if (moveToLattice()){
                return;
            }

            if (surroundOurHQ(Util.SAFETY_DISTANCE_OUR_HQ)){
                return;
            }
        }

        boolean flee(){
            try {
                if (rc.getCooldownTurns() >= 1) return true;
                MapLocation myLoc = rc.getLocation();
                int minDist[] = computeMinDistsToMuckraker();
                if (minDist[0] == 0) return false;

                int minValue = minDist[Direction.CENTER.ordinal()];
                double bestValue = 0;
                Direction bestDir = null;

                for (int i = directions.length; i-- > 0; ) {
                    Direction dir = directions[i];
                    if (dir == Direction.CENTER) continue;
                    if (!rc.canMove(dir)) continue;
                    double value = getFleeValue(minDist[i] - minValue, rc.sensePassability(myLoc.add(dir)));
                    if (bestDir == null || value > bestValue){
                        bestValue = value;
                        bestDir = dir;
                    }
                }
                if (bestDir != null){
                    moved = true;
                    if (bestDir != Direction.CENTER) rc.move(bestDir);
                    return true;
                }
            } catch (Exception e){
                e.printStackTrace();
            }
            return false;
        }

        double getFleeValue(int muckrakerDist, double p){
            //double dif = muckrakerDist - muckrakerActionDist;
            double dif = muckrakerDist;
            if (dif <= 0) return 0;
            return (p+0.01)*dif;
        }

        int[] computeMinDistsToMuckraker(){
            //System.err.println("Computing muckdists at bytecode " + Clock.getBytecodesLeft());
            MapLocation myLoc = rc.getLocation();
            int[] muckDists = new int[directions.length];
            RobotInfo[] robots = rc.senseNearbyRobots(RobotType.SLANDERER.sensorRadiusSquared, enemyTeam);
            for (RobotInfo r : robots){
                if (Clock.getBytecodesLeft() < fleeBytecode){
                    //System.err.println("Not enough bytecode!!");
                    return muckDists;
                }
                if (r.getType() != RobotType.MUCKRAKER) continue;
                MapLocation loc = r.getLocation();
                for (int i = directions.length; i-- > 0; ) {
                    int d = myLoc.add(directions[i]).distanceSquaredTo(loc) + 1;
                    int md = muckDists[i];
                    if (md == 0 || md > d) {
                        muckDists[i] = d;
                    }
                }
            }

            //if (muckDists[0] > 0) return muckDists;


            if (explore.closestMuckraker != null && rc.getRoundNum()  - explore.closestMuckrakerSeenRound <= MUCKRAKER_MEMORY){
                MapLocation loc = explore.closestMuckraker.getLocation();
                for (int i = directions.length; i-- > 0; ) {
                    int d = myLoc.add(directions[i]).distanceSquaredTo(loc) + 1;
                    int md = muckDists[i];
                    if (md == 0 || md > d) {
                        muckDists[i] = d;
                    }
                }
            }

            for (Communication.RInfo r = comm.firstNonEC; r != null; r = r.nextInfo){
                if (Clock.getBytecodesLeft() < fleeBytecode) return muckDists;
                MapLocation loc = r.getMapLocation();
                if (loc == null) continue;
                rc.setIndicatorLine(myLoc, loc, 0, 0, 0);
                for (int i = directions.length; i-- > 0; ) {
                    int d = myLoc.add(directions[i]).distanceSquaredTo(loc) + 1;
                    int md = muckDists[i];
                    if (md == 0 || md > d) {
                        muckDists[i] = d;
                    }
                }
            }
            return muckDists;
        }

        MapLocation bestLatticeLoc = null;
        static final int MIN_LATTICE_DIST = 5;
        static final int MAX_BYTECODE_REMAINING = 1500;

        boolean moveToLattice() {
            try {
                MapLocation currentLoc = rc.getLocation();
                MapLocation ecLoc = comm.getClosestEC();
                MapLocation bestLoc = null;
                MapLocation myLoc = rc.getLocation();
                int bestDist = 0;
                if (ecLoc == null) return false;
                int congruence = (ecLoc.x + ecLoc.y + 1) % 2;

                if ((myLoc.x + myLoc.y)%2 == congruence && myLoc.distanceSquaredTo(ecLoc) >= MIN_LATTICE_DIST){
                    bestDist = myLoc.distanceSquaredTo(ecLoc);
                    bestLoc = myLoc;
                }

                for (int i = dirPathSlanderer.length; i-- > 0; ) {
                    if (Clock.getBytecodesLeft() < MAX_BYTECODE_REMAINING) break;
                    currentLoc = currentLoc.add(dirPathSlanderer[i]);
                    if ((currentLoc.x + currentLoc.y) % 2 != congruence) continue;
                    if (!rc.onTheMap(currentLoc)) continue;
                    if (rc.isLocationOccupied(currentLoc)) continue;

                    int d = currentLoc.distanceSquaredTo(ecLoc);

                    if (d < MIN_LATTICE_DIST) continue;

                    if (bestLoc == null  || d < bestDist){
                        bestLoc = currentLoc;
                        bestDist = d;
                    }
                }
                if (bestLoc != null){
                    bfs.move(bestLoc, true);
                    //rc.setIndicatorLine(rc.getLocation(), bestLoc, 0, 0, 0 );
                    return true;
                }
            } catch (Exception e){
                e.printStackTrace();
            }

            return false;
        }

    }

}
