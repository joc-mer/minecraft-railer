package ch.lumarlie.railer;

import net.kyori.adventure.identity.Identity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static ch.lumarlie.railer.XYZ.*;

public class RailerEventListener implements Listener {

    public static final int MAX_EXPLORATION = 500000;
    public static final int POWER_PERIOD = 3;

    public static final Vector NORTH = new Vector(-1, 0, 0);
    public static final Vector SOUTH = new Vector(1, 0, 0);
    public static final Vector EAST = new Vector(0, 0, -1);
    public static final Vector WEST = new Vector(0, 0, 1);
    public static final Vector UP = new Vector(0, 1, 0);
    public static final Vector DOWN = new Vector(0, -1, 0);

    record RailEvent(Location location, boolean powerRail) {
    }

    record PathEvent(Location arrival, List<BlockChange> blockChanges) {
    }

    private final Map<Identity, RailEvent> lastRailPlacedLocation = new HashMap<>();
    private final Map<Identity, PathEvent> lastPathEvent = new HashMap<>();
    private final Logger logger;

    public RailerEventListener(Logger logger) {
        this.logger = logger;
    }

    @EventHandler
    public void onPlayerBrakes(BlockBreakEvent event) {
        var player = event.getPlayer();
        var identity = player.identity();

        if (materialIs(event.getBlock().getType(), Material.RAIL, Material.POWERED_RAIL)) {
            lastRailPlacedLocation.remove(identity);
        }

        var lastPath = lastPathEvent.get(identity);

        if (lastPath != null && event.getBlock().getLocation().equals(lastPath.arrival)) {
            lastPath.blockChanges().reversed().forEach(bc -> bc.undo());
            lastPathEvent.remove(identity);
        }
    }

    @EventHandler
    public void onPlayerBlock(BlockPlaceEvent event) {
        var player = event.getPlayer();
        var identity = player.identity();

        boolean isPoweredRail = event.getBlock().getType() == Material.POWERED_RAIL;

        if (event.getBlock().getType() == Material.RAIL || isPoweredRail) {
            var location = event.getBlockPlaced().getLocation();
            var prevRailEvent = lastRailPlacedLocation.get(identity);
            if (prevRailEvent != null) {
                logger.info("Previous location was " + prevRailEvent.location);
                if (prevRailEvent.location.distance(location) < 8) {
                    logger.info("Too close to trigger magic");
                    return;
                } else {
                    final int powerPeriod;
                    if (isPoweredRail) {
                        powerPeriod = POWER_PERIOD;
                    } else {
                        if (prevRailEvent.powerRail) {
                            powerPeriod = POWER_PERIOD;
                        } else {
                            powerPeriod = 1000000;
                        }
                    }
                    var path = buildCurvyPath(location, prevRailEvent.location, powerPeriod, 0, 12);

                    lastPathEvent.put(identity, new PathEvent(location, path));
                }
            }
            lastRailPlacedLocation.put(identity, new RailEvent(location, isPoweredRail));
        } else {
            lastRailPlacedLocation.remove(identity);
        }
    }

    private boolean buildStraightPath(Location first, Location second, int powerPeriod, int powerFrom) {
        var world = first.getWorld();
        if (first.getBlockY() == second.getBlockY()) {
            logger.info("Building path from " + first + " to " + second);
            int commonY = first.getBlockY();
            int i = 0;
            if (first.getBlockX() == second.getBlockX()) {
                int commonX = first.getBlockX();
                if (first.getBlockZ() > second.getBlockZ()) {
                    for (int z = first.getBlockZ(); z > second.getBlockZ(); --z) {
                        placeRail(world, commonX, commonY, z, false, (i++ % powerPeriod) == powerFrom);
                    }
                } else {
                    for (int z = first.getBlockZ(); z < second.getBlockZ(); ++z) {
                        placeRail(world, commonX, commonY, z, false, (i++ % powerPeriod) == powerFrom);
                    }
                }
            } else if (first.getBlockZ() == second.getBlockZ()) {
                int commonZ = first.getBlockZ();
                if (first.getBlockX() > second.getBlockX()) {
                    for (int x = first.getBlockX(); x > second.getBlockX(); --x) {
                        placeRail(world, x, commonY, commonZ, false, (i++ % powerPeriod) == powerFrom);
                    }
                } else {
                    for (int x = first.getBlockX(); x < second.getBlockX(); ++x) {
                        placeRail(world, x, commonY, commonZ, false, (i++ % powerPeriod) == powerFrom);
                    }
                }
            } else {
                logger.info("Not aligned: " + first + " / " + second);
                return false;
            }
            logger.info("Path finished, " + i + " rails created");
            return true;

        } else {
            logger.info("Not on the same level: " + first.getBlockZ() + " / " + second.getBlockZ());
        }
        return false;
    }

    private List<BlockChange> buildCurvyPath(final Location first, final Location second, final int powerPeriod, final int powerFrom, final int lampPeriod) {
        List<BlockChange> ret = new ArrayList<>();
        var world = first.getWorld();
        var pathFinder = new PathFinder(logger, world);
        var steps = pathFinder.findPSteps(first, second);
        var coordinates = steps.stream().map(step -> step.xyz).map(XYZ::toString).collect(Collectors.joining("; "));

        logger.info("Found a path of " + steps.size() + " steps: " + coordinates);

        int index = 0;
        Step prev = null;
        Step cur = null;
        boolean up = false;

        int noPusherSince = powerFrom;

        for (Step next : steps) {
            XYZ curXyz = cur != null ? cur.xyz : null;
            XYZ prevXyz = prev != null ? prev.xyz : null;
            XYZ nextXyz = next.xyz;

            if (cur != null) {
                boolean turn = prev != null && isTurn(prevXyz, curXyz, nextXyz);
                XYZ[] lampDirs;
                if (index % lampPeriod == lampPeriod - 1 && prevXyz != null) {
                    lampDirs = curXyz.nsew().filter(xyz -> !areVerticallyAligned(xyz, prevXyz) && !areVerticallyAligned(xyz, nextXyz)).toArray(XYZ[]::new);
                } else {
                    lampDirs = new XYZ[]{};
                }
                boolean pusher = false;
                if (noPusherSince >= powerPeriod && !turn) {
                    pusher = true;
                    noPusherSince = 0;
                } else {
                    noPusherSince++;
                }
                ret.addAll(placeRail(world, curXyz.x(), curXyz.y(), curXyz.z(), up, pusher, lampDirs));
            }
            prev = cur;
            cur = next;
            up = prevXyz != null && prevXyz.y() != curXyz.y();
            ++index;
        }
        if (cur != null) {
            ret.addAll(placeRail(world, cur.xyz.x(), cur.xyz.y(), cur.xyz.z(), up, false));
        }
        return ret;
    }

    static class PathFinder {
        public static final int HEAL_COST = 3;
        private final Logger logger;
        private final World world;
        private final Map<XYZ, Step> stepMap = new HashMap<>();
        private final PriorityQueue<Step> heap = new PriorityQueue<>();

        public PathFinder(Logger logger, World world) {
            this.logger = logger;
            this.world = world;
        }

        public List<Step> findPSteps(Location fromLoc, Location toLoc) {
            var from = XYZ.fromLocation(fromLoc);
            var to = XYZ.fromLocation(toLoc);

            Step cur = new Step(0, 0, from.manhattan(to, HEAL_COST), from, null);
            while (cur != null && !cur.xyz.equals(to) && stepMap.size() < MAX_EXPLORATION) {
                explore(to, cur, cur.xyz.north());
                explore(to, cur, cur.xyz.north().up());
                explore(to, cur, cur.xyz.north().down());
                explore(to, cur, cur.xyz.south());
                explore(to, cur, cur.xyz.south().up());
                explore(to, cur, cur.xyz.south().down());
                explore(to, cur, cur.xyz.east());
                explore(to, cur, cur.xyz.east().up());
                explore(to, cur, cur.xyz.east().down());
                explore(to, cur, cur.xyz.west());
                explore(to, cur, cur.xyz.west().up());
                explore(to, cur, cur.xyz.west().down());

                cur = heap.poll();
            }

            if (cur != null && cur.xyz.equals(to)) {
                var ret = new LinkedList<Step>();
                Step toAdd = cur;
                while (toAdd != null) {
                    ret.addLast(toAdd);
                    toAdd = toAdd.prev;
                }
                return ret;
            } else {
                logger.info("Cannot find a path after having explored " + stepMap.size() + " locations");
                return List.of();
            }
        }

        private void explore(XYZ to, Step from, XYZ next) {
            var violatesRules = from.prev != null && (isStepTurn(from.prev.xyz, from.xyz, next) || isHole(from.prev.xyz, from.xyz, next) || areVerticallyAligned(from.prev.xyz, next));
            var conflictingPath = from.stream().limit(12).anyMatch(pred -> areVerticallyAligned(pred.xyz, next) && Math.abs(pred.xyz.y() - next.y()) <= 4); // avoid blocking prev path
            if (!stepMap.containsKey(next) && !violatesRules && !conflictingPath) {
                var block = world.getBlockAt(next.x(), next.y(), next.z());
                var blockBellow2 = world.getBlockAt(next.x(), next.y() - 2, next.z());
                var blockBellow = world.getBlockAt(next.x(), next.y() - 1, next.z());
                var blockAbove = world.getBlockAt(next.x(), next.y() + 1, next.z());
                var healCost = elevation(from.xyz, next) * HEAL_COST;
                var waterCost = (blockBellow.getType() == Material.WATER ? 2 : 0) + (block.getType() == Material.WATER ? 4 : 0) + (blockAbove.getType() == Material.WATER ? 6 : 0);
                var constructionCost = constructionCost(blockBellow.getType(), block.getType(), blockAbove.getType());
                var destructionCost = to.equals(next) ? 0 : destructionCost(blockBellow2.getType(), blockBellow.getType(), block.getType(), blockAbove.getType());
                var toAdd = new Step(from.index + 1, from.cost + healCost + constructionCost + destructionCost + waterCost, to.manhattan(next, HEAL_COST), next, from);
                heap.add(toAdd);
                stepMap.put(next, toAdd);
            }
        }
    }

    static int destructionCost(Material below2, Material below, Material onPath, Material above) {
        var below2Cost = below2 == Material.RAIL || below2 == Material.POWERED_RAIL ? 150 : 0;
        var belowCost = below == Material.RAIL || below == Material.POWERED_RAIL ? 150 : 0;
        var pathCost = onPath == Material.RAIL || onPath == Material.POWERED_RAIL ? 150 : 0;
        var aboveCost = above == Material.RAIL || above == Material.POWERED_RAIL ? 150 : 0;
        return below2Cost + belowCost + pathCost + aboveCost;
    }

    static int constructionCost(Material below, Material onPath, Material above) {
        var belowCost = below.isSolid() ? 0 : 2;
        var onPathCost = onPath.isAir() ? 0 : 1 + above.getMaxDurability() / 50;
        var aboveCost = above.isAir() ? 0 : 1 + above.getMaxDurability() / 50;
        return belowCost + onPathCost + aboveCost;
    }


    static class Step implements Comparable<Step>, Iterable<Step> {
        final int index;
        final int cost;
        final int distTo;
        final XYZ xyz;
        final Step prev;

        public Step(int index, int cost, int distTo, XYZ xyz, Step prev) {
            this.index = index;
            this.cost = cost;
            this.distTo = distTo;
            this.xyz = xyz;
            this.prev = prev;
        }

        @Override
        public int compareTo(@NotNull Step o) {
            return (this.cost + this.distTo) - (o.cost + o.distTo);
        }

        @Override
        public @NotNull Iterator<Step> iterator() {
            return new Iterator<>() {
                Step cur = Step.this;

                @Override
                public boolean hasNext() {
                    return cur.prev != null;
                }

                @Override
                public Step next() {
                    var ret = cur;
                    cur = cur.prev;
                    return ret;
                }
            };
        }

        public Stream<Step> stream() {
            return StreamSupport.stream(this.spliterator(), false);
        }
    }

    private List<BlockChange> placeRail(World world, int x, int y, int z, boolean heal, boolean pusher, XYZ... lightLocations) {
        List<BlockChange> ret = new ArrayList<>(3);
        var aboveLoc = new Location(world, x, y + 1, z);
        var aboveAboveLoc = new Location(world, x, y + 2, z);
        ret.add(changeBlock(aboveLoc, Material.AIR));
        if (heal) {
            ret.add(changeBlock(aboveAboveLoc, Material.AIR));
        }
        var loc = new Location(world, x, y, z);
        var underLoc = new Location(loc.getWorld(), x, y - 1, z);

        if (pusher) {
            ret.add(changeBlock(underLoc, Material.REDSTONE_BLOCK));
            ret.add(changeBlock(loc, Material.POWERED_RAIL));
        } else {
            if (!underLoc.getBlock().getType().isSolid() || underLoc.getBlock().getType().isCompostable()) {
                ret.add(changeBlock(underLoc, Material.DIRT));
            }
            ret.add(changeBlock(loc, Material.RAIL));
        }

        boolean torchPlaced = false;
        for (XYZ lightLoc : lightLocations) {
            Location bellowBaseLoc = lightLoc.down().toVector().toLocation(world);
            Location baseLoc = lightLoc.toVector().toLocation(world);
            Location lampLoc = lightLoc.up().toVector().toLocation(world);
            if (lampLoc.getBlock().getType() == Material.AIR && bellowBaseLoc.getBlock().isSolid() && baseLoc.getBlock().getType().isEmpty()) {
                ret.add(changeBlock(baseLoc, Material.BAMBOO_FENCE));
                ret.add(changeBlock(lampLoc, Material.TORCH));
                ret.add(changeBlock(lampLoc, Material.TORCH));
                torchPlaced = true;
                break;
            }
        }

        if (lightLocations.length > 0 && !torchPlaced && !pusher) {
            ret.add(changeBlock(underLoc, Material.GLOWSTONE));
        }

        return ret;
    }

    static BlockChange changeBlock(Location location, Material newMaterial) {
        var currentMaterial = location.getBlock().getType();
        location.getBlock().setType(newMaterial);
        return new BlockChange(location, currentMaterial, newMaterial);
    }

    record BlockChange(Location location, Material before, Material after) {
        void undo() {
            location.getBlock().setType(before);
        }
    }

    private static int nearby(Location location, Material... materials) {
        int ret = 0;
        for (int modx = -1; modx <= 1; ++modx) {
            for (int mody = -1; mody <= 1; ++mody) {
                for (int modz = -1; modz <= 1; ++modz) {
                    if (modx == 0 && mody == 0 && modz == 0) {
                        continue;
                    }
                    var block = new Location(
                            location.getWorld(),
                            location.getBlockX() + modx,
                            location.getBlockY() + mody,
                            location.getBlockZ() + modz
                    ).getBlock();
                    if (materialIs(block.getType(), materials)) ret += 1;
                }
            }
        }
        return ret;
    }

    private static boolean materialIs(Material candidate, Material... materials) {
        for (Material material : materials) {
            if (candidate == material) {
                return true;
            }
        }
        return false;
    }
}
