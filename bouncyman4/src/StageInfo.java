	public class StageInfo {
	
	    private final int stageIndex;
	    private final String stageName;
	
	    private int totalStars;
	    private int collectedStars;
	    private int deathCount;
	
	    private long startTimeMs;
	    private long endTimeMs;
	
	    private boolean questionCorrect = false;
	
	    public StageInfo(int stageIndex, String stageName, int totalStars) {
	        this.stageIndex = stageIndex;
	        this.stageName = stageName;
	        this.totalStars = totalStars;
	        this.collectedStars = 0;
	        this.deathCount = 0;
	    }
	
	    public void startStage() {
	        startTimeMs = System.currentTimeMillis();
	        endTimeMs = 0;
	    }
	
	    public void finishStageNow() {
	        endTimeMs = System.currentTimeMillis();
	    }
	
	    public void resetForRetry(int totalStars) {
	        this.totalStars = totalStars;
	        this.collectedStars = 0;
	        this.questionCorrect = false;
	        // deathCount는 유지할지/초기화할지 팀 규칙에 따라: 일단 유지 안 바꿈
	        startStage();
	    }
	
	    public void incrementCollectedStars() {
	        collectedStars++;
	    }
	
	    public void incrementDeathCount() {
	        deathCount++;
	    }
	
	    public void setQuestionAnswered(boolean correct) {
	        this.questionCorrect = correct;
	    }
	
	    public String getStageName() { return stageName; }
	    public int getDeathCount() { return deathCount; }
	    public int getCollectedStars() { return collectedStars; }
	    public int getTotalStars() { return totalStars; }
	
	    public double getClearTimeSeconds() {
	        long end = (endTimeMs == 0 ? System.currentTimeMillis() : endTimeMs);
	        return (end - startTimeMs) / 1000.0;
	    }
	}
