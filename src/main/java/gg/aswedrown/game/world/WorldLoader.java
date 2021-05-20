package gg.aswedrown.game.world;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

@Slf4j
@RequiredArgsConstructor
public class WorldLoader {

    private static final String TOK_WORLD_WIDTH  = "WorldWidth";
    private static final String TOK_WORLD_HEIGHT = "WorldHeight";
    private static final String TOK_TILE_SIZE    = "TileSize";

    private final int dimension;

    private LevelParseState parseState = LevelParseState.EXPECTING_IDENTIFIER;

    @Getter
    private WorldLoadStatus loadStatus = WorldLoadStatus.NOT_LOADED;

    private void processToken(String token, WorldData targetWorldData) {
        switch (token) {
            case TOK_WORLD_WIDTH:
                if (parseState == LevelParseState.EXPECTING_IDENTIFIER)
                    parseState = LevelParseState.EXPECTING_WORLD_WIDTH_INT_VAL;
                else {
                    log.error("Invalid token in level data file: unexpected identifier");
                    loadStatus = WorldLoadStatus.PARSE_ERROR;
                }

                break;

            case TOK_WORLD_HEIGHT:
                if (parseState == LevelParseState.EXPECTING_IDENTIFIER)
                    parseState = LevelParseState.EXPECTING_WORLD_HEIGHT_INT_VAL;
                else {
                    log.error("Invalid token in level data file: unexpected identifier");
                    loadStatus = WorldLoadStatus.PARSE_ERROR;
                }

                break;

            case TOK_TILE_SIZE:
                if (parseState == LevelParseState.EXPECTING_IDENTIFIER)
                    parseState = LevelParseState.EXPECTING_TILE_SIZE_INT_VAL;
                else {
                    log.error("Invalid token in level data file: unexpected identifier");
                    loadStatus = WorldLoadStatus.PARSE_ERROR;
                }

                break;

            default:
                try {
                    int intVal = Integer.parseInt(token);

                    if (intVal <= 0)
                        throw new IllegalArgumentException("non-positive integers are not allowed");

                    switch (parseState) {
                        case EXPECTING_WORLD_WIDTH_INT_VAL:
                            targetWorldData.width = intVal;
                            break;

                        case EXPECTING_WORLD_HEIGHT_INT_VAL:
                            targetWorldData.height = intVal;
                            break;

                        case EXPECTING_TILE_SIZE_INT_VAL:
                            targetWorldData.tileSize = intVal;
                            break;

                        default:
                            log.error("Invalid token in level data file: expected identifier");
                            loadStatus = WorldLoadStatus.PARSE_ERROR;

                            break;
                    }

                    parseState = LevelParseState.EXPECTING_IDENTIFIER;
                } catch (IllegalArgumentException ex) {
                    log.error("Invalid token in level data file: expected positive 32-bit integer");
                    loadStatus = WorldLoadStatus.PARSE_ERROR;
                }

                break;
        }
    }

    private void processPixel(int x, int y, int rgb, WorldData targetWorldData) {
        // Конвертируем RGB-цвет пикселя в ID тайла (текстуры).
        Integer tileId = TileData.rgbToTileId(rgb);

        if (tileId == null) {
            log.error("Invalid tile RGB #{} at ({}, {})", Integer.toString(rgb, 16), x, y);
            loadStatus = WorldLoadStatus.BITMAP_ERROR;

            return;
        }

        // Сохраняем базовую информацию об этом тайле в памяти.
        TileBlock tileBlock = new TileBlock();

        tileBlock.tileId = tileId;
        tileBlock.posX   = x;
        tileBlock.posY   = y;

        targetWorldData.tiles.add(tileBlock);
    }

    public WorldData loadWorld() {
        if (loadStatus != WorldLoadStatus.NOT_LOADED)
            throw new IllegalStateException("loadWorld() called twice");

        log.info("Loading dimension {}", dimension);

        WorldData targetWorldData = new WorldData();
        targetWorldData.dimension = dimension;

        try {
            // Читаем и обрабатываем метаданные о мире (размеры текстур и т.п.).
            String dimFolder = "assets/worlds/dim_" + dimension;
            Scanner levelMeta = new Scanner(new File(dimFolder + "/level-meta.dat"));
            String token;

            while (levelMeta.hasNext()) {
                token = levelMeta.next();
                processToken(token, targetWorldData);

                if (loadStatus == WorldLoadStatus.PARSE_ERROR) {
                    log.error("World loader: Parse error");
                    return targetWorldData;
                }
            }

            if (targetWorldData.width == 0 || targetWorldData.height == 0 || targetWorldData.tileSize == 0) {
                log.error("World Loader: File error: reached EOF but not fully parsed yet");
                loadStatus = WorldLoadStatus.FILE_ERROR;

                return targetWorldData;
            }

            log.info("World size: {}x{} x {}x{}",
                    targetWorldData.width, targetWorldData.height,
                    targetWorldData.tileSize, targetWorldData.tileSize);

            // Читаем и обрабатываем "начинку" мира (местоположение тайлов и т.п.).
            BufferedImage scheme = ImageIO.read(new File(dimFolder + "/level-scheme.bmp"));

            if (scheme.getWidth() != targetWorldData.width
                    || scheme.getHeight() != targetWorldData.height) {
                log.error("World loader: Compat error: level-meta specifies world of size {}x{}, " +
                        "but the size of the level-scheme BMP is {}x{}",
                        targetWorldData.width, targetWorldData.height,
                        scheme.getWidth(), scheme.getHeight());

                loadStatus = WorldLoadStatus.COMPAT_ERROR;

                return targetWorldData;
            }

            for (int y = 0; y < scheme.getHeight(); y++) {
                for (int x = 0; x < scheme.getWidth(); x++) {
                    int rgb = scheme.getRGB(x, y) & 0x00ffffff; // удаляем альфа-канал (непрозрачность (не нужна))
                    processPixel(x, y, rgb, targetWorldData);

                    if (loadStatus == WorldLoadStatus.BITMAP_ERROR) {
                        log.error("World Loader: Bitmap error");
                        return targetWorldData;
                    }
                }
            }

            // Мир загружен успешно.
            loadStatus = WorldLoadStatus.LOADED;

            return targetWorldData;
        } catch (IOException ex) {
            log.error("World loader: File error: {}", ex.toString());
            loadStatus = WorldLoadStatus.FILE_ERROR;

            return targetWorldData;
        }
    }

    enum LevelParseState {
        EXPECTING_IDENTIFIER,
        EXPECTING_WORLD_WIDTH_INT_VAL,
        EXPECTING_WORLD_HEIGHT_INT_VAL,
        EXPECTING_TILE_SIZE_INT_VAL
    };

    enum WorldLoadStatus {
        NOT_LOADED,   // загрузка мира ещё на выполнялась (loadWorld)
        LOADED,       // мир успешно загружен
        FILE_ERROR,   // не удалось открыть/прочитать какой-либо из файлов, связанных с миром
        PARSE_ERROR,  // не удалось обработать файл с данными о мире из-за синтаксической ошибки в формате файла
        BITMAP_ERROR, // не удалось обработать файл с "начинкой" мира из-за ошибки в формате файла схемы (BMP)
        COMPAT_ERROR  // файлы level-meta.dat, level-scheme.bmp и/или tilemap.png не совместимы друг с другом
    };

}
