import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.awt.image.BufferedImage;

import java.io.File;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameCore {

    public static final String CARD_MENU = "MENU";
    public static final String CARD_GAME = "GAME";

    private static SoundManager soundManager;

    public static SoundManager getSoundManager() {
        return soundManager;
    }

    public static void main(String[] args) {

        soundManager =
                new SoundManager();

        soundManager
                .playBGM(
                        SoundManager.BGM_MAIN_MENU
                );

        SwingUtilities.invokeLater(
                () -> {

                    JFrame frame =
                            new JFrame(
                                    "Bounce Escape (Core Prototype)"
                            );

                    frame.setDefaultCloseOperation(
                            JFrame.EXIT_ON_CLOSE
                    );

                    JPanel mainContainer =
                            new JPanel(
                                    new CardLayout()
                            );

                    GamePanel gamePanel =
                            new GamePanel(
                                    mainContainer
                            );

                    MenuPanel menuPanel =
                            new MenuPanel(
                                    gamePanel,
                                    mainContainer
                            );

                    gamePanel.initPausePanel(
                            mainContainer
                    );

                    mainContainer.add(
                            menuPanel,
                            CARD_MENU
                    );

                    mainContainer.add(
                            gamePanel,
                            CARD_GAME
                    );

                    frame.setContentPane(
                            mainContainer
                    );

                    frame.pack();

                    frame.setLocationRelativeTo(
                            null
                    );

                    frame.setVisible(
                            true
                    );
                }
        );
    }

    private enum GameState {
        MENU,
        INTRO,
        TUTORIAL,
        STAGE_PLAY,
        QUESTION,
        ENDING,
        PAUSE
    }

    public static class GamePanel extends JPanel implements Runnable, KeyListener {

        public static final int WIDTH  = 960;
        public static final int HEIGHT = 540;

        public static final int TILE_SIZE = 32;

        private static final int MAP_OFFSET_Y = 60;

        private static final double TARGET_FPS      = 60.0;
        private static final double FRAME_TIME_NANO =
                1_000_000_000.0 / TARGET_FPS;

        private BufferedImage backBuffer;
        private Graphics2D    backG;

        private Thread gameThread;

        private volatile boolean running = false;

        private GameState state = GameState.INTRO;

        private boolean leftPressed  = false;
        private boolean rightPressed = false;

        private double currentFps = 0.0;

        private long fpsCounterStartTime = 0L;
        private int  frameCount          = 0;

        private int currentStageIndex = 0;

        private static final int MAX_STAGE_COUNT = 4;

        private StageInfo[] stageInfos =
                new StageInfo[MAX_STAGE_COUNT];

        private boolean[] stageSuccessStatus =
                new boolean[MAX_STAGE_COUNT];

        private boolean finalResultGood = false;

        private MapLoader.MapData currentMap;

        private Player player;

        private int totalStarsInStage = 0;
        private int collectedStars    = 0;

        private boolean waitingForQuestionAnswer = false;
        private boolean lastAnswerCorrect        = false;

        private MCQ currentQuiz;

        private BufferedImage basicStandImg;
        private BufferedImage basicJumpImg;

        private BufferedImage yellowStandImg;
        private BufferedImage yellowJumpImg;

        private BufferedImage blueStandImg;
        private BufferedImage blueJumpImg;

        private BufferedImage tileLavaImg;

        private BufferedImage coinImg;

        private BufferedImage gemYellowImg;
        private BufferedImage gemBlueImg;

        private BufferedImage introBGImg;

        private BufferedImage goodEndingBG;
        private BufferedImage badEndingBG;

        private BufferedImage bgTutorialImg;

        private BufferedImage bgStage1Img;
        private BufferedImage bgStage2Img;
        private BufferedImage bgStage3Img;

        private static final String BG_TUTORIAL_PATH =
                "image/bg_tutorial.jpg";

        private static final String BG_STAGE1_PATH =
                "image/bg_stage1.jpg";

        private static final String BG_STAGE2_PATH =
                "image/bg_stage2.jpg";

        private static final String BG_STAGE3_PATH =
                "image/bg_stage3.png";

        private double coinAnimTime = 0.0;

        private PausePanel pausePanel;

        private GameState lastPlayState =
                GameState.TUTORIAL;

        private IntroManager introManager =
                new IntroManager();

        private double phase2TypingTimer     = 0.0;
        private int    phase2CurrentCharIndex = 0;

        private final double phase2TextDisplaySpeed =
                0.1;

        private String finalEndingPhase2Text;

        private boolean phase2TypingSoundPlaying =
                false;

        private final JPanel mainContainer;

        private static final String LAVA_GIF_PATH =
                "image/lava real.gif";

        private static final double LAVA_FPS =
                12.0;

        private List<BufferedImage> lavaFrames =
                new ArrayList<>();

        private double lavaAnimTime =
                0.0;

        private static final String GEAR_GIF_PATH =
                "image/gear_G.gif";

        private static final double GEAR_FPS =
                12.0;

        private List<BufferedImage> gearFrames =
                new ArrayList<>();

        private double gearAnimTime =
                0.0;

        private static final double GEAR_MOVE_TILES =
                1.5;

        private static final double GEAR_MOVE_SPEED =
                1.8;

        private final List<Gear> gears =
                new ArrayList<>();

        private boolean tutMovedOnce  = false;
        private boolean tutShownMove  = false;
        private boolean tutShownGem   = false;
        private boolean tutShownDeath = false;

        private String tutHintText =
                null;

        private double tutHintTimer =
                0.0;

        private static final double TUT_HINT_DURATION =
                3.0;

        private boolean consoleOpen       = false;
        private boolean debugDrawHitbox   = false;
        private boolean debugShowInfo     = false;

        private boolean invincible =
                false;

        private double speedMul   = 1.0;
        private double gravityMul = 1.0;

        private final KeyMap keys =
                new KeyMap();

        public GamePanel(JPanel mainContainer) {

            this.mainContainer =
                    mainContainer;

            setPreferredSize(
                    new Dimension(
                            WIDTH,
                            HEIGHT
                    )
            );

            setFocusable(
                    true
            );

            requestFocusInWindow();

            addKeyListener(
                    this
            );

            initBackBuffer();

            loadImages();

            setLayout(
                    null
            );
        }

        private void initBackBuffer() {

            backBuffer =
                    new BufferedImage(
                            WIDTH,
                            HEIGHT,
                            BufferedImage.TYPE_INT_ARGB
                    );

            backG =
                    backBuffer.createGraphics();

            backG.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );
        }

        private void loadImages() {

            try {

                basicStandImg  = ImageIO.read(
                        new File("image/alienBiege_stand.png")
                );

                basicJumpImg   = ImageIO.read(
                        new File("image/alienBiege_jump.png")
                );

                yellowStandImg = ImageIO.read(
                        new File("image/alienYellow_stand.png")
                );

                yellowJumpImg  = ImageIO.read(
                        new File("image/alienYellow_jump.png")
                );

                blueStandImg   = ImageIO.read(
                        new File("image/alienBlue_stand.png")
                );

                blueJumpImg    = ImageIO.read(
                        new File("image/alienBlue_jump.png")
                );

                tileLavaImg    = ImageIO.read(
                        new File("image/lava.png")
                );

                coinImg        = ImageIO.read(
                        new File("image/coinGold.png")
                );

                gemYellowImg   = ImageIO.read(
                        new File("image/gemYellow.png")
                );

                gemBlueImg     = ImageIO.read(
                        new File("image/gemBlue.png")
                );

                introBGImg     = ImageIO.read(
                        new File("image/Intro.png")
                );

                goodEndingBG   = ImageIO.read(
                        new File("image/Ending_A.png")
                );

                badEndingBG    = ImageIO.read(
                        new File("image/Ending_B.png")
                );

                bgTutorialImg  =
                        safeRead(
                                BG_TUTORIAL_PATH
                        );

                bgStage1Img    =
                        safeRead(
                                BG_STAGE1_PATH
                        );

                bgStage2Img    =
                        safeRead(
                                BG_STAGE2_PATH
                        );

                bgStage3Img    =
                        safeRead(
                                BG_STAGE3_PATH
                        );

            } catch (IOException e) {

                e.printStackTrace();

            }

            lavaFrames =
                    loadGifFrames(
                            LAVA_GIF_PATH
                    );

            gearFrames =
                    loadGifFrames(
                            GEAR_GIF_PATH
                    );
        }

        private BufferedImage safeRead(String path) {

            try {

                File f =
                        new File(path);

                if (!f.exists()) {
                    return null;
                }

                return ImageIO.read(
                        f
                );

            } catch (IOException e) {

                return null;

            }
        }

        private List<BufferedImage> loadGifFrames(String path) {

            List<BufferedImage> frames =
                    new ArrayList<>();

            try (
                    ImageInputStream stream =
                            ImageIO.createImageInputStream(
                                    new File(path)
                            )
            ) {

                Iterator<javax.imageio.ImageReader> readers =
                        ImageIO.getImageReadersByFormatName(
                                "gif"
                        );

                if (!readers.hasNext()) {
                    return frames;
                }

                javax.imageio.ImageReader reader =
                        readers.next();

                reader.setInput(
                        stream,
                        false
                );

                int count =
                        reader.getNumImages(true);

                for (int i = 0; i < count; i++) {

                    BufferedImage frame =
                            reader.read(i);

                    if (frame != null) {
                        frames.add(frame);
                    }
                }

                reader.dispose();

            } catch (Exception e) {

                e.printStackTrace();

            }

            return frames;
        }

        private void drawAnimatedLava(Graphics2D g, int px, int py) {

            if (lavaFrames == null || lavaFrames.isEmpty()) {

                g.setColor(
                        Color.ORANGE
                );

                g.fillRect(
                        px,
                        py,
                        TILE_SIZE,
                        TILE_SIZE
                );

                return;
            }

            int idx =
                    (int) (lavaAnimTime * LAVA_FPS)
                            % lavaFrames.size();

            g.drawImage(
                    lavaFrames.get(idx),
                    px,
                    py,
                    TILE_SIZE,
                    TILE_SIZE,
                    null
            );
        }

        private void drawAnimatedGear(Graphics2D g, int px, int py) {

            if (gearFrames == null || gearFrames.isEmpty()) {

                g.setColor(
                        Color.GRAY
                );

                g.fillOval(
                        px + 3,
                        py + 3,
                        TILE_SIZE - 6,
                        TILE_SIZE - 6
                );

                return;
            }

            int idx =
                    (int) (gearAnimTime * GEAR_FPS)
                            % gearFrames.size();

            g.drawImage(
                    gearFrames.get(idx),
                    px,
                    py,
                    TILE_SIZE,
                    TILE_SIZE,
                    null
            );
        }

        public void startNewGame() {

            state =
                    GameState.INTRO;

            introManager =
                    new IntroManager();

            startGameThread();

            if (GameCore.getSoundManager() != null) {
                GameCore.getSoundManager()
                        .playBGM(
                                SoundManager.BGM_STAGE
                        );
            }

            SwingUtilities.invokeLater(
                    () -> requestFocusInWindow()
            );

            state =
                    GameState.INTRO;
        }

        public void startGameThread() {

            if (gameThread == null) {

                running =
                        true;

                gameThread =
                        new Thread(
                                this,
                                "GameLoopThread"
                        );

                gameThread.start();
            }
        }

        @Override
        public void run() {

            long previousTime =
                    System.nanoTime();

            fpsCounterStartTime =
                    previousTime;

            frameCount =
                    0;

            while (running) {

                long now =
                        System.nanoTime();

                attachSleep(previousTime, now);

                long elapsed =
                        now - previousTime;

                if (elapsed < FRAME_TIME_NANO) {
                    continue;
                }

                previousTime =
                        now;

                double dt =
                        elapsed / 1_000_000_000.0;

                if (state != GameState.PAUSE) {
                    update(dt);
                }

                render();

                repaint();

                frameCount++;

                updateFpsCounter(now);
            }
        }

        private void updateFpsCounter(long now) {

            long fpsElapsed =
                    now - fpsCounterStartTime;

            if (fpsElapsed >= 1_000_000_000L) {

                currentFps =
                        frameCount
                                * (1_000_000_000.0 / fpsElapsed);

                frameCount =
                        0;

                fpsCounterStartTime =
                        now;
            }
        }

        private void attachSleep(long previousTime, long now) {

            long elapsed =
                    now - previousTime;

            if (elapsed < FRAME_TIME_NANO) {

                long sleepMillis =
                        (long) ((FRAME_TIME_NANO - elapsed) / 1_000_000L);

                if (sleepMillis > 0) {
                    try {
                        Thread.sleep(sleepMillis);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        private void update(double dt) {

            if (state == GameState.INTRO) {
                updateIntro(dt);
                return;
            }

            if (state == GameState.TUTORIAL || state == GameState.STAGE_PLAY) {
                updateStagePlay(dt);
                return;
            }

            if (state == GameState.QUESTION) {
                return;
            }

            if (state == GameState.ENDING) {
                updateEndingTyping(dt);
                return;
            }
        }

        private void updateIntro(double dt) {
            if (introManager != null) {
                introManager.update(dt);
            }
        }

        private void updateStagePlay(double dt) {

            if (currentMap == null) {
                return;
            }

            if (player == null) {
                return;
            }

            tickAnimations(dt);

            tickGears(dt);

            updateTutorialHints(dt);

            if (!consoleOpen) {
                handlePlayerInput(dt);
            }

            if (isPlayerFallenOut()) {
                handlePlayerDeath();
                return;
            }

            handleTileInteractions();
        }

        private void tickAnimations(double dt) {

            coinAnimTime += dt;

            lavaAnimTime += dt;

            gearAnimTime += dt;
        }

        private void tickGears(double dt) {

            if (gears == null) {
                return;
            }

            for (Gear gear : gears) {

                if (gear == null) {
                    continue;
                }

                gear.update(dt);
            }
        }

        private boolean isPlayerFallenOut() {

            double py =
                    player.getY();

            return py > HEIGHT + TILE_SIZE * 2;
        }

        private void handlePlayerInput(double dt) {

            double moveSpeed =
                    220.0 * speedMul;

            double gravity =
                    900.0 * gravityMul;

            double vx =
                    computeMoveVx(moveSpeed);

            if (!player.isDashing()) {
                player.applyHorizontalVelocity(vx);
            }

            player.applyGravity(gravity, dt);

            player.moveAndCollide(currentMap, dt, TILE_SIZE);
        }

        private double computeMoveVx(double moveSpeed) {

            double vx = 0.0;

            boolean left  = leftPressed;
            boolean right = rightPressed;

            if (left && !right) {
                vx = -moveSpeed;
            } else if (right && !left) {
                vx = moveSpeed;
            }

            return vx;
        }

        private void handleTileInteractions() {

            Rectangle pr =
                    player.getRect();

            int leftTile =
                    clampInt(
                            (int) (pr.x / TILE_SIZE),
                            0,
                            currentMap.width - 1
                    );

            int rightTile =
                    clampInt(
                            (int) ((pr.x + pr.width) / TILE_SIZE),
                            0,
                            currentMap.width - 1
                    );

            int topTile =
                    clampInt(
                            (int) (pr.y / TILE_SIZE),
                            0,
                            currentMap.height - 1
                    );

            int bottomTile =
                    clampInt(
                            (int) ((pr.y + pr.height) / TILE_SIZE),
                            0,
                            currentMap.height - 1
                    );

            for (int ty = topTile; ty <= bottomTile; ty++) {

                for (int tx = leftTile; tx <= rightTile; tx++) {

                    MapLoader.TileType type =
                            currentMap.tiles[ty][tx];

                    boolean stop =
                            applyTileEffect(tx, ty, type);

                    if (stop) {
                        return;
                    }
                }
            }

            if (!invincible) {
                if (checkGearCollision(pr)) {
                    handlePlayerDeath();
                }
            }
        }

        private boolean checkGearCollision(Rectangle pr) {

            if (gears == null) {
                return false;
            }

            for (Gear gear : gears) {

                if (gear == null) {
                    continue;
                }

                if (gear.collidesWith(pr)) {
                    return true;
                }
            }

            return false;
        }

        private boolean applyTileEffect(int tx, int ty, MapLoader.TileType type) {

            if (type == null) {
                return false;
            }

            if (type == MapLoader.TileType.SPIKE || type == MapLoader.TileType.LAVA) {

                if (!invincible) {
                    handlePlayerDeath();
                }

                return true;
            }

            if (type == MapLoader.TileType.STAR) {

                collectStarAt(tx, ty);

                return false;
            }

            if (type == MapLoader.TileType.GEM_YELLOW) {

                pickupYellowGemAt(tx, ty);

                return false;
            }

            if (type == MapLoader.TileType.GEM_BLUE) {

                pickupBlueGemAt(tx, ty);

                return false;
            }

            return false;
        }

        private void collectStarAt(int tx, int ty) {

            currentMap.tiles[ty][tx] =
                    MapLoader.TileType.EMPTY;

            collectedStars++;

            StageInfo info =
                    stageInfos[currentStageIndex];

            if (info != null) {
                info.incrementCollectedStars();
            }

            if (GameCore.getSoundManager() != null) {
                GameCore.getSoundManager()
                        .playSFX(
                                SoundManager.SFX_STAR_COLLECT
                        );
            }

            if (collectedStars >= totalStarsInStage && !waitingForQuestionAnswer) {

                if (currentStageIndex == 0) {

                    lastAnswerCorrect = true;

                    StageInfo tutInfo =
                            stageInfos[currentStageIndex];

                    if (tutInfo != null) {
                        tutInfo.setQuestionAnswered(true);
                        tutInfo.finishStageNow();
                    }

                    state =
                            GameState.ENDING;

                    return;
                }

                enterQuestionState();
            }
        }

        private void pickupYellowGemAt(int tx, int ty) {

            currentMap.tiles[ty][tx] =
                    MapLoader.TileType.EMPTY;

            player.setFormYellow();

            onGemPickedTutorialHint();
        }

        private void pickupBlueGemAt(int tx, int ty) {

            currentMap.tiles[ty][tx] =
                    MapLoader.TileType.EMPTY;

            player.setFormBlue();

            onGemPickedTutorialHint();
        }

        private void handlePlayerDeath() {

            if (currentStageIndex == 0 && !tutShownDeath) {

                showTutHint("가시/용암은 즉사");

                tutShownDeath = true;
            }

            StageInfo info =
                    stageInfos[currentStageIndex];

            if (info != null) {
                info.incrementDeathCount();
            }

            resetCurrentStage();
        }

        private void resetCurrentStage() {

            MapLoader.MapData map =
                    MapLoader.loadStage(currentStageIndex);

            if (map == null) {
                return;
            }

            setMapAndResetCounters(map);

            player =
                    new Player(
                            map.playerStartX,
                            map.playerStartY,
                            TILE_SIZE * 0.7,
                            TILE_SIZE * 0.9
                    );

            ensureStageInfoForCurrentStage();

            StageInfo info =
                    stageInfos[currentStageIndex];

            if (info != null) {
                info.startStage();
            }

            rebuildGearsFromMap();

            if (currentStageIndex == 0) {
                resetTutorialHintState();
            }

            state =
                    (currentStageIndex == 0)
                            ? GameState.TUTORIAL
                            : GameState.STAGE_PLAY;
        }

        private void setMapAndResetCounters(MapLoader.MapData map) {

            currentMap =
                    map;

            totalStarsInStage =
                    map.totalStars;

            collectedStars =
                    0;

            waitingForQuestionAnswer =
                    false;

            lastAnswerCorrect =
                    false;

            currentQuiz =
                    null;
        }

        private void ensureStageInfoForCurrentStage() {

            if (stageInfos[currentStageIndex] == null) {

                stageInfos[currentStageIndex] =
                        new StageInfo(
                                currentStageIndex,
                                getStageName(currentStageIndex),
                                totalStarsInStage
                        );
            }
        }

        private void resetTutorialHintState() {

            tutMovedOnce =
                    false;

            tutShownMove =
                    false;

            tutShownGem =
                    false;

            tutHintText =
                    null;

            tutHintTimer =
                    0.0;
        }

        private String getStageName(int stageIndex) {

            if (stageIndex == 0) return "튜토리얼";
            if (stageIndex == 1) return "CPU 코어 구역";
            if (stageIndex == 2) return "메모리·캐시 구역";
            if (stageIndex == 3) return "I/O·방어 모듈 구역";

            return "Stage " + stageIndex;
        }

        private void loadStage(int stageIndex) {

            if (stageIndex < 0) {
                return;
            }

            if (stageIndex >= MAX_STAGE_COUNT) {
                return;
            }

            currentStageIndex =
                    stageIndex;

            MapLoader.MapData map =
                    MapLoader.loadStage(stageIndex);

            if (map == null) {
                return;
            }

            currentMap =
                    map;

            totalStarsInStage =
                    map.totalStars;

            collectedStars =
                    0;

            waitingForQuestionAnswer =
                    false;

            lastAnswerCorrect =
                    false;

            currentQuiz =
                    null;

            if (stageInfos[stageIndex] == null) {

                stageInfos[stageIndex] =
                        new StageInfo(
                                stageIndex,
                                getStageName(stageIndex),
                                totalStarsInStage
                        );

            } else {

                stageInfos[stageIndex]
                        .resetForRetry(
                                totalStarsInStage
                        );
            }

            stageInfos[stageIndex]
                    .startStage();

            player =
                    new Player(
                            map.playerStartX,
                            map.playerStartY,
                            TILE_SIZE * 0.7,
                            TILE_SIZE * 0.9
                    );

            if (stageIndex == 0) {

                stageSuccessStatus[0] =
                        true;

                resetTutorialHintState();
            }

            rebuildGearsFromMap();

            state =
                    (stageIndex == 0)
                            ? GameState.TUTORIAL
                            : GameState.STAGE_PLAY;
        }

        private void rebuildGearsFromMap() {

            gears.clear();

            if (currentMap == null) {
                return;
            }

            double ampPx =
                    TILE_SIZE * GEAR_MOVE_TILES;

            double speed =
                    GEAR_MOVE_SPEED;

            if (currentMap.gearDownSpawns != null) {

                for (MapLoader.Point p : currentMap.gearDownSpawns) {

                    if (p == null) continue;

                    gears.add(
                            Gear.horizontal(
                                    p.x * TILE_SIZE,
                                    p.y * TILE_SIZE,
                                    ampPx,
                                    speed
                            )
                    );
                }
            }

            if (currentMap.gearUpSpawns != null) {

                for (MapLoader.Point p : currentMap.gearUpSpawns) {

                    if (p == null) continue;

                    gears.add(
                            Gear.vertical(
                                    p.x * TILE_SIZE,
                                    p.y * TILE_SIZE,
                                    ampPx,
                                    speed
                            )
                    );
                }
            }
        }

        private void enterQuestionState() {

            waitingForQuestionAnswer =
                    true;

            currentQuiz =
                    MCQBank.pick(currentStageIndex);

            state =
                    GameState.QUESTION;
        }

        public void onQuestionAnswered(boolean correct) {

            if (!waitingForQuestionAnswer) {
                return;
            }

            waitingForQuestionAnswer =
                    false;

            stageSuccessStatus[currentStageIndex] =
                    correct;

            lastAnswerCorrect =
                    correct;

            StageInfo info =
                    stageInfos[currentStageIndex];

            if (info != null) {

                info.setQuestionAnswered(correct);

                info.finishStageNow();

                info.resetTypingState();
            }

            if (currentStageIndex == 3) {
                buildFinalEndingText();
            }

            state =
                    GameState.ENDING;
        }

        private void buildFinalEndingText() {

            int successCount =
                    0;

            for (boolean success : stageSuccessStatus) {
                if (success) {
                    successCount++;
                }
            }

            finalResultGood =
                    (successCount == stageSuccessStatus.length);

            if (finalResultGood) {

                finalEndingPhase2Text =
                        "당신은 동료들과 함께 우주선을 타고 이 행성을 떠납니다.\n"
                                + "동료들의 웃음소리에 당신의 입가에도 미소가 번집니다.";

            } else {

                finalEndingPhase2Text =
                        "당신은 거대한 컴퓨터 구조가 된 이 행성 위에 홀로 남아\n"
                                + "언젠가 또 올 탈출 시도를 위해 시스템을 지켜보기로 합니다.";
            }
        }

        private void goToNextStageOrFinishGame() {

            if (currentStageIndex + 1 < MAX_STAGE_COUNT) {

                loadStage(currentStageIndex + 1);

                return;
            }

            StageInfo info =
                    stageInfos[currentStageIndex];

            if (info != null && !info.isTypingFinished()) {

                info.finishTypingAndCheckEnd();

                return;
            }

            stopGameThread();

            CardLayout cl =
                    (CardLayout) mainContainer.getLayout();

            cl.show(
                    mainContainer,
                    GameCore.CARD_MENU
            );

            if (GameCore.getSoundManager() != null) {
                GameCore.getSoundManager()
                        .playBGM(
                                SoundManager.BGM_MAIN_MENU
                        );
            }

            resetStateToTutorial();
        }

        private void render() {

            if (backG == null) {
                return;
            }

            backG.setColor(
                    Color.WHITE
            );

            backG.fillRect(
                    0,
                    0,
                    WIDTH,
                    HEIGHT
            );

            if (state == GameState.INTRO) {

                renderStoryOverlay(backG);

                return;
            }

            if (state == GameState.TUTORIAL || state == GameState.STAGE_PLAY) {

                renderStagePlay(backG);

                renderHUD(backG);

                renderTutorialHintOverlay(backG);

                renderConsoleOverlay(backG);

                if (debugShowInfo) {
                    renderDebugInfo(backG);
                }

                return;
            }

            if (state == GameState.QUESTION) {

                renderStagePlay(backG);

                renderHUD(backG);

                renderQuestionOverlay(backG);

                renderConsoleOverlay(backG);

                if (debugShowInfo) {
                    renderDebugInfo(backG);
                }

                return;
            }

            if (state == GameState.ENDING) {

                if (currentStageIndex == 3) {
                    renderFinalEnding(backG);
                } else {
                    renderEndingOverlay(backG);
                }

                renderConsoleOverlay(backG);

                if (debugShowInfo) {
                    renderDebugInfo(backG);
                }

                return;
            }

            if (state == GameState.PAUSE) {

                renderStagePlay(backG);

                renderHUD(backG);

                renderConsoleOverlay(backG);

                if (debugShowInfo) {
                    renderDebugInfo(backG);
                }

                return;
            }

            renderSimpleMenu(backG);
        }

        private BufferedImage getStageBackground() {

            if (currentStageIndex == 0) return bgTutorialImg;
            if (currentStageIndex == 1) return bgStage1Img;
            if (currentStageIndex == 2) return bgStage2Img;
            if (currentStageIndex == 3) return bgStage3Img;

            return null;
        }

        private void renderStagePlay(Graphics2D g) {

            if (currentMap == null) return;
            if (player == null) return;

            BufferedImage bg =
                    getStageBackground();

            if (bg != null) {

                g.drawImage(
                        bg,
                        0,
                        MAP_OFFSET_Y,
                        WIDTH,
                        HEIGHT - MAP_OFFSET_Y,
                        null
                );
            }

            drawTiles(g);

            drawGears(g);

            drawPlayerSprite(g);
        }

        private void drawTiles(Graphics2D g) {

            for (int y = 0; y < currentMap.height; y++) {

                for (int x = 0; x < currentMap.width; x++) {

                    MapLoader.TileType type =
                            currentMap.tiles[y][x];

                    int px =
                            x * TILE_SIZE;

                    int py =
                            y * TILE_SIZE + MAP_OFFSET_Y;

                    drawOneTile(g, type, px, py);

                    if (debugDrawHitbox && type != MapLoader.TileType.EMPTY) {
                        g.setColor(new Color(0, 0, 0, 60));
                        g.drawRect(px, py, TILE_SIZE, TILE_SIZE);
                    }
                }
            }
        }

        private void drawOneTile(Graphics2D g, MapLoader.TileType type, int px, int py) {

            if (type == MapLoader.TileType.WALL) {

                if (tileLavaImg != null) {
                    g.drawImage(tileLavaImg, px, py, TILE_SIZE, TILE_SIZE, null);
                } else {
                    g.setColor(Color.LIGHT_GRAY);
                    g.fillRect(px, py, TILE_SIZE, TILE_SIZE);
                }

                return;
            }

            if (type == MapLoader.TileType.LAVA) {
                drawAnimatedLava(g, px, py);
                return;
            }

            if (type == MapLoader.TileType.SPIKE) {
                g.setColor(Color.RED);
                int[] xs = { px, px + TILE_SIZE / 2, px + TILE_SIZE };
                int[] ys = { py + TILE_SIZE, py, py + TILE_SIZE };
                g.fillPolygon(xs, ys, 3);
                return;
            }

            if (type == MapLoader.TileType.STAR) {
                drawAnimatedCoin(g, px, py);
                return;
            }

            if (type == MapLoader.TileType.GEM_YELLOW) {
                if (gemYellowImg != null) {
                    g.drawImage(gemYellowImg, px, py, TILE_SIZE, TILE_SIZE, null);
                }
                return;
            }

            if (type == MapLoader.TileType.GEM_BLUE) {
                if (gemBlueImg != null) {
                    g.drawImage(gemBlueImg, px, py, TILE_SIZE, TILE_SIZE, null);
                }
                return;
            }

            if (type == MapLoader.TileType.DOOR) {
                g.setColor(new Color(120, 80, 40));
                g.fillRect(px + TILE_SIZE / 8, py, TILE_SIZE * 3 / 4, TILE_SIZE);
                g.setColor(Color.BLACK);
                g.drawRect(px + TILE_SIZE / 8, py, TILE_SIZE * 3 / 4, TILE_SIZE);
                return;
            }
        }

        private void drawGears(Graphics2D g) {

            for (Gear gear : gears) {

                if (gear == null) {
                    continue;
                }

                int gx =
                        (int) gear.x;

                int gy =
                        (int) gear.y + MAP_OFFSET_Y;

                drawAnimatedGear(g, gx, gy);

                if (debugDrawHitbox) {

                    double r = gear.getRadius();

                    int cx = (int) (gx + TILE_SIZE / 2.0);
                    int cy = (int) (gy + TILE_SIZE / 2.0);

                    g.setColor(new Color(255, 0, 0, 160));

                    g.drawOval(
                            (int) (cx - r),
                            (int) (cy - r),
                            (int) (r * 2),
                            (int) (r * 2)
                    );
                }
            }
        }

        private void drawPlayerSprite(Graphics2D g) {

            Rectangle pr =
                    player.getRect();

            int plx =
                    pr.x;

            int ply =
                    pr.y + MAP_OFFSET_Y;

            int plw =
                    pr.width;

            int plh =
                    pr.height;

            boolean goingUp =
                    player.getVelY() < 0.0;

            BufferedImage sprite =
                    choosePlayerSprite(goingUp);

            if (sprite != null) {

                g.drawImage(
                        sprite,
                        plx,
                        ply,
                        plw,
                        plh,
                        null
                );

            } else {

                g.setColor(Color.BLACK);

                g.fillRect(plx, ply, plw, plh);
            }

            if (debugDrawHitbox) {

                g.setColor(Color.GREEN);

                g.drawRect(plx, ply, plw, plh);
            }
        }

        private BufferedImage choosePlayerSprite(boolean goingUp) {

            Player.Form form =
                    player.getForm();

            if (form == Player.Form.BASIC) {
                return goingUp ? basicJumpImg : basicStandImg;
            }

            if (form == Player.Form.YELLOW) {
                return goingUp ? yellowJumpImg : yellowStandImg;
            }

            if (form == Player.Form.BLUE) {
                return goingUp ? blueJumpImg : blueStandImg;
            }

            return null;
        }

        private void drawAnimatedCoin(Graphics2D g, int px, int py) {

            if (coinImg == null) {

                g.setColor(Color.YELLOW);

                g.fillOval(
                        px + TILE_SIZE / 4,
                        py + TILE_SIZE / 4,
                        TILE_SIZE / 2,
                        TILE_SIZE / 2
                );

                return;
            }

            double offset =
                    Math.sin(coinAnimTime * 4.0) * 3.0;

            g.drawImage(
                    coinImg,
                    px,
                    (int) (py + offset),
                    TILE_SIZE,
                    TILE_SIZE,
                    null
            );
        }

        private void renderHUD(Graphics2D g) {

            g.setColor(Color.BLACK);

            g.setFont(
                    g.getFont()
                            .deriveFont(Font.BOLD, 16f)
            );

            String stageName =
                    (stageInfos[currentStageIndex] != null)
                            ? stageInfos[currentStageIndex].getStageName()
                            : "Stage " + currentStageIndex;

            String starInfo =
                    "Stars: " + collectedStars + " / " + totalStarsInStage;

            String deathInfo =
                    "Deaths: " + (stageInfos[currentStageIndex] != null
                            ? stageInfos[currentStageIndex].getDeathCount()
                            : 0);

            g.drawString(stageName, 10, 20);

            g.drawString(starInfo, 10, 40);

            g.drawString(deathInfo, 10, 60);

            g.setFont(
                    g.getFont()
                            .deriveFont(Font.BOLD, 14f)
            );

            String mul =
                    String.format(
                            "xSpeed %.2f  xGravity %.2f  %s",
                            speedMul,
                            gravityMul,
                            invincible ? "INVINCIBLE" : ""
                    );

            g.drawString(mul, 10, 82);

            StageInfo info =
                    stageInfos[currentStageIndex];

            if (info != null) {

                long ms =
                        info.getElapsedPlayTimeMillis();

                if (ms < 0) {
                    ms = 0;
                }

                g.setFont(
                        g.getFont()
                                .deriveFont(Font.BOLD, 24f)
                );

                String timeString =
                        String.format("%.2f초", ms / 1000.0);

                int xPos =
                        WIDTH
                                - g.getFontMetrics().stringWidth(timeString)
                                - 20;

                g.drawString(timeString, xPos, 40);
            }
        }

        private void renderQuestionOverlay(Graphics2D g) {

            g.setColor(new Color(0, 0, 0, 180));

            g.fillRect(60, 60, WIDTH - 120, HEIGHT - 120);

            int x = 90;
            int y = 110;

            g.setColor(Color.WHITE);

            g.setFont(new Font("SansSerif", Font.BOLD, 22));

            String title =
                    getStageName(currentStageIndex);

            g.drawString("퀴즈 - " + title, x, y);

            y += 45;

            g.setFont(new Font("SansSerif", Font.PLAIN, 16));

            if (currentQuiz == null) {
                g.drawString("문제 로딩 중...", x, y);
                return;
            }

            g.drawString(currentQuiz.getQuestion(), x, y);

            y += 36;

            g.setFont(new Font("SansSerif", Font.PLAIN, 16));

            g.setColor(new Color(255, 255, 120));

            String[] choices =
                    currentQuiz.getChoices();

            for (int i = 0; i < 4; i++) {

                g.drawString((i + 1) + ") " + choices[i], x, y);

                y += 26;
            }

            y += 10;

            g.setColor(Color.WHITE);

            g.setFont(new Font("SansSerif", Font.BOLD, 14));

            g.drawString("1~4 중 하나를 눌러 선택하세요.", x, y);
        }

        private void renderStoryOverlay(Graphics2D g) {

            if (introBGImg != null) {
                g.drawImage(introBGImg, 0, 0, WIDTH, HEIGHT, null);
            } else {
                g.setColor(new Color(20, 20, 40));
                g.fillRect(0, 0, WIDTH, HEIGHT);
            }

            IntroManager manager =
                    this.introManager;

            final int BOX_HEIGHT  = 150;
            final int BOX_PADDING = 20;

            final int boxY =
                    HEIGHT - BOX_HEIGHT;

            g.setColor(new Color(20, 20, 40, 204));

            g.fillRect(0, boxY, WIDTH, BOX_HEIGHT);

            String speaker =
                    manager.getCurrentSpeaker();

            if (speaker != null) {

                final int SPEAKER_BOX_HEIGHT = 40;
                final int SPEAKER_BOX_WIDTH  = 200;

                int speakerX = BOX_PADDING;
                int speakerY = boxY - SPEAKER_BOX_HEIGHT - 5;

                g.setColor(new Color(40, 40, 60, 220));
                g.fillRect(speakerX, speakerY, SPEAKER_BOX_WIDTH, SPEAKER_BOX_HEIGHT);

                g.setColor(new Color(200, 200, 255));
                g.setFont(g.getFont().deriveFont(Font.BOLD, 20f));
                g.drawString(speaker, speakerX + 15, speakerY + 28);
            }

            g.setColor(Color.WHITE);

            g.setFont(
                    g.getFont().deriveFont(Font.PLAIN, 20f)
            );

            String line =
                    manager.getCurrentDialogue();

            int end =
                    Math.min(
                            manager.getCurrentCharIndex(),
                            line.length()
                    );

            String displayLine =
                    line.substring(0, end);

            g.drawString(displayLine, BOX_PADDING, boxY + BOX_PADDING + 25);

            if (manager.isStoryEnd() || manager.isTypingComplete()) {

                g.setColor(Color.YELLOW);

                g.setFont(
                        g.getFont().deriveFont(Font.BOLD, 22f)
                );

                String msg =
                        manager.isStoryEnd()
                                ? "ENTER: 튜토리얼 시작"
                                : "ENTER: 다음";

                int msgWidth =
                        g.getFontMetrics().stringWidth(msg);

                g.drawString(msg, WIDTH - msgWidth - BOX_PADDING, boxY + BOX_HEIGHT - BOX_PADDING);
            }
        }

        private void renderEndingOverlay(Graphics2D g) {

            StageInfo info =
                    stageInfos[currentStageIndex];

            boolean good =
                    lastAnswerCorrect;

            String title =
                    StageStory.getEndingTitle(currentStageIndex, good);

            String[] allLines =
                    StageStory.getEndingLines(currentStageIndex, good);

            g.setColor(new Color(0, 0, 0, 200));

            g.fillRect(50, 50, WIDTH - 100, HEIGHT - 100);

            int x = 80;
            int yCursor = 100;

            g.setFont(new Font("SansSerif", Font.BOLD, 30));

            g.setColor(
                    good
                            ? new Color(100, 200, 255)
                            : new Color(255, 150, 150)
            );

            g.drawString(title, x, yCursor);

            yCursor += 50;

            g.setColor(Color.WHITE);

            g.setFont(new Font("SansSerif", Font.PLAIN, 16));

            for (int i = 0; i < allLines.length; i++) {

                String line =
                        allLines[i];

                if (i < info.getCurrentLineIndex()) {
                    g.drawString(line, x, yCursor);
                } else if (i == info.getCurrentLineIndex()) {
                    int end = Math.min(info.getCurrentCharIndex(), line.length());
                    g.drawString(line.substring(0, end), x, yCursor);
                }

                yCursor += 28;
            }

            g.setFont(new Font("SansSerif", Font.BOLD, 18));

            g.setColor(new Color(100, 255, 100));

            g.drawString("ENTER: 다음 시스템", x, HEIGHT - 80);
        }

        private void renderFinalEnding(Graphics2D g) {

            StageInfo info =
                    stageInfos[currentStageIndex];

            if (info.isTypingFinished()) {

                renderFinalEndingPhase2(g);

                return;
            }

            boolean overallGood =
                    this.finalResultGood;

            String[] allLines =
                    StageStory.getEndingLines(currentStageIndex, overallGood);

            BufferedImage bg =
                    overallGood
                            ? goodEndingBG
                            : badEndingBG;

            if (bg != null) {
                g.drawImage(bg, 0, 0, WIDTH, HEIGHT, null);
                g.setColor(new Color(0, 0, 0, 150));
                g.fillRect(0, 0, WIDTH, HEIGHT);
            } else {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, WIDTH, HEIGHT);
            }

            g.setFont(new Font("SansSerif", Font.BOLD, 56));

            g.setColor(
                    overallGood
                            ? new Color(150, 220, 255)
                            : new Color(255, 150, 150)
            );

            String title =
                    StageStory.getEndingTitle(currentStageIndex, overallGood);

            int titleX =
                    (WIDTH - g.getFontMetrics().stringWidth(title)) / 2;

            g.drawString(title, titleX, 70);

            int x = 100;
            int y = 200;

            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.PLAIN, 22));

            for (int i = 0; i < allLines.length; i++) {

                String line = allLines[i];

                if (i < info.getCurrentLineIndex()) {
                    g.drawString(line, x, y);
                } else if (i == info.getCurrentLineIndex()) {
                    int end = Math.min(info.getCurrentCharIndex(), line.length());
                    g.drawString(line.substring(0, end), x, y);
                }

                y += 32;
            }

            g.setFont(new Font("SansSerif", Font.BOLD, 22));
            g.setColor(new Color(100, 255, 100));
            g.drawString("ENTER", WIDTH - 110, HEIGHT - 50);
        }

        private void renderFinalEndingPhase2(Graphics2D g) {

            g.setColor(Color.BLACK);
            g.fillRect(0, 0, WIDTH, HEIGHT);

            if (finalEndingPhase2Text == null) {
                finalEndingPhase2Text = "";
            }

            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.PLAIN, 18));

            String[] lines =
                    finalEndingPhase2Text.split("\n");

            int lineHeight =
                    g.getFontMetrics().getHeight();

            int startY =
                    HEIGHT / 2 - (lines.length * lineHeight) / 2;

            int totalChars =
                    finalEndingPhase2Text.length();

            boolean done =
                    (phase2CurrentCharIndex >= totalChars);

            int renderedBefore =
                    0;

            for (int i = 0; i < lines.length; i++) {

                String line =
                        lines[i];

                int len =
                        line.length();

                int can =
                        Math.max(0, phase2CurrentCharIndex - renderedBefore);

                int draw =
                        Math.min(len, can);

                if (draw > 0) {
                    int x = (WIDTH - g.getFontMetrics().stringWidth(line)) / 2;
                    g.drawString(line.substring(0, draw), x, startY + i * lineHeight);
                }

                renderedBefore += len + 1;
            }

            if (done) {
                g.setFont(new Font("SansSerif", Font.BOLD, 14));
                g.setColor(new Color(100, 255, 100));
                String prompt = "ENTER: 메인 화면";
                int px = (WIDTH - g.getFontMetrics().stringWidth(prompt)) / 2;
                g.drawString(prompt, px, HEIGHT - 50);
            }
        }

        private void renderSimpleMenu(Graphics2D g) {

            g.setColor(Color.WHITE);
            g.fillRect(0, 0, WIDTH, HEIGHT);

            g.setColor(Color.BLACK);

            g.setFont(
                    g.getFont().deriveFont(Font.BOLD, 24f)
            );

            String title =
                    "Bounce Escape";

            int titleWidth =
                    g.getFontMetrics().stringWidth(title);

            g.drawString(title, (WIDTH - titleWidth) / 2, 120);

            g.setFont(
                    g.getFont().deriveFont(Font.PLAIN, 14f)
            );

            String line1 =
                    "Enter 키를 눌러 게임을 시작합니다.";

            int l1w =
                    g.getFontMetrics().stringWidth(line1);

            g.drawString(line1, (WIDTH - l1w) / 2, 180);
        }

        private void renderDebugInfo(Graphics2D g) {

            g.setFont(new Font("Monospaced", Font.PLAIN, 12));
            g.setColor(new Color(0, 0, 0, 180));

            int x  = WIDTH - 240;
            int y  = 20;
            int dy = 16;

            g.drawString(String.format("FPS: %.1f", currentFps), x, y);
            y += dy;

            if (player != null) {

                g.drawString(String.format("Player: (%.1f, %.1f)", player.getX(), player.getY()), x, y);
                y += dy;

                g.drawString(String.format("VelY: %.1f", player.getVelY()), x, y);
                y += dy;

                g.drawString("OnGround: " + player.isOnGround(), x, y);
                y += dy;

                g.drawString("Form: " + player.getForm(), x, y);
                y += dy;
            }

            g.drawString("State: " + state, x, y);
            y += dy;

            g.drawString("Console: " + (consoleOpen ? "OPEN" : "CLOSED"), x, y);
            y += dy;
        }

        private void renderTutorialHintOverlay(Graphics2D g) {

            if (currentStageIndex != 0) return;

            if (tutHintText == null) return;

            int w = 360;
            int h = 46;

            int x = (WIDTH - w) / 2;
            int y = 90;

            g.setColor(new Color(0, 0, 0, 170));
            g.fillRoundRect(x, y, w, h, 14, 14);

            g.setColor(new Color(255, 255, 180));
            g.setFont(new Font("SansSerif", Font.BOLD, 16));

            int tw = g.getFontMetrics().stringWidth(tutHintText);

            g.drawString(tutHintText, x + (w - tw) / 2, y + 30);
        }

        private void renderConsoleOverlay(Graphics2D g) {

            if (!consoleOpen) {
                return;
            }

            int x = 14;
            int y = 110;

            int w = 330;
            int h = 190;

            g.setColor(new Color(10, 10, 10, 200));
            g.fillRoundRect(x, y, w, h, 14, 14);

            g.setColor(new Color(255, 255, 255, 220));
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.drawString("DEBUG CONSOLE (F 토글)", x + 14, y + 22);

            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            int yy = y + 44;

            g.drawString("0~3 : 스테이지 이동", x + 14, yy); yy += 18;
            g.drawString("I   : 무적 토글 (" + (invincible ? "ON" : "OFF") + ")", x + 14, yy); yy += 18;
            g.drawString("[ ] : 속도 배수 " + String.format("%.2f", speedMul), x + 14, yy); yy += 18;
            g.drawString("- = : 중력 배수 " + String.format("%.2f", gravityMul), x + 14, yy); yy += 18;
            g.drawString("C   : 히트박스 (" + (debugDrawHitbox ? "ON" : "OFF") + ")", x + 14, yy); yy += 18;
            g.drawString("V   : 디버그정보 (" + (debugShowInfo ? "ON" : "OFF") + ")", x + 14, yy); yy += 18;
            g.drawString("Q   : 즉시 퀴즈(현 스테이지)", x + 14, yy); yy += 18;

            g.setColor(new Color(255, 255, 120, 220));
            g.drawString("콘솔 열림: 이동/점프 입력 일시정지", x + 14, y + h - 18);
        }

        public void initPausePanel(JPanel mainContainer) {

            this.pausePanel =
                    new PausePanel(this, mainContainer);

            this.pausePanel.setBounds(0, 0, WIDTH, HEIGHT);

            this.pausePanel.setVisible(false);

            add(this.pausePanel);
        }

        public void pauseGame() {

            if (state == GameState.STAGE_PLAY || state == GameState.TUTORIAL) {

                lastPlayState =
                        state;

                state =
                        GameState.PAUSE;

                pausePanel.setVisible(true);

                pausePanel.requestFocusInWindow();

                revalidate();

                StageInfo info =
                        stageInfos[currentStageIndex];

                if (info != null) {
                    info.pauseTimer();
                }
            }
        }

        public void resumeGame() {

            if (state == GameState.PAUSE) {

                state =
                        lastPlayState;

                pausePanel.setVisible(false);

                requestFocusInWindow();

                revalidate();

                StageInfo info =
                        stageInfos[currentStageIndex];

                if (info != null) {
                    info.resumeTimer();
                }
            }
        }

        public void resetStateToTutorial() {

            this.state =
                    GameState.TUTORIAL;

            if (this.pausePanel != null) {
                this.pausePanel.setVisible(false);
            }

            this.phase2CurrentCharIndex = 0;
            this.phase2TypingTimer      = 0.0;

            this.phase2TypingSoundPlaying = false;

            for (int i = 0; i < stageInfos.length; i++) {

                if (stageInfos[i] == null) continue;

                stageInfos[i].resetForRetry(stageInfos[i].getTotalStars());
            }
        }

        public void stopGameThread() {

            running =
                    false;

            gameThread =
                    null;
        }

        private void updateEndingTyping(double dt) {

            StageInfo info =
                    stageInfos[currentStageIndex];

            if (info == null) {
                return;
            }

            if (!info.isTypingFinished()) {

                double newTimer =
                        info.getTypingTimer() + dt;

                info.setTypingTimer(newTimer);

                int currentLine =
                        info.getCurrentLineIndex();

                double textSpeed =
                        info.getTextDisplaySpeed();

                boolean endingResult =
                        (currentStageIndex == 3)
                                ? finalResultGood
                                : lastAnswerCorrect;

                String[] allLines =
                        StageStory.getEndingLines(currentStageIndex, endingResult);

                if (currentLine >= allLines.length) {
                    info.finishTypingAndCheckEnd();
                    return;
                }

                int totalCharsInLine =
                        allLines[currentLine].length();

                int targetCharCount =
                        (int) (info.getTypingTimer() / textSpeed);

                if (targetCharCount >= totalCharsInLine) {

                    info.setCurrentCharIndex(totalCharsInLine);

                    if (currentLine < allLines.length - 1) {

                        if (info.getTypingTimer() >= totalCharsInLine * textSpeed + 0.05) {

                            info.setCurrentLineIndex(currentLine + 1);

                            info.setTypingTimer(0.0);

                            info.setCurrentCharIndex(0);
                        }

                    } else {

                        if (info.getTypingTimer() >= totalCharsInLine * textSpeed + 0.05) {

                            if (currentStageIndex != 3) {
                                info.finishTypingAndCheckEnd();
                            }
                        }
                    }

                } else {

                    info.setCurrentCharIndex(targetCharCount);
                }

                return;
            }

            if (currentStageIndex != 3) return;

            if (finalEndingPhase2Text == null) return;

            int totalChars =
                    finalEndingPhase2Text.length();

            if (!phase2TypingSoundPlaying && phase2CurrentCharIndex == 0) {

                if (GameCore.getSoundManager() != null) {

                    GameCore.getSoundManager().stopBGM();

                    GameCore.getSoundManager().playBGM(SoundManager.SFX_TYPING);

                    phase2TypingSoundPlaying = true;
                }
            }

            if (phase2CurrentCharIndex < totalChars) {

                phase2TypingTimer += dt;

                int charsToDisplay =
                        (int) (phase2TypingTimer / phase2TextDisplaySpeed);

                phase2CurrentCharIndex =
                        Math.min(charsToDisplay, totalChars);
            }

            if (phase2TypingSoundPlaying && phase2CurrentCharIndex >= totalChars) {

                if (GameCore.getSoundManager() != null) {
                    GameCore.getSoundManager().stopBGM();
                }

                phase2TypingSoundPlaying =
                        false;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {

            super.paintComponent(g);

            if (backBuffer != null) {
                g.drawImage(backBuffer, 0, 0, null);
            }
        }

        private void showTutHint(String msg) {

            tutHintText =
                    msg;

            tutHintTimer =
                    0.0;
        }

        private void updateTutorialHints(double dt) {

            if (currentStageIndex != 0) {
                return;
            }

            if (!tutMovedOnce && !tutShownMove) {

                showTutHint("A/D로 이동");

                tutShownMove = true;
            }

            if (tutHintText != null) {

                tutHintTimer += dt;

                if (tutHintTimer >= TUT_HINT_DURATION) {
                    tutHintText = null;
                }
            }
        }

        private void onGemPickedTutorialHint() {

            if (currentStageIndex != 0) return;

            if (tutShownGem) return;

            showTutHint("보석 획득! Space로 사용");

            tutShownGem = true;
        }

        private void toggleConsole() {

            consoleOpen =
                    !consoleOpen;

            if (consoleOpen) {

                leftPressed  = false;
                rightPressed = false;
            }
        }

        private void adjustSpeed(double delta) {

            speedMul =
                    clamp(speedMul + delta, 0.25, 3.00);
        }

        private void adjustGravity(double delta) {

            gravityMul =
                    clamp(gravityMul + delta, 0.25, 3.00);
        }

        private double clamp(double v, double lo, double hi) {
            return Math.max(lo, Math.min(hi, v));
        }

        private int clampInt(int v, int lo, int hi) {
            return Math.max(lo, Math.min(hi, v));
        }

        @Override
        public void keyPressed(KeyEvent e) {

            int code =
                    e.getKeyCode();

            if (code == keys.CONSOLE_TOGGLE) {
                toggleConsole();
                return;
            }

            if (consoleOpen) {
                if (handleConsoleKeys(code)) {
                    return;
                }
            }

            if (state == GameState.INTRO) {
                handleIntroKeys(code);
                return;
            }

            if (code == keys.PAUSE_TOGGLE) {
                handlePauseToggle();
                return;
            }

            if (state == GameState.ENDING) {
                if (code == keys.CONFIRM) {
                    goToNextStageOrFinishGame();
                }
                return;
            }

            if (state == GameState.QUESTION) {
                handleQuestionKeys(code);
                return;
            }

            if (code == keys.RESET_STAGE) {
                resetCurrentStage();
                return;
            }

            handleMovementAbilityKeys(code);
        }

        private boolean handleConsoleKeys(int code) {

            if (code >= KeyEvent.VK_0 && code <= KeyEvent.VK_3) {

                int st =
                        code - KeyEvent.VK_0;

                loadStage(st);

                return true;
            }

            if (code == keys.INVINCIBLE_TOGGLE) {
                invincible = !invincible;
                return true;
            }

            if (code == keys.HITBOX_TOGGLE) {
                debugDrawHitbox = !debugDrawHitbox;
                return true;
            }

            if (code == keys.DEBUGINFO_TOGGLE) {
                debugShowInfo = !debugShowInfo;
                return true;
            }

            if (code == keys.SPEED_DOWN) {
                adjustSpeed(-0.10);
                return true;
            }

            if (code == keys.SPEED_UP) {
                adjustSpeed(+0.10);
                return true;
            }

            if (code == keys.GRAV_DOWN) {
                adjustGravity(-0.10);
                return true;
            }

            if (code == keys.GRAV_UP) {
                adjustGravity(+0.10);
                return true;
            }

            if (code == keys.FORCE_QUIZ) {

                if (state == GameState.TUTORIAL || state == GameState.STAGE_PLAY) {
                    enterQuestionState();
                }

                return true;
            }

            return false;
        }

        private void handleIntroKeys(int code) {

            if (code != keys.CONFIRM) {
                return;
            }

            if (introManager.advanceLine()) {
                if (introManager.isStoryEnd()) {
                    loadStage(0);
                    state = GameState.TUTORIAL;
                }
            }
        }

        private void handlePauseToggle() {

            if (state == GameState.PAUSE) {
                resumeGame();
                return;
            }

            if (state == GameState.TUTORIAL || state == GameState.STAGE_PLAY) {
                pauseGame();
            }
        }

        private void handleQuestionKeys(int code) {

            int pick =
                    -1;

            if (code == KeyEvent.VK_1) pick = 0;
            if (code == KeyEvent.VK_2) pick = 1;
            if (code == KeyEvent.VK_3) pick = 2;
            if (code == KeyEvent.VK_4) pick = 3;

            if (pick == -1) {
                return;
            }

            boolean correct =
                    (currentQuiz != null && currentQuiz.isCorrect(pick));

            onQuestionAnswered(correct);
        }

        private void handleMovementAbilityKeys(int code) {

            if (code == keys.MOVE_LEFT) {

                leftPressed = true;

                if (currentStageIndex == 0 && !tutMovedOnce) {
                    tutMovedOnce = true;
                    tutHintText  = null;
                }

                return;
            }

            if (code == keys.MOVE_RIGHT) {

                rightPressed = true;

                if (currentStageIndex == 0 && !tutMovedOnce) {
                    tutMovedOnce = true;
                    tutHintText  = null;
                }

                return;
            }

            if (code == keys.ABILITY) {

                if (player != null && (state == GameState.TUTORIAL || state == GameState.STAGE_PLAY)) {
                    player.useAbility();
                }
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {

            int code =
                    e.getKeyCode();

            if (code == keys.MOVE_LEFT) {
                leftPressed = false;
                return;
            }

            if (code == keys.MOVE_RIGHT) {
                rightPressed = false;
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }

        private static class KeyMap {

            final int MOVE_LEFT  = KeyEvent.VK_A;
            final int MOVE_RIGHT = KeyEvent.VK_D;

            final int ABILITY    = KeyEvent.VK_SPACE;

            final int RESET_STAGE = KeyEvent.VK_R;

            final int CONFIRM     = KeyEvent.VK_ENTER;

            final int PAUSE_TOGGLE = KeyEvent.VK_ESCAPE;

            final int CONSOLE_TOGGLE = KeyEvent.VK_F;

            final int INVINCIBLE_TOGGLE = KeyEvent.VK_I;

            final int HITBOX_TOGGLE = KeyEvent.VK_C;

            final int DEBUGINFO_TOGGLE = KeyEvent.VK_V;

            final int SPEED_DOWN = KeyEvent.VK_OPEN_BRACKET;
            final int SPEED_UP   = KeyEvent.VK_CLOSE_BRACKET;

            final int GRAV_DOWN  = KeyEvent.VK_MINUS;
            final int GRAV_UP    = KeyEvent.VK_EQUALS;

            final int FORCE_QUIZ = KeyEvent.VK_Q;
        }

        private static class Gear {

            enum Axis {
                HORIZONTAL,
                VERTICAL
            }

            final double baseX;
            final double baseY;

            final Axis axis;

            final double amp;
            final double speed;

            double t = 0.0;

            double x;
            double y;

            private Gear(double baseX, double baseY, Axis axis, double amp, double speed) {

                this.baseX  = baseX;
                this.baseY  = baseY;

                this.axis   = axis;

                this.amp    = amp;
                this.speed  = speed;

                this.x      = baseX;
                this.y      = baseY;
            }

            static Gear horizontal(double baseX, double baseY, double amp, double speed) {

                return new Gear(baseX, baseY, Axis.HORIZONTAL, amp, speed);
            }

            static Gear vertical(double baseX, double baseY, double amp, double speed) {

                return new Gear(baseX, baseY, Axis.VERTICAL, amp, speed);
            }

            void update(double dt) {

                t += dt;

                double offset =
                        Math.sin(t * speed) * amp;

                if (axis == Axis.HORIZONTAL) {

                    x = baseX + offset;
                    y = baseY;

                } else {

                    x = baseX;
                    y = baseY + offset;
                }
            }

            double getRadius() {

                return TILE_SIZE * 0.42;
            }

            boolean collidesWith(Rectangle pr) {

                double cx =
                        x + TILE_SIZE / 2.0;

                double cy =
                        y + TILE_SIZE / 2.0;

                double r =
                        getRadius();

                double nearestX =
                        Math.max(pr.x, Math.min(cx, pr.x + pr.width));

                double nearestY =
                        Math.max(pr.y, Math.min(cy, pr.y + pr.height));

                double dx =
                        cx - nearestX;

                double dy =
                        cy - nearestY;

                return (dx * dx + dy * dy) <= (r * r);
            }
        }
    }
}