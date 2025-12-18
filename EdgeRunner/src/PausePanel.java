import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PausePanel extends JPanel implements ActionListener {

    // 게임 패널 참조 (재개/중지 제어용)
    private final GameCore.GamePanel gamePanel;

    // 카드 레이아웃을 관리하는 메인 컨테이너
    private final JPanel mainContainer;

    // 화면 크기 상수
    private static final int WIDTH  =
            GameCore.GamePanel.WIDTH;

    private static final int HEIGHT =
            GameCore.GamePanel.HEIGHT;

    public PausePanel(GameCore.GamePanel gamePanel,
                      JPanel mainContainer) {

        this.gamePanel = gamePanel;
        this.mainContainer = mainContainer;

        initializePanel();
        initializeComponents();
    }

    // 패널 기본 설정 (반투명 오버레이)
    private void initializePanel() {

        setBackground(new Color(0, 0, 0, 180));
        setOpaque(false);
        setPreferredSize(
                new Dimension(WIDTH, HEIGHT)
        );

        setLayout(new GridBagLayout());
    }

    // 제목 및 버튼 컴포넌트 생성 및 배치
    private void initializeComponents() {

        JLabel titleLabel =
                createTitleLabel();

        JButton resumeButton =
                createButton(
                        "게임 재개",
                        "RESUME"
                );

        JButton menuButton =
                createButton(
                        "메인 메뉴",
                        "GO_TO_MENU"
                );

        JButton exitButton =
                createButton(
                        "게임 종료",
                        "EXIT"
                );

        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.insets =
                new Insets(15, 0, 15, 0);

        gbc.gridy = 0;
        gbc.insets =
                new Insets(0, 0, 40, 0);
        add(titleLabel, gbc);

        gbc.gridy = 1;
        gbc.insets =
                new Insets(10, 0, 10, 0);
        add(resumeButton, gbc);

        gbc.gridy = 2;
        add(menuButton, gbc);

        gbc.gridy = 3;
        add(exitButton, gbc);
    }

    // PAUSED 타이틀 라벨 생성
    private JLabel createTitleLabel() {

        JLabel label =
                new JLabel("PAUSED");

        label.setForeground(Color.WHITE);
        label.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        48
                )
        );

        return label;
    }

    // 공통 버튼 생성 메서드
    private JButton createButton(String text,
                                 String command) {

        JButton button =
                new JButton(text);

        button.setPreferredSize(
                new Dimension(220, 50)
        );

        button.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        18
                )
        );

        button.setActionCommand(command);
        button.addActionListener(this);

        return button;
    }

    // 버튼 클릭 이벤트 처리
    @Override
    public void actionPerformed(ActionEvent e) {

        String command =
                e.getActionCommand();

        CardLayout layout =
                (CardLayout) mainContainer.getLayout();

        if ("RESUME".equals(command)) {

            // 게임 재개
            gamePanel.resumeGame();
            return;
        }

        if ("GO_TO_MENU".equals(command)) {

            // 메인 메뉴 BGM 재생 및 게임 상태 초기화
            GameCore.getSoundManager()
                    .playBGM(
                            SoundManager.BGM_MAIN_MENU
                    );

            gamePanel.stopGameThread();
            gamePanel.resetStateToTutorial();

            layout.show(
                    mainContainer,
                    GameCore.CARD_MENU
            );

            return;
        }

        if ("EXIT".equals(command)) {

            // 프로그램 종료
            System.exit(0);
        }
    }

    // 반투명 배경 오버레이 그리기
    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        g.setColor(getBackground());
        g.fillRect(
                0,
                0,
                getWidth(),
                getHeight()
        );
    }
}