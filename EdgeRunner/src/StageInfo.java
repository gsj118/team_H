public class StageInfo {

    // 스테이지 기본 정보
    private final int stageIndex;
    private final String stageName;

    // 진행 정보
    private int totalStars;
    private int collectedStars;
    private int deathCount;

    // 문제 응답 상태
    private boolean questionAnswered;
    private boolean answerCorrect;

    // 시간 측정 관련
    private long startTimeMillis;
    private long clearTimeMillis;
    private long accumulatedPlayTimeMillis;

    // 텍스트 타이핑 효과 관련
    private double typingTimer;
    private final double textDisplaySpeed = 0.03;

    private int currentLineIndex;
    private int currentCharIndex;

    private boolean storyEnd = false;

    // 스토리 텍스트
    private String[] currentStoryLines;
    private String fullText;

    public StageInfo(int stageIndex, String stageName, int totalStars) {
        this.stageIndex = stageIndex;
        this.stageName = stageName;
        this.totalStars = totalStars;
    }

    // 스테이지 시작 시 상태 초기화
    public void startStage() {

        collectedStars = 0;
        questionAnswered = false;
        answerCorrect = false;

        startTimeMillis = System.currentTimeMillis();
        clearTimeMillis = 0L;
        accumulatedPlayTimeMillis = 0L;
    }

    // 재도전 시 스테이지 상태 초기화
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

    // 스테이지 즉시 종료 처리
    public void finishStageNow() {

        if (clearTimeMillis == 0L) {
            clearTimeMillis = System.currentTimeMillis();
        }
    }

    // 클리어 시간(초 단위) 반환
    public double getClearTimeSeconds() {

        if (startTimeMillis == 0L || clearTimeMillis == 0L) {
            return 0.0;
        }

        return (clearTimeMillis - startTimeMillis) / 1000.0;
    }

    // 타이머 일시 정지
    public void pauseTimer() {

        if (startTimeMillis != 0L) {
            accumulatedPlayTimeMillis +=
                    (System.currentTimeMillis() - startTimeMillis);

            startTimeMillis = 0L;
        }
    }

    // 타이머 재개
    public void resumeTimer() {

        if (startTimeMillis == 0L) {
            startTimeMillis = System.currentTimeMillis();
        }
    }

    // 현재까지의 총 플레이 시간 반환
    public long getElapsedPlayTimeMillis() {

        if (startTimeMillis == 0L) {
            return accumulatedPlayTimeMillis;
        }

        return accumulatedPlayTimeMillis
                + (System.currentTimeMillis() - startTimeMillis);
    }

    // 별 획득 수 증가
    public void incrementCollectedStars() {
        collectedStars++;
    }

    // 사망 횟수 증가
    public void incrementDeathCount() {
        deathCount++;
    }

    // 문제 응답 결과 기록
    public void setQuestionAnswered(boolean correct) {

        questionAnswered = true;
        answerCorrect = correct;
    }

    // 타이핑 상태 초기화
    public void resetTypingState() {

        typingTimer = 0.0;
        currentLineIndex = 0;
        currentCharIndex = 0;

        storyEnd = false;
    }

    // 모든 타이핑이 종료되었는지 여부
    public boolean isTypingFinished() {
        return storyEnd;
    }

    // 타이핑 즉시 종료 및 스토리 끝 처리
    public void finishTypingAndCheckEnd() {

        if (currentStoryLines != null && currentStoryLines.length > 0) {

            int lastLineIndex = currentStoryLines.length - 1;

            currentLineIndex = lastLineIndex;
            currentCharIndex =
                    currentStoryLines[lastLineIndex].length();
        }

        storyEnd = true;
    }

    // 전체 스토리 텍스트 설정
    public void setFullText(String text) {

        fullText = text;
        currentStoryLines = text.split("\n");
    }

    // ===== Getter =====
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

    // ===== Setter (타이핑 제어용) =====
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