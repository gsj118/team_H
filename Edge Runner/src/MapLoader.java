import java.util.ArrayList;
import java.util.List;

public class MapLoader {

    // ===============================
    // Tile Definition
    // ===============================

    public enum TileType {
        EMPTY,
        WALL,
        SPIKE,
        STAR,
        DOOR,
        GEM_YELLOW,
        GEM_BLUE,
        LAVA
    }

    // ===============================
    // Simple Coordinate Class
    // ===============================

    public static class Point {

        public final int x;
        public final int y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    // ===============================
    // Map Data Container
    // ===============================

    public static class MapData {

        public final TileType[][] tiles;

        public final int width;
        public final int height;

        public final int playerStartX;
        public final int playerStartY;

        public final int totalStars;

        public final List<Point> gearDownSpawns;
        public final List<Point> gearUpSpawns;

        public MapData(
                TileType[][] tiles,
                int width,
                int height,
                int playerStartX,
                int playerStartY,
                int totalStars,
                List<Point> gearDownSpawns,
                List<Point> gearUpSpawns
        ) {
            this.tiles = tiles;
            this.width = width;
            this.height = height;
            this.playerStartX = playerStartX;
            this.playerStartY = playerStartY;
            this.totalStars = totalStars;
            this.gearDownSpawns = gearDownSpawns;
            this.gearUpSpawns = gearUpSpawns;
        }
    }

    // ===============================
    // Stage Loader Entry
    // ===============================

    public static MapData loadStage(int stageIndex) {

        switch (stageIndex) {
            case 0:
                return loadFromLines(getTutorialMapLines());

            case 1:
                return loadFromLines(getStage1Lines());

            case 2:
                return loadFromLines(getStage2Lines());

            case 3:
                return loadFromLines(getStage3Lines());

            default:
                return null;
        }
    }

    // ===============================
    // Core Parsing Logic
    // ===============================

    private static MapData loadFromLines(String[] lines) {

        if (lines == null || lines.length == 0) {
            return null;
        }

        int mapHeight = lines.length;
        int mapWidth = lines[0].length();

        TileType[][] tiles =
                new TileType[mapHeight][mapWidth];

        int playerTileX = 0;
        int playerTileY = 0;

        int starCount = 0;

        List<Point> gearDownSpawns =
                new ArrayList<>();

        List<Point> gearUpSpawns =
                new ArrayList<>();

        for (int y = 0; y < mapHeight; y++) {

            String row = lines[y];

            validateRowLength(row, mapWidth);

            for (int x = 0; x < mapWidth; x++) {

                char symbol = row.charAt(x);

                TileType tileType =
                        parseTileSymbol(
                                symbol,
                                x,
                                y,
                                gearDownSpawns,
                                gearUpSpawns
                        );

                if (symbol == 'S') {
                    starCount++;
                }

                if (symbol == 'P') {
                    playerTileX = x;
                    playerTileY = y;
                }

                tiles[y][x] = tileType;
            }
        }

        return buildMapData(
                tiles,
                mapWidth,
                mapHeight,
                playerTileX,
                playerTileY,
                starCount,
                gearDownSpawns,
                gearUpSpawns
        );
    }

    // ===============================
    // Tile Parsing
    // ===============================

    private static TileType parseTileSymbol(
            char c,
            int x,
            int y,
            List<Point> gearDownSpawns,
            List<Point> gearUpSpawns
    ) {

        switch (c) {

            case '#':
                return TileType.WALL;

            case 'S':
                return TileType.STAR;

            case 'K':
                return TileType.SPIKE;

            case 'D':
                return TileType.DOOR;

            case 'Y':
                return TileType.GEM_YELLOW;

            case 'B':
                return TileType.GEM_BLUE;

            case 'L':
                return TileType.LAVA;

            case 'G':
                gearDownSpawns.add(new Point(x, y));
                return TileType.EMPTY;

            case 'H':
                gearUpSpawns.add(new Point(x, y));
                return TileType.EMPTY;

            case 'P':
                return TileType.EMPTY;

            default:
                return TileType.EMPTY;
        }
    }

    // ===============================
    // Validation
    // ===============================

    private static void validateRowLength(
            String row,
            int expectedWidth
    ) {
        if (row.length() != expectedWidth) {
            throw new IllegalArgumentException(
                    "맵 줄 길이 불일치"
            );
        }
    }

    // ===============================
    // MapData Construction
    // ===============================

    private static MapData buildMapData(
            TileType[][] tiles,
            int width,
            int height,
            int playerTileX,
            int playerTileY,
            int starCount,
            List<Point> gearDownSpawns,
            List<Point> gearUpSpawns
    ) {

        int tileSize = 32;

        int playerPixelX =
                playerTileX * tileSize
                        + (int) (tileSize * 0.15);

        int playerPixelY =
                playerTileY * tileSize
                        - (int) (tileSize * 0.1);

        return new MapData(
                tiles,
                width,
                height,
                playerPixelX,
                playerPixelY,
                starCount,
                gearDownSpawns,
                gearUpSpawns
        );
    }

    // ===============================
    // Map Line Definitions
    // ===============================

    private static String[] getTutorialMapLines() {

        List<String> rows = new ArrayList<>();

        rows.add("##############################");
        rows.add("#............................#");
        rows.add("#............................#");
        rows.add("#..............Y.............#");
        rows.add("#.........KKK................#");
        rows.add("#S######################...B.#");
        rows.add("#............................#");
        rows.add("#.........................B.##");
        rows.add("#............................#");
        rows.add("#.......................#....#");
        rows.add("#..P.B..KK...........B.......#");
        rows.add("##############################");
        rows.add("##############################");
        rows.add("##############################");
        rows.add("##############################");
        rows.add("##############################");

        return rows.toArray(new String[0]);
    }

    private static String[] getStage1Lines() {

        List<String> rows = new ArrayList<>();

        rows.add("##############################");
        rows.add("#............................#");
        rows.add("#............................#");
        rows.add("#............................#");
        rows.add("#............................#");
        rows.add("#............................#");
        rows.add("#............................#");
        rows.add("#......H..B....B.............#");
        rows.add("#......#...........Y.........#");
        rows.add("#....#.......................#");
        rows.add("#...#........................#");
        rows.add("#BG...P.....................S#");
        rows.add("#######LLLLLLLLLLLLLLLLLLLL###");
        rows.add("##############################");
        rows.add("##############################");
        rows.add("##############################");

        return rows.toArray(new String[0]);
    }

    private static String[] getStage2Lines() {

        List<String> rows = new ArrayList<>();

        rows.add("##############################");
        rows.add("#.....H..H...#...#...#...#...S");
        rows.add("#.P..........#...#...#.....G.#");
        rows.add("###..............#...#....#..#");
        rows.add("#..KKK#..#...#..........#....#");
        rows.add("#......KK..B..K.#...#.#......#");
        rows.add("#........KKKKK....B..........#");
        rows.add("#............................#");
        rows.add("#............................#");
        rows.add("#............................#");
        rows.add("#LLLLLLLLLLLLLLLLLLLLLLLLLLLL#");
        rows.add("##############################");
        rows.add("##############################");
        rows.add("##############################");
        rows.add("##############################");
        rows.add("##############################");

        return rows.toArray(new String[0]);
    }

    private static String[] getStage3Lines() {

        List<String> rows = new ArrayList<>();

        rows.add("##############################");
        rows.add("#............................#");
        rows.add("#.......HHHHHHH..............#");
        rows.add("#............................#");
        rows.add("#...S.....Y......Y....B.S....#");
        rows.add("#..################KKK####...#");
        rows.add("#..#.......#..............#.B#");
        rows.add("#..........#.................#");
        rows.add("#..##......#.................#");
        rows.add("#KK##GSSSSG...##H#........B..#");
        rows.add("############..##.....#########");
        rows.add("#####...SBG#...Y.......SB....#");
        rows.add("#####..####...####H##H########");
        rows.add("#P..B..................S.....#");
        rows.add("##############################");
        rows.add("##############################");

        return rows.toArray(new String[0]);
    }
}