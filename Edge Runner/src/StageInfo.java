public class StageInfo {

    // -------------------------------
    // Stage Identity
    // -------------------------------

    private final int stageIndex;
    private final String stageName;

    // -------------------------------
    // Progress / Result
    // -------------------------------

    private int totalStars;
    private int collectedStars;
    private int deathCount;

    private boolean questionAnswered;
    private boolean answerCorrect;

    // -------------------------------
    // Time Tracking
    // -------------------------------

    private long startTimeMillis;
    private long clearTimeMillis;
    private long accumulatedPlayTimeMillis;

    // -------------------------------
    // Typing / Story State
    // -------------------------------

    private double typingTimer;
    private final double textDisplaySpeed = 0.03;

    private int currentLineIndex;
    private int currentCharIndex;

    private boolean storyEnd = false;

    private String[] currentStoryLines;
    private String fullText;

    // ===============================
    // Constructor
    // ===============================

    public StageInfo(int stageIndex, String stageName, int totalStars) {
        this.stageIndex = stageIndex;
        this.stageName = stageName;
        this.totalStars = totalStars;
    }

    // ===============================
    // Stage Lifecycle
    // ===============================

    public void startStage() {

        collectedStars = 0;
        questionAnswered = false;
        answerCorrect = false;

        startTimeMillis = System.currentTimeMillis();
        clearTimeMillis = 0L;
        accumulatedPlayTimeMillis = 0L;
    }

    public void resetForRetry(int totalStars) {

        this.totalStars = totalStars;

        collectedStars = 0;
        deathCount = 0;

        questionAnswered = false;
        answerCorrect = false;

        startTimeMillis = 0L;
        clearTimeMillis = 0L;

        storyEnd = false;
        resetTypingState();
    }

    public void finishStageNow() {

        if (clearTimeMillis == 0L) {
            clearTimeMillis = System.currentTimeMillis();
        }
    }

    // ===============================
    // Time Calculation
    // ===============================

    public double getClearTimeSeconds() {

        if (startTimeMillis == 0L || clearTimeMillis == 0L) {
            return 0.0;
        }

        return (clearTimeMillis - startTimeMillis) / 1000.0;
    }

    public void pauseTimer() {

        if (startTimeMillis != 0L) {
            accumulatedPlayTimeMillis +=
                    (System.currentTimeMillis() - startTimeMillis);

            startTimeMillis = 0L;
        }
    }

    public void resumeTimer() {

        if (startTimeMillis == 0L) {
            startTimeMillis = System.currentTimeMillis();
        }
    }

    public long getElapsedPlayTimeMillis() {

        if (startTimeMillis == 0L) {
            return accumulatedPlayTimeMillis;
        }

        return accumulatedPlayTimeMillis
                + (System.currentTimeMillis() - startTimeMillis);
    }

    // ===============================
    // Progress Update
    // ===============================

    public void incrementCollectedStars() {
        collectedStars++;
    }

    public void incrementDeathCount() {
        deathCount++;
    }

    public void setQuestionAnswered(boolean correct) {

        questionAnswered = true;
        answerCorrect = correct;
    }

    // ===============================
    // Typing Control
    // ===============================

    public void resetTypingState() {

        typingTimer = 0.0;
        currentLineIndex = 0;
        currentCharIndex = 0;

        storyEnd = false;
    }

    public boolean isTypingFinished() {
        return storyEnd;
    }

    public void finishTypingAndCheckEnd() {

        if (currentStoryLines != null && currentStoryLines.length > 0) {

            int lastLineIndex = currentStoryLines.length - 1;

            currentLineIndex = lastLineIndex;
            currentCharIndex =
                    currentStoryLines[lastLineIndex].length();
        }

        storyEnd = true;
    }

    public void setFullText(String text) {

        fullText = text;
        currentStoryLines = text.split("\n");
    }

    // ===============================
    // Getters
    // ===============================

    public int getStageIndex() {
        return stageIndex;
    }

    public String getStageName() {
        return stageName;
    }

    public int getTotalStars() {
        return totalStars;
    }

    public int getCollectedStars() {
        return collectedStars;
    }

    public int getDeathCount() {
        return deathCount;
    }

    public boolean isQuestionAnswered() {
        return questionAnswered;
    }

    public boolean isAnswerCorrect() {
        return answerCorrect;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public double getTypingTimer() {
        return typingTimer;
    }

    public double getTextDisplaySpeed() {
        return textDisplaySpeed;
    }

    public int getCurrentLineIndex() {
        return currentLineIndex;
    }

    public int getCurrentCharIndex() {
        return currentCharIndex;
    }

    public String getFullText() {
        return fullText;
    }

    // ===============================
    // Setters
    // ===============================

    public void setTypingTimer(double value) {
        typingTimer = value;
    }

    public void setCurrentLineIndex(int value) {
        currentLineIndex = value;
    }

    public void setCurrentCharIndex(int value) {
        currentCharIndex = value;
    }
}