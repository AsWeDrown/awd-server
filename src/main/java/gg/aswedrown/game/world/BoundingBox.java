package gg.aswedrown.game.world;

import lombok.Getter;
import lombok.NonNull;

public class BoundingBox {

    @Getter
    private float minX,    minY,    // левый верхний угол в мире (в тайлах)
                  maxX,    maxY,    // правый нижний угол в мире (в тайлах)
                  centerX, centerY; // центр в мире (в тайлах)

    public BoundingBox(float minX, float minY, float maxX, float maxY) {
        if (minX < 0.0f || minY < 0.0f || maxX < 0.0f || maxY < 0.0f)
            throw new IllegalArgumentException(
                    "bounding box coordinates cannot be negative: " +
                            "{" + minX + ", " + minX + ", " + maxX + ", " + maxY + "}");

        this.minX    = Math.min(minX, maxX);
        this.minY    = Math.min(minY, maxY);
        this.maxX    = Math.max(minX, maxX);
        this.maxY    = Math.max(minY, maxY);
        this.centerX = (minX + maxX) / 2.0f;
        this.centerY = (minY + maxY) / 2.0f;
    }

    public boolean isHorizontallyWithinOf(@NonNull BoundingBox other) {
        return minX >= other.minX && maxX <= other.maxX;
    }

    public boolean isVerticallyWithinOf(@NonNull BoundingBox other) {
        return minY >= other.minY && maxY <= other.maxY;
    }

    public boolean isFullyWithinOf(@NonNull BoundingBox other) {
        return isHorizontallyWithinOf(other) && isVerticallyWithinOf(other);
    }

    public boolean isCenterHorizontallyWithinOf(@NonNull BoundingBox other) {
        return centerX >= other.minX && centerX <= other.maxX;
    }

    public boolean isCenterVerticallyWithinOf(@NonNull BoundingBox other) {
        return centerY >= other.minY && centerY <= other.maxY;
    }

    public boolean isCenterFullyWithinOf(@NonNull BoundingBox other) {
        return isCenterHorizontallyWithinOf(other) && isCenterVerticallyWithinOf(other);
    }

    public boolean intersectsWith(@NonNull BoundingBox other) {
        return minX < other.maxX && maxX > other.minX
            && minY < other.maxY && maxY > other.minY;
    }

    public boolean isAboveOf(@NonNull BoundingBox other) {
        return maxY <= other.minY;
    }

    public double distance(@NonNull BoundingBox other) {
        return Math.sqrt(distanceSquared(other));
    }

    public double distanceSquared(@NonNull BoundingBox other) {
        double dx = centerX - other.centerX;
        double dy = centerY - other.centerY;
        return dx * dx + dy * dy;
    }

    public BoundingBox deepCopy() {
        return new BoundingBox(minX, minY, maxX, maxY);
    }

    public BoundingBox move(float deltaX, float deltaY) {
        minX += deltaX;
        maxX += deltaX;
        minY += deltaY;
        maxY += deltaY;

        return this;
    }

    @Override
    public String toString() {
        return "BoundingBox{" +
                "minX=" + minX +
                ", minY=" + minY +
                ", maxX=" + maxX +
                ", maxY=" + maxY +
                '}';
    }

}
