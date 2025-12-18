import java.util.Random;

public class QuestionBox {

    public static class Quiz {
        public final String text;
        public final boolean answerO;

        public Quiz(String text, boolean answerO) {
            this.text = text;
            this.answerO = answerO;
        }
    }

    // 스테이지별 문제(예시)
    private static final Quiz[][] QUIZZES = new Quiz[][]{
            // stage 0(튜토리얼) - 사실상 안씀
            {
                    new Quiz("튜토리얼: O는 참, X는 거짓이다.", true),
                    new Quiz("튜토리얼: Java는 운영체제다.", false)
            },
            // stage 1
            {
                    new Quiz("CPU는 명령어를 해석하고 실행한다.", true),
                    new Quiz("레지스터는 보조기억장치이다.", false)
            },
            // stage 2
            {
                    new Quiz("캐시는 메모리 접근을 빠르게 해준다.", true),
                    new Quiz("RAM은 전원이 꺼져도 데이터가 유지된다.", false)
            },
            // stage 3
            {
                    new Quiz("I/O는 입력과 출력을 의미한다.", true),
                    new Quiz("네트워크 카드는 CPU 내부에만 존재한다.", false)
            }
    };

    private static final Random R = new Random();
    private static final int[] lastPick = new int[]{-1, -1, -1, -1};

    public static Quiz pick(int stageIndex) {
        if (stageIndex < 0 || stageIndex >= QUIZZES.length) stageIndex = 1;
        Quiz[] arr = QUIZZES[stageIndex];
        if (arr == null || arr.length == 0) return new Quiz("문제가 없습니다.", true);

        int idx = R.nextInt(arr.length);
        if (arr.length > 1 && idx == lastPick[stageIndex]) {
            idx = (idx + 1) % arr.length;
        }
        lastPick[stageIndex] = idx;
        return arr[idx];
    }
}
