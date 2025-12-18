/**
 * 인트로 컷신 (스토리)의 텍스트 데이터와 타이핑 애니메이션 로직을 관리합니다.
 */
public class IntroManager {

    private static final String[] INTRO_LINES = new String[] {
    		"파일럿|비상 상황! 메인 동력이 완전히 꺼졌어.",
    	    "시스템|경고. 핵심 모듈 정지 상태. CPU, 메모리, I/O 모두 응답 없음.",
    	    
    	    "파일럿|나는 주 모듈로 귀환했네. 상황 파악 후 즉시 탈출을 준비해야 해.",
    	    "파일럿|지금 자네가 있는 곳은 부트 로더 구역이야.", 
    	    
    	    "시스템|파일럿 요청 확인. 시스템 구조 담당자에게 복구 권한 이양.",
    	    
    	    "플레이어|좋아. 부트 로더부터 시작해서 시스템을 복구한다.",
    	    
    	    "시스템|각 구역을 성공적으로 복구하여 탈출 경로를 확보하십시오.",
    	    
    	    "|ENTER 키를 눌러 부트 시퀀스를 시작하십시오."
    };

    private static final double TEXT_DISPLAY_SPEED = 0.05; // 한 글자 표시 속도 (0.05초)
    private double typingTimer = 0.0;
    private int currentLineIndex = 0;
    private int currentCharIndex = 0;

    /**
     * GameCore의 update(double dt)에서 호출: 타이핑 애니메이션을 진행합니다.
     */
    public void update(double dt) {
        if (isStoryEnd()) return;

        String currentLine = INTRO_LINES[currentLineIndex];
        
        // 타이핑이 아직 완료되지 않았으면 진행
        if (currentCharIndex < currentLine.length()) {
            typingTimer += dt;
            
            while (typingTimer >= TEXT_DISPLAY_SPEED) {
                currentCharIndex++;
                typingTimer -= TEXT_DISPLAY_SPEED;
                
                if (currentCharIndex >= currentLine.length()) {
                    currentCharIndex = currentLine.length();
                    break;
                }
            }
        }
    }

    /**
     * ENTER 키 입력 처리: 타이핑을 강제 완료시키거나 다음 줄로 넘어갑니다.
     * @return true: ENTER 처리가 완료되어 GameCore가 다음 액션을 취할 필요 없음
     */
    public boolean advanceLine() {
        if (isStoryEnd()) {
            return false; // 더 이상 진행할 스토리가 없으므로 GameCore가 상태 전환 필요
        }
        
        String currentLine = INTRO_LINES[currentLineIndex];
        
        if (currentCharIndex < currentLine.length()) {
            // 현재 줄 타이핑이 완료되지 않았으면 강제 완료
            currentCharIndex = currentLine.length();
            typingTimer = 0.0;
            return true;
        } else {
            // 현재 줄 타이핑이 완료되었으면 다음 줄로
            currentLineIndex++;
            currentCharIndex = 0;
            typingTimer = 0.0;
            return true;
        }
    }
    
    // --- Getter 메서드 ---
    
    public String getCurrentLine() {
        if (isStoryEnd()) return ""; 
        return INTRO_LINES[currentLineIndex];
    }
    
    public int getCurrentCharIndex() {
        return currentCharIndex;
    }
    
    public boolean isStoryEnd() {
        // 현재 줄 인덱스가 총 줄 수를 넘어서면 스토리 종료
        return currentLineIndex >= INTRO_LINES.length;
    }
    
    public boolean isTypingComplete() {
        if (isStoryEnd()) return true;
        
        return currentCharIndex >= INTRO_LINES[currentLineIndex].length();
    }
    
    /** 현재 라인의 화자 이름(태그)을 추출 */
    public String getCurrentSpeaker() {
        String currentLine = getCurrentLine();
        if (currentLine.contains("|")) {
            return currentLine.substring(0, currentLine.indexOf("|")).trim();
        }
        return null; // 화자 이름이 없는 경우
    }

    /** 현재 라인의 실제 대화 내용만 추출 */
    public String getCurrentDialogue() {
        String currentLine = getCurrentLine();
        if (currentLine.contains("|")) {
            // 화자 태그 뒤의 내용만 반환
            return currentLine.substring(currentLine.indexOf("|") + 1).trim();
        }
        return currentLine.trim();
    }
}