import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 스테이지별 문제 은행
 * - stageIndex: 1,2,3에 맞춰 문제를 랜덤 반환
 * - stageIndex가 1/2가 아니면 기본적으로 STAGE3 반환(너 코드 흐름 유지)
 */
public class MCQBank {

    private static final List<MCQ> STAGE1 = new ArrayList<>();
    private static final List<MCQ> STAGE2 = new ArrayList<>();
    private static final List<MCQ> STAGE3 = new ArrayList<>();

    private static final Random random = new Random();

    static {
        initStage1();
        initStage2();
        initStage3();
    }

    private static void initStage1() {
        // CPU / 파이프라인 / 분기
        STAGE1.add(new MCQ(
                "파이프라이닝의 주요 목적은 무엇인가?",
                "클록 주파수를 낮춘다",
                "명령어 처리량(throughput)을 높인다",
                "메모리 용량을 늘린다",
                "캐시 미스를 줄인다",
                1
        ));

        STAGE1.add(new MCQ(
                "분기 예측 실패 시 흔히 발생하는 현상은?",
                "캐시 라인 잠금",
                "파이프라인 플러시/스톨",
                "TLB 엔트리 증가",
                "DMA 속도 저하",
                1
        ));
    }

    private static void initStage2() {
        // 캐시 / 메모리
        STAGE2.add(new MCQ(
                "캐시의 지역성(locality) 중 '최근 접근한 데이터에 다시 접근'은?",
                "공간 지역성",
                "시간 지역성",
                "참조 지역성",
                "동시 지역성",
                1
        ));

        STAGE2.add(new MCQ(
                "세트-연관 캐시에서 충돌 미스를 완화하는 가장 직접적인 방법은?",
                "블록 크기를 줄인다",
                "연관도(way)를 늘린다",
                "메모리 대역폭을 줄인다",
                "워드 크기를 늘린다",
                1
        ));
    }

    private static void initStage3() {
        // I/O / 인터럽트 / DMA
        STAGE3.add(new MCQ(
                "DMA의 장점으로 가장 적절한 것은?",
                "CPU가 모든 바이트 전송을 직접 수행한다",
                "CPU 개입을 줄여 대용량 전송 효율을 높인다",
                "캐시 적중률을 항상 증가시킨다",
                "분기 예측 정확도를 높인다",
                1
        ));

        STAGE3.add(new MCQ(
                "폴링 방식과 비교한 인터럽트 방식의 특징으로 적절한 것은?",
                "항상 더 단순한 구현이다",
                "장치가 준비될 때 CPU에 이벤트로 알린다",
                "CPU가 주기적으로 장치 상태를 읽는다",
                "동시성 문제가 없다",
                1
        ));
    }

    /**
     * stageIndex에 맞는 문제를 랜덤으로 1개 반환
     * - stageIndex == 1 -> STAGE1
     * - stageIndex == 2 -> STAGE2
     * - 그 외 -> STAGE3
     */
    public static MCQ pick(int stageIndex) {
        List<MCQ> list =
                (stageIndex == 1) ? STAGE1 :
                (stageIndex == 2) ? STAGE2 :
                STAGE3;

        if (list.isEmpty()) return null;
        return list.get(random.nextInt(list.size()));
    }
}
