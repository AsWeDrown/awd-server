package gg.aswedrown.game.world;

import java.util.HashMap;
import java.util.Map;

public final class TileData {

    private TileData() {}

    ///////////////////////////////////////////////////////////////////////////////////////
    // Для конверсии RGB-цвета пикселей из level-scheme в ID (номера) тайлов (текстур).
    // Подробнее в файле "bmp_pixel_rgb_to_tile_id_mapping.png".
    ///////////////////////////////////////////////////////////////////////////////////////
    private static final Map<Integer, Integer> rgbToTileIdMap = new HashMap<>();

    static {
        // Пустота (полностью прозрачная текстура).
        rgbToTileIdMap.put(0xffffff, 0);

        // Твёрдые (solid) тайлы
        rgbToTileIdMap.put(0x000000, 1);
        rgbToTileIdMap.put(0x4a4a4a, 2);
    }

    public static Integer rgbToTileId(int rgb) {
        return rgbToTileIdMap.get(rgb);
    }

}
