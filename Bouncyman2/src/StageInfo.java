/**
 * 각 스테이지의 진행/결과 정보를 저장
 * - 수집해야 할 별 개수
 * - 실제 수집한 별 개수
 * - 사망 횟수
 * - 문제 정답 여부
 * - 클리어 시간
 *
 * 분석/리포트용 데이터 구조에 가깝고,
 * 실제 렌더링은 GameCore에서 담당한다.
 */
public class StageInfo {

    private final int stageIndex;
    private final String stageName;

    private int totalStars;
    private int collectedStars;
    private int deathCount;

    private boolean questionAnswered;
    private boolean answerCorrect;

    private long startTimeMillis;
    private long clearTimeMillis;
    
    // [추가] 타이핑 애니메이션 관련
    private double typingTimer;
    private final double textDisplaySpeed = 0.03; 
    private int currentLineIndex;
    private int currentCharIndex;

    public StageInfo(int stageIndex, String stageName, int totalStars) {
        this.stageIndex = stageIndex;
        this.stageName = stageName;
        this.totalStars = totalStars;
        this.collectedStars = 0;
        this.deathCount = 0;
        this.questionAnswered = false;
        this.answerCorrect = false;
        this.startTimeMillis = 0L;
        this.clearTimeMillis = 0L;
    }

    public void startStage() {
        this.collectedStars = 0;
        this.questionAnswered = false;
        this.answerCorrect = false;
        this.startTimeMillis = System.currentTimeMillis();
        this.clearTimeMillis = 0L;
        this.accumulatedPlayTimeMillis = 0L; // [추가] 누적 시간 초기화
    }

    public void resetForRetry(int totalStars) {
        this.totalStars = totalStars;
        this.collectedStars = 0;
        this.questionAnswered = false;
        this.answerCorrect = false;
        this.deathCount = 0;
        this.startTimeMillis = 0L;
        this.clearTimeMillis = 0L;
    }

    public void finishStageNow() {
        if (clearTimeMillis == 0L) {
            clearTimeMillis = System.currentTimeMillis();
        }
    }

    public double getClearTimeSeconds() {
        if (startTimeMillis == 0L || clearTimeMillis == 0L) {
            return 0.0;
        }
        long diff = clearTimeMillis - startTimeMillis;
        return diff / 1000.0;
    }

    public void incrementCollectedStars() {
        collectedStars++;
    }

    public void incrementDeathCount() {
        deathCount++;
    }

    public void setQuestionAnswered(boolean correct) {
        this.questionAnswered = true;
        this.answerCorrect = correct;
    }

    // Getter

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
    
    // [추가] startTimeMillis에 접근하기 위한 Getter
    public long getStartTimeMillis() {
        return startTimeMillis;
    }
    
    // [추가] Pause 전까지의 순수 플레이 시간을 누적할 필드
    private long accumulatedPlayTimeMillis;
    
    // ⭐️ [추가] 타이머 일시정지 메서드
    public void pauseTimer() {
        if (startTimeMillis != 0L) {
            // 현재까지의 시간을 누적 시간에 더하고, 시작 시점을 0으로 설정하여 정지 상태임을 표시
            accumulatedPlayTimeMillis += (System.currentTimeMillis() - startTimeMillis);
            startTimeMillis = 0L; 
        }
    }

    // [추가] 타이머 재개 메서드
    public void resumeTimer() {
        if (startTimeMillis == 0L) { // 타이머가 정지 상태일 때만
            // 새로운 시작 시점을 기록하여 다시 시간 측정을 시작
            startTimeMillis = System.currentTimeMillis();
        }
    }

    // [추가] 현재 누적된 경과 시간을 가져오는 Getter
    public long getElapsedPlayTimeMillis() {
        if (startTimeMillis == 0L) {
            // 정지 상태일 경우, 누적된 시간만 반환 (시간이 멈춘 상태)
            return accumulatedPlayTimeMillis;
        }
        // 실행 중일 경우: 누적 시간 + 현재 시점부터의 경과 시간을 더해 반환
        return accumulatedPlayTimeMillis + (System.currentTimeMillis() - startTimeMillis);
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
    
    // [추가] 세터(Setter) 메서드 추가
    
    public void setTypingTimer(double typingTimer) {
        this.typingTimer = typingTimer;
    }

    public void setCurrentLineIndex(int currentLineIndex) {
        this.currentLineIndex = currentLineIndex;
    }

    public void setCurrentCharIndex(int currentCharIndex) {
        this.currentCharIndex = currentCharIndex;
    }
    
    // [추가] 타이핑 변수 초기화 메서드
    public void resetTypingState() {
        this.typingTimer = 0.0;
        this.currentLineIndex = 0;
        this.currentCharIndex = 0;
    }
}