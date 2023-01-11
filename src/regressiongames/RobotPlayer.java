package regressiongames;

import battlecode.common.*;

import java.util.*;

class Tuple<K, V> {

    public K first;
    public V last;

    public Tuple(K first, V last) {
        this.first = first;
        this.last = last;
    }

}

/**
 * RobotPlayer.java is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public strictfp class RobotPlayer {

    /**
     * We will use this variable to count the number of turns this robot has been alive.
     * You can use static variables like this to save any information you want. Keep in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between your robots.
     */
    static int turnCount = 0;

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static final Random rng = new Random();

    /** Array containing all the possible movement directions. */
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

    static MapLocation myHQ = null;

    static MapLocation lastLocation = null;
    static Integer lastLocationCount = null;

    static Integer lastAnchorTime = 0;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!
     *
     * @param rc  The RobotController object. You use it to perform actions from this robot, and to get
     *            information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // Hello world! Standard output is very useful for debugging.
        // Everything you say here will be directly viewable in your terminal when you run a match!
        System.out.println("I'm a " + rc.getType() + " and I just got created! I have health " + rc.getHealth());

        if (myHQ == null) {
            List<MapLocation> hqs = getNearbyHQLocations(rc);
            if (hqs.size() > 0) {
                myHQ = hqs.get(0);
            }
        }

        while (true) {
            // This code runs during the entire lifespan of the robot, which is why it is in an infinite
            // loop. If we ever leave this loop and return from run(), the robot dies! At the end of the
            // loop, we call Clock.yield(), signifying that we've done everything we want to do.

            turnCount += 1;  // We have now been alive for one more turn!

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                // The same run() function is called for every robot on your team, even if they are
                // different types. Here, we separate the control depending on the RobotType, so we can
                // use different strategies on different robots. If you wish, you are free to rewrite
                // this into a different control structure!
                switch (rc.getType()) {
                    case HEADQUARTERS:     runHeadquarters(rc);  break;
                    case CARRIER:      runCarrier(rc);   break;
                    case LAUNCHER: runLauncher(rc); break;
                    case BOOSTER: // Examplefuncsplayer doesn't use any of these robot types below.
                    case DESTABILIZER: // You might want to give them a try!
                    case AMPLIFIER:       break;
                }

            } catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You should
                // handle GameActionExceptions judiciously, in case unexpected events occur in the game
                // world. Remember, uncaught exceptions cause your robot to explode!
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();

            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();

            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop again.
                Clock.yield();
            }
            // End of loop: go back to the top. Clock.yield() has ended, so it's time for another turn!
        }

        // Your code should never reach here (unless it's intentional)! Self-destruction imminent...
    }

    static boolean isInventoryFull(RobotController rc) {
        int totalAmount = 0;
        for (ResourceType t: ResourceType.values()) {
            totalAmount += rc.getResourceAmount(t);
        }
        return totalAmount >= 40;
    }

    static boolean hasInventory(RobotController rc) {
        int totalAmount = 0;
        for (ResourceType t: ResourceType.values()) {
            totalAmount += rc.getResourceAmount(t);
        }
        return totalAmount > 0;
    }

    static Tuple getNextDepositable(RobotController rc) {
        for (ResourceType t: ResourceType.values()) {
            int amount = rc.getResourceAmount(t);
            if( amount > 0) {
                return new Tuple(t, amount);
            }
        }
        return null;
    }

    static List<MapLocation> getNearbyHQLocations(RobotController rc) throws GameActionException {
        List<MapLocation> c = new ArrayList<>();
        for (RobotInfo r : rc.senseNearbyRobots(-1, rc.getTeam())) {
            if (r.getType() == RobotType.HEADQUARTERS) {
                c.add(r.location);
            }
        }
        return c;
    }

    /**
     * Run a single turn for a Headquarters.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runHeadquarters(RobotController rc) throws GameActionException {
        // Pick a direction to build in.
        Direction dir = directions[rng.nextInt(directions.length)];
        MapLocation newLoc = rc.getLocation().add(dir);

        if (rc.canBuildAnchor(Anchor.STANDARD) && rc.getResourceAmount(ResourceType.ADAMANTIUM) > 100 && rc.getResourceAmount(ResourceType.MANA) > 100 && rc.getNumAnchors(Anchor.STANDARD) < 1 && turnCount > 350 && (turnCount - lastAnchorTime) > 300) {
            rc.setIndicatorString("BUILDING AN ANCHOR");
            rc.buildAnchor(Anchor.STANDARD);
            lastAnchorTime = turnCount;
        }

        if (rc.canBuildRobot(RobotType.CARRIER, newLoc) && (turnCount < 250 || rc.getResourceAmount(ResourceType.ADAMANTIUM) > 110) && rng.nextFloat() > 0.5) {
            rc.setIndicatorString("BUILDING A CARRIER");
            rc.buildRobot(RobotType.CARRIER, newLoc);
        }
    }

    /**
     * Run a single turn for a Carrier.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runCarrier(RobotController rc) throws GameActionException {
        MapLocation me = rc.getLocation();

        if (myHQ != null) {
            Tuple<ResourceType, Integer> depositable = getNextDepositable(rc);
            if (depositable != null && rc.canTransferResource(myHQ, depositable.first, depositable.last)) {
                rc.transferResource(myHQ, depositable.first, depositable.last);
            }

            // If empty, near an HQ, and the HQ has an anchor, grab it
            if (rc.canTakeAnchor(myHQ, Anchor.STANDARD)) {
                rc.takeAnchor(myHQ, Anchor.STANDARD);
            }
        }

        if (rc.getAnchor() != null) {
            rc.setIndicatorString("IM CARRYING AN ANCHOR! Wandering until I find an island");
            // Find an island, or wander around
            if(rc.canPlaceAnchor() && rc.senseAnchor(rc.senseIsland(me)) == null) {
                rc.placeAnchor();
                return;
            }
            int[] islands = rc.senseNearbyIslands();
            for (int island : islands) {
                if (rc.senseAnchor(island) != null) {
                    continue;
                }
                MapLocation[] locations = rc.senseNearbyIslandLocations(island);
                for (MapLocation l : locations) {
                    Direction dir = me.directionTo(l);
                    if (rc.canMove(dir))
                        rc.move(dir);
                    return;
                }
            }
            Direction dir = directions[rng.nextInt(directions.length)];
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
            return;
        }


        if (lastLocation == null) {
            lastLocation = me;
            lastLocationCount = 0;
        } else {
            if (me == lastLocation) {
                lastLocationCount += 1;
            } else {
                lastLocation = me;
                lastLocationCount = 0;
            }
            if (lastLocationCount > 5) {
                Direction dir = directions[rng.nextInt(directions.length)];
                if (rc.canMove(dir)) {
                    rc.move(dir);
                }
            }
        }
        if (isInventoryFull(rc) && myHQ != null) {
            Direction dir = me.directionTo(myHQ);
            if (rc.canMove(dir))
                rc.move(dir);
            return;
        }
        // Try to gather from squares around us.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation wellLocation = new MapLocation(me.x + dx, me.y + dy);
                if (rc.canCollectResource(wellLocation, -1)) {
                    if (rng.nextBoolean()) {
                        rc.collectResource(wellLocation, -1);
                        rc.setIndicatorString("Collecting, now have, AD:" +
                                rc.getResourceAmount(ResourceType.ADAMANTIUM) +
                                " MN: " + rc.getResourceAmount(ResourceType.MANA) +
                                " EX: " + rc.getResourceAmount(ResourceType.ELIXIR));
                    }
                }
            }
        }
        // Try out the carriers attack
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if (enemyRobots.length > 0) {
            if (rc.canAttack(enemyRobots[0].location)) {
                rc.attack(enemyRobots[0].location);
            }
        }

        // If we can see a well, move towards it
        WellInfo[] wells = rc.senseNearbyWells();
        if (wells.length > 1) {
            WellInfo well_one = wells[1];
            Direction dir = me.directionTo(well_one.getMapLocation());
            if (rc.canMove(dir))
                rc.move(dir);
        }
        // Also try to move randomly.
        Direction dir = directions[rng.nextInt(directions.length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
    }

    /**
     * Run a single turn for a Launcher.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runLauncher(RobotController rc) throws GameActionException {
        // Try to attack someone
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        if (enemies.length >= 0) {
            // MapLocation toAttack = enemies[0].location;
            MapLocation toAttack = rc.getLocation().add(Direction.EAST);

            if (rc.canAttack(toAttack)) {
                rc.setIndicatorString("Attacking");
                rc.attack(toAttack);
            }
        }

        // Also try to move randomly.
        Direction dir = directions[rng.nextInt(directions.length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
    }
}
