package gg.aswedrown.game.world;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class BoundingBox {

    private final float minX, minY, // левый верхний угол в мире (в тайлах)
                        maxX, maxY; // правый нижний угол в мире (в тайлах)

    public BoundingBox(float minX, float minY, float maxX, float maxY) {
        if (minX < 0.0f || minY < 0.0f || maxX < 0.0f || maxY < 0.0f)
            throw new IllegalArgumentException(
                    "bounding box coordinates cannot be negative: " +
                            "{" + minX + ", " + minX + ", " + maxX + ", " + maxY + "}");

        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
    }

    public float getCenterX() {
        return (minX + maxX) / 2.0f;
    }

    public float getCenterY() {
        return (minY + maxY) / 2.0f;
    }

    public boolean isFullyWithinOf(@NonNull BoundingBox other) {
        return minX >= other.minX && maxX <= other.maxX
            && minY >= other.minY && maxY <= other.maxY;
    }

    public boolean intersectsWith(@NonNull BoundingBox other) {
        return minX < other.maxX && maxX > other.minX
            && minY < other.maxY && maxY > other.minY;
    }

}
