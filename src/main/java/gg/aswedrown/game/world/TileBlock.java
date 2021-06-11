package gg.aswedrown.game.world;

import gg.aswedrown.game.world.tile.TileHandler;

public class TileBlock implements Collidable {

    public int tileId = 0, // ID текстуры этого тайла в tilemap
               posX   = 0, // координата X левого верхнего угла этого тайла в мире
               posY   = 0; // координата Y левого верхнего угла этого тайла в мире

    public TileHandler handler;

    @Override
    public BoundingBox getBoundingBox() {
        return new BoundingBox(posX, posY, posX + 1.0f, posY + 1.0f);
    }

}
