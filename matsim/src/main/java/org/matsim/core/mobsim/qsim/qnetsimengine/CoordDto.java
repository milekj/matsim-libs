package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.Coord;

public class CoordDto {

    private double x;
    private double y;
    private double z;

    public CoordDto(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public CoordDto() {
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public Coord toCoord() {
        if (z == Double.NEGATIVE_INFINITY)
            return new Coord(x, y);
        return new Coord(x, y, z);
    }
}
