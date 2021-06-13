package gg.aswedrown.game.world;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class Location {

    private float x, y, faceAngle;

    public float distance(Location other) {
        return (float) Math.sqrt(distanceSquared(other));
    }

    public float distanceSquared(@NonNull Location other) {
        float dx = x - other.x;
        float dy = y - other.y;
        return dx * dx + dy * dy;
    }

}
