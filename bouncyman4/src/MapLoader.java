import java.util.ArrayList;
import java.util.List;

/**
 * 문자 기반 맵 정의/로드
 * - 사용 가능한 문자:
 *   '.' : EMPTY
 *   '#' : WALL
 *   'S' : STAR (힌트/별)
 *   'K' : SPIKE (가시)
 *   'D' : DOOR (문)
 *   'Y' : GEM_YELLOW (노란 보석)
 *   'B' : GEM_BLUE (파란 보석)
 *   'P' : 플레이어 시작 위치
 *   'L' : LAVA (용암 타일)
 *   'G' : GEAR_LR   "좌우로 1~2칸" 움직이는 톱니바퀴 스폰
 *   'H' : GEAR_UD     "위아래로 1~2칸" 움직이는 톱니바퀴 스폰
 *
 * 맵 담당자는 아래 getXXXLines() 메서드의 문자열만 수정/추가하면 됨.
 */
public class MapLoader {

    public enum TileType {
        EMPTY,
        WALL,
        SPIKE,
        STAR,
        DOOR,
        GEM_YELLOW,
        GEM_BLUE,
        LAVA,   // 용암
        // *추가* 움직이는 톱니바퀴 
        GEAR_LR,   // 'G'   left and right
        GEAR_UD     // 'H'  // up and down
    }

    /** 실제 게임에 사용되는 맵 데이터 */
    public static class MapData {
        public final TileType[][] tiles;
        public final int width;
        public final int height;
        public final int playerStartX;
        public final int playerStartY;
        public final int totalStars;

        // (추가) 기어 스폰(타일 좌표)
        public final List<Point> gearDownSpawns; // 'G' : 좌우 이동 기어
        public final List<Point> gearUpSpawns;   // 'H' : 상하 이동 기어

        public MapData(TileType[][] tiles, int width, int height,
                       int playerStartX, int playerStartY, int totalStars,
                       List<Point> gearDownSpawns, List<Point> gearUpSpawns) {
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

    // (추가) MapLoader 내부에서만 쓰는 간단한 좌표 클래스(외부 라이브러리 추가 없이)
    public static class Point {
        public final int x;
        public final int y;
        public Point(int x, int y) { this.x = x; this.y = y; }
    }

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

    /**
     * 문자열 배열을 실제 타일 2차원 배열로 변환
     */
    private static MapData loadFromLines(String[] lines) {
        if (lines == null || lines.length == 0) return null;

        int height = lines.length;
        int width = lines[0].length();

        TileType[][] tiles = new TileType[height][width];
        int playerStartX = 0;
        int playerStartY = 0;
        int starCount = 0;

        // (추가) 기어 스폰 위치 수집
        List<Point> gearDownSpawns = new ArrayList<>(); // 'G'
        List<Point> gearUpSpawns   = new ArrayList<>(); // 'H'

        for (int y = 0; y < height; y++) {
            String row = lines[y];
            if (row.length() != width) {
                throw new IllegalArgumentException("맵의 모든 줄은 동일한 길이여야 합니다.");
            }
            for (int x = 0; x < width; x++) {
                char c = row.charAt(x);
                TileType type;
                switch (c) {
                    case '#':
                        type = TileType.WALL;
                        break;
                    case 'S':
                        type = TileType.STAR;
                        starCount++;
                        break;
                    case 'K':
                        type = TileType.SPIKE;
                        break;
                    case 'Y':
                        type = TileType.GEM_YELLOW;
                        break;
                    case 'B':
                        type = TileType.GEM_BLUE;
                        break;
                    case 'P':
                        type = TileType.EMPTY;
                        playerStartX = x;
                        playerStartY = y;
                        break;
                    case 'L':
                        type = TileType.LAVA;
                        break;

                    // (추가) 톱니바퀴 스폰. 타일은 EMPTY로 둔다.
                    //  - 이유: 기어는 "오브젝트"로 따로 움직이므로, 맵 충돌(벽)과 섞지 않기 위해
                    case 'G':
                        type = TileType.EMPTY;
                        gearDownSpawns.add(new Point(x, y));
                        break;
                    case 'H':
                        type = TileType.EMPTY;
                        gearUpSpawns.add(new Point(x, y));
                        break;

                    case '.':
                    default:
                        type = TileType.EMPTY;
                        break;
                }
                tiles[y][x] = type;
            }
        }

        // GamePanel.TILE_SIZE와 일치해야 함
        int tileSize = 32;
        int px = playerStartX * tileSize + (int) (tileSize * 0.15);
        int py = playerStartY * tileSize - (int) (tileSize * 0.1);

        return new MapData(tiles, width, height, px, py, starCount, gearDownSpawns, gearUpSpawns);
    }

    // ---------------- 튜토리얼 맵 ----------------

    private static String[] getTutorialMapLines() {
        List<String> rows = new ArrayList<>();

        rows.add("##############################"); // 0
        rows.add("#............................#");
        rows.add("#............................#");// 1
        rows.add("#............................#"); // 2
        rows.add("#..............Y.............#"); // 3 (2층 노란 보석)
        rows.add("#.........KKK................#"); // 4
        rows.add("#S######################...B.#"); // 5 (왼쪽 별 + 2층 발판)
        rows.add("#............................#"); // 6
        rows.add("#.........................B.##"); // 7
        rows.add("#............................#"); // 8
        rows.add("#.......................#....#"); // 9
        rows.add("#..P.B..KK....G..H...B.......#"); //10 (1층 시작 + 파란 보석)
        rows.add("##############################"); //11
        rows.add("##############################"); //12
        rows.add("##############################"); //13
        rows.add("##############################"); //14
        rows.add("##############################"); //15

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
        rows.add("#...#........................#"); // 플레이어 시작 위치
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
        rows.add("#........K...K....B..........#");
        rows.add("#.........KKK................#");
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
        rows.add("#............................#");
        rows.add("#............................#");
        rows.add("#............................#");
        rows.add("#............................#");
        rows.add("#............................#");
        rows.add("#............................#");
        rows.add("#............................#");
        rows.add("#............................#");
        rows.add("#P...#......................S#");
        rows.add("##############################");
        rows.add("##############################");
        rows.add("##############################");
        rows.add("##############################");
        rows.add("##############################");
        return rows.toArray(new String[0]);
    }
}
