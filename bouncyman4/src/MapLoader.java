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
        LAVA,
        
    }

    /** 실제 게임에 사용되는 맵 데이터 */
    public static class MapData {
        public final TileType[][] tiles;
        public final int width;
        public final int height;
        public final int playerStartX;
        public final int playerStartY;
        public final int totalStars;

        public MapData(TileType[][] tiles, int width, int height,
                       int playerStartX, int playerStartY, int totalStars) {
            this.tiles = tiles;
            this.width = width;
            this.height = height;
            this.playerStartX = playerStartX;
            this.playerStartY = playerStartY;
            this.totalStars = totalStars;
        }
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

        return new MapData(tiles, width, height, px, py, starCount);
    }

    // ---------------- 튜토리얼 맵 ----------------

    /**
     * 튜토리얼
     * - 1층 이동 연습
     * - 오른쪽 파란 보석(B) → 2층 점프
     * - 2층 노란 보석(Y) → 왼쪽 별(S) 대시로 획득
     */
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
        rows.add("#..P.B..KK...........B.......#"); //10 (1층 시작 + 파란 보석)
        rows.add("##############################"); //11
        rows.add("##############################"); //12
        rows.add("##############################"); //13
        rows.add("##############################"); //14
        rows.add("##############################"); //15

        return rows.toArray(new String[0]);
    }

    // ---------------- Stage1~3 : 스토리 전용 빈 맵 ----------------

    /**
     * Stage1: CPU 코어 구역 - 스토리 전용, 별 없음 (자동 엔딩)
     */
    private static String[] getStage1Lines() {
        List<String> rows = new ArrayList<>();
        rows.add("##############################");
        rows.add("#............................#");
        rows.add("#............................#");
        rows.add("#............................#");
        rows.add("#............................#");
        rows.add("#............................#");
        rows.add("#............................#");
        rows.add("#.........B....B.............#");
        rows.add("#......#...........Y.........#");
        rows.add("#....#.......................#");
        rows.add("#P.#........................S#"); // 플레이어 시작 위치
        rows.add("####KKKKKKKKKKKKKKKKKKKKKK..##");
        rows.add("##############################");
        rows.add("##############################");
        rows.add("##############################");
        rows.add("##############################");
        return rows.toArray(new String[0]);
    }

    /**
     * Stage2: 메모리·캐시 구역 - 스토리 전용, 별 없음 (자동 엔딩)
     */
    private static String[] getStage2Lines() {
        List<String> rows = new ArrayList<>();
        rows.add("##############################");
        rows.add("#.....#..#...#...#...#...#...S");
        rows.add("#.P......#...#...#...#.......#");
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

    /**
     * Stage3: I/O·방어 모듈 구역 - 스토리 전용, 별 없음 (자동 엔딩)
     */
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
        rows.add("#P...........................#");
        rows.add("##############################");
        rows.add("##############################");
        rows.add("##############################");
        rows.add("##############################");
        rows.add("##############################");
        return rows.toArray(new String[0]);
    }
}
