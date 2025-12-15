import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * UI 담당: 메인 메뉴 화면
 * - Swing 컴포넌트를 사용하여 버튼 등을 배치
 * - GameCore의 CardLayout과 GamePanel에 접근하여 화면을 전환하고 게임 시작
 */

public class MenuPanel extends JPanel implements ActionListener {

    private final GameCore.GamePanel gamePanel;
    private final JPanel mainContainer;

    // 생성자를 통해 GamePanel과 CardLayout을 가진 부모 컨테이너를 전달받음
    public MenuPanel(GameCore.GamePanel gamePanel, JPanel mainContainer) {
    	this.gamePanel = gamePanel;
        this.mainContainer = mainContainer;

        // 패널 설정
        setBackground(new Color(20, 20, 40)); 
        setLayout(new BorderLayout()); 
        
        // 중앙 패널
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new GridBagLayout());
        centerPanel.setOpaque(false);

        // 제목 레이블
        JLabel titleLabel = new JLabel("Bounce Escape");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 48));

        // 버튼 스타일 설정
        Dimension buttonSize = new Dimension(200, 50);
        Font buttonFont = new Font("SansSerif", Font.BOLD, 18);
        
        // 게임 시작 버튼
        JButton startButton = new JButton("게임 시작");
        startButton.setPreferredSize(buttonSize);
        startButton.setFont(buttonFont);
        startButton.setActionCommand("START");
        startButton.addActionListener(this);
        
        // 게임 종료 버튼
        JButton exitButton = new JButton("게임 종료");
        exitButton.setPreferredSize(buttonSize);
        exitButton.setFont(buttonFont);
        exitButton.setActionCommand("EXIT");
        exitButton.addActionListener(this);

        // GridBagLayout을 위한 Constraints 설정
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 0, 15, 0); 

        // 제목 추가
        gbc.gridy = 0;
        centerPanel.add(titleLabel, gbc);

        // 버튼 추가
        gbc.gridy = 1;
        gbc.insets = new Insets(50, 0, 15, 0); 
        centerPanel.add(startButton, gbc);
        
        gbc.gridy = 2;
        gbc.insets = new Insets(15, 0, 15, 0); 
        centerPanel.add(exitButton, gbc);
        
        // 중앙 콘텐츠를 MenuPanel의 CENTER에 추가
        add(centerPanel, BorderLayout.CENTER);


        // --- 제작자 정보(우하단쪽) ---
        // 제작자 정보를 담을 JLabel 생성
        JLabel creatorLabel = new JLabel("Team: H  |  팀장: 구서준  |  팀원: 송효인 정다운", SwingConstants.RIGHT);
        creatorLabel.setForeground(new Color(200, 200, 200));
        creatorLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        // 패딩을 위한 JPanel 생성
        JPanel creatorPanel = new JPanel(new BorderLayout());
        creatorPanel.setOpaque(false);
        creatorPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 10)); 
        creatorPanel.add(creatorLabel, BorderLayout.EAST);

        add(creatorPanel, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(GameCore.GamePanel.WIDTH, GameCore.GamePanel.HEIGHT));
    }

    /**
     * 버튼 클릭 이벤트 처리
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        CardLayout cl = (CardLayout) mainContainer.getLayout();

        if ("START".equals(command)) {
        	gamePanel.resetStateToTutorial();
            // CardLayout을 GAME 카드로 전환
            cl.show(mainContainer, GameCore.CARD_GAME);
            // 게임 루프 시작, 튜토리얼 로드
            gamePanel.startNewGame(); 
        } else if ("EXIT".equals(command)) {
            System.exit(0);
        }
    }
}