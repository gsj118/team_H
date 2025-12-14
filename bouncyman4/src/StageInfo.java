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
}