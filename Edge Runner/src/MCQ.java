/**
 * 객관식 문제(4지선다) 데이터 구조
 * - 문제 문장(question)
 * - 보기(choices[4])
 * - 정답 인덱스(answerIndex) : 0~3
 */
public class MCQ {

    private final String question;
    private final String[] choices;   // length = 4
    private final int answerIndex;    // 0~3

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

    public String getQuestion() {
        return question;
    }

    public String[] getChoices() {
        return choices;
    }

    public int getAnswerIndex() {
        return answerIndex;
    }

    /** pick(0~3)이 정답인지 여부 */
    public boolean isCorrect(int pick) {
        return pick == answerIndex;
    }
}
