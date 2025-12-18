public class MCQ {

    // 문제 본문
    private final String question;

    // 선택지 목록 (항상 4개)
    private final String[] choices;

    // 정답 인덱스 (0~3)
    private final int answerIndex;

    // 객관식 문제 생성자
    public MCQ(String question,
               String c1,
               String c2,
               String c3,
               String c4,
               int answerIndex) {

        this.question = question;
        this.choices = new String[]{c1, c2, c3, c4};
        this.answerIndex = answerIndex;
    }

    // 문제 텍스트 반환
    public String getQuestion() {
        return question;
    }

    // 선택지 배열 반환
    public String[] getChoices() {
        return choices;
    }

    // 정답 인덱스 반환
    public int getAnswerIndex() {
        return answerIndex;
    }

    // 선택한 번호(pick)가 정답인지 판별
    public boolean isCorrect(int pick) {
        return pick == answerIndex;
    }
}