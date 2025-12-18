import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MenuPanel extends JPanel implements ActionListener {

    private final GameCore.GamePanel gamePanel;
    private final JPanel mainContainer;

    private JButton startButton;

    public MenuPanel(GameCore.GamePanel gamePanel, JPanel mainContainer) {
        this.gamePanel = gamePanel;
        this.mainContainer = mainContainer;

        setPreferredSize(new Dimension(GameCore.GamePanel.WIDTH, GameCore.GamePanel.HEIGHT));
        setLayout(null);

        startButton = new JButton("START");
        startButton.setBounds(GameCore.GamePanel.WIDTH / 2 - 80, 250, 160, 50);
        startButton.addActionListener(this);
        add(startButton);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == startButton) {
            gamePanel.startNewGame();
            CardLayout cl = (CardLayout) mainContainer.getLayout();
            cl.show(mainContainer, GameCore.CARD_GAME);
            gamePanel.requestFocusInWindow();
        }
    }
}
