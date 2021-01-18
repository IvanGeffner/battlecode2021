package twelve;

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
        SLANDERER, MUCKRAKER, POLITICIAN, BIG_POLITICIAN
    }

    //Util: default order (TODO: optimize this, make this adaptive?)
    TypeDescription[] typeOrder = {
            TypeDescription.SLANDERER,
            TypeDescription.BIG_POLITICIAN,
            TypeDescription.MUCKRAKER,
            TypeDescription.POLITICIAN,
    };

    TypeDescription[] typeOrder2 = {
            TypeDescription.SLANDERER,
            TypeDescription.BIG_POLITICIAN,
            TypeDescription.MUCKRAKER,
            TypeDescription.POLITICIAN,
            TypeDescription.MUCKRAKER,
            TypeDescription.BIG_POLITICIAN,
            TypeDescription.MUCKRAKER,
            TypeDescription.POLITICIAN,
            TypeDescription.MUCKRAKER,
            TypeDescription.POLITICIAN,
    };

    final int incomeTreshold = 100;

    //Bidding info
    int infBeginning = 0; //influence at the beginning of our turn
    int infEnd = GameConstants.INITIAL_ENLIGHTENMENT_CENTER_INFLUENCE; //influence at the end of our turn
    int income = 0; //income (beginning - end)

    //We update these at the beginning of each round
    RobotInfo closestEnemyMuckraker;
    RobotInfo closestEnemyPolitician;
    int enemyCombinedAttack;
    boolean nearbySlanderer;

    int CDTurns; //cooldown turns (depends on passability)

    final int SAFE_CONVICTION = 5; //conviction we keep at our base (maybe we can set it to 0?)

    //Building info
    int currentType = 0; //current type we are trying to build
    int currentType2 = 0;
    int smallPoliticianIndex = 1; //politician index (this decides the influence given)
    int muckrakerIndex = 1; //muckraker index (this decides the influence given)
    int bigPoliticianIndex = 1;

    public Enlightment(RobotController rc){
        super(rc);
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

    final int maxSlandererInfIndex = influences.length - 1;

    final int MAX_SLANDERER_INF = 203;

    //maximum slanderer we ever build
    final int maxSlandererInf = influences[influences.length - 1];


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

        //update the influence at the end of turn
        infEnd = rc.getInfluence();

        System.err.println("Income " + income);
    }

    //Right now this just updates the locations of the closest enemy muckraker and politician (null if none found)
    void updateStatus(){
        closestEnemyMuckraker = null;
        closestEnemyPolitician = null;
        enemyCombinedAttack = 0;
        nearbySlanderer = false;

        MapLocation myLoc = rc.getLocation();
        double enemyBuff = rc.getEmpowerFactor(rc.getTeam().opponent(), 0);

        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, rc.getTeam().opponent());

        for (RobotInfo r : enemies){
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

        RobotInfo[] allies = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, rc.getTeam());

        for (RobotInfo r : allies){
            switch(r.getType()){
                case SLANDERER:
                    nearbySlanderer = true;
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
        int influence; //influence
        MapLocation closestLoc = null; //if we want to prioritize building it towards a given location
        boolean standardBuild = true; //if it comes from the standard build (in this case we must increase the counter after building it)
        boolean count = false;
        boolean small = true;


        public NewRobot(RobotType robotType, int influence, boolean standardBuild, boolean count, boolean small){
            this.robotType = robotType;
            this.influence = influence;
            this.standardBuild = standardBuild;
            this.count = count;
            this.small = small;
        }
        public NewRobot(RobotType robotType, int influence, MapLocation closestLoc, boolean standardBuild, boolean count, boolean small){
            this.robotType = robotType;
            this.influence = influence;
            this.closestLoc = closestLoc;
            this.standardBuild = standardBuild;
            this.count = count;
            this.small = small;
        }
    }

    //Builds a robot trying to minimize (distanceToObjective)/passability
    void build(NewRobot r){
        try {
            if (r == null) return;

            Direction bestSpawnDir = null;
            double bestSpawnValue = 0;

            for (Direction dir : directions) {
                if (!rc.canBuildRobot(r.robotType, dir, r.influence)) continue;
                double v = getValue(r, dir);
                if (bestSpawnDir == null || v < bestSpawnValue){
                    bestSpawnDir = dir;
                    bestSpawnValue = v;
                }
            }

            if (bestSpawnDir != null && rc.canBuildRobot(r.robotType, bestSpawnDir, r.influence)){
                rc.buildRobot(r.robotType, bestSpawnDir, r.influence);
                if (r.standardBuild){
                    advanceUnitType();
                    if (r.robotType == RobotType.POLITICIAN){
                        if (r.small) ++smallPoliticianIndex;
                        else ++bigPoliticianIndex;
                    }
                    if (should2() && r.robotType == RobotType.MUCKRAKER) ++muckrakerIndex;
                }
                if (r.count) buildDirCont[bestSpawnDir.ordinal()]++;
            }

        } catch (Exception e){
            e.printStackTrace();
        }
    }


    //updated current type after building a unit the standard way
    void advanceUnitType(){
        if (should2()) currentType2 = (currentType2 + 1)%typeOrder2.length;
        else currentType = (currentType + 1)%typeOrder.length;
    }

    boolean should2(){
        return income > incomeTreshold;
    }


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
            return new NewRobot(RobotType.MUCKRAKER, 1, closestEnemyPolitician.getLocation(), false, true, false); //build muckrakers to block attack
        }

        //Case there is an enemy muckraker trolling around
        if (nearbyMuckraker){
            if (!nearbySlanderer) return getStandardNewRobot();
            if (closestEnemyMuckraker == null || nearbyPolitician(closestEnemyMuckraker.conviction)){ //this shouldn't happen but just in case
                return getNewRobot(false, false);
            }
            //build a politician that kills the muckraker
            int politicianConviction = getConvictionToKillMuckraker(closestEnemyMuckraker.getConviction());
            if (politicianConviction > rc.getConviction()) return new NewRobot(RobotType.MUCKRAKER, 1, lowestContDir(), false, true, false);
            return new NewRobot(RobotType.POLITICIAN, politicianConviction, closestEnemyMuckraker.getLocation(), false, false, true);
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
        while (closestEnemyMuckraker != null && getTypeStandard() == TypeDescription.SLANDERER) advanceUnitType();
        //NewRobot conqueror = getConqueror();
        //if (conqueror != null) return conqueror;
        TypeDescription type = getTypeStandard(); //we choose the type (right now this just gets typeOrder[currentType])
        switch(type){
            case MUCKRAKER:
                return new NewRobot(RobotType.MUCKRAKER, getMuckrakerInfluence(), lowestContDir(), true, true, false); //standard muckraker build
            case SLANDERER:
                int slandererInf = getSlandererInf(rc.getInfluence());
                if (slandererInf > 0) return new NewRobot(RobotType.SLANDERER, slandererInf, true, false, false); //build slanderer with maximal influence
                break;
            case POLITICIAN:
                int politicianInf = getSmallPoliticianInfluence();
                if (politicianInf > 0) return new NewRobot(RobotType.POLITICIAN, politicianInf, true, false, true); //standard politician build
                break;
            case BIG_POLITICIAN:
                int safety = SAFE_CONVICTION + enemyCombinedAttack + 30; //check if we have enough to resist attack
                //int safety2 = maxSlandererInf  - income*8; //check if we have enough for our next slanderer
                int safety2 = 0;
                safety = Math.max(safety, safety2);
                int minInf = getBigPoliticianInfluence(); //minimum influence to build a big politician
                if (minInf + safety > rc.getInfluence()) return new NewRobot(RobotType.MUCKRAKER, getMuckrakerInfluence(), lowestContDir(),true, true, false); //do muckraker if cant build big poli
                return new NewRobot(RobotType.POLITICIAN, rc.getInfluence() - safety, true, false, false); //dump all our remaining influence into big poli
        }
        return new NewRobot(RobotType.MUCKRAKER, 1, lowestContDir(), false, true, false); //if we couldnt build what we wanted, build a muckraker
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
        if (should2()) return typeOrder2[currentType2];
        return typeOrder[currentType];
    }

    /*
    Methods to compute the influence of the next politician/muckraker. We use the typical 1 2 1 3 1 2 1 4 1 2 1 3 1 ... order (position of the first 1-bit in n).
    If the current number (or tier) is t, we build a politician/muckraker with f(t) influence, where f is hardcoded into the arrays below (if t > length of the array we pick the last one).

     */

    NewRobot getConqueror(){
        Communication.RInfo r = comm.getBestNeutralECInfluence();
        if (r == null) return null;
        if (r.influence == null) return null;
        int infConqueror = r.influence + GameConstants.EMPOWER_TAX + 10;
        int minRound = 20 + infConqueror/5;
        if (rc.getRoundNum() <= minRound) return null;
        if (rc.getConviction() < infConqueror) return null;
        return new NewRobot(RobotType.POLITICIAN, infConqueror, r.getMapLocation(), true, false, true);
    }

    int getSlandererInf(int inf){
        if (inf > maxSlandererInf) inf = maxSlandererInf;
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
        return smallPolitician(bit);
    }

    final int[] poliTiers = new int[] {14, 16, 22, 30, 40, 54};

    int smallPolitician(int tier){
        int ans = 0;
        for (int i = 0; i < poliTiers.length; ++i){
            if (i > tier) return ans;
            if (poliTiers[i] > rc.getInfluence()) return ans;
            ans = poliTiers[i];
        }
        return ans;
    }

    final int[] poliTiersBig = new int[] {110, 160, 260, 410, 510};

    int bigPolitician(int tier){
        tier = Math.min(tier, poliTiersBig.length-1);
        return poliTiersBig[tier];
    }

    int getBigPoliticianInfluence(){
        int bit = 0;
        int base = 2;
        while ((bigPoliticianIndex %base) == 0){
            ++bit;
            base *= 2;
        }
        return bigPolitician(bit);
    }

    int getMuckrakerInfluence(){
        int bit = 0;
        int base = 2;
        while ((muckrakerIndex%base) == 0){
            ++bit;
            base *= 2;
        }
        return muckrakerInf(bit);
    }

    final int[] muckTiers = new int[] {1, 13, 26, 43};

    int muckrakerInf(int tier){
        int ans = 1;
        for (int i = 0; i < muckTiers.length; ++i){
            if (i > tier) return ans;
            if (muckTiers[i] > rc.getInfluence()) return ans;
            ans = muckTiers[i];
        }
        return ans;
    }

    //TODO: improve this
    void bid(){
        try {
            if (rc.getRobotCount() <= 200 && rc.getRoundNum() <= 1000) return;
            int bid = income /3;
            if (rc.canBid(bid)){
                rc.bid(bid);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

}
