import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MCQBank {

    // 스테이지별 문제 저장소
    private static final List<MCQ> STAGE1 = new ArrayList<>();
    private static final List<MCQ> STAGE2 = new ArrayList<>();
    private static final List<MCQ> STAGE3 = new ArrayList<>();

    // 문제 랜덤 선택용
    private static final Random random = new Random();

    // 클래스 로딩 시 문제 초기화
    static {
        initStage1();
        initStage2();
        initStage3();
    }

    // 스테이지 1 문제 초기화 (제어 유니트 / 마이크로연산 기초)
    private static void initStage1() {

        STAGE1.add(new MCQ(
                "제어 유니트(control unit)의 가장 기본적인 기능은 무엇인가?",
                "기억장치 용량을 확장한다",
                "명령어를 해독하고 제어 신호를 발생시킨다",
                "산술 논리 연산을 수행한다",
                "데이터를 캐시에 저장한다",
                1
        ));

        STAGE1.add(new MCQ(
                "마이크로명령어(micro-instruction)에 대한 설명으로 옳은 것은?",
                "하나의 기계어 명령어를 의미한다",
                "명령어 사이클의 각 단계에서 수행될 마이크로연산을 지정한다",
                "고급 언어로 작성된다",
                "주기억장치에 저장된다",
                1
        ));
    }

    // 스테이지 2 문제 초기화 (기억장치 / 디스크 / 캐시 개념)
    private static void initStage2() {

        STAGE2.add(new MCQ(
                "디스크 액세스 시간에 포함되지 않는 요소는?",
                "탐색 시간(seek time)",
                "회전 지연 시간(rotational latency)",
                "데이터 전송 시간",
                "캐시 일관성 유지 시간",
                3
        ));

        STAGE2.add(new MCQ(
                "캐시의 지역성(locality) 중 시간 지역성에 대한 설명으로 가장 적절한 것은?",
                "서로 인접한 주소가 함께 접근되는 특성",
                "최근에 접근한 데이터가 다시 접근될 가능성이 높은 특성",
                "여러 장치가 동시에 접근하는 특성",
                "데이터와 명령어가 분리되는 특성",
                1
        ));
    }

    // 스테이지 3 문제 초기화 (시스템 버스 / 인터럽트 / DMA)
    private static void initStage3() {

        STAGE3.add(new MCQ(
                "DMA(Direct Memory Access) 방식의 가장 큰 장점은 무엇인가?",
                "CPU가 모든 데이터 전송을 직접 제어한다",
                "CPU 개입 없이 기억장치와 I/O 장치 간 데이터 전송이 가능하다",
                "버스 중재가 필요 없다",
                "인터럽트를 사용하지 않는다",
                1
        ));

        STAGE3.add(new MCQ(
                "폴링 방식과 비교했을 때 인터럽트 방식의 특징으로 옳은 것은?",
                "CPU가 주기적으로 장치 상태를 검사한다",
                "장치가 준비되었을 때 CPU에 이벤트로 알린다",
                "구현이 항상 더 단순하다",
                "동시성 문제가 발생하지 않는다",
                1
        ));
    }

    /**
     * stageIndex에 맞는 문제를 무작위로 1개 반환
     *  - stageIndex == 1 : STAGE1
     *  - stageIndex == 2 : STAGE2
     *  - 그 외           : STAGE3
     */
    public static MCQ pick(int stageIndex) {

        List<MCQ> list =
                (stageIndex == 1) ? STAGE1 :
                (stageIndex == 2) ? STAGE2 :
                STAGE3;

        if (list.isEmpty()) {
            return null;
        }

        return list.get(random.nextInt(list.size()));
    }
}