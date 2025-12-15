import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

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

        //  메인 메뉴 BGM
        soundManager.playBGM(SoundManager.BGM_MAIN_MENU); 
        
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Bounce Escape (Core Prototype)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            // CardLayout을 사용할 메인 컨테이너
            JPanel mainContainer = new JPanel(new CardLayout());
            
            GamePanel gamePanel = new GamePanel(mainContainer);
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
        INTRO,
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
        private GameState state = GameState.INTRO;
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
        private boolean[] stageSuccessStatus = new boolean[4]; // 스테이지 문제 정답 여부
        private boolean finalResultGood = false;
        
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
        private BufferedImage tileLavaImg;
        private BufferedImage coinImg;
        private BufferedImage gemYellowImg;
        private BufferedImage gemBlueImg;
        
        // 배경 이미지
        private BufferedImage introBGImg; // 인트로
        private BufferedImage goodEndingBG; // 굿 엔딩
        private BufferedImage badEndingBG;  // 배드 엔딩

        // 코인 애니메이션
        private double coinAnimTime = 0.0;
        
       // UI: 일시정지 패널 인스턴스
        private PausePanel pausePanel;
        
        // UI: 일시정지 전 상태 저장
        private GameState lastPlayState = GameState.TUTORIAL;
        
        // 인트로 객체
        private IntroManager introManager = new IntroManager();
        
        private double phase2TypingTimer = 0.0;
        private int phase2CurrentCharIndex = 0;
        private final double phase2TextDisplaySpeed = 0.1;
        
        private final JPanel mainContainer;
        
        private String finalEndingPhase2Text;
        
        public GamePanel(JPanel mainContainer) {
        	this.mainContainer = mainContainer;
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setFocusable(true);
            requestFocusInWindow();
            addKeyListener(this);

            initBackBuffer();
            loadImages();
            
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
                
                introBGImg     = ImageIO.read(new File("image/Intro.png"));
                goodEndingBG   = ImageIO.read(new File("image/Ending_A.png"));
                badEndingBG    = ImageIO.read(new File("image/Ending_B.png"));
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        public void startNewGame() {
        	state = GameState.INTRO;
        	introManager = new IntroManager();
            
        	startGameThread();
            // 게임 시작 시 BGM을 스테이지 음악으로 교체
            GameCore.getSoundManager().playBGM(SoundManager.BGM_STAGE);
            
            SwingUtilities.invokeLater(() -> requestFocusInWindow());
            
            state = GameState.INTRO;
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
            	case INTRO:
            		introManager.update(dt);
            		break;
                case TUTORIAL:
                case STAGE_PLAY:
                    updateStagePlay(dt);
                    break;
                case QUESTION:
                	break;
                case ENDING:
                	updateEndingTyping(dt); // 엔딩 타이핑 애니메이션
                	break;
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
        	// ※ 여긴 그대로 둠(가시/아이템 판정은 기존 크기 유지)
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
                            
                            // 별 획득 효과음 재생 추가
                            GameCore.getSoundManager().playSFX(SoundManager.SFX_STAR_COLLECT);
                            // 별을 다 먹었을 때 처리
                            if (collectedStars >= totalStarsInStage && !waitingForQuestionAnswer) {

                                /* 튜토리얼(0)은 문제 없이 바로 다음 스테이지
                                   [UI] 튜토리얼 클리어 화면 때문에 일부 수정*/
                                if (currentStageIndex == 0) {
                                	lastAnswerCorrect = true; // [UI] 튜토리얼은 성공으로 간주
                                    StageInfo info = stageInfos[currentStageIndex];
                                    if (info != null) {
                                    	info.setQuestionAnswered(true); // [UI] 화면 표시를 위해 정답 처리
                                    	info.finishStageNow();
                                    	}
                                    state = GameState.ENDING; // ENDING 상태로 전환
                                    return; // 처리 종료
                                }

                                // Stage 1~3은 문제로 진입
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
                currentQuiz = null; // (추가) 재시작하면 퀴즈도 초기화

                player = new Player(map.playerStartX, map.playerStartY,
                        TILE_SIZE * 0.7, TILE_SIZE * 0.9);

                if (stageInfos[currentStageIndex] == null) {
                    stageInfos[currentStageIndex] =
                            new StageInfo(currentStageIndex, getStageName(currentStageIndex), totalStarsInStage);
                }
                stageInfos[currentStageIndex].startStage();
                
                // 여기서 상태도 다시 맞춰줌(재시작 깔끔하게)
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

        /**
         * 스테이지 로드
         * - 튜토리얼(0)은 TUTORIAL 상태
         * - 1~3은 STAGE_PLAY 상태(이제 자동 엔딩 제거)
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
            
            if (stageIndex == 0) {
                stageSuccessStatus[0] = true; 
                // 튜토리얼은 퀴즈가 없으므로 정답 상태를 미리 설정
            }

            // 상태 설정
            state = (stageIndex == 0) ? GameState.TUTORIAL : GameState.STAGE_PLAY;
        }

        /** 별을 모두 먹었을 때 호출 (Stage1~3) */
        private void enterQuestionState() {
            waitingForQuestionAnswer = true;
            questionAnsweredThisStage = false;

            // (추가) 스테이지에 맞는 문제 하나 가져오기 (2개 중 랜덤, 연속중복 방지)
            currentQuiz = QuestionBox.pick(currentStageIndex);

            state = GameState.QUESTION;
        }

        /**
         * 문제 풀이 결과 처리
         * 정답 오답 관계없이 스테이지는 진행
         */
        public void onQuestionAnswered(boolean correct) {
            if (!waitingForQuestionAnswer || questionAnsweredThisStage) return;

            questionAnsweredThisStage = true;
            waitingForQuestionAnswer = false;
            
            // 정답 여부를 전역 배열에 기록
            // currentStageIndex는 현재 퀴즈를 푼 스테이지 인덱스
            stageSuccessStatus[currentStageIndex] = correct; 

            lastAnswerCorrect = correct; // 수정 전 기준필드인데 오류나서 일단 유지함

            StageInfo info = stageInfos[currentStageIndex];
            if (info != null) {
                info.setQuestionAnswered(correct);
                info.finishStageNow();
                
                // [UI] 엔딩화면 타이핑 인덱스 초기화
                info.resetTypingState();
            }
            
            if (currentStageIndex == 3) {
                int successCount = 0;
                // 배열 전체를 순회하며 성공 횟수를 계산
                for (boolean success : stageSuccessStatus) {
                    if (success) {
                        successCount++;
                    }
                }
                // Stage 0, 1, 2, 3 모두 성공했을 때만 finalResultGood = true
                this.finalResultGood = (successCount == stageSuccessStatus.length);
                
                if (this.finalResultGood) {
                    // good 엔딩 2차
                    info.setFullText(info.getFullText() + "\n\nPress ENTER to continue..."); 
                    finalEndingPhase2Text = "당신은 동료들과 함께 우주선을 타고 이 행성을 떠납니다."
                    		+ "\n동료들의 웃음소리에 당신의 입가에도 미소가 번집니다.";
                } else {
                    // bad 엔딩 2차
                    info.setFullText(info.getFullText() + "\n\nPress ENTER to continue..."); 
                    finalEndingPhase2Text = "당신은 거대한 컴퓨터 구조가 된 이 행성 위에 홀로 남아 "
                    		+ "\n언젠가 또 올 탈출 시도를 위해 시스템을 지켜보기로 합니다."; // 2단계 텍스트 저장
                }
            }

            // 정답 여부와 관계없이 무조건 ENDING 상태로 전환
            state = GameState.ENDING;
            
        }

        /** 엔딩에서 Enter 누르면 다음 */
        private void goToNextStageOrFinishGame() {
            
            // Stage 0, 1, 2 클리어 > 다음 스테이지
            if (currentStageIndex + 1 < MAX_STAGE_COUNT) {
                loadStage(currentStageIndex + 1); 
                return;
            } 
            
            // Stage 3 클리어 > 최종 엔딩
            StageInfo info = stageInfos[currentStageIndex]; 
            
            // 1차 Enter: 타이핑 강제 완료
            if (!info.isTypingFinished()) {
                info.finishTypingAndCheckEnd(); 
                return;
            }
            stopGameThread();
            
            // 메인 화면으로
            CardLayout cl = (CardLayout) mainContainer.getLayout();
            cl.show(mainContainer, GameCore.CARD_MENU); 
            
            GameCore.getSoundManager().playBGM(SoundManager.BGM_MAIN_MENU);
            resetStateToTutorial();
        }

        /** 백버퍼에 렌더 */
        private void render() {
            if (backG == null) return;

            backG.setColor(Color.WHITE);
            backG.fillRect(0, 0, WIDTH, HEIGHT);

            switch (state) {
            	case INTRO:
            		renderStoryOverlay(backG);
            		break;
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
                	// Stage 3인지 확인하여 최종 엔딩 분기 처리
                    if (currentStageIndex == 3) { 
                        renderFinalEnding(backG); // 최종 엔딩
                    } else {
                        renderEndingOverlay(backG); // 스테이지 클리어시
                    }
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

        /** HUD: 스테이지 이름, 별/사망 횟수 표시 + 타이머*/
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
            
            // [추가] 실시간 타이머 표시 로직 추가
            if (stageInfos[currentStageIndex] != null) {
                
                StageInfo currentInfo = stageInfos[currentStageIndex];
                
                long currentElapsedTime = currentInfo.getElapsedPlayTimeMillis();
                
                if (currentElapsedTime < 0) {
                    currentElapsedTime = 0;
                }
                
                g.setColor(Color.BLACK); 
                g.setFont(g.getFont().deriveFont(Font.BOLD, 24f));

                String timeString = formatTime(currentElapsedTime);
                
                int xPos = WIDTH - g.getFontMetrics().stringWidth(timeString) - 20; 
                int yPos = 40; 
                
                g.drawString(timeString, xPos, yPos);
            }
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

        /** (수정) 문제 단계 오버레이: O/X 퀴즈 */
        private void renderQuestionOverlay(Graphics2D g) {
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRect(60, 60, WIDTH - 120, HEIGHT - 120);

            int x = 90;
            int y = 110;

            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 22));

            // (추가) 타이틀은 스테이지 이름 그대로 사용
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
        }
        
        /**
         * 인트로 상태에서 스토리 텍스트를 그립니다.
         */
        private void renderStoryOverlay(Graphics2D g) {
            
            // 배경 이미지/전체 배경 렌더링
            if (introBGImg != null) {
                g.drawImage(introBGImg, 0, 0, WIDTH, HEIGHT, null);
            } else {
                g.setColor(new Color(20, 20, 40)); 
                g.fillRect(0, 0, WIDTH, HEIGHT);
            }
            
            IntroManager manager = this.introManager;
            
            // 대화 박스
            final int BOX_HEIGHT = 150; 
            final int BOX_PADDING = 20; 
            final int boxY = HEIGHT - BOX_HEIGHT; 
            
            g.setColor(new Color(20, 20, 40, 204)); 
            g.fillRect(0, boxY, WIDTH, BOX_HEIGHT); 
            
            // 화자 이름 박스 렌더링
            String speaker = manager.getCurrentSpeaker(); // 화자 이름 가져오기
            
            if (speaker != null) {
                final int SPEAKER_BOX_HEIGHT = 40;
                final int SPEAKER_BOX_WIDTH = 200; // 이름 상자 너비
                final int speakerX = BOX_PADDING;
                final int speakerY = boxY - SPEAKER_BOX_HEIGHT - 5; // 대화 박스 위 5px 간격
                
                // 이름 박스 배경
                g.setColor(new Color(40, 40, 60, 220)); 
                g.fillRect(speakerX, speakerY, SPEAKER_BOX_WIDTH, SPEAKER_BOX_HEIGHT);
                
                // 이름 텍스트
                g.setColor(new Color(200, 200, 255));
                g.setFont(g.getFont().deriveFont(Font.BOLD, 20f));
                g.drawString(speaker, speakerX + 15, speakerY + 28); // 박스 내부에 이름 출력
            }
            // 대화 내용 렌더링
            
            g.setColor(Color.WHITE);
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 20f));
            
            String line = manager.getCurrentDialogue(); 
            
            int end = Math.min(manager.getCurrentCharIndex(), line.length());
            String displayLine = line.substring(0, end); 
            
            int xPos = BOX_PADDING; 
            int yPos = boxY + BOX_PADDING + 25; 
            g.drawString(displayLine, xPos, yPos);
            
            // 안내 메시지 렌더링
            if (manager.isStoryEnd() || manager.isTypingComplete()) { 
                 g.setColor(Color.YELLOW);
                 g.setFont(g.getFont().deriveFont(Font.BOLD, 24f));
                 String msg = manager.isStoryEnd() ? "Press ENTER to start the Tutorial!" : "Enter 키를 눌러 진행";
                 
                 int msgWidth = g.getFontMetrics().stringWidth(msg);
                 int msgX = WIDTH - msgWidth - BOX_PADDING; 
                 int msgY = boxY + BOX_HEIGHT - BOX_PADDING; 

                 g.drawString(msg, msgX, msgY);
            }
        }
        
        /** 엔딩 화면: StageStory의 텍스트를 사용 */
        private void renderEndingOverlay(Graphics2D g) {
            
            StageInfo info = stageInfos[currentStageIndex]; 
            
            boolean good = lastAnswerCorrect; 
            String title = StageStory.getEndingTitle(currentStageIndex, good);
            // StageStory에서 모든 라인 가져오기
            String[] allLines = StageStory.getEndingLines(currentStageIndex, good);
            
            // 반투명 박스 배경
            g.setColor(new Color(0, 0, 0, 200));
            g.fillRect(50, 50, WIDTH - 100, HEIGHT - 100);

            int x = 80;
            int yCursor = 100;
            int lineHeight = 28;

            // 제목
            g.setFont(new Font("SansSerif", Font.BOLD, 30));
            g.setColor(good ? new Color(100, 200, 255) : new Color(255, 150, 150)); 
            g.drawString(title, x, yCursor);
            
            yCursor += 50;

            // 스토리 텍스트 출력
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.PLAIN, 16));
            
            for (int i = 0; i < allLines.length; i++) {
                String line = allLines[i];
                
                if (i < info.getCurrentLineIndex()) {
                    g.drawString(line, x, yCursor);
                } else if (i == info.getCurrentLineIndex()) {
                    int end = Math.min(info.getCurrentCharIndex(), line.length());
                    String partialLine = line.substring(0, end);
                    g.drawString(partialLine, x, yCursor);
                }
                
                yCursor += lineHeight;
            }

            // 복구 성과
            if (info != null) {
                // 성과 섹션 시작 고정 좌표
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

            // 다음 진행 안내
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
        
        /** 최종 엔딩 화면: Stage3 클리어 후 호출 */
        private void renderFinalEnding(Graphics2D g) {
        	boolean overallGoodEnding = this.finalResultGood;
        	StageInfo info = stageInfos[currentStageIndex];
        	
        	if (info.isTypingFinished()) {
                
                // 2단계 엔딩 화면
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, WIDTH, HEIGHT);
                

                g.setColor(Color.WHITE);
                g.setFont(new Font("SansSerif", Font.PLAIN, 18)); // 텍스트 크기를 작게 조정
                
                String[] lines = finalEndingPhase2Text.split("\n");
                int lineHeight = g.getFontMetrics().getHeight();
                int startY = HEIGHT / 2 - (lines.length * lineHeight) / 2;
                
                int totalCharsInPhase2 = finalEndingPhase2Text.length();
                boolean isTypingFinished = (phase2CurrentCharIndex >= totalCharsInPhase2); 
                int charsRenderedBeforeCurrentLine = 0;
                
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];
                    int lineLength = line.length();
                    
                    int charsToDrawOnThisLine = 0;
                    
                    if (phase2CurrentCharIndex > charsRenderedBeforeCurrentLine) {
                        int availableChars = phase2CurrentCharIndex - charsRenderedBeforeCurrentLine;
                        charsToDrawOnThisLine = Math.min(lineLength, availableChars);
                    }
                    
                    if (charsToDrawOnThisLine > 0) {
                        // ⭐️ 핵심: 부분 문자열(partialLine)만 그립니다. ⭐️
                        String partialLine = line.substring(0, charsToDrawOnThisLine);
                        
                        int x = (WIDTH - g.getFontMetrics().stringWidth(line)) / 2;
                        g.drawString(partialLine, x, startY + i * lineHeight);
                    }
                    
                    // 다음 줄로 넘어가기 전 카운터 업데이트
                    charsRenderedBeforeCurrentLine += lineLength + 1;
                }
                
                g.setFont(new Font("SansSerif", Font.BOLD, 14));
                g.setColor(new Color(100, 255, 100));
                String prompt = "Enter 키를 눌러 메인 화면으로";
                int promptX = (WIDTH - g.getFontMetrics().stringWidth(prompt)) / 2;
                if (isTypingFinished) {
                    g.drawString(prompt, promptX, HEIGHT - 50);
                }

                return;
            }
            String[] allLines = StageStory.getEndingLines(currentStageIndex, overallGoodEnding); 
            
            BufferedImage bgImage = null;

            if (overallGoodEnding) {
                bgImage = this.goodEndingBG; // 굿 엔딩 이미지
            } else {
                bgImage = this.badEndingBG;  // 배드 엔딩 이미지
            }

            if (bgImage != null) {
                g.drawImage(bgImage, 0, 0, WIDTH, HEIGHT, null);
                
                g.setColor(new Color(0, 0, 0, 150));
                g.fillRect(0, 0, WIDTH, HEIGHT);
            } else {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, WIDTH, HEIGHT);
            }
            
            // ----------------------------------------------------------------------
            // 최종 엔딩 타이틀
            g.setFont(new Font("SansSerif", Font.BOLD, 60));
            
            g.setColor(overallGoodEnding ? new Color(150, 220, 255) : new Color(255, 150, 150)); 
            
            String title = StageStory.getEndingTitle(currentStageIndex, overallGoodEnding);
            int titleX = (WIDTH - g.getFontMetrics().stringWidth(title)) / 2;
            g.drawString(title, titleX, 70);
            
            // 스토리 텍스트
            int x = 100;
            int yCursor = 200; 
            int lineHeight = 35;
            
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.PLAIN, 24));
            
            for (int i = 0; i < allLines.length; i++) {
                String line = allLines[i];
                
                if (i < info.getCurrentLineIndex()) {
                    g.drawString(line, x, yCursor);
                } else if (i == info.getCurrentLineIndex()) {
                    int end = Math.min(info.getCurrentCharIndex(), line.length());
                    String partialLine = line.substring(0, end);
                    g.drawString(partialLine, x, yCursor);
                }
                
                yCursor += lineHeight;
            }
            
            // ----------------------------------------------------------------------
            // 게임 종료 안내
            int finalYPosition = HEIGHT - 50;

            g.setFont(new Font("SansSerif", Font.BOLD, 22));
            g.setColor(new Color(100, 255, 100)); 
            String finalMsg = "Enter";

            int margin = 50;
            int msgWidth = g.getFontMetrics().stringWidth(finalMsg);
            int msgX = WIDTH - margin - msgWidth; 

            g.drawString(finalMsg, msgX, finalYPosition);
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
                
                // 타이머 정지 로직
                if (stageInfos[currentStageIndex] != null) {
                    stageInfos[currentStageIndex].pauseTimer();
                }
            }
        }

        // UI: 게임 재개 (PausePanel 버튼/ESC 키에서 호출)
        public void resumeGame() {
            if (state == GameState.PAUSE) {
                state = lastPlayState; 
                pausePanel.setVisible(false); 
                requestFocusInWindow(); // GamePanel에 포커스 반환
                revalidate();
                
                // 타이머 재개 로직
                if (stageInfos[currentStageIndex] != null) {
                    stageInfos[currentStageIndex].resumeTimer();
                }
            }
        }
        
        // MenuPanel에서 게임 시작시 상태 초기화 (Pause - Menu 돌아온 후 게임 시작시 PAUSE 화면이 나오는 오류 때문에)
        public void resetStateToTutorial() {
            this.state = GameState.TUTORIAL; 
            
            if (this.pausePanel != null) {
                this.pausePanel.setVisible(false);
            }
            // 2차 엔딩 타이핑 상태 초기화
            this.phase2CurrentCharIndex = 0;
            this.phase2TypingTimer = 0.0;
            
            // 모든 StageInfo 객체의 엔딩 플래그 초기화 (resetForRetry 사용)
            if (this.stageInfos != null) {
                for (int i = 0; i < this.stageInfos.length; i++) {
                    if (this.stageInfos[i] != null) {
                        // resetForRetry 호출
                        this.stageInfos[i].resetForRetry(this.stageInfos[i].getTotalStars()); 
                    }
                }
            }
        }
        
        public void stopGameThread() {
            running = false; // 게임 루프를 멈추는 플래그를 비활성화
            gameThread = null;
        }
        
        // 타이머 표시 방식
        private String formatTime(long millis) {
            double seconds = (double) millis / 1000.0;
            return String.format("%.2f초", seconds);
        }
        
        // 타이핑 업데이트 메서드
        private void updateEndingTyping(double dt) {
            StageInfo info = stageInfos[currentStageIndex];
            if (info == null) return;
            
            // 1차 엔딩 타이핑 로직
            if (!info.isTypingFinished()) { 
                
                // 타이머 업데이트
                double newTimer = info.getTypingTimer() + dt;
                info.setTypingTimer(newTimer);
                
                int currentLine = info.getCurrentLineIndex();
                double textSpeed = info.getTextDisplaySpeed();
                
                // StageStory에서 모든 줄 가져오기
                boolean endingResult = (currentStageIndex == 3) ? this.finalResultGood : this.lastAnswerCorrect;
                String[] allLines = StageStory.getEndingLines(currentStageIndex, endingResult);
                
                // 현재 줄 인덱스가 유효 범위를 벗어나면, 타이핑을 완료 상태로 처리하고 종료
                if (currentLine >= allLines.length) {
                    info.finishTypingAndCheckEnd();
                    return; 
                }

                int totalCharsInLine = allLines[currentLine].length();
                
                int targetCharCount = (int) (info.getTypingTimer() / textSpeed); 
                
                if (targetCharCount >= totalCharsInLine) {
                    info.setCurrentCharIndex(totalCharsInLine);

                    if (currentLine < allLines.length - 1) { 

                        if (info.getTypingTimer() >= totalCharsInLine * textSpeed + 0.05) { 
                             info.setCurrentLineIndex(currentLine + 1);
                             info.setTypingTimer(0.0); // 타이머 초기화 중요!
                             info.setCurrentCharIndex(0);
                        }
                    } else { 
                        if (info.getTypingTimer() >= totalCharsInLine * textSpeed + 0.05) { 
                            if (currentStageIndex == 3) {
                            } else {
                                info.finishTypingAndCheckEnd(); 
                            }
                        }
                    }
                } else {
                    info.setCurrentCharIndex(targetCharCount);
                }
                
                return;
            }
            
            /** 2차 엔딩 (검은 화면) 타이핑 로직 (1차 엔딩 완료 후 실행됨)
             *  1차 타이핑이 완료 이후 2차 타이핑 시작 */
            if (info.isTypingFinished()) {
            	
            	if (phase2CurrentCharIndex == 0 && phase2TypingTimer == 0.0) { 
                    if (GameCore.getSoundManager() != null) {
                        // 기존 스테이지 BGM 정지
                        GameCore.getSoundManager().stopBGM(); 
                        // 타이핑 소리 재생
                        GameCore.getSoundManager().playBGM(SoundManager.SFX_TYPING); 
                    }
                }
            	
                // 2차 엔딩 텍스트 길이 계산
                int totalChars = finalEndingPhase2Text.length();
                
                if (phase2CurrentCharIndex < totalChars) {
                	
                    phase2TypingTimer += dt;
                    
                    // 타이머 값에 따라 출력할 글자 수 계산
                    int charsToDisplay = (int) (phase2TypingTimer / phase2TextDisplaySpeed);
                    
                    if (charsToDisplay > phase2CurrentCharIndex) {
                        phase2CurrentCharIndex = Math.min(charsToDisplay, totalChars);
                    }
                    
                    if (phase2CurrentCharIndex >= totalChars) {
                        // 타이핑 소리 정지
                        if (GameCore.getSoundManager() != null) {
                             GameCore.getSoundManager().stopBGM(); 
                        }
                    }
                }
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
            
            // ⭐️ INTRO 상태 처리
            if (state == GameState.INTRO) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (introManager.advanceLine()) { // 다음 줄로 넘어가거나 타이핑 강제 완료
                        if (introManager.isStoryEnd()) {
                            // 스토리가 끝났으면 다음 상태로 전환
                            loadStage(0); // 튜토리얼 시작
                            state = GameState.TUTORIAL; 
                        }
                    }
                }
                return;
            }

            if (code == KeyEvent.VK_ESCAPE) {
                if (state == GameState.PAUSE) {
                    resumeGame();
                } else if (state == GameState.TUTORIAL || state == GameState.STAGE_PLAY) {
                    pauseGame();
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
                // (요구사항) 1번 = O, 2번 = X
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
        private double dashTimeRemaining = 0.0;
        private static final double DASH_DURATION = 0.25;

        // 물리 파라미터
        private static final double BOUNCE_SPEED    = 280.0;
        private static final double DASH_SPEED      = 550.0;
        private static final double BLUE_JUMP_SPEED = 450.0;

        // (추가) 벽 충돌만 줄이기 위한 패딩
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

        /** Space 눌렀을 때 능력 사용 */
        public void useAbility() {
            if (!abilityReady) return;

            switch (form) {
                case YELLOW:
                    dashActive = true;
                    dashTimeRemaining = DASH_DURATION;
                    velX = (facingRight ? DASH_SPEED : -DASH_SPEED);
                    velY = -BLUE_JUMP_SPEED * 0.45;   // 대시 점프 높이
                    break;

                case BLUE:
                    velY = -BLUE_JUMP_SPEED * 0.80;   // 슈퍼점프 높이
                    break;

                case BASIC:
                default:
                    return;
            }

            // 능력 사용 후 BASIC 복귀
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

        /** 타일 기반 충돌 처리 */
        public void moveAndCollide(MapLoader.MapData map, double dt, int tileSize) {
            double newX = x + velX * dt;
            double newY = y + velY * dt;

            x = moveAndCollideAxis(map, newX, y, tileSize, true);

            double resultY = moveAndCollideAxis(map, x, newY, tileSize, false);

            onGround = false;

            if (resultY != newY && velY > 0) {
                // 바닥에 닿으면 튕김
                velY = -BOUNCE_SPEED;
                onGround = true;
                // [추가] 점프 소리
                if (GameCore.getSoundManager() != null) {
                    GameCore.getSoundManager().playSFX(SoundManager.SFX_JUMP);
                }
            } else if (resultY != newY && velY < 0) {
                // 천장에 닿으면 위속도 제거
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

        /** 한 축 이동/충돌 처리 */
        private double moveAndCollideAxis(MapLoader.MapData map, double targetX, double targetY,
                                          int tileSize, boolean horizontal) {
            double resultX = x;
            double resultY = y;

            if (horizontal) resultX = targetX;
            else resultY = targetY;

            // (수정) WALL 충돌은 작은 히트박스로 계산
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
                            // colRight가 tileLeft에 닿게
                            resultX = tileLeft - 0.01 - (width - COL_PAD_X);
                        } else if (velX < 0) {
                            // colLeft가 tileRight에 닿게
                            resultX = tileRight + 0.01 - COL_PAD_X;
                        }
                        left  = resultX + COL_PAD_X;
                        right = resultX + width - COL_PAD_X;
                    } else {
                        if (velY > 0) {
                            // colBottom이 tileTop에 닿게
                            resultY = tileTop - 0.01 - (height - COL_PAD_Y);
                        } else if (velY < 0) {
                            // colTop이 tileBottom에 닿게
                            resultY = tileBottom + 0.01 - COL_PAD_Y;
                        }
                        top    = resultY + COL_PAD_Y;
                        bottom = resultY + height - COL_PAD_Y;
                    }
                }
            }

            return horizontal ? resultX : resultY;
        }

        // ---- getter ----
        // (설명) 밖(GamePanel)에서 플레이어 위치/크기/상태를 읽을 수 있게 해주는 함수들
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
