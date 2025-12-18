import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MenuPanel extends JPanel implements ActionListener {

    // ===============================
    // References
    // ===============================

    private final GameCore.GamePanel gamePanel;
    private final JPanel mainContainer;

    // ===============================
    // Constructor
    // ===============================

    public MenuPanel(GameCore.GamePanel gamePanel,
                     JPanel mainContainer) {

        this.gamePanel = gamePanel;
        this.mainContainer = mainContainer;

        initializePanel();
        initializeLayout();
    }

    // ===============================
    // Panel Initialization
    // ===============================

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

    // ===============================
    // Layout / Components
    // ===============================

    private void initializeLayout() {

        JPanel centerPanel =
                createCenterPanel();

        add(centerPanel, BorderLayout.CENTER);

        JPanel footerPanel =
                createFooterPanel();

        add(footerPanel, BorderLayout.SOUTH);
    }

    // -------------------------------
    // Center Area
    // -------------------------------

    private JPanel createCenterPanel() {

        JPanel panel =
                new JPanel(new GridBagLayout());

        panel.setOpaque(false);

        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.insets =
                new Insets(15, 0, 15, 0);

        // ---- Title ----

        JLabel titleLabel =
                createTitleLabel();

        gbc.gridy = 0;
        panel.add(titleLabel, gbc);

        // ---- Start Button ----

        JButton startButton =
                createButton(
                        "게임 시작",
                        "START"
                );

        gbc.gridy = 1;
        gbc.insets =
                new Insets(50, 0, 15, 0);
        panel.add(startButton, gbc);

        // ---- Exit Button ----

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

    // -------------------------------
    // Footer Area
    // -------------------------------

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

    // ===============================
    // Component Factory
    // ===============================

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

    // ===============================
    // Button Events
    // ===============================

    @Override
    public void actionPerformed(ActionEvent e) {

        String command =
                e.getActionCommand();

        CardLayout layout =
                (CardLayout) mainContainer.getLayout();

        if ("START".equals(command)) {

            gamePanel.resetStateToTutorial();

            layout.show(
                    mainContainer,
                    GameCore.CARD_GAME
            );

            gamePanel.startNewGame();
            return;
        }

        if ("EXIT".equals(command)) {

            System.exit(0);
        }
    }
}