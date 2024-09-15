package ch.lumarlie.railer;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.stream.Stream;

record XYZ(int x, int y, int z) {
    static XYZ fromLocation(Location location) {
        return new XYZ(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public XYZ north() {
        return new XYZ(x, y, z - 1);
    }

    public XYZ south() {
        return new XYZ(x, y, z + 1);
    }

    public XYZ east() {
        return new XYZ(x + 1, y, z);
    }

    public XYZ west() {
        return new XYZ(x - 1, y, z);
    }

    public XYZ up() {
        return new XYZ(x, y + 1, z);
    }

    public XYZ down() {
        return new XYZ(x, y - 1, z);
    }

    public XYZ add(XYZ other) {
        return add(other.x, other.y, other.z);
    }

    public XYZ add(int modX, int modY, int modZ) {
        return new XYZ(x + modX, y + modY, z + modZ);
    }

    public Vector toVector() {
        return new Vector(x, y, z);
    }

    public int manhattan(XYZ other, int verticalFactor) {
        return Math.abs(x - other.x) + Math.abs(y - other.y) * verticalFactor + Math.abs(z - other.z);
    }

    public static boolean isTurn(XYZ one, XYZ other, XYZ another) {
        return Math.abs(one.x - other.x) != Math.abs(other.x - another.x) || Math.abs(one.z - other.z) != Math.abs(other.z - another.z);
    }

    public static boolean isStepTurn(XYZ a, XYZ b, XYZ c) {
        return isTurn(a, b, c) && (a.y() != b.y() || b.y() != c.y());
    }

    public static boolean areVerticallyAligned(XYZ a, XYZ c) {
        return a.x == c.x && a.z == c.z;
    }

    public static boolean isHole(XYZ a, XYZ b, XYZ c) {
        return a.y - b.y == 1 && c.y - b.y == 1;
    }

    public static int elevation(XYZ a, XYZ b) {
        return Math.abs(a.y - b.y);
    }

    public Stream<XYZ> nsew() {
        return Stream.of(this.north(), this.south(), this.east(), this.west());
    }

    @Override
    public String toString() {
        return String.format("[%d, %d, %d]", x, y, z);
    }
}
