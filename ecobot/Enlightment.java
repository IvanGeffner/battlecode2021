package ecobot;

import battlecode.common.*;

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
        if (rc.getRobotCount() > 70) return;
        NewRobot nr = getNewRobot();
        if (nr == null) return;
        if (!build(nr)) return;
        buildNewRobots();
    }

    void bid(){
        try {
            int inf = rc.getInfluence();
            int bid = (inf / Math.max(1, (GameConstants.GAME_MAX_NUMBER_OF_ROUNDS - rc.getRoundNum())));
            if (rc.canBid(bid)) rc.bid(bid);
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
        int slandererInf = getBestInf(rc.getInfluence());
        if (slandererInf > 0) return new NewRobot(RobotType.SLANDERER, slandererInf);
        return null;
    }

    boolean build(NewRobot nr){
        try {
            for (Direction dir : directions) {
                if (rc.canBuildRobot(nr.robotType, dir, nr.influence)) {
                    rc.buildRobot(nr.robotType, dir, nr.influence);
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
