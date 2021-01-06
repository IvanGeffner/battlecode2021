package pathfindingtest;

import battlecode.common.Direction;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Enlightment extends MyRobot {

    boolean built = false;
    public Enlightment(RobotController rc){
        super(rc);
    }

    public void play(){
        NewRobot nr = getNewRobot();
        if (!built && nr != null) built = build(nr);
    }

    NewRobot getNewRobot(){
        return new NewRobot(RobotType.MUCKRAKER, 1);
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


    public void initTurn(){

    }

    public void endTurn(){

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
