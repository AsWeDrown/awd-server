package gg.aswedrown.game.world;

import gg.aswedrown.game.world.tile.SolidHandler;
import gg.aswedrown.game.world.tile.TileHandler;
import gg.aswedrown.game.world.tile.VoidHandler;

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

    private static final Map<Integer, TileHandler> tileHandlers = new HashMap<>();

    private static void reg(int rgb, int tileId, TileHandler handler) {
        if (rgbToTileIdMap.containsKey(rgb) || tileHandlers.containsKey(tileId))
            throw new IllegalArgumentException(
                    "duplicate tile data: rgb=#" + Integer.toString(rgb, 16) + ", id=" + tileId);

        rgbToTileIdMap.put(rgb,    tileId );
        tileHandlers  .put(tileId, handler);
    }

    static {
        // Пустота (void) (тайлы, сквозь которые всегда можно спокойно, беспрепятственно проходить).
        VoidHandler voidHandler = new VoidHandler();
        reg(0xffffff, 0, voidHandler);

        // Твёрдые тайлы (solid).
        SolidHandler solidHandler = new SolidHandler();
        reg(0x000000, 1, solidHandler);
        reg(0x4a4a4a, 2, solidHandler);
        reg(0x636363, 3, solidHandler);
        reg(0x7e7e7e, 4, solidHandler);
        reg(0x9a9a9a, 5, solidHandler);
        reg(0xb5b5b5, 6, solidHandler);
    }

    static Integer rgbToTileId(int rgb) {
        return rgbToTileIdMap.get(rgb);
    }

    static TileHandler getTileHandler(int tileId) {
        return tileHandlers.get(tileId);
    }

}
