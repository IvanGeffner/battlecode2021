package elevenb;

import battlecode.common.*;

public class Politician extends MyRobot {

    int[] attackRanges = new int[] {
            1,
            2,
            4,
            5,
            8,
            9
    };

    int[] damages = null;
    double[] efficiency = null;
    boolean[] kill = null;

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

    final static int KILL_BONUS = 5;
    final static int STRONG_THRESHOLD = 100;

    boolean explorer = false;
    //boolean shouldBerserk = false;

    public Politician (RobotController rc){
        super(rc);
        myTeam = rc.getTeam();
        enemyTeam = myTeam.opponent();
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
        //politician
        tryAttack();
        tryMove();
        //debugEfficiency();
    }

    void computeDamages(){
        int ad = getAttackDamage();
        //System.err.println("Attack Damage: " + ad);
        if (ad <= 0) return;
        for (int i = attackRanges.length; i-- > 0; ){
            RobotInfo[] robots = rc.senseNearbyRobots(attackRanges[i]);
            int nRobots = robots.length;
            //System.err.println("Sensing " + nRobots + " robots at distance " + attackRanges[i]);
            if (nRobots == 0) continue;
            damages[i] = ad/nRobots;
            //int res = ad%nRobots;
            //if (res > 0 && res + GameConstants.EMPOWER_TAX >= nRobots) ++damages[i];
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
                                    kill[0] = true;
                                }
                            }
                            else {
                                efficiency[0] += e * damages[0];
                                if (!ally) {
                                    kill[0] = true;
                                    if (str) efficiency[0] += GameConstants.EMPOWER_TAX + KILL_BONUS;
                                }
                            }
                        case 2:
                            if (neutral){
                                if (damages[1] > r.getConviction()) {
                                    efficiency[1] += e * damages[1] + GameConstants.EMPOWER_TAX + KILL_BONUS;
                                    kill[1] = true;
                                }
                            }
                            else {
                                efficiency[1] += e * damages[1];
                                if (!ally) {
                                    kill[1] = true;
                                    if (str) efficiency[1] += GameConstants.EMPOWER_TAX + KILL_BONUS;
                                }
                            }
                        case 3:
                        case 4:
                            if (neutral){
                                if (damages[2] > r.getConviction()) {
                                    efficiency[2] += e * damages[2] + GameConstants.EMPOWER_TAX + KILL_BONUS;
                                    kill[2] = true;
                                }
                            }
                            else {
                                efficiency[2] += e * damages[2];
                                if (!ally) {
                                    kill[2] = true;
                                    if (str) efficiency[2] += GameConstants.EMPOWER_TAX + KILL_BONUS;
                                }
                            }
                        case 5:
                            if (neutral){
                                if (damages[3] > r.getConviction()) {
                                    efficiency[3] += e * damages[3] + GameConstants.EMPOWER_TAX + KILL_BONUS;
                                    kill[3] = true;
                                }
                            }
                            else {
                                efficiency[3] += e * damages[3];
                                if (!ally) {
                                    kill[3] = true;
                                    if (str) efficiency[3] += GameConstants.EMPOWER_TAX + KILL_BONUS;
                                }
                            }
                        case 6:
                        case 7:
                        case 8:
                            if (neutral){
                                if (damages[4] > r.getConviction()) {
                                    efficiency[4] += e * damages[4] + GameConstants.EMPOWER_TAX + KILL_BONUS;
                                    kill[4] = true;
                                }
                            }
                            else {
                                efficiency[4] += e * damages[4];
                                if (!ally) {
                                    kill[4] = true;
                                    if (str) efficiency[4] += GameConstants.EMPOWER_TAX + KILL_BONUS;
                                }
                            }
                        case 9:
                            if (neutral){
                                if (damages[5] > r.getConviction()) {
                                    efficiency[5] += e * damages[5] + GameConstants.EMPOWER_TAX + KILL_BONUS;
                                    kill[5] = true;
                                }
                            }
                            else {
                                efficiency[5] += e * damages[5];
                                if (!ally) {
                                    kill[5] = true;
                                    if (str) efficiency[5] += GameConstants.EMPOWER_TAX + KILL_BONUS;
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
                            maxInf = r.getConviction() + 1;
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
                                case 949:
                                    maxInf = r.getInfluence() + 1 + r.getConviction();
                                    break;
                            }
                            //maxInf = r.getInfluence() + 1 + r.getConviction();
                        }
                    }
                    switch (d) {
                        case 1:
                            efficiency[0] += e*Math.min(maxInf, damages[0]);
                            if (!ally && damages[0] > r.getConviction()){
                                efficiency[0] += GameConstants.EMPOWER_TAX + KILL_BONUS;
                                kill[0] = true;
                            }
                        case 2:
                            efficiency[1] += e*Math.min(maxInf, damages[1]);
                            if (!ally && damages[1] > r.getConviction()){
                                efficiency[1] += GameConstants.EMPOWER_TAX + KILL_BONUS;
                                kill[1] = true;
                            }
                        case 3:
                        case 4:
                            efficiency[2] += e*Math.min(maxInf, damages[2]);
                            if (!ally && damages[2] > r.getConviction()){
                                efficiency[2] += GameConstants.EMPOWER_TAX + KILL_BONUS;
                                kill[2] = true;
                            }
                        case 5:
                            efficiency[3] += e*Math.min(maxInf, damages[3]);
                            if (!ally && damages[3] > r.getConviction()){
                                efficiency[3] += GameConstants.EMPOWER_TAX + KILL_BONUS;
                                kill[3] = true;
                            }
                        case 6:
                            efficiency[4] += e*Math.min(maxInf, damages[4]);
                            if (!ally && damages[4] > r.getConviction()){
                                efficiency[4] += GameConstants.EMPOWER_TAX + KILL_BONUS;
                                kill[4] = true;
                            }
                        case 7:
                        case 8:
                        case 9:
                            efficiency[5] += e*Math.min(maxInf, damages[5]);
                            if (!ally && damages[5] > r.getConviction()){
                                efficiency[5] += GameConstants.EMPOWER_TAX + KILL_BONUS;
                                kill[5] = true;
                            }
                    }
            }
        }
    }

    void tryMove(){
        MapLocation ans = getEfficientEnemy();
        if (ans != null){
            path.move(ans);
            return;
        }
        Communication.RInfo nEC = comm.getClosestNeutralEC(getAttackDamage());
        if (nEC != null && nEC.getMapLocation() != null){
            path.move(nEC.getMapLocation());
            return;
        }
        /*if (explorer){
            ans = explore.getExplore2Target(EXPLORE_2_BYTECODE_REMAINING);
            if (ans != null) {
                path.move(ans);
                return;
            }
        }*/
        if (!weak()){
            ans = getGridPosition();
            if (ans != null){
                path.move(ans);
                return;
            }
        }
        if (surroundEnemyHQ()) return;
        explore();
    }

    void tryAttack(){
        try {
            damages = new int[attackRanges.length];
            kill = new boolean[attackRanges.length];
            efficiency = new double[attackRanges.length];

            computeDamages();
            computeEfficiency();

            if (rc.getCooldownTurns() >= 1) return;

            //shouldBerserk = berserk();
            double bestEff = 0;
            int bestRange = -1;
            for (int i = attackRanges.length; i-- > 0;) {
                double e = efficiency[i];
                if (e <= bestEff) continue;
                if (!kill[i]) continue;
                //System.out.println("Efficiency at range " + attackRange + ": " + e);
                bestRange = attackRanges[i];
                bestEff = e;
            }
            if (bestEff <= 0) return;
            /*if (berserk() || efficiency >= minConviction()){
                rc.empower(bestRange);
                moved = true;
            }*/
            if (bestEff >= rc.getConviction() || comm.dominating()){
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
        return (int)(rc.getEmpowerFactor(myTeam,0)*rc.getConviction()) - GameConstants.EMPOWER_TAX;
    }

    MapLocation getTarget(){
        MapLocation ans = getEfficientEnemy();
        if (ans != null) return ans;
        if (explorer){
            ans = explore.getExplore2Target(EXPLORE_2_BYTECODE_REMAINING);
            if (ans != null) return ans;
        }
        return comm.getClosestEnemyEC();
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

    MapLocation getEfficientEnemy(){
        MapLocation myLoc = rc.getLocation();
        int ad = getAttackDamage();
        MapLocation bestLoc = null;
        int bestDist = 0;
        RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, rc.getTeam().opponent());
        for (RobotInfo r : robots){
            int enemyConv = r.getConviction();
            if (enemyConv >= ad) continue;
            enemyConv = Math.max(1, enemyConv);
            double ef = 1;
            if (r.getType() == RobotType.MUCKRAKER) ef = efficiencyMuckraker;
            double e = ef*(enemyConv+1) + GameConstants.EMPOWER_TAX + KILL_BONUS;
            if (e < rc.getConviction()) continue;
            int d = r.getLocation().distanceSquaredTo(myLoc);
            if (bestLoc == null || bestDist > d){
                bestDist = d;
                bestLoc = r.getLocation();
            }
        }
        return bestLoc;
    }

    boolean weak(){
        if (rc.getConviction() >= STRONG_THRESHOLD) return false;
        if (rc.getEmpowerFactor(rc.getTeam(), 0)*rc.getConviction() - GameConstants.EMPOWER_TAX < rc.getConviction()) return true;
        return false;
    }





    /* Slanderer */
    class Slanderer {

        final int MUCKRAKER_MEMORY = 10;

        final int fleeBytecode = 1200;
        final double muckrakerActionDist = Math.sqrt(RobotType.MUCKRAKER.actionRadiusSquared);

        MapLocation farLocation = null;
        Integer farValue = 0;
        RobotInfo lastEnemyMuckrakerSeen = null;
        int turnSeen = 0;

        public Slanderer(){}

        public void play(){
            tryMoveS();
            computeFarLocation();
        }

        void tryMoveS(){
            if (flee()){
                System.out.println("Fled");
                return;
            }
            //if (moveFar()) return;
            if (moveToLattice()){
                System.out.println("Moved to lattice");
                return;
            }
            if (surroundOurHQ()){
                System.out.println("Moving around HQ");
                return;
            }
            System.out.println("Exploring");
            explore();
        }

        boolean flee(){
            try {
                if (rc.getCooldownTurns() >= 1) return true;
                MapLocation myLoc = rc.getLocation();
                int minDist[] = computeMinDistsToMuckraker();
                /*RobotInfo[] robots = rc.senseNearbyRobots(RobotType.SLANDERER.sensorRadiusSquared, rc.getTeam().opponent());
                int minDistMuckraker = -1;
                RobotInfo closestMuckraker = null;
                for (RobotInfo r : robots) {
                    if (r.getType() != RobotType.MUCKRAKER) continue;
                    int dist = myLoc.distanceSquaredTo(r.getLocation());
                    if (closestMuckraker == null || dist < minDistMuckraker){
                        minDistMuckraker = dist;
                        closestMuckraker = r;
                    }
                    MapLocation loc = r.getLocation();
                    for (int i = directions.length; i-- > 0; ) {
                        int d = myLoc.add(directions[i]).distanceSquaredTo(loc) + 1;
                        int md = minDist[i];
                        if (md == 0 || md > d) {
                            minDist[i] = d;
                        }
                    }
                }*/
                if (minDist[0] == 0) return false;

                double bestValue = getFleeValue(minDist[Direction.CENTER.ordinal()], rc.sensePassability(myLoc));
                Direction bestDir = Direction.CENTER;

                for (int i = directions.length; i-- > 0; ) {
                    Direction dir = directions[i];
                    if (dir == Direction.CENTER) continue;
                    if (!rc.canMove(dir)) continue;
                    double value = getFleeValue(minDist[i], rc.sensePassability(myLoc.add(dir)));
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = dir;
                    }
                }
                if (bestDir != null){
                    moved = true;
                    if (bestDir != Direction.CENTER) rc.move(bestDir);
                    //System.err.println("Fleeing in direction " + bestDir);
                    return true;
                }
            } catch (Exception e){
                e.printStackTrace();
            }
            return false;
        }

        double getFleeValue(int muckrakerDist, double p){
            double dif = Math.sqrt(muckrakerDist) - muckrakerActionDist;
            if (dif <= 0) return 0;
            return p*dif;
        }

        boolean moveFar(){
            if (rc.getCooldownTurns() >= 1) return true;
            if (farLocation != null) {
                path.move(farLocation);
                return true;
            }
            return false;
        }

        void computeFarLocation(){
            try {

                if (Clock.getBytecodesLeft() < 1000) return;

                if (farLocation != null) farValue = comm.getECDistDiff(farLocation);

                if (Clock.getBytecodesLeft() < 1000) return;

                MapLocation myLoc = rc.getLocation();
                int X = myLoc.x;
                int Y = myLoc.y;
                int dx = (int) (Math.random() * MAX_MAP_SIZE2 - MAX_MAP_SIZE);
                int dy = (int) (Math.random() * MAX_MAP_SIZE2 - MAX_MAP_SIZE);
                MapLocation loc = new MapLocation(X + dx, Y + dy);
                checkLoc(loc);



                Direction dir = directions[(int)(Math.random()*directions.length)];

                for (int i = directions.length; i-- > 0; dir = dir.rotateLeft()) {
                    if (Clock.getBytecodesLeft() < 1000) return;
                    MapLocation newLoc = myLoc.add(dir);
                    if (!rc.onTheMap(newLoc)) continue;
                    if (rc.isLocationOccupied(newLoc)) continue;
                    checkLoc(newLoc);
                }
                /*int X = myLoc.x;
                int Y = myLoc.y;
                int cont = 5;
                while (Clock.getBytecodesLeft() > 2000) {
                    if (--cont <= 0) return;
                    int dx = (int) (Math.random() * MAX_MAP_SIZE2 - MAX_MAP_SIZE);
                    int dy = (int) (Math.random() * MAX_MAP_SIZE2 - MAX_MAP_SIZE);
                    MapLocation loc = new MapLocation(X + dx, Y + dy);
                    checkLoc(loc);
                }*/
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        void checkLoc(MapLocation loc){
            Integer d = comm.getECDistDiff(loc);
            if (d == null) return;
            if (farValue == null || d > farValue){
                farValue = d;
                farLocation = loc;
            }
        }

        int[] computeMinDistsToMuckraker(){
            //System.err.println("Computing muckdists at bytecode " + Clock.getBytecodesLeft());
            MapLocation myLoc = rc.getLocation();
            int[] muckDists = new int[directions.length];
            RobotInfo[] robots = rc.senseNearbyRobots(RobotType.SLANDERER.sensorRadiusSquared, enemyTeam);
            for (RobotInfo r : robots){
                if (Clock.getBytecodesLeft() < fleeBytecode) return muckDists;
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
            if (muckDists[0] > 0) return muckDists;
            if (explore.closestMuckraker != null && rc.getRoundNum()  - explore.closestMuckrakerSeenRound <= MUCKRAKER_MEMORY){
                MapLocation loc = explore.closestMuckraker;
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
        final int MIN_LATTICE_DIST = 5;
        final int MAX_BYTECODE_REMAINING = 1200;

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
                    if (rc.isLocationOccupied(currentLoc)) continue;

                    int d = currentLoc.distanceSquaredTo(ecLoc);

                    if (d < MIN_LATTICE_DIST) continue;

                    if (bestLoc == null  || d < bestDist){
                        bestLoc = currentLoc;
                        bestDist = d;
                    }
                }
                if (bestLoc != null){
                    path.move(bestLoc);
                    rc.setIndicatorLine(rc.getLocation(), bestLoc, 0, 0, 0 );
                    return true;
                }
            } catch (Exception e){
                e.printStackTrace();
            }

            return false;
        }

    }

}
