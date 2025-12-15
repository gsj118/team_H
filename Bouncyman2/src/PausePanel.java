import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * UI 담당: 일시정지 메뉴 화면 (Pause Overlay)
 * - Swing 컴포넌트를 사용하여 게임 화면 위에 겹쳐서 표시됨
 * - GameCore의 GamePanel과 CardLayout을 가진 메인 컨테이너에 접근하여 상태를 변경
 */
public class PausePanel extends JPanel implements ActionListener {

    private final GameCore.GamePanel gamePanel;
    private final JPanel mainContainer;
    
    // 게임 패널의 크기를 가져와 사용
    private static final int WIDTH = GameCore.GamePanel.WIDTH;
    private static final int HEIGHT = GameCore.GamePanel.HEIGHT;

    public PausePanel(GameCore.GamePanel gamePanel, JPanel mainContainer) {
        this.gamePanel = gamePanel;
        this.mainContainer = mainContainer;

        // 패널 설정
        // 투명도를 가진 배경색 (게임 화면이 뒤에 살짝 보이도록)
        setBackground(new Color(0, 0, 0, 180)); 
        setOpaque(false);
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        
        // PausePanel은 GamePanel 위에 정확히 겹치도록 null 레이아웃을 사용
        setLayout(new GridBagLayout()); 
        
        // --- 컴포넌트 구성 ---
        
        JLabel titleLabel = new JLabel("PAUSED");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 48));

        // 버튼 스타일 설정
        Dimension buttonSize = new Dimension(220, 50);
        Font buttonFont = new Font("SansSerif", Font.BOLD, 18);
        
        // 1. 게임 재개 버튼
        JButton resumeButton = new JButton("게임 재개");
        resumeButton.setPreferredSize(buttonSize);
        resumeButton.setFont(buttonFont);
        resumeButton.setActionCommand("RESUME");
        resumeButton.addActionListener(this);
        
        // 2. 메인 메뉴로 버튼
        JButton menuButton = new JButton("메인 메뉴");
        menuButton.setPreferredSize(buttonSize);
        menuButton.setFont(buttonFont);
        menuButton.setActionCommand("GO_TO_MENU");
        menuButton.addActionListener(this);
        
        // 3. 게임 종료 버튼
        JButton exitButton = new JButton("게임 종료");
        exitButton.setPreferredSize(buttonSize);
        exitButton.setFont(buttonFont);
        exitButton.setActionCommand("EXIT");
        exitButton.addActionListener(this);

        // GridBagLayout 제약 조건 설정
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 0, 15, 0); 

        // 제목 추가
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 40, 0);
        add(titleLabel, gbc);

        // 버튼 추가
        gbc.gridy = 1;
        gbc.insets = new Insets(10, 0, 10, 0); 
        add(resumeButton, gbc);
        
        gbc.gridy = 2;
        add(menuButton, gbc);
        
        gbc.gridy = 3;
        add(exitButton, gbc);
    }

    /**
     * 버튼 클릭 이벤트 처리
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        CardLayout cl = (CardLayout) mainContainer.getLayout();

        if ("RESUME".equals(command)) {
            // GamePanel의 재개 메서드를 호출하여 상태 변경 및 PausePanel 숨김
            gamePanel.resumeGame();
            
        } else if ("GO_TO_MENU".equals(command)) {
        	// 메인메뉴 BGM으로 교체
        	GameCore.getSoundManager().playBGM(SoundManager.BGM_MAIN_MENU);
        	// 게임 루프 정지(메인화면 갔을 때)
        	gamePanel.stopGameThread();
            // CardLayout을 MENU 카드로 전환
        	gamePanel.resetStateToTutorial();
            cl.show(mainContainer, GameCore.CARD_MENU);
            
        } else if ("EXIT".equals(command)) {
            // 프로그램 종료
            System.exit(0);
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); 
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
    }
}