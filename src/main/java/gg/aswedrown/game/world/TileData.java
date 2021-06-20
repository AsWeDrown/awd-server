package gg.aswedrown.game.world;

import gg.aswedrown.game.world.tile.*;
import lombok.NonNull;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

final class TileData {

    private TileData() {}

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    //   Для конверсии RGB-цвета пикселей из level-scheme в ID (номера) тайлов (текстур).
    //   https://github.com/AsWeDrown/awd-protocol/wiki/%D0%9D%D0%B0%D1%87%D0%B8%D0%BD%D0%BA%D0%B0-%D0%BC%D0%B8%D1%80%D0%B0-(World-Contents)
    //
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private static final Map<Integer, Integer> rgbToTileIdMap = new HashMap<>();

    private static final Map<Integer, Constructor<? extends TileHandler>> tileHandlerConstructors = new HashMap<>();

    private static void reg(int rgb, int tileId, Class<? extends TileHandler> tileHandlerClass) {
        if (rgbToTileIdMap.containsKey(rgb) || tileHandlerConstructors.containsKey(tileId))
            throw new IllegalArgumentException(
                    "duplicate tile data: rgb=#" + Integer.toString(rgb, 16) + ", id=" + tileId);

        try {
            Constructor<? extends TileHandler> constructor
                    = tileHandlerClass.getConstructor(TileBlock.class);
            tileHandlerConstructors.put(tileId, constructor);
            rgbToTileIdMap.put(rgb, tileId);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException("missing single-argument " +
                    "(TileBlock) constructor in " + tileHandlerClass.getName(), ex);
        }
    }

    static {
        // Пустота (void) (тайлы, сквозь которые всегда можно спокойно, беспрепятственно проходить).
        reg(0xffffff,   0, VoidHandler.class);
        reg(0x84659c,   8, VoidHandler.class);
        reg(0x9a78b4,   9, VoidHandler.class);
        reg(0xacb4e2,  10, VoidHandler.class);
        reg(0x5e5e5e,  11, VoidHandler.class);
        reg(0xd24a44,  12, VoidHandler.class);
        reg(0x6a66aa,  17, VoidHandler.class);
        reg(0xa6a5c5,  18, VoidHandler.class);
        reg(0x93cec8,  22, VoidHandler.class);
        reg(0x708986,  23, VoidHandler.class);
        reg(0xce93bf,  24, VoidHandler.class);

        // Твёрдые тайлы (solid).
        reg(0x000000,   1, SolidHandler.class);
        reg(0x4a4a4a,   2, SolidHandler.class);
        reg(0x636363,   3, SolidHandler.class);
        reg(0x7e7e7e,   4, SolidHandler.class);
        reg(0x9a9a9a,   5, SolidHandler.class);
        reg(0xb5b5b5,   6, SolidHandler.class);
        reg(0xce93bf,  19, SolidHandler.class);
        reg(0xaf85a4,  20, SolidHandler.class);
        reg(0x947e8e,  21, SolidHandler.class);

        // Лестницы.
        reg(0x865e3a,   7, LadderHandler.class);

        // Переключатели.
        reg(0x9ac561,  13, SwitchHandler.class);
        reg(0x6e8a47,  14, SwitchHandler.class);

        // Люки.
        reg(0xc9d95b,  15, HatchClosedHandler.class);
        reg(0xd4d9b2,  16, HatchOpenHandler  .class);
    }

    static Integer rgbToTileId(int rgb) {
        return rgbToTileIdMap.get(rgb);
    }

    static TileHandler newTileHandler(@NonNull TileBlock tile) {
        Constructor<? extends TileHandler> constructor = tileHandlerConstructors.get(tile.tileId);

        if (constructor == null)
            throw new IllegalArgumentException(
                    "no tile handler constructor for tile id: " + tile.tileId);

        try {
            return constructor.newInstance(tile);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException("failed to create " +
                    "a new instance of " + constructor.getName(), ex);
        }
    }

}
