package gg.aswedrown.game.world;

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
    
    private static final Map<Integer, Integer> tileFeatures = new HashMap<>();

    private static final int SOLID = 0b1;
    
    private static void reg(int rgb, int tileId, int features) {
        if (rgbToTileIdMap.containsKey(rgb) || tileFeatures.containsKey(tileId))
            throw new IllegalArgumentException(
                    "duplicate tile data: rgb=#" + Integer.toString(rgb, 16) + ", id=" + tileId);

        rgbToTileIdMap.put(rgb,    tileId  );
        tileFeatures  .put(tileId, features);
    }

    static {
        // Пустота (полностью прозрачная текстура).
        rgbToTileIdMap.put(0xffffff, 0);

        // Твёрдые тайлы (solid).
        reg(0x000000, 1, SOLID);
        reg(0x4a4a4a, 2, SOLID);
        reg(0x636363, 3, SOLID);
        reg(0x7e7e7e, 4, SOLID);
        reg(0x9a9a9a, 5, SOLID);
        reg(0xb5b5b5, 6, SOLID);
    }

    static Integer rgbToTileId(int rgb) {
        return rgbToTileIdMap.get(rgb);
    }

    static boolean isSolid(int tileId) {
        return (tileFeatures.get(tileId) & SOLID) != 0;
    }

}
