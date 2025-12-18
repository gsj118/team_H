public class IntroManager {

    // 인트로 대사 목록 (화자|대사 형식)
    private static final String[] INTRO_LINES = {

            "파일럿|비상 상황! 메인 동력이 완전히 꺼졌어.",
            "시스템|경고. 핵심 모듈 정지 상태. CPU, 메모리, I/O 모두 응답 없음.",

            "파일럿|나는 주 모듈로 귀환했네. 상황 파악 후 즉시 탈출을 준비해야 해.",
            "파일럿|지금 자네가 있는 곳은 부트 로더 구역이야.",

            "시스템|파일럿 요청 확인. 시스템 구조 담당자에게 복구 권한 이양.",

            "플레이어|좋아. 부트 로더부터 시작해서 시스템을 복구한다.",

            "시스템|각 구역을 성공적으로 복구하여 탈출 경로를 확보하십시오.",

            "|ENTER 키를 눌러 부트 시퀀스를 시작하십시오."
    };

    // 한 글자 출력 간격(초)
    private static final double TEXT_DISPLAY_SPEED = 0.05;

    // 타이핑 누적 시간
    private double typingTimer = 0.0;

    // 현재 출력 중인 줄 인덱스
    private int currentLineIndex = 0;

    // 현재 줄에서 출력된 문자 수
    private int currentCharIndex = 0;

    // 매 프레임 호출되는 업데이트
    public void update(double deltaTime) {

        if (isStoryEnd()) {
            return;
        }

        String currentLine = INTRO_LINES[currentLineIndex];

        if (isTypingInProgress(currentLine)) {
            typingTimer += deltaTime;
            processTyping(currentLine);
        }
    }

    // 현재 줄의 타이핑이 진행 중인지 여부
    private boolean isTypingInProgress(String line) {
        return currentCharIndex < line.length();
    }

    // 타이핑 속도에 맞춰 문자 출력 처리
    private void processTyping(String line) {

        while (typingTimer >= TEXT_DISPLAY_SPEED) {

            currentCharIndex++;
            typingTimer -= TEXT_DISPLAY_SPEED;

            if (currentCharIndex >= line.length()) {
                currentCharIndex = line.length();
                break;
            }
        }
    }

    // ENTER 입력 시 호출: 타이핑 스킵 또는 다음 줄 이동
    public boolean advanceLine() {

        if (isStoryEnd()) {
            return false;
        }

        String currentLine = INTRO_LINES[currentLineIndex];

        if (isTypingInProgress(currentLine)) {
            finishCurrentLineTyping();
            return true;
        } else {
            moveToNextLine();
            return true;
        }
    }

    // 현재 줄을 즉시 끝까지 출력
    private void finishCurrentLineTyping() {
        currentCharIndex = INTRO_LINES[currentLineIndex].length();
        typingTimer = 0.0;
    }

    // 다음 줄로 이동
    private void moveToNextLine() {
        currentLineIndex++;
        currentCharIndex = 0;
        typingTimer = 0.0;
    }

    // 모든 대사가 끝났는지 여부
    public boolean isStoryEnd() {
        return currentLineIndex >= INTRO_LINES.length;
    }

    // 현재 줄의 타이핑 완료 여부
    public boolean isTypingComplete() {

        if (isStoryEnd()) {
            return true;
        }

        return currentCharIndex >= INTRO_LINES[currentLineIndex].length();
    }

    // 현재 전체 줄 반환
    public String getCurrentLine() {

        if (isStoryEnd()) {
            return "";
        }

        return INTRO_LINES[currentLineIndex];
    }

    // 현재 출력 중인 문자 인덱스 반환
    public int getCurrentCharIndex() {
        return currentCharIndex;
    }

    // 화자 이름 추출
    public String getCurrentSpeaker() {

        String line = getCurrentLine();

        if (line.contains("|")) {
            return line.substring(0, line.indexOf("|")).trim();
        }

        return null;
    }

    // 대사 내용 추출
    public String getCurrentDialogue() {

        String line = getCurrentLine();

        if (line.contains("|")) {
            return line.substring(line.indexOf("|") + 1).trim();
        }

        return line.trim();
    }
}