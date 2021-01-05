package firstbot;
import battlecode.common.*;

public class Enlightment extends MyRobot {

    public Enlightment(RobotController rc){
        super(rc);
    }

    public void play(){
        buildNewRobots();
        NewRobot nr = getNewRobot();
        if (nr != null) build(nr);
    }

    void buildNewRobots(){
        NewRobot nr = getNewRobot();
        if (nr == null) return;
        if (!build(nr)) return;
        buildNewRobots();
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
