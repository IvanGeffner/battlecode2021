package nineb;

import battlecode.common.*;

public class Enlightment extends MyRobot {

    //TODO: do not build slanderers if there are enemy muckrakers

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

    RobotType[] typeOrder = {
            RobotType.SLANDERER,
            RobotType.MUCKRAKER,
            RobotType.MUCKRAKER,
            RobotType.POLITICIAN
    };

    int infBeginning = 0;

    int infEnd = 0;

    int infWon = 0;

    int currentType = 0;

    int politicianIndex = 1;
    int muckrakerIndex = 1;

    public Enlightment(RobotController rc){
        super(rc);
    }

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
            //282,
            //310,
            //339,
            //368,
            //399,
            //431,
            //463,
            //497,
            //532,
            //568,
            //605,
            //643,
            //683,
            //724,
            //766,
            //810,
            //855,
            //902,
            //949
    };


    final int maxInf = influences[influences.length - 1];

    public void play(){
        infBeginning = rc.getInfluence();
        infWon = infBeginning - infEnd;
        bid();
        buildNewRobots();
        infEnd = rc.getInfluence();
        //rc.setIndicatorDot(rc.getLocation(), 0, 0, 0);
    }

    void buildNewRobots(){
        NewRobot nr = getNewRobot();
        if (nr != null) {
            build(nr, true);
            return;
        }
        if (rc.getRobotCount() <= 200) build (new NewRobot(RobotType.MUCKRAKER, 1), false);
    }

    void bid(){
        try {
            if (rc.getRobotCount() <= 200 && rc.getRoundNum() <= 1000) return;
            int bid = infWon/3;
            if (rc.canBid(bid)){
                rc.bid(bid);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    int getBestInf(int inf){
        if (inf > maxInf) inf = maxInf;
        for (int i = influences.length; i-- > 0;){
            if (influences[i] <= inf) return influences[i];
        }
        return 0;
    }

    //TODO
    NewRobot getNewRobot(){
        if (muckrackerNearby() && typeOrder[currentType] == RobotType.SLANDERER) advanceUnitType();
        if(currentType == 1 && rc.getInfluence() > 500) return new NewRobot(RobotType.POLITICIAN, rc.getInfluence()/2);
        //if (rc.getRobotCount() > 75){ //TODO fix this mess
            //while (typeOrder[currentType] == RobotType.MUCKRAKER) advanceUnitType();
        //}
        switch(typeOrder[currentType]) {
            case MUCKRAKER:
                return new NewRobot(RobotType.MUCKRAKER, getMuckrakerInf());
            case SLANDERER:
                int slandererInf = getBestInf(rc.getInfluence());
                if (slandererInf > 0) return new NewRobot(RobotType.SLANDERER, slandererInf);
                return null;
            case POLITICIAN: return getPolitician();
        }
        return null;
    }

    NewRobot getPolitician(){
        int politicianInf = getPoliticianInfluence();
        if (politicianInf == 0 || politicianInf > rc.getInfluence()) return null;
        return new NewRobot(RobotType.POLITICIAN, politicianInf);
    }

    void advanceUnitType(){
        currentType = (currentType + 1)%typeOrder.length;
    }

    boolean muckrackerNearby(){
        RobotInfo[] robots = rc.senseNearbyRobots(RobotType.ENLIGHTENMENT_CENTER.sensorRadiusSquared, rc.getTeam().opponent());
        for (RobotInfo r : robots){
            if (r.getType() == RobotType.MUCKRAKER) return true;
        }
        return false;
    }


    int getPoliticianInfluence(){
        int bit = 0;
        while ((politicianIndex & (1 << bit)) == 0){
            ++bit;
        }
        return politicianInf(bit);
    }

    int getMuckrakerInf(){
        int bit = 0;
        while ((muckrakerIndex & (1 << bit)) == 0){
            ++bit;
        }
        return MuckrakerInf(bit);
    }

    int MuckrakerInf(int tier){
        return 1 + 10*tier;
    }

    int politicianInf(int tier){
        double ans = 4;
        for (int i = 0; i < tier; ++i){
            ans *=1.33;
        }
        return (int) (ans + GameConstants.EMPOWER_TAX);
    }



    boolean build(NewRobot nr, boolean raiseCounter){
        try {
            for (Direction dir : directions) {
                if (rc.canBuildRobot(nr.robotType, dir, nr.influence)) {
                    rc.buildRobot(nr.robotType, dir, nr.influence);
                    if (raiseCounter){
                        advanceUnitType();
                        if (nr.robotType == RobotType.POLITICIAN) ++politicianIndex;
                        if (nr.robotType == RobotType.MUCKRAKER) ++muckrakerIndex;
                    }
                    return true;
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    class NewRobot{
        public NewRobot(RobotType robotType, int influence){
            this.robotType = robotType;
            this.influence = influence;
        }
        RobotType robotType;
        int influence;
    }

}
