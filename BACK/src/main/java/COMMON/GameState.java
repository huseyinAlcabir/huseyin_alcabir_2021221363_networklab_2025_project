package COMMON;


import java.util.*;

public class GameState {
    public int[] board = new int[24];
    public int currentPlayer = 1;
    public List<Integer> dice = new ArrayList<>();
    public int[] bar = new int[2];
    public int[] borneOff = new int[2];
    public boolean diceRolled = false;

    public GameState() {
        setupBoard();
    }

    public void setupBoard() {
        Arrays.fill(board, 0);
        board[0] = 2; board[11] = 5; board[16] = 3; board[18] = 5;
        board[23] = -2; board[12] = -5; board[7] = -3; board[5] = -5;
        Arrays.fill(bar, 0);
        Arrays.fill(borneOff, 0);
    }

    public void rollDice() {
        dice.clear();
        int d1 = (int)(Math.random() * 6) + 1;
        int d2 = (int)(Math.random() * 6) + 1;
        if (d1 == d2) {
            for (int i = 0; i < 4; i++) dice.add(d1);
        } else {
            dice.add(d1);
            dice.add(d2);
        }
        diceRolled = true;
    }

    public boolean moveChecker(int from, int to) {
        if (!isValidMove(from, to)) return false;

        int moveDist = getDistance(from, to);
        Integer dieUsed = findMatchingDie(moveDist);
        if (dieUsed == null) return false;

        if (from == -1) {
            moveFromBar(to);
        } else {
            makeNormalMove(from, to);
        }

        dice.remove(dieUsed);
        if (isBearingOffMove(to)) {
            bearOff(to);
        }

        if (dice.isEmpty() || !hasValidMoves()) {
            switchTurn();
        }

        return true;
    }

    public boolean isValidMove(int from, int to) {
        if (from == -1) {
            return canEnterFromBar(to);
        }

        if (!inBounds(from) || !inBounds(to)) return false;
        if (!ownsCheckerAt(from)) return false;

        int dist = getDistance(from, to);
        if (!hasMatchingDie(dist)) return false;

        if (!canLandOn(to)) return false;

        return true;
    }

    private int getDistance(int from, int to) {
        if (from == -1) {
            return currentPlayer == 1 ? 24 - to : to + 1;
        }
        return currentPlayer == 1 ? to - from : from - to;
    }

    private boolean hasMatchingDie(int dist) {
        return dice.contains(dist);
    }

    private Integer findMatchingDie(int dist) {
        for (int die : dice) {
            if (die == dist) return die;
        }
        return null;
    }

    private boolean canLandOn(int to) {
        if (!inBounds(to)) return false;
        int point = board[to];
        if (currentPlayer == 1) return point >= -1;
        else return point <= 1;
    }

    private boolean ownsCheckerAt(int pos) {
        int val = board[pos];
        return (currentPlayer == 1 && val > 0) || (currentPlayer == 2 && val < 0);
    }

    private boolean inBounds(int pos) {
        return pos >= 0 && pos < 24;
    }

    private boolean canEnterFromBar(int to) {
        int required = currentPlayer == 1 ? 24 - to : to;
        if (!hasMatchingDie(required + 1)) return false;
        if (!canLandOn(to)) return false;
        return currentPlayer == 1 ? bar[0] > 0 : bar[1] > 0;
    }

    private void moveFromBar(int to) {
        if (currentPlayer == 1) {
            bar[0]--;
            if (board[to] == -1) {
                board[to] = 1;
                bar[1]++;
            } else {
                board[to]++;
            }
        } else {
            bar[1]--;
            if (board[to] == 1) {
                board[to] = -1;
                bar[0]++;
            } else {
                board[to]--;
            }
        }
    }

    private void makeNormalMove(int from, int to) {
        if (currentPlayer == 1) {
            board[from]--;
            if (board[to] == -1) {
                board[to] = 1;
                bar[1]++;
            } else {
                board[to]++;
            }
        } else {
            board[from]++;
            if (board[to] == 1) {
                board[to] = -1;
                bar[0]++;
            } else {
                board[to]--;
            }
        }
    }

    private boolean isBearingOffMove(int to) {
        if (!canBearOff(currentPlayer)) return false;
        if (currentPlayer == 1 && to >= 18) return true;
        if (currentPlayer == 2 && to <= 5) return true;
        return false;
    }

    private void bearOff(int from) {
        if (currentPlayer == 1) {
            board[from]--;
            borneOff[0]++;
        } else {
            board[from]++;
            borneOff[1]++;
        }
    }

    public boolean canBearOff(int player) {
        if (player == 1) {
            if (bar[0] > 0) return false;
            for (int i = 0; i < 18; i++) {
                if (board[i] > 0) return false;
            }
        } else {
            if (bar[1] > 0) return false;
            for (int i = 6; i < 24; i++) {
                if (board[i] < 0) return false;
            }
        }
        return true;
    }

    public boolean hasValidMoves() {
        if ((currentPlayer == 1 && bar[0] > 0) || (currentPlayer == 2 && bar[1] > 0)) {
            for (int die : dice) {
                int to = currentPlayer == 1 ? 24 - die : die - 1;
                if (canEnterFromBar(to)) return true;
            }
            return false;
        }

        for (int from = 0; from < 24; from++) {
            if (!ownsCheckerAt(from)) continue;
            for (int die : dice) {
                int to = currentPlayer == 1 ? from + die : from - die;
                if (to >= 0 && to < 24 && isValidMove(from, to)) return true;
                if (canBearOff(currentPlayer) && isBearingOffMove(from)) return true;
            }
        }
        return false;
    }

    public void switchTurn() {
        currentPlayer = 3 - currentPlayer;
        dice.clear();
        diceRolled = false;
    }

    public boolean isGameOver() {
        return borneOff[0] >= 15 || borneOff[1] >= 15;
    }

    public String boardToString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 24; i++) {
            sb.append(board[i]).append(" ");
        }
        sb.append("| Bar: ").append(bar[0]).append("/").append(bar[1]);
        sb.append(" | Off: ").append(borneOff[0]).append("/").append(borneOff[1]);
        return sb.toString();
    }

    // ✅ الدالة الرئيسية لتشغيل اللعبة
    public static void main(String[] args) {
        GameState game = new GameState();
        Scanner scanner = new Scanner(System.in);

        System.out.println("🎲 Backgammon (نسخة Console بسيطة)");
        while (!game.isGameOver()) {
            if (!game.diceRolled) {
                System.out.println("\nPlayer " + game.currentPlayer + " roll the dice...");
                game.rollDice();
                System.out.println("Dice: " + game.dice);
            }

            System.out.println("Board: " + game.boardToString());
            System.out.print("Enter move (from to), -1 means from bar: ");

            try {
                int from = scanner.nextInt();
                int to = scanner.nextInt();

                if (!game.moveChecker(from, to)) {
                    System.out.println("❌ Invalid move, try again.");
                }
            } catch (Exception e) {
                System.out.println("⚠️ Invalid input. Try again.");
                scanner.nextLine(); // clear input buffer
            }
        }

        System.out.println("\n🏁 Player " + game.currentPlayer + " wins! 🎉");
    }
}
