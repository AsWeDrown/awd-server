package gg.aswedrown.game.world;

import java.util.Collection;

public class WorldData {

    public int dimension, // ID измерения (отсчёт с единцы!)
               width,     // ширина всего мира, в тайлах
               height,    // высота всего мира, в тайлах
               tileSize;  // ширина и высота всех тайлов, в пикселях

    public Collection<TileBlock> tiles; // базовая информация о загруженных тайлах

}
