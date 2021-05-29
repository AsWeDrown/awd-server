package gg.aswedrown.game.world;

import gg.aswedrown.game.world.tile.TileHandler;

import java.util.Objects;

public class TileBlock implements Collidable {

    public int tileId = 0, // ID текстуры этого тайла в tilemap
               posX   = 0, // координата X левого верхнего угла этого тайла в мире
               posY   = 0; // координата Y левого верхнего угла этого тайла в мире

    public TileHandler getHandler() {
        return TileData.getTileHandler(tileId);
    }

    @Override
    public BoundingBox getBoundingBox() {
        return new BoundingBox(posX, posY, posX + 1.0f, posY + 1.0f);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TileBlock)) return false;
        TileBlock tileBlock = (TileBlock) o;
        return tileId == tileBlock.tileId && posX == tileBlock.posX && posY == tileBlock.posY;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tileId, posX, posY);
    }

}
