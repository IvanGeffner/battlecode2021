package secondbot;

import battlecode.common.Direction;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Enlightment extends MyRobot {


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
            RobotType.MUCKRAKER,
            RobotType.POLITICIAN
    };

    int currentType = 0;

    int politicianIndex = 1;
    int bit = 0;

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


    final int maxInf = influences[influences.length - 1];

    public void play(){
        bid();
        buildNewRobots();
        //rc.setIndicatorDot(rc.getLocation(), 0, 0, 0);
    }

    void buildNewRobots(){
        NewRobot nr = getNewRobot();
        if (nr != null) {
            build(nr, true);
            return;
        }
        build (new NewRobot(RobotType.MUCKRAKER, 1), false);
    }

    void bid(){
        /*
        try {
            int inf = rc.getInfluence();
            int bid = (inf / Math.max(1, (GameConstants.GAME_MAX_NUMBER_OF_ROUNDS - rc.getRoundNum())));
            if (rc.canBid(bid)) rc.bid(bid);
        } catch (Exception e){
            e.printStackTrace();
        }*/

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
        switch(typeOrder[currentType]) {
            case MUCKRAKER: return new NewRobot(RobotType.MUCKRAKER, 1);
            case SLANDERER:
                int slandererInf = getBestInf(rc.getInfluence());
                if (slandererInf > 0) return new NewRobot(RobotType.SLANDERER, slandererInf);
                return null;
            case POLITICIAN:
                int politicianInf = getPoliticianInfluence();
                if (politicianInf == 0 || politicianInf > rc.getInfluence()) return null;
                return new NewRobot(RobotType.POLITICIAN, politicianInf);
        }
        return null;
    }

    int getPoliticianInfluence(){
        for (; ;increasePoliticianIndex()) {
            if (((1 << bit) & politicianIndex) > 0) return politicianInf(bit);
        }
    }

    void increasePoliticianIndex(){
        ++bit;
        if ((1 << bit) > politicianIndex){
            bit = 0;
            ++politicianIndex;
        }
    }

    int politicianInf(int x){
        return GameConstants.EMPOWER_TAX*(1 << x) + 1;
    }



    boolean build(NewRobot nr, boolean raiseCounter){
        try {
            for (Direction dir : directions) {
                if (rc.canBuildRobot(nr.robotType, dir, nr.influence)) {
                    rc.buildRobot(nr.robotType, dir, nr.influence);
                    if (raiseCounter){
                        currentType = (currentType + 1)%typeOrder.length;
                        if (nr.robotType == RobotType.POLITICIAN) increasePoliticianIndex();
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
