import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PausePanel extends JPanel implements ActionListener {

    private final GameCore.GamePanel gamePanel;
    private final JPanel mainContainer;

    private JButton resumeBtn;
    private JButton toMenuBtn;

    public PausePanel(GameCore.GamePanel gamePanel, JPanel mainContainer) {
        this.gamePanel = gamePanel;
        this.mainContainer = mainContainer;

        setOpaque(false);
        setLayout(null);

        resumeBtn = new JButton("RESUME");
        resumeBtn.setBounds(GameCore.GamePanel.WIDTH / 2 - 90, 220, 180, 45);
        resumeBtn.addActionListener(this);
        add(resumeBtn);

        toMenuBtn = new JButton("MENU");
        toMenuBtn.setBounds(GameCore.GamePanel.WIDTH / 2 - 90, 280, 180, 45);
        toMenuBtn.addActionListener(this);
        add(toMenuBtn);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(Color.WHITE);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 28f));
        g2.drawString("PAUSE", getWidth()/2 - 50, 170);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == resumeBtn) {
            gamePanel.resumeGame();
        } else if (e.getSource() == toMenuBtn) {
            // (추가) 메뉴로 복귀 + BGM 전환
            CardLayout cl = (CardLayout) mainContainer.getLayout();
            cl.show(mainContainer, GameCore.CARD_MENU);
            GameCore.getSoundManager().playBGM(SoundManager.BGM_MAIN_MENU);
            gamePanel.resetStateToTutorial();
        }
    }
}
