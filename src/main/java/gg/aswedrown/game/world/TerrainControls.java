package gg.aswedrown.game.world;

import gg.aswedrown.game.entity.Entity;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TerrainControls {

    private final WorldData worldData;

    public TileBlock getTileAt(int posX, int posY) {
        return worldData.tiles.stream()
                .filter(tile -> tile.posX == posX && tile.posY == posY)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(
                        "tile position out of world range: (" + posX + ", " + posY + "); expected " +
                                "x in range [0; " + worldData.width + "), " +
                                "y in range [0; " + worldData.height + ")"));
    }

    /**
     * Вычисляет значение X, в которое может переместиться указанная сущность в этом мире,
     * максимально близкое к указанному (желаемому). Для этого учитывается "начинка" мира
     * (окружающие сущность тайлы и прочее), а также размеры и текущая позиция сущности.
     *
     * @param entity сущность, которая хочет передвинуться по X.
     * @param destWorldX желаемая координата X в мире.
     *
     * @return координата X в мире, в которую указанная сущность может (имеет право) передвинуться
     *         с учётом различных законов физики и взаимодействий, наиболее близкая к желаемому X.
     */
    public float advanceTowardsXUntilTerrainCollision(@NonNull Entity entity, float destWorldX) {
        float entityX = entity.getPosX();
        float dx = destWorldX - entityX;

        if (dx == 0.0f)
            return entityX;

        BoundingBox entityBb = entity.getBoundingBox();
        BoundingBox destEntityBb = entityBb.deepCopy().move(dx, 0.0f);

        int leftmostTileX   = (int) Math.floor(destEntityBb.getMinX());
        int rightmostTileX  = (int) Math.ceil (    entityBb.getMaxX());
        int topmostTileY    = (int) Math.floor(destEntityBb.getMinY());
        int bottommostTileY = (int) Math.ceil (    entityBb.getMaxY());

        boolean pathingRight = dx > 0.0f;
        int tileX = pathingRight ? rightmostTileX : leftmostTileX;

        for (int tileY = topmostTileY; tileY < bottommostTileY; tileY++) {
            TileBlock nearbyTile = getTileAt(tileX, tileY);
            BoundingBox nearbyTileBb = nearbyTile.getBoundingBox();

            if (!nearbyTile.getHandler().isPassableBy(entity) && destEntityBb.intersectsWith(nearbyTileBb))
                return pathingRight
                        ? entityX + (nearbyTileBb.getMinX() - entityBb.getMaxX())
                        : entityX - (entityBb.getMinX() - nearbyTileBb.getMaxX());
        }

        return destWorldX;
    }

    /**
     * Вычисляет значение Y, в которое может переместиться указанная сущность в этом мире,
     * максимально близкое к указанному (желаемому). Для этого учитывается "начинка" мира
     * (окружающие сущность тайлы и прочее), а также размеры и текущая позиция сущности.
     *
     * @param entity сущность, которая хочет передвинуться по Y.
     * @param destWorldY желаемая координата Y в мире.
     *
     * @return координата Y в мире, в которую указанная сущность может (имеет право) передвинуться
     *         с учётом различных законов физики и взаимодействий, наиболее близкая к желаемому Y.
     */
    public float advanceTowardsYUntilTerrainCollision(@NonNull Entity entity, float destWorldY) {
        float entityY = entity.getPosY();
        float dy = destWorldY - entityY;

        if (dy == 0.0f)
            return entityY;

        BoundingBox entityBb = entity.getBoundingBox();
        BoundingBox destEntityBb = entityBb.deepCopy().move(0.0f, dy);

        int leftmostTileX   = (int) Math.floor(destEntityBb.getMinX());
        int rightmostTileX  = (int) Math.ceil (    entityBb.getMaxX());
        int topmostTileY    = (int) Math.floor(destEntityBb.getMinY());
        int bottommostTileY = (int) Math.ceil (    entityBb.getMaxY());

        boolean pathingBottom = dy > 0.0f;
        int tileY = pathingBottom ? bottommostTileY : topmostTileY;

        for (int tileX = leftmostTileX; tileX < rightmostTileX; tileX++) {
            TileBlock nearbyTile = getTileAt(tileX, tileY);
            BoundingBox nearbyTileBb = nearbyTile.getBoundingBox();

            if (!nearbyTile.getHandler().isPassableBy(entity) && destEntityBb.intersectsWith(nearbyTileBb)) {
                return pathingBottom
                        ? entityY + (nearbyTileBb.getMinY() - entityBb.getMaxY())
                        : entityY - (entityBb.getMinY() - nearbyTileBb.getMaxY());
            }
        }

        return destWorldY;
    }

    /**
     * Проверяет, находится ли указанная сущность сейчас в устойчивом положении на земле.
     * При проверке используется "начинка" мира и местоположение и размеры сущности.
     *
     * @param entity сущность, которую нужно проверить.
     *
     * @return true, если центр масс указанной сущность сейчас устойчив, т.е. сущность не падает
     *         вниз; false - если сущность падает вниз.
     */
    public boolean isOnGround(Entity entity) {
        BoundingBox entityBb = entity.getBoundingBox();

        float massCenterX = entityBb.getCenterX();
        float entityFeetY = entityBb.getMaxY   ();

        if ((entityFeetY % 1.0f) != 0.0f)
            return false;

        int leftmostTileX   = (int) Math.floor(massCenterX);
        int rightmostTileX  = (int) Math.ceil (massCenterX);
        int tileY           = (int) entityFeetY;

        TileBlock[] tilesBeneath = {
                getTileAt(leftmostTileX,  tileY),
                getTileAt(rightmostTileX, tileY)
        };

        for (TileBlock tileBeneath : tilesBeneath) {
            if (!tileBeneath.getHandler().isPassableBy(entity)) {
                BoundingBox tileBeneathBb = tileBeneath.getBoundingBox();

                if (entityBb.isAboveOf(tileBeneathBb)
                        && entityBb.isCenterHorizontallyWithinOf(tileBeneathBb))
                    return true;
            }
        }

        return false;
    }

}
