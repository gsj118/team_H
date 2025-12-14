import java.util.Random;

/**
 * QuestionBox
 * - 스테이지별 O/X 문제 제공
 * - 1번 = O(참), 2번 = X(거짓)
 *
 * 스테이지 1~3은 "2문제"만 두고,
 *  매번 랜덤으로 뽑되, 같은 문제 연속 출제는 피함.
 */
//===== 퀴즈(QuestionBox) =====
//스테이지마다 1문제씩(O/X) 내기 위한 데이터/상태
//- currentQuiz : 현재 스테이지에서 문제 1개 랜덤 (2개 중 번갈아)
//- waitingForAnswer : 문제 화면(QUESTION)에서 입력을 기다리는 중인지
//- lastAnswerCorrect : 엔딩/연출 분기용 (O/X -> 1 or 2키)
public class QuestionBox {

    public static class Quiz {
        public final String text;
        public final boolean answerO; // true=O, false=X

        public Quiz(String text, boolean answerO) {
            this.text = text;
            this.answerO = answerO;
        }
    }

    private static final Random R = new Random();

    // stageIndex: 0=튜토리얼(문제 없음), 1~3만 사용

    private static final Quiz[] CPU_CORE = {
            // O / X 섞기 (2개만)
            new Quiz("파이프라인은 명령을 단계별로 나눠 겹쳐 처리해서 처리량을 늘린다.", true),
            new Quiz("레지스터는 하드디스크처럼 전원이 꺼져도 내용이 그대로 남는다.", false)
    };

    private static final Quiz[] MEM_CACHE = {
            new Quiz("캐시는 자주 쓰는 데이터를 CPU 가까이에 두어 평균 접근 시간을 줄인다.", true),
            new Quiz("캐시가 있으면 RAM(메인 메모리)은 더 이상 필요 없다.", false)
    };

    private static final Quiz[] IO_DEFENSE = {
            new Quiz("인터럽트가 오면 CPU는 처리 후 원래 하던 일로 돌아갈 수 있다.", true),
            new Quiz("DMA는 데이터를 옮길 때 항상 CPU가 한 바이트씩 직접 옮기는 방식이다.", false)
    };

    // (추가) 스테이지별 마지막으로 뽑은 인덱스 저장 -> 연속 중복 방지
    private static int lastPickStage1 = -1;
    private static int lastPickStage2 = -1;
    private static int lastPickStage3 = -1;

    /** 스테이지별로 문제 1개 뽑기 (연속 중복 방지) */
    public static Quiz pick(int stageIndex) {
        Quiz[] pool;
        int last;

        switch (stageIndex) {
            case 1:
                pool = CPU_CORE;
                last = lastPickStage1;
                break;
            case 2:
                pool = MEM_CACHE;
                last = lastPickStage2;
                break;
            case 3:
                pool = IO_DEFENSE;
                last = lastPickStage3;
                break;
            default:
                return new Quiz("튜토리얼은 문제 없이 다음 스테이지로 넘어갑니다.", true);
        }

        // 2개 중 랜덤으로 뽑되, 직전과 같으면 다른 걸로
        int pick = R.nextInt(pool.length);
        if (pool.length >= 2 && pick == last) {
            pick = 1 - pick; // 0<->1 (지금은 2개만 쓰니까)
        }

        // 마지막 출제 기록 갱신
        if (stageIndex == 1) lastPickStage1 = pick;
        if (stageIndex == 2) lastPickStage2 = pick;
        if (stageIndex == 3) lastPickStage3 = pick;

        return pool[pick];
    }
}
