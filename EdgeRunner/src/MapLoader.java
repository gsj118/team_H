import java.util.ArrayList;
import java.util.List;

public class MapLoader {

    // 맵 문자 기호를 게임 타일 타입으로 변환하기 위한 열거형
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

    // 스폰/좌표 저장용 (타일 좌표)
    public static class Point {

        public final int x;
        public final int y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    // 스테이지 로딩 결과(타일맵 + 플레이어 시작 위치 + 수집물/기믹 정보)
    public static class MapData {

        public final TileType[][] tiles;

        public final int width;
        public final int height;

        // 플레이어 시작 위치(픽셀 좌표로 변환된 값)
        public final int playerStartX;
        public final int playerStartY;

        // 스테이지 내 STAR 총 개수
        public final int totalStars;

        // 기어 스폰 위치(타일 좌표)
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

    // 스테이지 인덱스로 맵 라인을 선택하고 파싱해 MapData 생성
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

    // 문자열 배열(행 단위)을 타일맵으로 파싱
    private static MapData loadFromLines(String[] lines) {

        if (lines == null || lines.length == 0) {
            return null;
        }

        int mapHeight = lines.length;
        int mapWidth = lines[0].length();

        TileType[][] tiles =
                new TileType[mapHeight][mapWidth];

        // 플레이어 시작 타일 좌표
        int playerTileX = 0;
        int playerTileY = 0;

        int starCount = 0;

        // 기믹 스폰 좌표들(타일 좌표로 저장)
        List<Point> gearDownSpawns =
                new ArrayList<>();

        List<Point> gearUpSpawns =
                new ArrayList<>();

        for (int y = 0; y < mapHeight; y++) {

            String row = lines[y];

            // 모든 줄의 길이가 동일해야 직사각형 맵이 됨
            validateRowLength(row, mapWidth);

            for (int x = 0; x < mapWidth; x++) {

                char symbol = row.charAt(x);

                // 문자 기호를 타일 타입으로 변환(+ 일부는 스폰 리스트에 좌표 기록)
                TileType tileType =
                        parseTileSymbol(
                                symbol,
                                x,
                                y,
                                gearDownSpawns,
                                gearUpSpawns
                        );

                // 별 개수 집계
                if (symbol == 'S') {
                    starCount++;
                }

                // 플레이어 시작 위치 기록(P는 타일 자체는 EMPTY)
                if (symbol == 'P') {
                    playerTileX = x;
                    playerTileY = y;
                }

                tiles[y][x] = tileType;
            }
        }

        // 최종 MapData 구성(플레이어 위치는 픽셀 좌표로 보정)
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

    // 맵 문자 1개를 TileType으로 변환 (G/H는 스폰 좌표만 추가하고 EMPTY로 처리)
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

    // 행 길이 검증(맵이 찌그러지는 오류 방지)
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

    // 타일 좌표 기반 데이터를 최종 MapData로 변환(플레이어 좌표는 픽셀 + 보정)
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

        // 플레이어를 타일 중앙/바닥에 맞추기 위한 픽셀 보정
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

    // 튜토리얼 맵 문자열 정의
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

    // 스테이지 1 맵 문자열 정의
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

    // 스테이지 2 맵 문자열 정의
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

    // 스테이지 3 맵 문자열 정의
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