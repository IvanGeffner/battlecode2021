package rushbot;

import battlecode.common.Direction;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Enlightment extends MyRobot {

    public Enlightment(RobotController rc){
        super(rc);
    }

    int max_robots = 1000000;
    int robots_built = 0;

    public void play(){
        buildNewRobots();
        //rc.setIndicatorDot(rc.getLocation(), 0, 0, 0);
    }

    void buildNewRobots(){
        if (robots_built >= max_robots) return;
        NewRobot nr = getNewRobot();
        if (nr == null) return;
        if (!build(nr)) return;
        buildNewRobots();
    }

    //TODO
    NewRobot getNewRobot(){
        return new NewRobot(RobotType.SLANDERER, 1);
    }

    boolean build(NewRobot nr){
        try {
            for (Direction dir : directions) {
                if (rc.canBuildRobot(nr.robotType, dir, nr.influence)) {
                    rc.buildRobot(nr.robotType, dir, nr.influence);
                    ++robots_built;
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
