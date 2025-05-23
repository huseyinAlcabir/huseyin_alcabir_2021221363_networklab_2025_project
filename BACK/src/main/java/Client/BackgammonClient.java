package Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.HashSet;
import java.util.Set;

public class BackgammonClient extends JFrame {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private JTextArea gameArea;
    private JPanel boardPanel;
    private JLabel statusLabel;
    private JLabel piecesCountLabel;

    private JLabel die1Label;
    private JLabel die2Label;

    private int playerId;
    private int currentPlayer;
    private int[] board = new int[24];
    private int die1, die2;
    private int[] bar = new int[2];
    private int[] borneOff = new int[2];
    private boolean isMyTurn = false;
    private int selectedPoint = -1;
    private final Color[] pointColors = new Color[24];
    private Set<Integer> legalMoves = new HashSet<>(); // ✅ جديد

    public BackgammonClient(String serverIP) throws IOException {
        socket = new Socket("52.87.187.39", 5000);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        initializeUI();
        startMessageReceiver();
    }

    private void initializeUI() {
        setTitle("Backgammon Client");
        setSize(1200, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(5, 5));

        // Status Panel (Top)
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new GridLayout(3, 1)); // ← فقط 3 عناصر: الحالة، القطع، الزهر

        statusLabel = new JLabel("Connecting to server...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 20));
        statusPanel.add(statusLabel);

        piecesCountLabel = new JLabel("Pieces on board", SwingConstants.CENTER);
        piecesCountLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        statusPanel.add(piecesCountLabel);

        JPanel dicePanel = new JPanel();
        dicePanel.setBackground(new Color(240, 240, 240));
        die1Label = new JLabel("Die 1: -");
        die1Label.setFont(new Font("Arial", Font.PLAIN, 16));
        die2Label = new JLabel("Die 2: -");
        die2Label.setFont(new Font("Arial", Font.PLAIN, 16));
        dicePanel.add(die1Label);
        dicePanel.add(Box.createHorizontalStrut(20));
        dicePanel.add(die2Label);
        statusPanel.add(dicePanel);

        add(statusPanel, BorderLayout.NORTH);

        // Main Game Area
        JPanel gamePanel = new JPanel(new BorderLayout(5, 5));

        // Board Panel
        boardPanel = new BoardPanel();
        boardPanel.setPreferredSize(new Dimension(800, 500));
        gamePanel.add(boardPanel, BorderLayout.CENTER);

        // Game Message Area
        gameArea = new JTextArea(8, 25);
        gameArea.setEditable(false);
        gameArea.setFont(new Font("Arial", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(gameArea);
        gamePanel.add(scrollPane, BorderLayout.EAST);

        // Control Panel
        JPanel controlPanel = new JPanel(new BorderLayout(5, 5));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JButton rollButton = new JButton("Roll Dice");
        rollButton.setFont(new Font("Arial", Font.BOLD, 16));
        rollButton.addActionListener(e -> rollDice());

        JButton helpButton = new JButton("Help");
        helpButton.setFont(new Font("Arial", Font.BOLD, 16));
        helpButton.addActionListener(e -> showHelp());

        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 10, 0)); // ✅ 3 أزرار بدل 2
        buttonPanel.add(rollButton);
        buttonPanel.add(helpButton);

// ✅ زر إلغاء التحديد
        JButton cancelButton = new JButton("Cancel Selection");
        cancelButton.setFont(new Font("Arial", Font.BOLD, 16));
        cancelButton.addActionListener(e -> {
            selectedPoint = -1;
            legalMoves.clear();
            statusLabel.setText("Selection canceled. Select checker to move.");
            statusLabel.setForeground(Color.DARK_GRAY);
            boardPanel.repaint();
        });

        buttonPanel.add(cancelButton);

        controlPanel.add(buttonPanel, BorderLayout.CENTER);

        add(gamePanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        setupPointColors();
    }

    private class BoardPanel extends JPanel {

        public BoardPanel() {
            setBackground(new Color(210, 180, 140));
            addMouseListener(new BoardMouseListener());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            drawBoard(g);
        }
    }

    private void setupPointColors() {
        for (int i = 0; i < 24; i++) {
            pointColors[i] = (i < 12) ? new Color(139, 69, 19) : new Color(245, 222, 179);
        }
    }

    private void startMessageReceiver() {
        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    System.out.println("📩 Received from Player " + playerId + ": " + line);

                    processServerMessage(line);
                }
            } catch (IOException e) {
                appendMessage("Disconnected from server\n");
            }
        }).start();
    }

    private void processServerMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            if (message.startsWith("WELCOME")) {
                playerId = Integer.parseInt(message.substring(8));
                if (playerId == 0) {
                    setTitle("Backgammon - Spectator/Waiting Mode");
                    statusLabel.setText("You are in waiting queue. Please wait...");
                    statusLabel.setForeground(Color.GRAY);
                } else {
                    setTitle("Backgammon - Player " + playerId);
                    statusLabel.setText("Connected as Player " + playerId);
                    statusLabel.setForeground(Color.BLUE);
                }

            } else if (message.startsWith("STATUS")) {
                String statusMsg = message.substring(7);
                statusLabel.setText(statusMsg);
                appendMessage(statusMsg + "\n");

                if (statusMsg.contains("disconnected")) {
                    if (playerId != 0 && isMyTurn) {
                        JOptionPane.showMessageDialog(this, "🎉 All opponents disconnected. You win!", "Victory", JOptionPane.INFORMATION_MESSAGE);
                    }
                }

                if (statusMsg.contains("disconnected")) {

                    // ✅ لا تُظهر رسالة الفوز إلا للاعب الفعّال (Player 1 أو 2) وفي دوره فقط
                    if (playerId != 0 && isMyTurn) {
                        JOptionPane.showMessageDialog(this, "🎉 All opponents disconnected. You win!", "Victory", JOptionPane.INFORMATION_MESSAGE);
                    }

                    if (playerId == 0) {
                        appendMessage("Still in queue. Waiting for your turn...\n");
                    } else {

                    }
                } else {
                    statusLabel.setForeground(Color.BLUE);
                }
            } else if (message.startsWith("BOARD")) {
                updateBoard(message.substring(6));
            } else if (message.startsWith("DICE")) {
                String[] parts = message.substring(5).split(" ");
                die1 = Integer.parseInt(parts[0]);
                die2 = Integer.parseInt(parts[1]);

                die1Label.setText("Die 1: " + die1); // ✅ تحديث التسمية العلوية
                die2Label.setText("Die 2: " + die2);

            } else if (message.startsWith("TURN")) {
                currentPlayer = Integer.parseInt(message.substring(5));
                isMyTurn = (currentPlayer == playerId);

                if (isMyTurn) {
                    statusLabel.setText("YOUR TURN - Roll or move");
                    statusLabel.setForeground(new Color(0, 150, 0));
                    boardPanel.setBackground(new Color(230, 255, 230));

                } else {
                    statusLabel.setText("Waiting for Player " + currentPlayer);
                    statusLabel.setForeground(Color.BLUE);
                    boardPanel.setBackground(new Color(210, 180, 140));

                }

            } else if (message.startsWith("BAR")) {
                String[] parts = message.substring(4).split(" ");
                bar[0] = Integer.parseInt(parts[0]);
                bar[1] = Integer.parseInt(parts[1]);
            } else if (message.startsWith("BORNE_OFF")) {
                String[] parts = message.substring(10).split(" ");
                borneOff[0] = Integer.parseInt(parts[0]);
                borneOff[1] = Integer.parseInt(parts[1]);
            } else if (message.equals("GAME_OVER YOU_WIN")) {
                if (playerId == currentPlayer) {
                    statusLabel.setText("Game Over! You win! 🎉");
                    statusLabel.setForeground(Color.RED);
                    gameArea.append("Game Over! You win!\n");
                    JOptionPane.showMessageDialog(this, "🏆 You are the last active player. You win!", "Victory", JOptionPane.INFORMATION_MESSAGE);
                }
            } else if (message.startsWith("GAME_OVER")) {
                String winner = message.substring(10);
                statusLabel.setText("Game Over! Player " + winner + " wins!");
                statusLabel.setForeground(Color.RED);
                gameArea.append("Game Over! Player " + winner + " wins!\n");
            } else if (message.startsWith("ERROR")) {
                gameArea.append("Error: " + message.substring(6) + "\n");
                statusLabel.setText("Invalid Move - Try Again");
                statusLabel.setForeground(Color.RED);
                new Timer(2000, e -> {
                    if (isMyTurn) {
                        statusLabel.setText("YOUR TURN - Roll or move");
                        statusLabel.setForeground(new Color(0, 150, 0));
                    } else {
                        statusLabel.setText("Waiting for Player " + currentPlayer);
                        statusLabel.setForeground(Color.BLUE);
                    }
                }).start();
            }
            boardPanel.repaint();
        });
    }

    private void updateBoard(String boardStr) {
        String[] parts = boardStr.split(" ");
        for (int i = 0; i < 24; i++) {
            board[i] = Integer.parseInt(parts[i]);
        }
        bar[0] = Integer.parseInt(parts[24]);
        bar[1] = Integer.parseInt(parts[25]);
        borneOff[0] = Integer.parseInt(parts[26]);
        borneOff[1] = Integer.parseInt(parts[27]);
    }

    private void appendMessage(String message) {
        gameArea.append(message);
        gameArea.setCaretPosition(gameArea.getDocument().getLength());
    }

    private void drawBoard(Graphics g) {
        int width = boardPanel.getWidth();
        int height = boardPanel.getHeight();
        int colWidth = width / 13;
        int pointHeight = height / 2 - 50;

        // Draw points
        for (int i = 0; i < 12; i++) {
            // Top points (right to left)
            int xTop = width - (i + 1) * colWidth;
            drawPoint(g, xTop, 0, colWidth, pointHeight, i, false);

            // Bottom points (left to right)
            int xBottom = i * colWidth;
            drawPoint(g, xBottom, height - pointHeight, colWidth, pointHeight, 12 + i, true);
        }

        // Draw bar
        drawBar(g, width / 2 - 25, height / 2 - 50, 50, 100);

        // Draw borne off
        drawBorneOff(g, 20, height / 2 - 25, 40, 50, 1);
        drawBorneOff(g, width - 60, height / 2 - 25, 40, 50, 2);

        // Draw dice
        if (die1 > 0) {
            drawDie(g, width / 2 - 50, height / 2 - 25, 40, die1);
        }
        if (die2 > 0) {
            drawDie(g, width / 2 + 10, height / 2 - 25, 40, die2);
        }

        // Highlight selected point
        if (selectedPoint != -1) {
            highlightPoint(g, selectedPoint);
        }
    }

    private void drawPoint(Graphics g, int x, int y, int width, int height, int pointIndex, boolean isBottom) {
        int checkerCount = board[pointIndex];

        // Draw point triangle
        g.setColor(pointColors[pointIndex]);
        int[] xPoints = {x, x + width, x + width / 2};
        int[] yPoints = isBottom ? new int[]{y, y, y + height} : new int[]{y + height, y + height, y};
        g.fillPolygon(xPoints, yPoints, 3);
        g.setColor(Color.BLACK);
        g.drawPolygon(xPoints, yPoints, 3);

        // Draw point number
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString(String.valueOf(pointIndex), x + width / 2 - 5, isBottom ? y + height + 15 : y - 5);

        // Draw checkers
        if (checkerCount != 0) {
            Color checkerColor = (checkerCount > 0) ? Color.WHITE : Color.BLACK;
            int checkerRadius = 15;
            int maxCheckers = 5;
            int checkersToShow = Math.min(Math.abs(checkerCount), maxCheckers);

            for (int i = 0; i < checkersToShow; i++) {
                int checkerY = isBottom
                        ? y + height - 15 - i * 10
                        : y + 15 + i * 10;

                g.setColor(checkerColor);
                g.fillOval(x + width / 2 - checkerRadius, checkerY - checkerRadius,
                        checkerRadius * 2, checkerRadius * 2);
                g.setColor(checkerColor == Color.WHITE ? Color.BLACK : Color.WHITE);
                g.drawOval(x + width / 2 - checkerRadius, checkerY - checkerRadius,
                        checkerRadius * 2, checkerRadius * 2);
            }

            if (Math.abs(checkerCount) > maxCheckers) {
                g.setColor(Color.YELLOW);
                g.drawString("x" + Math.abs(checkerCount), x + width / 2 - 5,
                        isBottom ? y + height - 15 - maxCheckers * 10 - 5
                                : y + 15 + maxCheckers * 10 + 15);
            }
        }
    }

    private void drawBar(Graphics g, int x, int y, int width, int height) {
        g.setColor(new Color(150, 150, 150));
        g.fillRect(x, y, width, height);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, width, height);

        // Player 1 checkers on bar
        for (int i = 0; i < Math.min(bar[0], 5); i++) {
            g.setColor(Color.WHITE);
            g.fillOval(x + width / 2 - 10, y + 10 + i * 15, 20, 20);
            g.setColor(Color.BLACK);
            g.drawOval(x + width / 2 - 10, y + 10 + i * 15, 20, 20);
        }

        // Player 2 checkers on bar
        for (int i = 0; i < Math.min(bar[1], 5); i++) {
            g.setColor(Color.BLACK);
            g.fillOval(x + width / 2 - 10, y + height - 30 - i * 15, 20, 20);
            g.setColor(Color.WHITE);
            g.drawOval(x + width / 2 - 10, y + height - 30 - i * 15, 20, 20);
        }
    }

    private void drawBorneOff(Graphics g, int x, int y, int width, int height, int player) {
        g.setColor(new Color(200, 200, 200));
        g.fillRect(x, y, width, height);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, width, height);

        int count = (player == 1) ? borneOff[0] : borneOff[1];
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("P" + player + ": " + count, x + 5, y + 20);
    }

    private void drawDie(Graphics g, int x, int y, int size, int value) {
        g.setColor(Color.WHITE);
        g.fillRect(x, y, size, size);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, size, size);

        int dotSize = size / 5;
        int dotOffset = size / 4;

        if (value % 2 == 1) { // Center dot
            g.fillOval(x + size / 2 - dotSize / 2, y + size / 2 - dotSize / 2, dotSize, dotSize);
        }
        if (value > 1) { // Corners
            g.fillOval(x + dotOffset - dotSize / 2, y + dotOffset - dotSize / 2, dotSize, dotSize);
            g.fillOval(x + size - dotOffset - dotSize / 2, y + size - dotOffset - dotSize / 2, dotSize, dotSize);
        }
        if (value > 3) { // Other corners
            g.fillOval(x + size - dotOffset - dotSize / 2, y + dotOffset - dotSize / 2, dotSize, dotSize);
            g.fillOval(x + dotOffset - dotSize / 2, y + size - dotOffset - dotSize / 2, dotSize, dotSize);
        }
        if (value == 6) { // Middle
            g.fillOval(x + dotOffset - dotSize / 2, y + size / 2 - dotSize / 2, dotSize, dotSize);
            g.fillOval(x + size - dotOffset - dotSize / 2, y + size / 2 - dotSize / 2, dotSize, dotSize);
        }
    }

    private void highlightPoint(Graphics g, int pointIndex) {
        int width = boardPanel.getWidth();
        int height = boardPanel.getHeight();
        int colWidth = width / 13;
        int pointHeight = height / 2 - 50;

        int x, y, h;
        if (pointIndex < 12) {
            x = width - (pointIndex + 1) * colWidth;
            y = 0;
            h = pointHeight;
        } else {
            x = (pointIndex - 12) * colWidth;
            y = height - pointHeight;
            h = pointHeight;
        }

        g.setColor(new Color(255, 255, 0, 100));
        int[] xPoints = {x, x + colWidth, x + colWidth / 2};
        int[] yPoints = (pointIndex < 12)
                ? new int[]{y, y, y + h} : new int[]{y + h, y + h, y};
        g.fillPolygon(xPoints, yPoints, 3);
    }

    private int getClickedPoint(int x, int y) {
        int width = boardPanel.getWidth();
        int height = boardPanel.getHeight();
        int colWidth = width / 13;
        int pointHeight = height / 2 - 50;

        for (int i = 0; i < 12; i++) {
            // Top points (0-11)
            int xTop = width - (i + 1) * colWidth;
            if (x >= xTop && x <= xTop + colWidth && y >= 0 && y <= pointHeight) {
                return i;
            }

            // Bottom points (12-23)
            int xBottom = i * colWidth;
            if (x >= xBottom && x <= xBottom + colWidth
                    && y >= height - pointHeight && y <= height) {
                return 12 + i;
            }
        }

        // Check bar area
        int barX = width / 2 - 25;
        int barY = height / 2 - 50;
        if (x >= barX && x <= barX + 50 && y >= barY && y <= barY + 100) {
            return -1; // Bar
        }

        return -2; // No point clicked
    }

    private void rollDice() {
        if (playerId == 0) {
            appendMessage("You are in queue. Wait for your turn.\n");
            return;
        }

        if (isMyTurn) {
            out.println("ROLL");

        } else {
            appendMessage("Wait for your turn to roll\n");
            statusLabel.setText("Not your turn - Wait for Player " + currentPlayer);
            statusLabel.setForeground(Color.RED);
            new Timer(2000, e -> {
                statusLabel.setText("Waiting for Player " + currentPlayer);
                statusLabel.setForeground(Color.BLUE);
            }).start();
        }
    }

    private void showHelp() {
        String helpText = "<html><h2>Backgammon Help</h2>"
                + "<h3>How to Play:</h3>"
                + "<ol>"
                + "<li>Wait for your turn (shown in status bar)</li>"
                + "<li>Click 'Roll Dice' button when it's your turn</li>"
                + "<li>Click on your checker (white if Player 1, black if Player 2)</li>"
                + "<li>Click on destination point to move</li>"
                + "</ol>"
                + "<h3>Rules:</h3>"
                + "<ul>"
                + "<li>Player 1 moves clockwise (0→23), Player 2 moves counter-clockwise (23→0)</li>"
                + "<li>Can't land on points with 2+ opponent checkers</li>"
                + "<li>Must move checkers from bar first if any</li>"
                + "<li>Bear off when all checkers are in home board</li>"
                + "<li>First to bear off all 15 checkers wins</li>"
                + "</ul></html>";
        JOptionPane.showMessageDialog(this, helpText, "Backgammon Help", JOptionPane.INFORMATION_MESSAGE);
    }

    private class BoardMouseListener extends MouseAdapter {

        @Override

        public void mouseClicked(MouseEvent e) {
            if (playerId == 0) {
                statusLabel.setText("You are in queue. Wait to join the game.");
                return;
            }

            if (!isMyTurn) {
                statusLabel.setText("Not your turn - Wait for Player " + currentPlayer);
                statusLabel.setForeground(Color.RED);
                new Timer(2000, evt -> {
                    statusLabel.setText("Waiting for Player " + currentPlayer);
                    statusLabel.setForeground(Color.BLUE);
                }).start();
                return;
            }

            if (die1 == 0 && die2 == 0) {
                statusLabel.setText("Please roll dice first");
                statusLabel.setForeground(Color.RED);
                new Timer(2000, evt -> {
                    statusLabel.setText("YOUR TURN - Roll or move");
                    statusLabel.setForeground(new Color(0, 150, 0));
                }).start();
                return;
            }

            int point = getClickedPoint(e.getX(), e.getY());
            if (point == -2) {
                return;
            }

            if (selectedPoint == -1) {
                // Selecting a checker to move
                if (point == -1) { // Bar
                    if ((playerId == 1 && bar[0] > 0) || (playerId == 2 && bar[1] > 0)) {
                        selectedPoint = -1;
                        statusLabel.setText("Selected BAR - Click destination point");
                        boardPanel.repaint();
                    }
                } else { // Normal point
                    if ((playerId == 1 && board[point] > 0) || (playerId == 2 && board[point] < 0)) {
                        selectedPoint = point;
                        statusLabel.setText("Selected point " + point + " - Click destination");
                        boardPanel.repaint();
                    }
                }
            } else {
                // Moving to destination
                int from = selectedPoint;
                int to = point;

                if (from == to) { // Deselect
                    selectedPoint = -1;
                    statusLabel.setText("YOUR TURN - Select checker to move");
                    boardPanel.repaint();
                    return;
                }

                if (to == -1) { // Can't move to bar
                    statusLabel.setText("Invalid destination - Can't move to bar");
                    statusLabel.setForeground(Color.RED);
                    new Timer(2000, evt -> {
                        statusLabel.setText("YOUR TURN - Select destination");
                        statusLabel.setForeground(new Color(0, 150, 0));
                    }).start();
                    selectedPoint = -1;
                    boardPanel.repaint();
                    return;
                }

                // Send move command
                String fromStr = (from == -1) ? "BAR" : String.valueOf(from);
                out.println("MOVE " + fromStr + " " + to);
                selectedPoint = -1;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                BackgammonClient client = new BackgammonClient("localhost");
                client.setVisible(true);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Connection failed: " + e.getMessage());
            }
        });
    }

}
