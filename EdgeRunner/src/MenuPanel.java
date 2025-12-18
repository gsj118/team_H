import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MenuPanel extends JPanel implements ActionListener {

    // 게임 화면 패널 참조
    private final GameCore.GamePanel gamePanel;

    // 카드 레이아웃을 관리하는 최상위 컨테이너
    private final JPanel mainContainer;

    public MenuPanel(GameCore.GamePanel gamePanel,
                     JPanel mainContainer) {

        this.gamePanel = gamePanel;
        this.mainContainer = mainContainer;

        initializePanel();
        initializeLayout();
    }

    // 패널 기본 설정
    private void initializePanel() {

        setBackground(new Color(20, 20, 40));
        setLayout(new BorderLayout());

        setPreferredSize(
                new Dimension(
                        GameCore.GamePanel.WIDTH,
                        GameCore.GamePanel.HEIGHT
                )
        );
    }

    // 전체 레이아웃 구성 (중앙 + 하단)
    private void initializeLayout() {

        JPanel centerPanel =
                createCenterPanel();

        add(centerPanel, BorderLayout.CENTER);

        JPanel footerPanel =
                createFooterPanel();

        add(footerPanel, BorderLayout.SOUTH);
    }

    // 제목 및 버튼이 배치된 중앙 패널
    private JPanel createCenterPanel() {

        JPanel panel =
                new JPanel(new GridBagLayout());

        panel.setOpaque(false);

        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.insets =
                new Insets(15, 0, 15, 0);

        JLabel titleLabel =
                createTitleLabel();

        gbc.gridy = 0;
        panel.add(titleLabel, gbc);

        JButton startButton =
                createButton(
                        "게임 시작",
                        "START"
                );

        gbc.gridy = 1;
        gbc.insets =
                new Insets(50, 0, 15, 0);
        panel.add(startButton, gbc);

        JButton exitButton =
                createButton(
                        "게임 종료",
                        "EXIT"
                );

        gbc.gridy = 2;
        gbc.insets =
                new Insets(15, 0, 15, 0);
        panel.add(exitButton, gbc);

        return panel;
    }

    // 하단 제작자 정보 패널
    private JPanel createFooterPanel() {

        JLabel creatorLabel =
                new JLabel(
                        "Team: H  |  팀장: 구서준  |  팀원: 송효인 정다운",
                        SwingConstants.RIGHT
                );

        creatorLabel.setForeground(
                new Color(200, 200, 200)
        );

        creatorLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        14
                )
        );

        JPanel panel =
                new JPanel(new BorderLayout());

        panel.setOpaque(false);
        panel.setBorder(
                BorderFactory.createEmptyBorder(
                        0, 0, 10, 10
                )
        );

        panel.add(
                creatorLabel,
                BorderLayout.EAST
        );

        return panel;
    }

    // 게임 제목 라벨 생성
    private JLabel createTitleLabel() {

        JLabel label =
                new JLabel("Edge Runner");

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
                new Dimension(200, 50)
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

        if ("START".equals(command)) {

            // 게임 상태 초기화 후 게임 화면으로 전환
            gamePanel.resetStateToTutorial();

            layout.show(
                    mainContainer,
                    GameCore.CARD_GAME
            );

            gamePanel.startNewGame();
            return;
        }

        if ("EXIT".equals(command)) {

            // 프로그램 종료
            System.exit(0);
        }
    }
}