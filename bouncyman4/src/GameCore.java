import javax.swing.*;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

//(추가) O/X 퀴즈 시스템
//- 별을 다 먹으면(스테이지 1~3) QUESTION 상태로 전환
//- QuestionBox에서 문제 1개를 뽑아와 표시/정답 판정

/**
 * GameCore
 * - 게임 루프, 더블 버퍼링, 입력 처리, 상태 전환 담당
 * - UI 담당: QUESTION/ENDING 상태 화면을 자신 코드로 교체 가능
 * - 맵 담당: MapLoader의 문자 맵만 수정/추가하면 스테이지 확장 가능
 */
public class GameCore {

    public static final String CARD_MENU = "MENU";
    public static final String CARD_GAME = "GAME";

    // 사운드 관리 객체
    private static SoundManager soundManager;

    public static SoundManager getSoundManager() {
        return soundManager;
    }

    public static void main(String[] args) {

        // SoundManager 초기화 (GameCore의 static 필드에 저장)
        soundManager = new SoundManager();

        // 메인 메뉴 BGM
        soundManager.playBGM(SoundManager.BGM_MAIN_MENU);

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Bounce Escape (Core Prototype)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // CardLayout을 사용할 메인 컨테이너
            JPanel mainContainer = new JPanel(new CardLayout());

            GamePanel gamePanel = new GamePanel();
            MenuPanel menuPanel = new MenuPanel(gamePanel, mainContainer);
            gamePanel.initPausePanel(mainContainer);

            mainContainer.add(menuPanel, CARD_MENU);
            mainContainer.add(gamePanel, CARD_GAME);

            frame.setContentPane(mainContainer);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    /** 전체 게임 상태 */
    private enum GameState {
        MENU,
        TUTORIAL,
        STAGE_PLAY,
        QUESTION,
        ENDING,
        PAUSE
    }

    /**
     * 실제 렌더링/업데이트를 담당하는 패널
     */
    public static class GamePanel extends JPanel implements Runnable, KeyListener {

        public static final int WIDTH = 960;
        public static final int HEIGHT = 540;
        public static final int TILE_SIZE = 32;

        private static final long serialVersionUID = 1L;

        // 맵을 화면 위에서 약간 내리기 위한 오프셋
        private static final int MAP_OFFSET_Y = 60;

        // 고정 FPS 루프
        private static final double TARGET_FPS = 60.0;
        private static final double FRAME_TIME_NANO = 1_000_000_000.0 / TARGET_FPS;

        // 더블 버퍼링용
        private BufferedImage backBuffer;
        private Graphics2D backG;

        // 게임 루프 스레드
        private Thread gameThread;
        private volatile boolean running = false;

        // 상태
        private GameState state = GameState.TUTORIAL;
        private boolean questionAnsweredThisStage = false;

        // 입력 상태
        private boolean leftPressed;
        private boolean rightPressed;

        // 디버그
        private boolean debugDrawHitbox = false;
        private boolean debugShowInfo = false;

        private double currentFps = 0.0;
        private long fpsCounterStartTime = 0L;
        private int frameCount = 0;

        // 스테이지 관련
        private int currentStageIndex = 0;
        private static final int MAX_STAGE_COUNT = 4; // 0~3
        private StageInfo[] stageInfos = new StageInfo[MAX_STAGE_COUNT];

        private MapLoader.MapData currentMap;
        private Player player;

        // 별/힌트 관련
        private int totalStarsInStage = 0;
        private int collectedStars = 0;

        // 문제/엔딩 관련
        private boolean waitingForQuestionAnswer = false;
        private boolean lastAnswerCorrect = false;

        // (추가) 현재 스테이지 퀴즈 (QuestionBox.java에서 뽑아옴)
        private QuestionBox.Quiz currentQuiz;

        // 튜토리얼 도움말 표시 여부(H 키로 토글)
        private boolean tutorialOverlayVisible = true;

        // 플레이어 스프라이트
        private BufferedImage basicStandImg;
        private BufferedImage basicJumpImg;
        private BufferedImage yellowStandImg;
        private BufferedImage yellowJumpImg;
        private BufferedImage blueStandImg;
        private BufferedImage blueJumpImg;

        // 타일/아이템 이미지
        private BufferedImage tileLavaImg;   // 기존 WALL 이미지(lava.png)
        private BufferedImage coinImg;
        private BufferedImage gemYellowImg;
        private BufferedImage gemBlueImg;

        // 코인 애니메이션
        private double coinAnimTime = 0.0;

        // UI: 일시정지 패널 인스턴스
        private PausePanel pausePanel;

        // UI: 일시정지 전 상태 저장
        private GameState lastPlayState = GameState.TUTORIAL;

        // ---------------- (추가) 스테이지 배경 ----------------
        private BufferedImage bgTutorialImg;
        private BufferedImage bgStage1Img;
        private BufferedImage bgStage2Img;
        private BufferedImage bgStage3Img;

        private static final String BG_TUTORIAL_PATH = "image/bg_tutorial.jpg";
        private static final String BG_STAGE1_PATH   = "image/bg_stage1.jpg";
        private static final String BG_STAGE2_PATH   = "image/bg_stage2.jpg";
        private static final String BG_STAGE3_PATH   = "image/bg_stage3.jpg";
        // ------------------------------------------------------

        // ---------------- (추가) L(용암) GIF 타일 ----------------
        private static final String LAVA_GIF_PATH = "image/lava real.gif"; // 네 파일명 그대로
        private static final double LAVA_FPS = 12.0;
        private List<BufferedImage> lavaFrames = new ArrayList<>();
        private double lavaAnimTime = 0.0;
        // ------------------------------------------------------

        public GamePanel() {
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setFocusable(true);
            requestFocusInWindow();
            addKeyListener(this);

            initBackBuffer();
            loadImages();
            loadStage(0); // 튜토리얼부터 시작

            setLayout(null);
        }

        private void initBackBuffer() {
            backBuffer = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
            backG = backBuffer.createGraphics();
            backG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }

        /** 이미지 리소스 로딩 */
        private void loadImages() {
            try {
                basicStandImg  = ImageIO.read(new File("image/alienBiege_stand.png"));
                basicJumpImg   = ImageIO.read(new File("image/alienBiege_jump.png"));
                yellowStandImg = ImageIO.read(new File("image/alienYellow_stand.png"));
                yellowJumpImg  = ImageIO.read(new File("image/alienYellow_jump.png"));
                blueStandImg   = ImageIO.read(new File("image/alienBlue_stand.png"));
                blueJumpImg    = ImageIO.read(new File("image/alienBlue_jump.png"));

                tileLavaImg    = ImageIO.read(new File("image/lava.png"));
                coinImg        = ImageIO.read(new File("image/coinGold.png"));
                gemYellowImg   = ImageIO.read(new File("image/gemYellow.png"));
                gemBlueImg     = ImageIO.read(new File("image/gemBlue.png"));

                // 배경 로딩
                bgTutorialImg = ImageIO.read(new File(BG_TUTORIAL_PATH));
                bgStage1Img   = ImageIO.read(new File(BG_STAGE1_PATH));
                bgStage2Img   = ImageIO.read(new File(BG_STAGE2_PATH));
                bgStage3Img   = ImageIO.read(new File(BG_STAGE3_PATH));

            } catch (IOException e) {
                System.out.println("[IMG] 이미지 로딩 실패: 경로/파일명 확인 필요");
                e.printStackTrace();
            }

            // (추가) 용암 GIF 프레임 로딩
            lavaFrames = loadGifFrames(LAVA_GIF_PATH);
            if (lavaFrames.isEmpty()) {
                System.out.println("[LAVA] 로딩 실패: " + LAVA_GIF_PATH);
            }
        }

        // (추가) GIF를 프레임 리스트로 분해
        private List<BufferedImage> loadGifFrames(String path) {
            List<BufferedImage> frames = new ArrayList<>();
            try (ImageInputStream stream = ImageIO.createImageInputStream(new File(path))) {
                Iterator<javax.imageio.ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
                if (!readers.hasNext()) return frames;

                javax.imageio.ImageReader reader = readers.next();
                reader.setInput(stream, false);

                int count = reader.getNumImages(true);
                for (int i = 0; i < count; i++) {
                    BufferedImage frame = reader.read(i);
                    if (frame != null) frames.add(frame);
                }
                reader.dispose();
            } catch (Exception e) {
                System.out.println("[LAVA] GIF 프레임 분해 실패: " + path);
                e.printStackTrace();
            }
            return frames;
        }

        // (추가) LAVA 타일 그리기
        private void drawAnimatedLava(Graphics2D g, int px, int py) {
            if (lavaFrames == null || lavaFrames.isEmpty()) {
                g.setColor(Color.ORANGE);
                g.fillRect(px, py, TILE_SIZE, TILE_SIZE);
                return;
            }
            int idx = (int)(lavaAnimTime * LAVA_FPS) % lavaFrames.size();
            g.drawImage(lavaFrames.get(idx), px, py, TILE_SIZE, TILE_SIZE, null);
        }

        public void startNewGame() {
            loadStage(0);
            startGameThread();

            // 게임 시작 시 BGM 스테이지 음악
            GameCore.getSoundManager().playBGM(SoundManager.BGM_STAGE);

            SwingUtilities.invokeLater(() -> requestFocusInWindow());
        }

        public void startGameThread() {
            if (gameThread == null) {
                running = true;
                gameThread = new Thread(this, "GameLoopThread");
                gameThread.start();
            }
        }

        @Override
        public void run() {
            long previousTime = System.nanoTime();
            fpsCounterStartTime = previousTime;
            frameCount = 0;

            while (running) {
                long now = System.nanoTime();
                long elapsed = now - previousTime;

                if (elapsed < FRAME_TIME_NANO) {
                    long sleepMillis = (long) ((FRAME_TIME_NANO - elapsed) / 1_000_000L);
                    if (sleepMillis > 0) {
                        try {
                            Thread.sleep(sleepMillis);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    continue;
                }

                previousTime = now;
                double dt = elapsed / 1_000_000_000.0;

                if (state != GameState.PAUSE) {
                    update(dt);
                }
                render();
                repaint();

                frameCount++;
                long fpsElapsed = now - fpsCounterStartTime;
                if (fpsElapsed >= 1_000_000_000L) {
                    currentFps = frameCount * (1_000_000_000.0 / fpsElapsed);
                    frameCount = 0;
                    fpsCounterStartTime = now;
                }
            }
        }

        /** 상태별 업데이트 */
        private void update(double dt) {
            switch (state) {
                case TUTORIAL:
                case STAGE_PLAY:
                    updateStagePlay(dt);
                    break;
                case QUESTION:
                case ENDING:
                case PAUSE:
                case MENU:
                default:
                    break;
            }
        }

        /** 실제 조작이 들어가는 구간 업데이트 */
        private void updateStagePlay(double dt) {
            if (currentMap == null || player == null) return;

            coinAnimTime += dt;
            lavaAnimTime += dt; // (추가) 용암 애니메이션 시간

            handlePlayerInput(dt);

            // 화면 아래로 떨어지면 사망 처리
            if (player.getY() > HEIGHT + TILE_SIZE * 2) {
                handlePlayerDeath();
                return;
            }

            handleTileInteractions();
        }

        /** 키 입력을 이용해 플레이어 속도/중력 적용 */
        private void handlePlayerInput(double dt) {
            double moveSpeed = 220.0;
            double gravity   = 900.0;

            double vx = 0.0;
            if (leftPressed && !rightPressed) {
                vx = -moveSpeed;
            } else if (rightPressed && !leftPressed) {
                vx = moveSpeed;
            }

            // 대시 중에는 수평 입력 무시
            if (!player.isDashing()) {
                player.applyHorizontalVelocity(vx);
            }

            player.applyGravity(gravity, dt);
            player.moveAndCollide(currentMap, dt, TILE_SIZE);
        }

        /**
         * 타일과의 상호작용
         */
        private void handleTileInteractions() {
            int leftTile   = Math.max(0, (int) (player.getLeft()   / TILE_SIZE));
            int rightTile  = Math.min(currentMap.width  - 1, (int) (player.getRight()  / TILE_SIZE));
            int topTile    = Math.max(0, (int) (player.getTop()    / TILE_SIZE));
            int bottomTile = Math.min(currentMap.height - 1, (int) (player.getBottom() / TILE_SIZE));

            for (int ty = topTile; ty <= bottomTile; ty++) {
                for (int tx = leftTile; tx <= rightTile; tx++) {
                    MapLoader.TileType type = currentMap.tiles[ty][tx];

                    switch (type) {
                        case SPIKE:
                            handlePlayerDeath();
                            return;

                        // (추가) L(용암) 밟으면 즉사
                        case LAVA:
                            handlePlayerDeath();
                            return;

                        case STAR:
                            currentMap.tiles[ty][tx] = MapLoader.TileType.EMPTY;
                            collectedStars++;
                            if (stageInfos[currentStageIndex] != null) {
                                stageInfos[currentStageIndex].incrementCollectedStars();
                            }

                            if (collectedStars >= totalStarsInStage && !waitingForQuestionAnswer) {
                                if (currentStageIndex == 0) {
                                    StageInfo info = stageInfos[currentStageIndex];
                                    if (info != null) info.finishStageNow();

                                    loadStage(1);
                                    return;
                                }
                                enterQuestionState();
                            }
                            break;

                        case GEM_YELLOW:
                            currentMap.tiles[ty][tx] = MapLoader.TileType.EMPTY;
                            player.setFormYellow();
                            break;

                        case GEM_BLUE:
                            currentMap.tiles[ty][tx] = MapLoader.TileType.EMPTY;
                            player.setFormBlue();
                            break;

                        case DOOR:
                        case WALL:
                        case EMPTY:
                        default:
                            break;
                    }
                }
            }
        }

        /** 사망 시 스테이지 정보 갱신 후 리셋 */
        private void handlePlayerDeath() {
            if (stageInfos[currentStageIndex] != null) {
                stageInfos[currentStageIndex].incrementDeathCount();
            }
            resetCurrentStage();
        }

        /** 현재 스테이지만 다시 로드 */
        private void resetCurrentStage() {
            MapLoader.MapData map = MapLoader.loadStage(currentStageIndex);
            if (map != null) {
                currentMap = map;
                totalStarsInStage = map.totalStars;
                collectedStars = 0;
                waitingForQuestionAnswer = false;
                questionAnsweredThisStage = false;
                lastAnswerCorrect = false;
                currentQuiz = null;

                player = new Player(map.playerStartX, map.playerStartY,
                        TILE_SIZE * 0.7, TILE_SIZE * 0.9);

                if (stageInfos[currentStageIndex] == null) {
                    stageInfos[currentStageIndex] =
                            new StageInfo(currentStageIndex, getStageName(currentStageIndex), totalStarsInStage);
                }
                stageInfos[currentStageIndex].startStage();

                state = (currentStageIndex == 0) ? GameState.TUTORIAL : GameState.STAGE_PLAY;
            }
        }

        /** 스테이지 이름(엔딩/HUD용) */
        private String getStageName(int stageIndex) {
            switch (stageIndex) {
                case 0: return "튜토리얼";
                case 1: return "CPU 코어 구역";
                case 2: return "메모리·캐시 구역";
                case 3: return "I/O·방어 모듈 구역";
                default: return "Stage " + stageIndex;
            }
        }

        private void loadStage(int stageIndex) {
            if (stageIndex < 0 || stageIndex >= MAX_STAGE_COUNT) return;

            currentStageIndex = stageIndex;
            MapLoader.MapData map = MapLoader.loadStage(stageIndex);
            if (map == null) {
                System.err.println("Map load failed for stage " + stageIndex);
                return;
            }

            currentMap = map;
            totalStarsInStage = map.totalStars;
            collectedStars = 0;
            waitingForQuestionAnswer = false;
            questionAnsweredThisStage = false;
            lastAnswerCorrect = false;
            currentQuiz = null;

            if (stageInfos[stageIndex] == null) {
                stageInfos[stageIndex] =
                        new StageInfo(stageIndex, getStageName(stageIndex), totalStarsInStage);
            } else {
                stageInfos[stageIndex].resetForRetry(totalStarsInStage);
            }
            stageInfos[stageIndex].startStage();

            player = new Player(map.playerStartX, map.playerStartY,
                    TILE_SIZE * 0.7, TILE_SIZE * 0.9);

            state = (stageIndex == 0) ? GameState.TUTORIAL : GameState.STAGE_PLAY;
        }

        /** 별을 모두 먹었을 때 호출 (Stage1~3) */
        private void enterQuestionState() {
            waitingForQuestionAnswer = true;
            questionAnsweredThisStage = false;

            currentQuiz = QuestionBox.pick(currentStageIndex);
            state = GameState.QUESTION;
        }

        public void onQuestionAnswered(boolean correct) {
            if (!waitingForQuestionAnswer || questionAnsweredThisStage) return;

            questionAnsweredThisStage = true;
            waitingForQuestionAnswer = false;
            lastAnswerCorrect = correct;

            StageInfo info = stageInfos[currentStageIndex];
            if (info != null) {
                info.setQuestionAnswered(correct);
                info.finishStageNow();
            }

            state = GameState.ENDING;
        }

        private void goToNextStageOrFinishGame() {
            if (lastAnswerCorrect) {
                if (currentStageIndex + 1 < MAX_STAGE_COUNT) {
                    loadStage(currentStageIndex + 1);
                } else {
                    loadStage(0);
                }
            } else {
                resetCurrentStage();
            }
        }

        /** 백버퍼에 렌더 */
        private void render() {
            if (backG == null) return;

            backG.setColor(Color.WHITE);
            backG.fillRect(0, 0, WIDTH, HEIGHT);

            switch (state) {
                case TUTORIAL:
                case STAGE_PLAY:
                    renderStagePlay(backG);
                    renderHUD(backG);
                    if (state == GameState.TUTORIAL && tutorialOverlayVisible) {
                        renderTutorialOverlay(backG);
                    }
                    break;

                case QUESTION:
                    renderStagePlay(backG);
                    renderHUD(backG);
                    renderQuestionOverlay(backG);
                    break;

                case ENDING:
                    renderStagePlay(backG);
                    renderHUD(backG);
                    renderEndingOverlay(backG);
                    break;

                case PAUSE:
                    renderStagePlay(backG);
                    renderHUD(backG);
                    break;

                case MENU:
                default:
                    renderSimpleMenu(backG);
                    break;
            }

            if (debugShowInfo) {
                renderDebugInfo(backG);
            }
        }

        /** 맵 + 플레이어 그리기 */
        private void renderStagePlay(Graphics2D g) {
            if (currentMap == null || player == null) return;

            // 스테이지별 배경
            BufferedImage bg = null;
            switch (currentStageIndex) {
                case 0: bg = bgTutorialImg; break;
                case 1: bg = bgStage1Img; break;
                case 2: bg = bgStage2Img; break;
                case 3: bg = bgStage3Img; break;
            }
            if (bg != null) {
                g.drawImage(bg, 0, MAP_OFFSET_Y, WIDTH, HEIGHT - MAP_OFFSET_Y, null);
            }

            // 타일
            for (int y = 0; y < currentMap.height; y++) {
                for (int x = 0; x < currentMap.width; x++) {
                    MapLoader.TileType type = currentMap.tiles[y][x];
                    int px = x * TILE_SIZE;
                    int py = y * TILE_SIZE + MAP_OFFSET_Y;

                    switch (type) {
                        case WALL:
                            if (tileLavaImg != null) {
                                g.drawImage(tileLavaImg, px, py, TILE_SIZE, TILE_SIZE, null);
                            } else {
                                g.setColor(Color.LIGHT_GRAY);
                                g.fillRect(px, py, TILE_SIZE, TILE_SIZE);
                            }
                            break;

                        // (추가) L 타일 = 용암 GIF
                        case LAVA:
                            drawAnimatedLava(g, px, py);
                            break;

                        case SPIKE:
                            g.setColor(Color.RED);
                            int[] xs = {px, px + TILE_SIZE / 2, px + TILE_SIZE};
                            int[] ys = {py + TILE_SIZE, py, py + TILE_SIZE};
                            g.fillPolygon(xs, ys, 3);
                            break;

                        case STAR:
                            drawAnimatedCoin(g, px, py);
                            break;

                        case GEM_YELLOW:
                            if (gemYellowImg != null) {
                                g.drawImage(gemYellowImg, px, py, TILE_SIZE, TILE_SIZE, null);
                            }
                            break;

                        case GEM_BLUE:
                            if (gemBlueImg != null) {
                                g.drawImage(gemBlueImg, px, py, TILE_SIZE, TILE_SIZE, null);
                            }
                            break;

                        case DOOR:
                            g.setColor(new Color(120, 80, 40));
                            g.fillRect(px + TILE_SIZE / 8, py, TILE_SIZE * 3 / 4, TILE_SIZE);
                            g.setColor(Color.BLACK);
                            g.drawRect(px + TILE_SIZE / 8, py, TILE_SIZE * 3 / 4, TILE_SIZE);
                            break;

                        case EMPTY:
                        default:
                            break;
                    }

                    if (debugDrawHitbox && type != MapLoader.TileType.EMPTY) {
                        g.setColor(new Color(0, 0, 0, 60));
                        g.drawRect(px, py, TILE_SIZE, TILE_SIZE);
                    }
                }
            }

            // 플레이어
            int plx = (int) player.getLeft();
            int ply = (int) player.getTop() + MAP_OFFSET_Y;

            int plw = (int) (player.getWidth() * 0.90);
            int plh = (int) (player.getHeight() * 0.90);

            BufferedImage sprite = null;
            Player.Form form = player.getForm();
            boolean goingUp = player.getVelY() < 0.0;

            switch (form) {
                case BASIC:
                    sprite = goingUp ? basicJumpImg : basicStandImg;
                    break;
                case YELLOW:
                    sprite = goingUp ? yellowJumpImg : yellowStandImg;
                    break;
                case BLUE:
                    sprite = goingUp ? blueJumpImg : blueStandImg;
                    break;
            }

            if (sprite != null) {
                g.drawImage(sprite, plx, ply, plw, plh, null);
            } else {
                g.setColor(Color.BLACK);
                g.fillRect(plx, ply, plw, plh);
            }

            if (debugDrawHitbox) {
                g.setColor(Color.GREEN);
                g.drawRect(plx, ply, plw, plh);
            }
        }

        /** 코인(별) 살짝 떠 있는 애니메이션 */
        private void drawAnimatedCoin(Graphics2D g, int px, int py) {
            if (coinImg == null) {
                g.setColor(Color.YELLOW);
                g.fillOval(px + TILE_SIZE / 4, py + TILE_SIZE / 4,
                        TILE_SIZE / 2, TILE_SIZE / 2);
                return;
            }
            double offset = Math.sin(coinAnimTime * 4.0) * 3.0;
            int drawY = (int) (py + offset);
            g.drawImage(coinImg, px, drawY, TILE_SIZE, TILE_SIZE, null);
        }

        /** HUD */
        private void renderHUD(Graphics2D g) {
            g.setColor(Color.BLACK);
            g.setFont(g.getFont().deriveFont(Font.BOLD, 16f));

            String stageName = (stageInfos[currentStageIndex] != null)
                    ? stageInfos[currentStageIndex].getStageName()
                    : "Stage " + currentStageIndex;

            String starInfo = "Stars: " + collectedStars + " / " + totalStarsInStage;
            String deathInfo = "Deaths: " +
                    (stageInfos[currentStageIndex] != null ? stageInfos[currentStageIndex].getDeathCount() : 0);

            g.drawString(stageName, 10, 20);
            g.drawString(starInfo, 10, 40);
            g.drawString(deathInfo, 10, 60);
        }

        private void renderTutorialOverlay(Graphics2D g) {
            g.setColor(new Color(0, 0, 0, 140));
            g.fillRect(80, 60, WIDTH - 160, 210);

            g.setColor(Color.WHITE);
            g.setFont(g.getFont().deriveFont(Font.BOLD, 18f));

            int x = 100;
            int y = 90;
            int dy = 24;

            g.drawString("튜토리얼", x, y);
            y += dy;
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 14f));
            g.drawString("A / D : 좌우 이동", x, y); y += dy;
            g.drawString("Space : 보석 능력 사용 (노랑: 대시 / 파랑: 고점프)", x, y); y += dy;
            g.drawString("바닥에 닿으면 자동으로 튕겨 오릅니다.", x, y); y += dy;
            g.drawString("노란 코인을 모두 먹으면 다음 스테이지로 넘어갑니다.", x, y); y += dy;
            g.drawString("가시에 닿거나 떨어지면 스테이지 처음부터 다시 시작합니다.", x, y); y += dy;
            g.drawString("R 키 : 스테이지 재도전", x, y); y += dy;
            g.drawString("H 키 : 이 도움말 창 숨기기 / 다시 보기", x, y);
        }

        private void renderQuestionOverlay(Graphics2D g) {
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRect(60, 60, WIDTH - 120, HEIGHT - 120);

            int x = 90;
            int y = 110;

            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 22));

            String title = getStageName(currentStageIndex);
            g.drawString("퀴즈 - " + title, x, y);

            y += 45;
            g.setFont(new Font("SansSerif", Font.PLAIN, 16));

            String q = (currentQuiz != null) ? currentQuiz.text : "문제 로딩 중...";
            g.drawString(q, x, y);

            y += 40;
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            g.setColor(new Color(255, 255, 120));
            g.drawString("1번: O    2번: X", x, y);

            y += 28;
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.setColor(Color.LIGHT_GRAY);
            g.drawString("정답이면 다음 스테이지 / 오답이면 이 스테이지부터 다시 시작", x, y);
        }

        private void renderEndingOverlay(Graphics2D g) {
            boolean good = lastAnswerCorrect;
            String title = StageStory.getEndingTitle(currentStageIndex, good);
            String[] lines = StageStory.getEndingLines(currentStageIndex, good);

            g.setColor(new Color(0, 0, 0, 200));
            g.fillRect(50, 50, WIDTH - 100, HEIGHT - 100);

            int x = 80;
            int yCursor = 100;
            int lineHeight = 28;

            g.setFont(new Font("SansSerif", Font.BOLD, 30));
            g.setColor(good ? new Color(100, 200, 255) : new Color(255, 150, 150));
            g.drawString(title, x, yCursor);

            yCursor += 50;

            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.PLAIN, 16));

            for (String line : lines) {
                g.drawString(line, x, yCursor);
                yCursor += lineHeight;
            }

            StageInfo info = stageInfos[currentStageIndex];
            if (info != null) {
                int statsX = WIDTH - 300;
                int statsY = 100;
                int xValue = WIDTH - 120;
                int statsLineHeight = 22;

                g.setFont(new Font("SansSerif", Font.BOLD, 18));
                g.setColor(new Color(255, 255, 100));
                g.drawString("◆ 복구 성과 ◆", statsX, statsY);
                statsY += statsLineHeight + 10;

                g.setFont(new Font("SansSerif", Font.PLAIN, 16));
                g.setColor(Color.WHITE);

                String stars = info.getCollectedStars() + " / " + info.getTotalStars();
                g.drawString("시스템 모듈 수집:", statsX, statsY);
                g.drawString(stars, xValue - g.getFontMetrics().stringWidth(stars), statsY);
                statsY += statsLineHeight;

                String deaths = String.valueOf(info.getDeathCount());
                g.drawString("치명적 오류 발생:", statsX, statsY);
                g.drawString(deaths, xValue - g.getFontMetrics().stringWidth(deaths), statsY);
                statsY += statsLineHeight;

                String time = String.format("%.2f초", info.getClearTimeSeconds());
                g.drawString("복구 소요 시간:", statsX, statsY);
                g.drawString(time, xValue - g.getFontMetrics().stringWidth(time), statsY);
            }

            int finalYPosition = HEIGHT - 80;
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            g.setColor(new Color(100, 255, 100));
            g.drawString("Enter 키를 눌러 진행합니다.", x, finalYPosition);
        }

        private void renderSimpleMenu(Graphics2D g) {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, WIDTH, HEIGHT);

            g.setColor(Color.BLACK);
            g.setFont(g.getFont().deriveFont(Font.BOLD, 24f));
            String title = "Bounce Escape";
            int titleWidth = g.getFontMetrics().stringWidth(title);
            g.drawString(title, (WIDTH - titleWidth) / 2, 120);

            g.setFont(g.getFont().deriveFont(Font.PLAIN, 14f));
            String line1 = "Enter 키를 눌러 게임을 시작합니다.";
            int l1w = g.getFontMetrics().stringWidth(line1);
            g.drawString(line1, (WIDTH - l1w) / 2, 180);
        }

        private void renderDebugInfo(Graphics2D g) {
            g.setFont(new Font("Monospaced", Font.PLAIN, 12));
            g.setColor(new Color(0, 0, 0, 180));

            int x = WIDTH - 220;
            int y = 20;
            int dy = 16;

            g.drawString(String.format("FPS: %.1f", currentFps), x, y); y += dy;

            if (player != null) {
                g.drawString(String.format("Player: (%.1f, %.1f)", player.getX(), player.getY()), x, y); y += dy;
                g.drawString(String.format("VelY: %.1f", player.getVelY()), x, y); y += dy;
                g.drawString("OnGround: " + player.isOnGround(), x, y); y += dy;
                g.drawString("Form: " + player.getForm(), x, y); y += dy;
            }

            g.drawString("State: " + state, x, y); y += dy;
        }

        public void initPausePanel(JPanel mainContainer) {
            this.pausePanel = new PausePanel(this, mainContainer);
            this.pausePanel.setBounds(0, 0, WIDTH, HEIGHT);
            this.pausePanel.setVisible(false);
            add(this.pausePanel);
        }

        public void pauseGame() {
            if (state == GameState.STAGE_PLAY || state == GameState.TUTORIAL) {
                lastPlayState = state;
                state = GameState.PAUSE;
                pausePanel.setVisible(true);
                pausePanel.requestFocusInWindow();
                revalidate();
            }
        }

        public void resumeGame() {
            if (state == GameState.PAUSE) {
                state = lastPlayState;
                pausePanel.setVisible(false);
                requestFocusInWindow();
                revalidate();
            }
        }

        public void resetStateToTutorial() {
            this.state = GameState.TUTORIAL;
            if (this.pausePanel != null) {
                this.pausePanel.setVisible(false);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (backBuffer != null) {
                g.drawImage(backBuffer, 0, 0, null);
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {
            int code = e.getKeyCode();

            if (code == KeyEvent.VK_ESCAPE) {
                if (state == GameState.PAUSE) {
                    resumeGame();
                } else if (state == GameState.TUTORIAL || state == GameState.STAGE_PLAY) {
                    pauseGame();
                }
                return;
            }

            if (code == KeyEvent.VK_H) {
                tutorialOverlayVisible = !tutorialOverlayVisible;
                return;
            }

            if (state == GameState.ENDING) {
                if (code == KeyEvent.VK_ENTER) {
                    goToNextStageOrFinishGame();
                }
                return;
            }

            if (state == GameState.QUESTION) {
                if (code == KeyEvent.VK_1) {
                    boolean userPickO = true;
                    boolean correct = (currentQuiz != null) && (userPickO == currentQuiz.answerO);
                    onQuestionAnswered(correct);
                } else if (code == KeyEvent.VK_2) {
                    boolean userPickO = false;
                    boolean correct = (currentQuiz != null) && (userPickO == currentQuiz.answerO);
                    onQuestionAnswered(correct);
                }
                return;
            }

            if (code == KeyEvent.VK_F1) {
                debugDrawHitbox = !debugDrawHitbox;
                return;
            }
            if (code == KeyEvent.VK_F2) {
                debugShowInfo = !debugShowInfo;
                return;
            }
            if (code == KeyEvent.VK_R) {
                resetCurrentStage();
                return;
            }

            if (code == KeyEvent.VK_A) {
                leftPressed = true;
            } else if (code == KeyEvent.VK_D) {
                rightPressed = true;
            } else if (code == KeyEvent.VK_SPACE) {
                if (player != null &&
                        (state == GameState.TUTORIAL || state == GameState.STAGE_PLAY)) {
                    player.useAbility();
                }
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            int code = e.getKeyCode();

            if (code == KeyEvent.VK_A) {
                leftPressed = false;
            } else if (code == KeyEvent.VK_D) {
                rightPressed = false;
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }
    }

    /**
     * 플레이어 캐릭터
     * - 튕김, 대시, 고점프 물리 처리
     */
    private static class Player {

        enum Form {
            BASIC,   // 기본
            YELLOW,  // 노란 보석: 대시
            BLUE     // 파란 보석: 고점프
        }

        private double x;
        private double y;
        private double width;
        private double height;

        private double velX;
        private double velY;

        private boolean onGround;
        private boolean facingRight = true;

        private Form form = Form.BASIC;
        private boolean abilityReady = false;

        // 대시 상태
        private boolean dashActive = false;
        private double dashTimeRemaining = 0.0;
        private static final double DASH_DURATION = 0.25;

        // 물리 파라미터
        private static final double BOUNCE_SPEED    = 280.0;
        private static final double DASH_SPEED      = 550.0;
        private static final double BLUE_JUMP_SPEED = 450.0;

        // 벽 충돌만 줄이기 위한 패딩
        private static final double COL_PAD_X = 4.0;
        private static final double COL_PAD_Y = 3.0;

        public Player(double x, double y, double width, double height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public void applyHorizontalVelocity(double vx) {
            this.velX = vx;
            if (vx > 0) facingRight = true;
            else if (vx < 0) facingRight = false;
        }

        public void applyGravity(double gravity, double dt) {
            velY += gravity * dt;
        }

        public void useAbility() {
            if (!abilityReady) return;

            switch (form) {
                case YELLOW:
                    dashActive = true;
                    dashTimeRemaining = DASH_DURATION;
                    velX = (facingRight ? DASH_SPEED : -DASH_SPEED);
                    velY = -BLUE_JUMP_SPEED * 0.45;
                    break;

                case BLUE:
                    velY = -BLUE_JUMP_SPEED * 0.80;
                    break;

                case BASIC:
                default:
                    return;
            }

            form = Form.BASIC;
            abilityReady = false;
        }

        public void setFormYellow() {
            this.form = Form.YELLOW;
            this.abilityReady = true;
        }

        public void setFormBlue() {
            this.form = Form.BLUE;
            this.abilityReady = true;
        }

        public void moveAndCollide(MapLoader.MapData map, double dt, int tileSize) {
            double newX = x + velX * dt;
            double newY = y + velY * dt;

            x = moveAndCollideAxis(map, newX, y, tileSize, true);

            double resultY = moveAndCollideAxis(map, x, newY, tileSize, false);

            onGround = false;

            if (resultY != newY && velY > 0) {
                velY = -BOUNCE_SPEED;
                onGround = true;
            } else if (resultY != newY && velY < 0) {
                velY = 0;
            }

            y = resultY;

            if (dashActive) {
                dashTimeRemaining -= dt;
                if (dashTimeRemaining <= 0.0) {
                    dashActive = false;
                }
            }
        }

        private double moveAndCollideAxis(MapLoader.MapData map, double targetX, double targetY,
                                          int tileSize, boolean horizontal) {
            double resultX = x;
            double resultY = y;

            if (horizontal) resultX = targetX;
            else resultY = targetY;

            double left   = resultX + COL_PAD_X;
            double right  = resultX + width - COL_PAD_X;
            double top    = resultY + COL_PAD_Y;
            double bottom = resultY + height - COL_PAD_Y;

            int leftTile   = (int) Math.floor(left   / tileSize);
            int rightTile  = (int) Math.floor(right  / tileSize);
            int topTile    = (int) Math.floor(top    / tileSize);
            int bottomTile = (int) Math.floor(bottom / tileSize);

            leftTile   = Math.max(0, leftTile);
            rightTile  = Math.min(map.width  - 1, rightTile);
            topTile    = Math.max(0, topTile);
            bottomTile = Math.min(map.height - 1, bottomTile);

            for (int ty = topTile; ty <= bottomTile; ty++) {
                for (int tx = leftTile; tx <= rightTile; tx++) {
                    MapLoader.TileType type = map.tiles[ty][tx];
                    if (type != MapLoader.TileType.WALL) continue;

                    int tileLeft   = tx * tileSize;
                    int tileRight  = tileLeft + tileSize;
                    int tileTop    = ty * tileSize;
                    int tileBottom = tileTop + tileSize;

                    if (right <= tileLeft || left >= tileRight ||
                            bottom <= tileTop || top >= tileBottom) {
                        continue;
                    }

                    if (horizontal) {
                        if (velX > 0) {
                            resultX = tileLeft - 0.01 - (width - COL_PAD_X);
                        } else if (velX < 0) {
                            resultX = tileRight + 0.01 - COL_PAD_X;
                        }
                    } else {
                        if (velY > 0) {
                            resultY = tileTop - 0.01 - (height - COL_PAD_Y);
                        } else if (velY < 0) {
                            resultY = tileBottom + 0.01 - COL_PAD_Y;
                        }
                    }
                }
            }

            return horizontal ? resultX : resultY;
        }

        // ---- getter ----
        public double getX()      { return x; }
        public double getY()      { return y; }
        public double getWidth()  { return width; }
        public double getHeight() { return height; }

        public double getLeft()   { return x; }
        public double getTop()    { return y; }
        public double getRight()  { return x + width; }
        public double getBottom() { return y + height; }

        public double getVelY()        { return velY; }
        public boolean isOnGround()    { return onGround; }
        public Form getForm()          { return form; }
        public boolean isFacingRight() { return facingRight; }

        public boolean isDashing() {
            return dashActive;
        }
    }
}
