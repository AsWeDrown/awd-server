package gg.aswedrown.game.world;

import java.util.Collection;
import java.util.HashSet;

public class WorldData {

    /**
     * ID типа/номера измерения. Отсчёт с единицы.
     */
    public int dimension;

    /**
     * Ширина измерения, в тайлах.
     */
    public int width;

    /**
     * Высота измерения, в тайлах.
     */
    public int height;

    /**
     * Базовый размер (и ширина, и высота) одного тайла, в пикселях.
     * Используется для выделения тайла по ID и координатам текстуры из tilemap.
     *
     * Для прорисовки на экране не подходит - см. displayTileSize.
     *
     * (На сервере не используется.)
     */
    public int tileSize;

    /**
     * Базовая информация о загруженных тайлах (используется для
     * физики, взаимодействия с миром и пр. - не для прорисовки).
     */
    public Collection<TileBlock> tiles = new HashSet<>();

}
