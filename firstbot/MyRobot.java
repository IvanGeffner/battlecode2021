package firstbot;
import battlecode.common.*;

public abstract class MyRobot {

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

    RobotController rc;
    Pathfinding path;


    public MyRobot(RobotController rc){
        this.rc = rc;
        path = new Pathfinding(rc);
    }

    abstract void play();

    abstract void initTurn();

    abstract void endTurn();


}
