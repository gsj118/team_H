import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

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

        //  메인 메뉴 BGM 재생 요청 시작
        soundManager.playBGM(SoundManager.BGM_MAIN_MENU); 
        
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Bounce Escape (Core Prototype)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            // CardLayout을 사용할 메인 컨테이너
            JPanel mainContainer = new JPanel(new CardLayout());
            
            GamePanel gamePanel = new GamePanel();
            MenuPanel menuPanel = new MenuPanel(gamePanel, mainContainer); // GamePanel 참조 전달
            // UI: PausePanel 초기화 호출
            gamePanel.initPausePanel(mainContainer);
            
            mainContainer.add(menuPanel, CARD_MENU);
            mainContainer.add(gamePanel, CARD_GAME);
            
            
            frame.setContentPane(mainContainer);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            // panel.startGameThread(); 메뉴에서 호출
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
        private BufferedImage tileLavaImg;
        private BufferedImage coinImg;
        private BufferedImage gemYellowImg;
        private BufferedImage gemBlueImg;

        // 코인 애니메이션
        private double coinAnimTime = 0.0;
        
       // UI: 일시정지 패널 인스턴스
        private PausePanel pausePanel;
        
        // UI: 일시정지 전 상태 저장
        private GameState lastPlayState = GameState.TUTORIAL;

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

        /**
         * 이미지 리소스 로딩
         * - 실제 프로젝트에서는 image/ 폴더에 파일 배치 필요
         */
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
            } catch (IOException e) {
                // 이미지가 없어도 기본 사각형/원으로 대신 그림
                e.printStackTrace();
            }
        }
        
        public void startNewGame() {
            loadStage(0); // 튜토리얼 로드
            startGameThread(); // 게임 루프 시작
            state = GameState.TUTORIAL; // 초기 상태 설정
            
            // 게임 시작 시 BGM을 스테이지 음악으로 교체
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
         * - SPIKE: 즉시 사망
         * - STAR: 별 카운트 증가 → 모두 먹으면 QUESTION 상태로 진입
         * - GEM_YELLOW / GEM_BLUE: 능력 부여
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
                        case STAR:
                            currentMap.tiles[ty][tx] = MapLoader.TileType.EMPTY;
                            collectedStars++;
                            if (stageInfos[currentStageIndex] != null) {
                                stageInfos[currentStageIndex].incrementCollectedStars();
                            }
                            if (collectedStars >= totalStarsInStage && !waitingForQuestionAnswer) {
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

                player = new Player(map.playerStartX, map.playerStartY,
                        TILE_SIZE * 0.7, TILE_SIZE * 0.9);

                if (stageInfos[currentStageIndex] == null) {
                    stageInfos[currentStageIndex] =
                            new StageInfo(currentStageIndex, getStageName(currentStageIndex), totalStarsInStage);
                }
                stageInfos[currentStageIndex].startStage();
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

        /**
         * 스테이지 로드
         * - stageIndex == 0 : 실제 플레이
         * - stageIndex 1~3 : 스토리(자동 엔딩) 전용
         */
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

            if (stageInfos[stageIndex] == null) {
                stageInfos[stageIndex] =
                        new StageInfo(stageIndex, getStageName(stageIndex), totalStarsInStage);
            } else {
                stageInfos[stageIndex].resetForRetry(totalStarsInStage);
            }
            stageInfos[stageIndex].startStage();

            player = new Player(map.playerStartX, map.playerStartY,
                    TILE_SIZE * 0.7, TILE_SIZE * 0.9);

            // 튜토리얼 이후 Stage1~3은 스토리 전용 자동 클리어
            if (stageIndex == 1 || stageIndex == 2 || stageIndex == 3) {
                waitingForQuestionAnswer = false;
                questionAnsweredThisStage = true;
                lastAnswerCorrect = true;   // 스토리상 정상 진행으로 처리

                StageInfo info = stageInfos[stageIndex];
                if (info != null) {
                    info.setQuestionAnswered(true);
                    info.finishStageNow();
                }

                state = GameState.ENDING;   // 바로 엔딩 화면
                return;
            }

            // 튜토리얼만 실제 플레이 상태로 진입
            state = GameState.TUTORIAL;
        }

        /** 별을 모두 먹었을 때 호출 */
        private void enterQuestionState() {
            waitingForQuestionAnswer = true;
            questionAnsweredThisStage = false;
            state = GameState.QUESTION;
        }

        /**
         * UI 담당에서 문제를 풀고 나서 호출할 수 있는 메서드
         * - 정답/오답에 따라 엔딩 분기
         */
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

        /** 엔딩에서 Enter 누르면 다음 스테이지 or 다시 튜토리얼 */
        private void goToNextStageOrFinishGame() {
            if (currentStageIndex + 1 < MAX_STAGE_COUNT) {
                loadStage(currentStageIndex + 1);
            } else {
                loadStage(0);
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
            int plw = (int) player.getWidth();
            int plh = (int) player.getHeight();

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

        /** HUD: 스테이지 이름, 별/사망 횟수 표시 */
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

        /** 튜토리얼 도움말 오버레이 (H로 토글) */
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
            g.drawString("노란 코인을 모두 먹으면 문제 단계로 넘어갑니다.", x, y); y += dy;
            g.drawString("가시에 닿거나 떨어지면 스테이지 처음부터 다시 시작합니다.", x, y); y += dy;
            g.drawString("R 키 : 스테이지 재도전", x, y); y += dy;
            g.drawString("H 키 : 이 도움말 창 숨기기 / 다시 보기", x, y);
        }

        /** 문제 단계 오버레이 (지금은 테스트 전용) */
        private void renderQuestionOverlay(Graphics2D g) {
            g.setColor(new Color(0, 0, 0, 170));
            g.fillRect(60, 60, WIDTH - 120, HEIGHT - 120);

            g.setColor(Color.WHITE);
            g.setFont(g.getFont().deriveFont(Font.BOLD, 20f));
            int x = 80;
            int y = 100;
            int dy = 26;

            g.drawString("문제 단계", x, y);
            y += dy;
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 14f));
            g.drawString("실제 프로젝트에서는 UI 담당이 이 화면을 구현합니다.", x, y); y += dy;
            g.drawString("여기서는 테스트를 위해 키보드로 정답/오답을 입력합니다.", x, y); y += dy;
            g.drawString("1 키 : 정답으로 처리", x, y); y += dy;
            g.drawString("2 키 : 오답으로 처리", x, y); y += dy;
        }

        /** 엔딩 화면: StageStory의 텍스트를 사용 */
        private void renderEndingOverlay(Graphics2D g) {
            // StageStory 클래스의 데이터 사용
            boolean good = lastAnswerCorrect; 
            String title = StageStory.getEndingTitle(currentStageIndex, good);
            String[] lines = StageStory.getEndingLines(currentStageIndex, good);
            
            // 1. 반투명 박스 배경
            g.setColor(new Color(0, 0, 0, 200));
            g.fillRect(50, 50, WIDTH - 100, HEIGHT - 100);

            int x = 80;
            int yCursor = 100;
            int lineHeight = 28;

            // 2. 제목
            g.setFont(new Font("SansSerif", Font.BOLD, 30));
            g.setColor(good ? new Color(100, 200, 255) : new Color(255, 150, 150)); 
            g.drawString(title, x, yCursor);
            
            yCursor += 50; // 제목과 스토리 간격

            // 3. 스토리 텍스트 출력
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.PLAIN, 16));
            
            for (String line : lines) {
                g.drawString(line, x, yCursor);
                yCursor += lineHeight;
            }

            // 4. 복구 성과 (우상단으로 이동 및 고정 좌표 사용)
            StageInfo info = stageInfos[currentStageIndex];
            if (info != null) {
                // ⭐️ 성과 섹션 시작 고정 좌표
                int statsX = WIDTH - 300;
                int statsY = 100;
                
                int xValue = WIDTH - 120;
                int statsLineHeight = 22;
                
                // 성과 제목
                g.setFont(new Font("SansSerif", Font.BOLD, 18));
                g.setColor(new Color(255, 255, 100)); 
                g.drawString("◆ 복구 성과 ◆", statsX, statsY);
                statsY += statsLineHeight + 10; // 제목 아래 간격

                g.setFont(new Font("SansSerif", Font.PLAIN, 16));
                g.setColor(Color.WHITE);

                // 별 수집
                String stars = info.getCollectedStars() + " / " + info.getTotalStars();
                g.drawString("시스템 모듈 수집:", statsX, statsY);
                g.drawString(stars, xValue - g.getFontMetrics().stringWidth(stars), statsY);
                statsY += statsLineHeight;
                
                // 사망 횟수
                String deaths = String.valueOf(info.getDeathCount());
                g.drawString("치명적 오류 발생:", statsX, statsY);
                g.drawString(deaths, xValue - g.getFontMetrics().stringWidth(deaths), statsY);
                statsY += statsLineHeight;
                
                // 클리어 시간
                String time = String.format("%.2f초", info.getClearTimeSeconds());
                g.drawString("복구 소요 시간:", statsX, statsY);
                g.drawString(time, xValue - g.getFontMetrics().stringWidth(time), statsY);
            }

            // 5. 다음 진행 안내 (하단 고정)
            int finalYPosition = HEIGHT - 80;
            
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            g.setColor(new Color(100, 255, 100)); 
            g.drawString("Enter 키를 눌러 다음 시스템에 접속합니다.", x, finalYPosition);
        }

        private void renderPauseOverlay(Graphics2D g) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            g.setColor(Color.WHITE);
            g.setFont(g.getFont().deriveFont(Font.BOLD, 24f));
            String text = "PAUSED";
            int strW = g.getFontMetrics().stringWidth(text);
            g.drawString(text, (WIDTH - strW) / 2, HEIGHT / 2);
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
        
        // UI: PausePanel 초기화 및 GamePanel에 연결 (main()에서 호출 필요)
        public void initPausePanel(JPanel mainContainer) {
            this.pausePanel = new PausePanel(this, mainContainer);
            // PausePanel의 위치와 크기를 GamePanel과 동일하게 설정
            this.pausePanel.setBounds(0, 0, WIDTH, HEIGHT); 
            
            // 초기에는 숨김 상태로 추가
            this.pausePanel.setVisible(false);
            add(this.pausePanel);
        }

        // UI: 일시정지 상태 전환 (ESC 키에서 호출)
        public void pauseGame() {
            if (state == GameState.STAGE_PLAY || state == GameState.TUTORIAL) {
                lastPlayState = state; 
                state = GameState.PAUSE;
                pausePanel.setVisible(true); 
                pausePanel.requestFocusInWindow(); // 포커스를 줘야 버튼 조작 가능
                revalidate(); 
            }
        }

        // UI: 게임 재개 (PausePanel 버튼/ESC 키에서 호출)
        public void resumeGame() {
            if (state == GameState.PAUSE) {
                state = lastPlayState; 
                pausePanel.setVisible(false); 
                requestFocusInWindow(); // GamePanel에 포커스 반환
                revalidate();
            }
        }
        
        // MenuPanel에서 게임 시작시 상태 초기화 (Pause - Menu 돌아온 후 게임 시작시 PAUSE 화면이 나오는 오류 때문에)
        public void resetStateToTutorial() {
            // GameState를 TUTORIAL로 강제 변경
            this.state = GameState.TUTORIAL; 
            
            // PausePanel이 떠 있는 상태라면 숨김 처리 (안전 장치)
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

        // ----------------- KeyListener -----------------

        @Override
        public void keyPressed(KeyEvent e) {
            int code = e.getKeyCode();

            if (code == KeyEvent.VK_ESCAPE) {
            	if (state == GameState.PAUSE) {
                    resumeGame(); // 새로 만든 메서드 호출
                } else if (state == GameState.TUTORIAL || state == GameState.STAGE_PLAY) {
                    pauseGame(); // 새로 만든 메서드 호출
                }
                return;
            }

            // 튜토리얼 도움말 토글
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
                // 테스트용: UI 담당은 여기 대신 onQuestionAnswered(...)를 직접 호출하면 됨
                if (code == KeyEvent.VK_1) {
                    onQuestionAnswered(true);
                } else if (code == KeyEvent.VK_2) {
                    onQuestionAnswered(false);
                }
                return;
            }

            // 디버그
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

            // 조작키
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
        private double  dashTimeRemaining = 0.0;
        private static final double DASH_DURATION = 0.25;

        // 물리 파라미터
        private static final double BOUNCE_SPEED    = 250.0;
        private static final double DASH_SPEED      = 550.0;
        private static final double BLUE_JUMP_SPEED = 550.0;

        public Player(double x, double y, double width, double height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public void applyHorizontalVelocity(double vx) {
            this.velX = vx;
            if (vx > 0)      facingRight = true;
            else if (vx < 0) facingRight = false;
        }

        public void applyGravity(double gravity, double dt) {
            velY += gravity * dt;
        }

        /**
         * Space 눌렀을 때 능력 사용
         * - YELLOW: 진행 방향으로 대시 + 약간 점프
         * - BLUE  : 위로 강한 점프
         */
        public void useAbility() {
            if (!abilityReady) return;

            switch (form) {
                case YELLOW:
                    dashActive = true;
                    dashTimeRemaining = DASH_DURATION;
                    velX = (facingRight ? DASH_SPEED : -DASH_SPEED);
                    velY = -BLUE_JUMP_SPEED * 0.6;
                    break;

                case BLUE:
                    velY = -BLUE_JUMP_SPEED;
                    break;

                case BASIC:
                default:
                    return;
            }

            // 능력 사용 후 다시 BASIC으로 복귀
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

        /**
         * 타일 기반 충돌 처리
         * - 수평/수직을 나누어 처리
         * - 바닥 충돌 시 자동 튕김
         */
        public void moveAndCollide(MapLoader.MapData map, double dt, int tileSize) {
            double newX = x + velX * dt;
            double newY = y + velY * dt;

            x = moveAndCollideAxis(map, newX, y, tileSize, true);

            double resultY = moveAndCollideAxis(map, x, newY, tileSize, false);

            onGround = false;

            if (resultY != newY && velY > 0) {
                // 바닥에 닿은 경우: 튕겨 오름
                velY = -BOUNCE_SPEED;
                onGround = true;
            } else if (resultY != newY && velY < 0) {
                // 천장에 부딪힌 경우: 위쪽 속도 제거
                velY = 0;
            }

            y = resultY;

            // 대시 시간 감소
            if (dashActive) {
                dashTimeRemaining -= dt;
                if (dashTimeRemaining <= 0.0) {
                    dashActive = false;
                }
            }
        }

        /**
         * 한 축 방향으로만 이동/충돌 처리
         */
        private double moveAndCollideAxis(MapLoader.MapData map, double targetX, double targetY,
                                          int tileSize, boolean horizontal) {
            double resultX = x;
            double resultY = y;

            if (horizontal) resultX = targetX;
            else           resultY = targetY;

            double left   = resultX;
            double right  = resultX + width;
            double top    = resultY;
            double bottom = resultY + height;

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

                    if (right  <= tileLeft || left >= tileRight ||
                        bottom <= tileTop  || top  >= tileBottom) {
                        continue;
                    }

                    if (horizontal) {
                        if (velX > 0) {
                            resultX = tileLeft - width - 0.01;
                        } else if (velX < 0) {
                            resultX = tileRight + 0.01;
                        }
                        left  = resultX;
                        right = resultX + width;
                    } else {
                        if (velY > 0) {
                            resultY = tileTop - height - 0.01;
                        } else if (velY < 0) {
                            resultY = tileBottom + 0.01;
                        }
                        top    = resultY;
                        bottom = resultY + height;
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

        public double  getVelY()       { return velY; }
        public boolean isOnGround()    { return onGround; }
        public Form    getForm()       { return form; }
        public boolean isFacingRight() { return facingRight; }

        public boolean isDashing() {
            return dashActive;
        }
    }
}