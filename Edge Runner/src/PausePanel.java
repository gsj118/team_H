import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PausePanel extends JPanel implements ActionListener {

    // ===============================
    // References
    // ===============================

    private final GameCore.GamePanel gamePanel;
    private final JPanel mainContainer;

    // ===============================
    // Panel Size
    // ===============================

    private static final int WIDTH  =
            GameCore.GamePanel.WIDTH;

    private static final int HEIGHT =
            GameCore.GamePanel.HEIGHT;

    // ===============================
    // Constructor
    // ===============================

    public PausePanel(GameCore.GamePanel gamePanel,
                      JPanel mainContainer) {

        this.gamePanel = gamePanel;
        this.mainContainer = mainContainer;

        initializePanel();
        initializeComponents();
    }

    // ===============================
    // Initialization
    // ===============================

    private void initializePanel() {

        setBackground(new Color(0, 0, 0, 180));
        setOpaque(false);
        setPreferredSize(
                new Dimension(WIDTH, HEIGHT)
        );

        setLayout(new GridBagLayout());
    }

    private void initializeComponents() {

        // ---- Title ----

        JLabel titleLabel =
                createTitleLabel();

        // ---- Buttons ----

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

        // ---- Layout ----

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

    // ===============================
    // Component Factory
    // ===============================

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

    // ===============================
    // Button Events
    // ===============================

    @Override
    public void actionPerformed(ActionEvent e) {

        String command =
                e.getActionCommand();

        CardLayout layout =
                (CardLayout) mainContainer.getLayout();

        if ("RESUME".equals(command)) {

            gamePanel.resumeGame();
            return;
        }

        if ("GO_TO_MENU".equals(command)) {

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

            System.exit(0);
        }
    }

    // ===============================
    // Background Rendering
    // ===============================

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