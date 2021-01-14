package thirdbot;

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

    Team myTeam, enemyTeam;
    final double efficiencyThreshold = 0.75;
    //boolean shouldBerserk = false;

    public Politician (RobotController rc){
        super(rc);
        myTeam = rc.getTeam();
        enemyTeam = myTeam.opponent();
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
        MapLocation loc = getTarget();
        if (loc != null) path.move(loc);
        else explore2();
    }

    void tryAttack(){
        try {
            //shouldBerserk = berserk();
            if (rc.getCooldownTurns() >= 1) return;
            int efficiency = 0;
            int bestRange = -1;
            for (int attackRange : attackRanges) {
                int e = getEfficiency(attackRange);
                if (e == 0) continue;
                //System.out.println("Efficiency at range " + attackRange + ": " + e);
                if (e > efficiency) {
                    bestRange = attackRange;
                    efficiency = e;
                }
            }
            if (efficiency <= 0) return;
            if (berserk() || efficiency >= minConviction()){
                rc.empower(bestRange);
                moved = true;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    double minConviction(){
        double s = rc.getConviction() - GameConstants.EMPOWER_TAX;
        return s*efficiencyThreshold;
    }

    int getAttackDamage(){
        return (int)(rc.getEmpowerFactor(myTeam,0)*rc.getConviction()) - GameConstants.EMPOWER_TAX;
    }

    //TODO it doesn't attack neutral units;
    int getEfficiency(int range){
        RobotInfo[] robots = rc.senseNearbyRobots(range);
        if (robots.length == 0) return 0;
        int conv = getAttackDamage();
        if (conv <= 0) return 0;
        int baseAttack = conv/robots.length;
        int res = conv%robots.length;
        if (res > 0 && res + GameConstants.EMPOWER_TAX >= robots.length) ++baseAttack;
        int ans = 0;

        //enemies
        robots = rc.senseNearbyRobots(range, enemyTeam);
        for (RobotInfo r : robots){
            //if (r.getTeam() == myTeam) continue;
            int enemyConv = r.getConviction();
            switch(r.getType()){
                case ENLIGHTENMENT_CENTER:
                    ans += baseAttack;
                    break;
                default:
                    if (enemyConv < baseAttack) ans+= Math.max(1, enemyConv) + 1;
            }
        }
        return ans;
    }

    MapLocation getTarget(){
        MapLocation ans = getEfficientEnemy();
        if (ans != null) return ans;
        return comm.getClosestEnemyEC();
    }

    MapLocation getEfficientEnemy(){
        MapLocation myLoc = rc.getLocation();
        int ad = getAttackDamage();
        MapLocation bestLoc = null;
        int bestDist = 0;
        RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, rc.getTeam().opponent());
        for (RobotInfo r : robots){
            int enemyConv = Math.max(1, r.getConviction());
            if (enemyConv >= ad) continue;
            if (enemyConv + 1 < minConviction()) continue;
            int d = r.getLocation().distanceSquaredTo(myLoc);
            if (bestLoc == null || bestDist > d){
                bestDist = d;
                bestLoc = r.getLocation();
            }
        }
        return bestLoc;
    }





    /* Slanderer */
    class Slanderer {

        MapLocation farLocation = null;
        Integer farValue = 0;

        public Slanderer(){}

        public void play(){
            flee();
            moveFar();
            if (!moved) explore();
            computeFarLocation();
        }

        void flee(){
            try {
                if (moved) return;
                if (rc.getCooldownTurns() >= 1) return;
                int minDist[] = new int[directions.length];
                MapLocation myLoc = rc.getLocation();
                RobotInfo[] robots = rc.senseNearbyRobots(RobotType.SLANDERER.sensorRadiusSquared, rc.getTeam().opponent());
                for (RobotInfo r : robots) {
                    if (r.getType() != RobotType.MUCKRAKER) continue;
                    MapLocation loc = r.getLocation();
                    for (int i = directions.length; i-- > 0; ) {
                        int d = myLoc.add(directions[i]).distanceSquaredTo(loc) + 1;
                        int md = minDist[i];
                        if (md == 0 || md > d) {
                            minDist[i] = d;
                        }
                    }
                }

                Direction toMoveMaxDist = null;
                Direction toMoveMinCD = null;
                int maxDist = 0;
                double minCD = 0;
                for (int i = directions.length; i-- > 0; ) {
                    Direction dir = directions[i];
                    if (!rc.canMove(dir)) continue;
                    int d = minDist[i];
                    if (d == 0) return;
                    if (toMoveMaxDist == null || d > maxDist) {
                        maxDist = d;
                        toMoveMaxDist = dir;
                    }
                    if (d > RobotType.MUCKRAKER.actionRadiusSquared){
                        double cd = rc.sensePassability(myLoc.add(dir));
                        if (toMoveMinCD == null || cd < minCD) {
                            minCD = cd;
                            toMoveMinCD = dir;
                        }
                    }
                }
                if (toMoveMinCD != null){
                    moved = true;
                    rc.move(toMoveMinCD);
                    return;
                }
                if (toMoveMaxDist != null) {
                    moved = true;
                    rc.move(toMoveMaxDist);
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        void moveFar(){
            if (moved) return;
            if (rc.getCooldownTurns() >= 1) return;
            if (farLocation != null) {
                moved = true;
                rc.setIndicatorDot(farLocation, 0, 0, 0);
                path.move(farLocation);
            }
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

    }

}
