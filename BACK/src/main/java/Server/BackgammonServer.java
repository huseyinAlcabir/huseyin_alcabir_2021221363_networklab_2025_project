package Server;


import java.io.*;
import java.net.*;
import java.util.*;

public class BackgammonServer {
    private static ServerSocket serverSocket;
    private static Queue<Socket> waitingQueue = new LinkedList<>();
    private static List<MatchHandler> activeMatches = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        serverSocket = new ServerSocket(5000);
        System.out.println("Server started on port 5000");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("✅ New connection from: " + clientSocket.getInetAddress().getHostAddress());

            System.out.println("New player connected");
            waitingQueue.add(clientSocket);
            tryStartMatch();
        }
    }

    private static void tryStartMatch() {
        while (waitingQueue.size() >= 2) {
            Socket p1 = waitingQueue.poll();
            Socket p2 = waitingQueue.poll();

            MatchHandler match = new MatchHandler(p1, p2);
            activeMatches.add(match);
            new Thread(match).start();
        }
    }

    static class MatchHandler implements Runnable {
        private Socket player1Socket, player2Socket;
        private PrintWriter out1, out2;
        private BufferedReader in1, in2;
        private GameState gameState;

        public MatchHandler(Socket p1, Socket p2) {
            this.player1Socket = p1;
            this.player2Socket = p2;
            this.gameState = new GameState();
        }

        @Override
        public void run() {
            try {
                in1 = new BufferedReader(new InputStreamReader(player1Socket.getInputStream()));
                out1 = new PrintWriter(player1Socket.getOutputStream(), true);
                in2 = new BufferedReader(new InputStreamReader(player2Socket.getInputStream()));
                out2 = new PrintWriter(player2Socket.getOutputStream(), true);

                out1.println("WELCOME 1");
                out2.println("WELCOME 2");
                broadcast("STATUS Match started between Player 1 and Player 2");
                sendGameState();

                while (true) {
                    if (gameState.isGameOver()) {
                        int winner = (gameState.borneOff[0] >= 15) ? 1 : 2;
                        broadcast("GAME_OVER " + winner);
                        break;
                    }

                    BufferedReader currentIn = (gameState.currentPlayer == 1) ? in1 : in2;
                    PrintWriter currentOut = (gameState.currentPlayer == 1) ? out1 : out2;

                    String line = currentIn.readLine();
                    if (line == null) break;

                    synchronized (gameState) {
                        if (line.equalsIgnoreCase("ROLL") && !gameState.diceRolled) {
                            gameState.rollDice();
                            broadcast("STATUS Player " + gameState.currentPlayer +
                                    " rolled " + gameState.die1 + " and " + gameState.die2);
                            sendGameState();

                            if (!gameState.hasValidMoves()) {
                                broadcast("STATUS No valid moves. Turn skipped.");
                                gameState.switchTurn();
                                sendGameState();
                            }
                        } else if (line.startsWith("MOVE") && gameState.diceRolled) {
                            String[] parts = line.split(" ");
                            if (parts.length == 3) {
                                try {
                                    int from = parts[1].equalsIgnoreCase("BAR") ? -1 : Integer.parseInt(parts[1]);
                                    int to = Integer.parseInt(parts[2]);

                                    if (gameState.moveChecker(from, to)) {
                                        broadcast("STATUS Player " + gameState.currentPlayer +
                                                " moved from " + (from == -1 ? "BAR" : from) + " to " + to);
                                        sendGameState();

                                        if (gameState.isGameOver()) {
                                            int winner = (gameState.borneOff[0] >= 15) ? 1 : 2;
                                            broadcast("GAME_OVER " + winner);
                                            break;
                                        }
                                    } else {
                                        currentOut.println("ERROR Invalid move");
                                    }
                                } catch (Exception e) {
                                    currentOut.println("ERROR Invalid format");
                                }
                            } else {
                                currentOut.println("ERROR Invalid MOVE format");
                            }
                        } else {
                            currentOut.println("ERROR Not your turn or invalid command");
                        }
                    }
                }

            } catch (IOException e) {
                System.out.println("Match ended due to disconnect");
            } finally {
                closeSocket(player1Socket);
                closeSocket(player2Socket);
                activeMatches.remove(this);
                tryStartMatch();
            }
        }

        private void broadcast(String msg) {
            out1.println(msg);
            out2.println(msg);
        }

        private void sendGameState() {
            broadcast("BOARD " + gameState.boardToString());
            broadcast("DICE " + gameState.die1 + " " + gameState.die2);
            broadcast("TURN " + gameState.currentPlayer);
            broadcast("BAR " + gameState.bar[0] + " " + gameState.bar[1]);
            broadcast("BORNE_OFF " + gameState.borneOff[0] + " " + gameState.borneOff[1]);
        }

        private void closeSocket(Socket socket) {
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException ignored) {}
        }
    }
}







 














