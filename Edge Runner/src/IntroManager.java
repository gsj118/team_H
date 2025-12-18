public class IntroManager {

    // ===============================
    // Intro Script Data
    // ===============================

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

    // ===============================
    // Typing Configuration
    // ===============================

    private static final double TEXT_DISPLAY_SPEED =
            0.05;

    // ===============================
    // Typing State
    // ===============================

    private double typingTimer =
            0.0;

    private int currentLineIndex =
            0;

    private int currentCharIndex =
            0;

    // ===============================
    // Update Logic
    // ===============================

    public void update(double deltaTime) {

        if (isStoryEnd()) {
            return;
        }

        String currentLine =
                INTRO_LINES[currentLineIndex];

        if (isTypingInProgress(currentLine)) {

            typingTimer += deltaTime;

            processTyping(currentLine);
        }
    }

    private boolean isTypingInProgress(String line) {

        return currentCharIndex < line.length();
    }

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

    // ===============================
    // Input Handling
    // ===============================

    public boolean advanceLine() {

        if (isStoryEnd()) {
            return false;
        }

        String currentLine =
                INTRO_LINES[currentLineIndex];

        if (isTypingInProgress(currentLine)) {

            finishCurrentLineTyping();
            return true;

        } else {

            moveToNextLine();
            return true;
        }
    }

    private void finishCurrentLineTyping() {

        currentCharIndex =
                INTRO_LINES[currentLineIndex].length();

        typingTimer = 0.0;
    }

    private void moveToNextLine() {

        currentLineIndex++;
        currentCharIndex = 0;
        typingTimer = 0.0;
    }

    // ===============================
    // State Queries
    // ===============================

    public boolean isStoryEnd() {

        return currentLineIndex
                >= INTRO_LINES.length;
    }

    public boolean isTypingComplete() {

        if (isStoryEnd()) {
            return true;
        }

        return currentCharIndex
                >= INTRO_LINES[currentLineIndex].length();
    }

    // ===============================
    // Text Access
    // ===============================

    public String getCurrentLine() {

        if (isStoryEnd()) {
            return "";
        }

        return INTRO_LINES[currentLineIndex];
    }

    public int getCurrentCharIndex() {

        return currentCharIndex;
    }

    // ===============================
    // Speaker / Dialogue Parsing
    // ===============================

    public String getCurrentSpeaker() {

        String line =
                getCurrentLine();

        if (line.contains("|")) {

            return line
                    .substring(0, line.indexOf("|"))
                    .trim();
        }

        return null;
    }

    public String getCurrentDialogue() {

        String line =
                getCurrentLine();

        if (line.contains("|")) {

            return line
                    .substring(line.indexOf("|") + 1)
                    .trim();
        }

        return line.trim();
    }
}