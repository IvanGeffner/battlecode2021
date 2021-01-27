package twentyfivec;

import battlecode.common.*;

public class Enlightment extends MyRobot {

    //Util: array of directions
    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    int[] buildDirCont = new int[directions.length];

    enum TypeDescription {
        SLANDERER, MUCKRAKER, POLITICIAN, BIG_POLITICIAN, BIG_MUCKRAKER, CONQUEROR, SMALL_POLITICIAN
    }

    TypeDescription[] typeDescriptions = TypeDescription.values();

    double[] typeProportions = new double[TypeDescription.values().length];
    double[] currentProportions = new double[TypeDescription.values().length];
    //double totalCurrentProportions = 0;
    //double totalProportions = 0;

    final double bigMuckrakerOffset = 3;
    final int SLANDERER_ADVANCE = 2;

    final int MIN_BIG_POLI_INF = 200;

    final int BIG_MUCK_INF = 143;

    final int WAIT_TURNS_CONQUER = 20;
    final int SMALL_POLITICIAN_INFLUENCE = 16;

    final int SMALL_POLI_ROUND = 70;

    void updateTypeProportions(){
        //int turnsAlive = rc.getRoundNum() - turnCreation;
        double turns = rc.getRoundNum();
        typeProportions[TypeDescription.SLANDERER.ordinal()] = Math.max(3.0, 3.5 - turns/200);
        typeProportions[TypeDescription.POLITICIAN.ordinal()] = Math.min(4.5, 2.0 + turns/100);
        typeProportions[TypeDescription.BIG_POLITICIAN.ordinal()] = Math.min(2.0, 2.0 + turns/100);
        typeProportions[TypeDescription.MUCKRAKER.ordinal()] = Math.max(2.5, 4.5 - turns/100);
        typeProportions[TypeDescription.BIG_MUCKRAKER.ordinal()] = Math.max(0.5, 0.5);
        typeProportions[TypeDescription.CONQUEROR.ordinal()] = Math.max(10000, 10000);
        typeProportions[TypeDescription.SMALL_POLITICIAN.ordinal()] = Math.min(4.5, 2.0 + turns/100);
    }

    void addBigMuckOffset(){

        currentProportions[TypeDescription.BIG_MUCKRAKER.ordinal()] += bigMuckrakerOffset;
        currentProportions[TypeDescription.CONQUEROR.ordinal()] = -10;

    }

    //Util: default order (TODO: optimize this, make this adaptive?)
    /*TypeDescription[] typeOrder = {
            TypeDescription.SLANDERER,
            TypeDescription.BIG_POLITICIAN,
            TypeDescription.MUCKRAKER,
            TypeDescription.POLITICIAN,
            //TypeDescription.SLANDERER,
            //TypeDescription.POLITICIAN,
            //TypeDescription.MUCKRAKER,
            //TypeDescription.POLITICIAN,
    };*/

    final int strongMuckrakersIncome = 50;


    //Bidding info
    int infBeginning = 0; //influence at the beginning of our turn
    int infEnd = 0; //influence at the end of our turn
    int income = 0; //income (beginning - end)

    //We update these at the beginning of each round
    RobotInfo closestEnemyMuckraker;
    RobotInfo closestEnemyPolitician;
    int enemyCombinedAttack;

    int CDTurns; //cooldown turns (depends on passability)

    //Building info
    //int currentType = 0; //current type we are trying to build
    int smallPoliticianIndex = 1; //politician index (this decides the influence given)
    int muckrakerIndex = 1; //muckraker index (this decides the influence given)

    boolean safe = false;
    boolean localSafe = false;
    int lastTurnEnemy = -100;
    int turnCreation;
    final int MIN_TURNS_SAFE = 40;
    final int MIN_TURNS_WITHOUT_ENEMY = 20;

    final int SAFE_CONVICTION = 4;

    public Enlightment(RobotController rc){
        super(rc);
        infEnd = rc.getInfluence();
        turnCreation = rc.getRoundNum();
        lastTurnEnemy = rc.getRoundNum();
        computeInitialSlandIndex();
        addBigMuckOffset();
        if (rc.getRoundNum() < 10) safe = true;
        try{
            CDTurns = (int) Math.ceil(RobotType.ENLIGHTENMENT_CENTER.actionRadiusSquared/rc.sensePassability(rc.getLocation()));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    //necessary slanderer influence to get one more production/turn.
    final int[] influences = new int[] {
            21,
            41,
            63,
            85,
            107,
            130,
            154,
            178,
            203,
            228,
            255,
            282,
            310,
            339,
            368,
            399,
            431,
            463,
            497,
            532,
            568,
            605,
            643,
            683,
            724,
            766,
            810,
            855,
            902,
            949
    };

    int currentMaxIndex;

    //maximum slanderer we ever build
    final int maxSlandererInf = influences[influences.length - 1];

    void increaseMuckrakerIndex(){
        if (income > strongMuckrakersIncome) ++muckrakerIndex;
    }

    boolean shouldBuild1HPMuck(){
        return (income <= strongMuckrakersIncome);
    }

    int getMaxSlandererIndex(){
         //return Math.min(influences.length - 1, 10 + Math.max(0, income - 50)/5);
        //return influences.length - 1;
        return currentMaxIndex;
    }

    int getMaxSlandererInf(){
        return influences[getMaxSlandererIndex()];
    }

    void computeInitialSlandIndex(){
        for (int i = influences.length; i-- > 0; ){
            if (influences[i] <= rc.getInfluence()){
                currentMaxIndex = i;
                return;
            }
        }
    }

    void checkIndex(int inf){
        if (inf < getMaxSlandererInf()) return;
        for (int i = 0; i < influences.length; ++i){
            if (influences[i] >= inf){
                currentMaxIndex = Math.min(influences.length - 1, i+SLANDERER_ADVANCE);
                return;
            }
        }
    }

    boolean isSafe(){
        if (safe) return true;
        if (rc.getRoundNum() - lastTurnEnemy >= MIN_TURNS_WITHOUT_ENEMY) return true;
        return false;
        //return true;
    }


    public void play(){
        //update income info
        infBeginning = rc.getInfluence();
        income = infBeginning - infEnd;

        //bids (TODO: improve this)
        bid();

        //updates information about environment
        updateStatus();

        //build robots
        buildNewRobots();

        comm.debugNeutralEC();

        //update the influence at the end of turn
        infEnd = rc.getInfluence();
    }

    //Right now this just updates the locations of the closest enemy muckraker and politician (null if none found)
    void updateStatus(){
        if (rc.getRoundNum() - turnCreation >= MIN_TURNS_SAFE) safe = true;

        updateTypeProportions();

        closestEnemyMuckraker = null;
        closestEnemyPolitician = null;
        enemyCombinedAttack = 0;

        /*totalProportions = 0;
        for (int i = 0; i < typeProportions.length; ++i){
            totalProportions += typeProportions[i];
        }*/

        MapLocation myLoc = rc.getLocation();
        double enemyBuff = rc.getEmpowerFactor(rc.getTeam().opponent(), 0);

        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, rc.getTeam().opponent());

        for (RobotInfo r : enemies){
            lastTurnEnemy = rc.getRoundNum();
            switch(r.getType()){
                case POLITICIAN:
                    int d = myLoc.distanceSquaredTo(r.getLocation());
                    if (closestEnemyPolitician == null || d < myLoc.distanceSquaredTo(closestEnemyPolitician.getLocation())){
                        closestEnemyPolitician = r;
                    }
                    enemyCombinedAttack += Math.max(0, (int)(enemyBuff*(r.getConviction() - GameConstants.EMPOWER_TAX)));
                    break;
                case MUCKRAKER:
                    d = myLoc.distanceSquaredTo(r.getLocation());
                    if (closestEnemyMuckraker == null || d < myLoc.distanceSquaredTo(closestEnemyMuckraker.getLocation())){
                        closestEnemyMuckraker = r;
                    }
                    break;

            }
        }
    }



    //generic method to build robots
    void buildNewRobots(){
        NewRobot r = getNewRobot(closestEnemyPolitician != null, closestEnemyMuckraker != null);
        build (r);
    }


    //class that encodes the next robot we want to build
    class NewRobot{


        RobotType robotType; //type
        TypeDescription typeDesc;
        int influence; //influence
        MapLocation closestLoc = null; //if we want to prioritize building it towards a given location
        boolean standardBuild = true; //if it comes from the standard build (in this case we must increase the counter after building it)
        boolean count = false;


        public NewRobot(RobotType robotType, TypeDescription typeDesc, int influence, boolean standardBuild, boolean count){
            this.robotType = robotType;
            this.typeDesc = typeDesc;
            this.influence = influence;
            this.standardBuild = standardBuild;
            this.count = count;
        }
        public NewRobot(RobotType robotType, TypeDescription typeDesc, int influence, MapLocation closestLoc, boolean standardBuild, boolean count){
            this.robotType = robotType;
            this.typeDesc = typeDesc;
            this.influence = influence;
            this.closestLoc = closestLoc;
            this.standardBuild = standardBuild;
            this.count = count;
        }
    }

    final double eps = 1e-5;

    //Builds a robot trying to minimize (distanceToObjective)/passability
    void build(NewRobot r){
        try {
            if (r == null) return;

            Direction bestSpawnDir = null;
            double bestSpawnValue = 0;

            for (Direction dir : directions) {
                if (!rc.canBuildRobot(r.robotType, dir, r.influence)) continue;
                double v = getValue(r, dir);
                if (bestSpawnDir == null || v < bestSpawnValue - eps || (Math.abs(v - bestSpawnValue) < 2*eps && buildDirCont[bestSpawnDir.ordinal()] < buildDirCont[dir.ordinal()])){
                    bestSpawnDir = dir;
                    bestSpawnValue = v;

                }
            }

            if (bestSpawnDir != null && rc.canBuildRobot(r.robotType, bestSpawnDir, r.influence)){
                rc.buildRobot(r.robotType, bestSpawnDir, r.influence);
                if (r.robotType == RobotType.SLANDERER) checkIndex(r.influence);
                if (r.standardBuild){
                    if (r.robotType == RobotType.MUCKRAKER) increaseMuckrakerIndex();
                }
                if (r.count) buildDirCont[bestSpawnDir.ordinal()]++;
                if (r.typeDesc != null){
                    double addP = 1.0/typeProportions[r.typeDesc.ordinal()];
                    currentProportions[r.typeDesc.ordinal()] += addP;
                    //totalCurrentProportions += addP;
                    if (r.typeDesc == TypeDescription.CONQUEROR){
                        if (r.closestLoc != null) comm.markConqueror(r.closestLoc);
                    }
                    if (r.typeDesc == TypeDescription.SMALL_POLITICIAN){
                        currentProportions[TypeDescription.POLITICIAN.ordinal()] += addP;
                    }
                    if (r.typeDesc == TypeDescription.POLITICIAN) ++smallPoliticianIndex;
                }
            }

        } catch (Exception e){
            e.printStackTrace();
        }
    }


    //updated current type after building a unit the standard way
    /*void advanceUnitType(){
        currentType = (currentType + 1)%typeOrder.length;
    }*/

    //heuristic to choose position
    double getValue(NewRobot r, Direction dir) throws GameActionException {
        double dist = 1;
        if (r.closestLoc != null){
            dist = rc.getLocation().add(dir).distanceSquaredTo(r.closestLoc);
        }
        return dist/rc.sensePassability(rc.getLocation().add(dir));
    }


    //Decides which robot to build
    NewRobot getNewRobot(boolean attacked, boolean nearbyMuckraker){

        //Case we are being attacked
        if (attacked){
            NewRobot r = getNewRobot(false, nearbyMuckraker);
            if (r.influence + enemyCombinedAttack + SAFE_CONVICTION <= rc.getInfluence()) return r; //if we can build whatever we wanted and still resist the attack we are fine
            if (closestEnemyPolitician == null) return r; //this shouldn't happen...
            return new NewRobot(RobotType.MUCKRAKER, TypeDescription.MUCKRAKER, 1, closestEnemyPolitician.getLocation(), false, true); //build muckrakers to block attack
        }

        //Case there is an enemy muckraker trolling around
        if (nearbyMuckraker){
            if (closestEnemyMuckraker == null || !comm.slanderersNearby()){ //TODO: remove nearbyPolitician
                return getNewRobot(false, false);
            }
            //build a politician that kills the muckraker
            int politicianConviction = getConvictionToKillMuckraker(closestEnemyMuckraker.getConviction());
            if (politicianConviction > rc.getConviction()) return new NewRobot(RobotType.MUCKRAKER, TypeDescription.MUCKRAKER, 1, lowestContDir(), false, true);
            return new NewRobot(RobotType.POLITICIAN, TypeDescription.POLITICIAN, politicianConviction, closestEnemyMuckraker.getLocation(), false, false);
        }
        //standard case
        return getStandardNewRobot();
    }

    //min influence to kill a muckracker with given conviction
    int getConvictionToKillMuckraker(int muckrakerConviction){
        int ans = (int) ((double)(muckrakerConviction + GameConstants.EMPOWER_TAX + 3)/rc.getEmpowerFactor(rc.getTeam(), 25));
        return Math.max(ans, poliTiers[0]);
    }

    //checks if there's already a politician that can kill the muckraker efficiently
    boolean nearbyPolitician(int muckConviction){
        RobotInfo[] robots = rc.senseNearbyRobots(2, rc.getTeam());
        for (RobotInfo r : robots){
            if (r.getType() != RobotType.POLITICIAN) continue;
            int conv = r.getConviction();
            if ((int)(rc.getEmpowerFactor(rc.getTeam(), 25)*conv) <= muckConviction + GameConstants.EMPOWER_TAX) continue;
            if (1.5*(double)muckConviction + GameConstants.EMPOWER_TAX + 5 < r.getConviction()) continue;
            return true;
        }
        return false;
    }

    //this is what we do if there are no emergencies (enemy politicians or muckrakers nearby)
    NewRobot getStandardNewRobot(){
        //System.err.println("Getting standard unit");//while ((closestEnemyMuckraker != null || !isSafe()) && getTypeStandard() == TypeDescription.SLANDERER) advanceUnitType();
        TypeDescription type = getTypeStandard(); //we choose the type (right now this just gets typeOrder[currentType])
        switch(type){
            case MUCKRAKER:
                return new NewRobot(RobotType.MUCKRAKER, TypeDescription.MUCKRAKER, getMuckrakerInfluence(), lowestContDir(), true, true); //standard muckraker build
            case SLANDERER:
                int slandererInf = getSlandererInf(rc.getInfluence() - getSafetyInfRaw());
                if (slandererInf > 0) return new NewRobot(RobotType.SLANDERER, TypeDescription.SLANDERER, slandererInf, true, false); //build slanderer with maximal influence
                break;
            case POLITICIAN:
                int politicianInf = getSmallPoliticianInfluence();
                if (politicianInf > 0) return new NewRobot(RobotType.POLITICIAN, TypeDescription.POLITICIAN, politicianInf, true, false); //standard politician build
                break;
            case BIG_POLITICIAN:
                int bpInf = getBigPoliInf();
                if (bpInf > 0) return new NewRobot(RobotType.POLITICIAN, TypeDescription.BIG_POLITICIAN, bpInf, true, false); //dump all our remaining influence into big poli
                break;
            case BIG_MUCKRAKER:
                int bmInf = getBigMuckInf();
                if (bmInf > 0) return new NewRobot(RobotType.MUCKRAKER, TypeDescription.BIG_MUCKRAKER, bmInf, true, false); //dump all our remaining influence into big poli
                break;
            case CONQUEROR:
                int conqInf = getInfConqueror();
                MapLocation loc = comm.getBestNeutralEC().getMapLocation();
                if (loc != null) {
                    if (conqInf > 0)
                        return new NewRobot(RobotType.POLITICIAN, TypeDescription.CONQUEROR, conqInf, loc,true, false);
                }
                break;
            case SMALL_POLITICIAN:
                int spInf = SMALL_POLITICIAN_INFLUENCE;
                if (spInf <= rc.getInfluence()){
                    return new NewRobot(RobotType.POLITICIAN, TypeDescription.SMALL_POLITICIAN, spInf,true, false);
                }
                break;
        }
        return new NewRobot(RobotType.MUCKRAKER, TypeDescription.MUCKRAKER, 1, lowestContDir(), false, true); //if we couldnt build what we wanted, build a muckraker
    }

    int getSafetyInfRaw(){
        if (enemyCombinedAttack > 0) return SAFE_CONVICTION + enemyCombinedAttack;
        return 0;
    }

    int getSafetyInf(){
        int safety = getSafetyInfRaw(); //check if we have enough to resist attack
        int safety2 = Math.max(0, getMaxSlandererInf() - 2*income);
        safety2 += Math.max(0, getSmallPoliticianInfluence() - 2*income);
        return Math.max(safety, safety2);
    }

    int getBigPoliInf(){
        int safety = getSafetyInf();
        int minInf = getMinBigPoliticianInfluence(); //minimum influence to build a big politician
        if (minInf + safety > rc.getInfluence()) {
            return 0;
        }
        return rc.getInfluence() - safety;
    }

    MapLocation lowestContDir(){
        Direction bestDir = null;
        for (Direction dir : directions){
            if (!rc.canBuildRobot(RobotType.MUCKRAKER, dir, 1)) continue;
            if (bestDir == null || buildDirCont[dir.ordinal()] < buildDirCont[bestDir.ordinal()]) bestDir = dir;
        }
        if (bestDir != null) return rc.getLocation().add(bestDir);
        return null;
    }

    TypeDescription getTypeStandard(){
        boolean[] canBuild = new boolean[typeProportions.length];
        if (shouldBuildSlanderer()) canBuild[TypeDescription.SLANDERER.ordinal()] = true;
        if (shouldBuildBigPoli()) canBuild[TypeDescription.BIG_POLITICIAN.ordinal()] = true;
        if (shouldBuildPolitician()) canBuild[TypeDescription.POLITICIAN.ordinal()] = true;
        if (shouldBuildMuckraker()) canBuild[TypeDescription.MUCKRAKER.ordinal()] = true;
        if (shouldBuildBigMuckraker()) canBuild[TypeDescription.BIG_MUCKRAKER.ordinal()] = true;
        if (shouldBuildConqueror()) canBuild[TypeDescription.CONQUEROR.ordinal()] = true;
        if (shouldbuildSmallPoli()) canBuild[TypeDescription.SMALL_POLITICIAN.ordinal()] = true;

        //System.err.println(canBuild[TypeDescription.CONQUEROR.ordinal()]);

        TypeDescription ans = null;
        double minCont = 0;

        for (int i = 0; i < typeDescriptions.length;++i){
            if (!canBuild[i]) continue;
            //TypeDescription t = typeDescriptions[i];
            if (ans == null || minCont > currentProportions[i]){
                minCont = currentProportions[i];
                ans = typeDescriptions[i];
            }
        }

        return ans;
    }

    boolean shouldbuildSmallPoli(){
        return rc.getRoundNum() <= SMALL_POLI_ROUND;
    }



    boolean shouldBuildConqueror(){
        int conqInf = getInfConqueror();
        if (conqInf > 0) return true;
        return false;
    }

    boolean shouldBuildSlanderer(){
        if (closestEnemyMuckraker != null || !isSafe()) return false;
        return true;
    }

    boolean shouldBuildBigPoli(){
        int bpInf = getBigPoliInf();
        if (bpInf <= 0) return false;
        return true;
    }

    boolean shouldBuildMuckraker(){
        return true;
    }

    boolean shouldBuildPolitician(){
        if (rc.getRoundNum() <= SMALL_POLI_ROUND) return false;
        return true;
    }

    boolean shouldBuildBigMuckraker() {
        int bmInf = getBigMuckInf();
        if (bmInf <= 0) return false;
        return true;
    }

    /*
    Methods to compute the influence of the next politician/muckraker. We use the typical 1 2 1 3 1 2 1 4 1 2 1 3 1 ... order (position of the first 1-bit in n).
    If the current number (or tier) is t, we build a politician/muckraker with f(t) influence, where f is hardcoded into the arrays below (if t > length of the array we pick the last one).

     */

    int getInfConqueror(){
        //TODO: save for slanderer maybe?
        //int safetyInf = getSafetyInf();
        int safetyInf = getSafetyInfRaw();

        Communication.RInfo ec = comm.getBestNeutralEC();
        if (ec ==  null) return 0;
        if (ec.influence > 2*getMaxSlandererInf()) return 0;
        int desiredInf = ec.influence + GameConstants.EMPOWER_TAX + 10;
        if (rc.getInfluence() < desiredInf + safetyInf) return 0;
        return desiredInf;
    }

    int getSlandererInf(int inf){
        int msInf = getMaxSlandererInf();
        if (inf > msInf) inf = msInf;
        for (int i = influences.length; i-- > 0;){
            if (influences[i] <= inf) return influences[i];
        }
        return 0;
    }

    int getSmallPoliticianInfluence(){
        int bit = 0;
        int base = 2;
        while ((smallPoliticianIndex %base) == 0){
            ++bit;
            base *= 2;
        }
        return politicianInf(bit);
    }

    final int[] poliTiers = new int[] {16, 24, 36, 54, 81, 121, 181};

    int politicianInf(int tier){
        int ans = 0;
        for (int i = 0; i < poliTiers.length; ++i){
            if (i > tier) return ans;
            if (poliTiers[i] > rc.getInfluence()) return ans;
            ans = poliTiers[i];
        }
        return ans;
    }

    int getMinBigPoliticianInfluence(){
        return poliTiers[poliTiers.length - 1];
        /*Communication.RInfo ec = comm.getBestNeutralEC();
        if (ec ==  null) return 150;
        //int minInf = (int)((double)(ec.influence + GameConstants.EMPOWER_TAX + 10)/rc.getEmpowerFactor(rc.getTeam(), 30));
        if (ec.influence > 2*getMaxSlandererInf()) return 150;
        return ec.influence + GameConstants.EMPOWER_TAX + 10;
        //return 150;*/
    }

    int getBigMuckInf(){
        int safety = getSafetyInf();
        int minInf = getMinBigMuckInf(); //minimum influence to build a big politician
        if (minInf + safety > rc.getInfluence()) {
            return 0;
        }
        /*int safety = getSafetyInf();
        int minInf = getMinBigMuckInf(); //minimum influence to build a big politician
        if (minInf + safety > rc.getInfluence()) {
            return 0;
        }*/
        return minInf;
    }

    int getMinBigMuckInf(){
        return BIG_MUCK_INF;
    }

    int getMuckrakerInfluence(){
        if (shouldBuild1HPMuck()) return 1;
        int bit = 0;
        int base = 2;
        while ((muckrakerIndex%base) == 0){
            ++bit;
            base *= 2;
        }
        return muckrakerInf(bit);
    }

    final int[] muckTiers = new int[] {1, 13, 33};

    int muckrakerInf(int tier){
        int ans = 1;
        for (int i = 0; i < muckTiers.length; ++i){
            if (i > tier) return ans;
            if (muckTiers[i] > rc.getInfluence()) return ans;
            ans = muckTiers[i];
        }
        return ans;
    }

    double fractionIncome(double r){
        if (r < 400) return 0;
        return Math.min(0.5, (r-400)/2000);
    }

    //TODO: improve this
    void bid(){
        try {
            if (rc.getRoundNum() < 400) return;
            if (rc.getTeamVotes() >= 751) return;
            int bid = 2;
            bid = Math.max(2, (int)(fractionIncome(rc.getRoundNum())*income));
            if (rc.getInfluence() >= 10000){
                bid = Math.max(bid, rc.getInfluence()/100);
            }
            //if (rc.getRobotCount() <= 200 && rc.getRoundNum() <= 1000) return;
            if (rc.canBid(bid)){
                rc.bid(bid);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
