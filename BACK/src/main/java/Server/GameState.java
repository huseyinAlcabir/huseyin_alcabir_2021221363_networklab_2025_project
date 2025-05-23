package Server;

import java.util.Arrays;

public class GameState {
    public int[] board = new int[24]; // positive: player 1, negative: player 2
    public int currentPlayer = 1;
    public int die1 = 0, die2 = 0;
    public boolean diceRolled = false;
    public int[] bar = new int[2]; // bar[0]=player1, bar[1]=player2
    public int[] borneOff = new int[2];
    private boolean doubleRoll = false;

    public GameState() {
        setupBoard();
    }

    public void setupBoard() {
        Arrays.fill(board, 0);
        // Standard starting position
        board[0] = 2; board[11] = 5; board[16] = 3; board[18] = 5; // Player 1
        board[23] = -2; board[12] = -5; board[7] = -3; board[5] = -5; // Player 2
        Arrays.fill(bar, 0);
        Arrays.fill(borneOff, 0);
    }

    public void rollDice() {
        die1 = (int)(Math.random() * 6) + 1;
        die2 = (int)(Math.random() * 6) + 1;
        if (die1 == die2) doubleRoll = true;
        diceRolled = true;
    }

    public boolean isValidMove(int from, int to) {
        if (from == -1) { // Moving from bar
            return isValidBarMove(to);
        }
        
        // Normal move validation
        if (from < 0 || from > 23 || to < 0 || to > 23) return false;
        
        int distance = (currentPlayer == 1) ? to - from : from - to;
        if (distance <= 0) return false; // Must move forward
        
        // Check if move matches dice
        if (!isValidDiceMove(distance)) return false;
        
        // Check checker ownership
        if ((currentPlayer == 1 && board[from] <= 0) || 
            (currentPlayer == 2 && board[from] >= 0)) return false;
            
        // Check destination
        if ((currentPlayer == 1 && board[to] < -1) || 
            (currentPlayer == 2 && board[to] > 1)) return false;
            
        return true;
    }

    private boolean isValidBarMove(int to) {
        int entryPoint = (currentPlayer == 1) ? 24 - die1 : die1 - 1;
        if (to != entryPoint && (die2 != 0 && to != (currentPlayer == 1 ? 24 - die2 : die2 - 1))) {
            return false;
        }
        
        if (currentPlayer == 1) {
            return bar[0] > 0 && board[to] >= -1;
        } else {
            return bar[1] > 0 && board[to] <= 1;
        }
    }

    private boolean isValidDiceMove(int distance) {
        if (die1 == distance || die2 == distance) return true;
        if (die1 != 0 && die2 != 0 && distance == die1 + die2) return true;
        return false;
    }

    public boolean moveChecker(int from, int to) {
        if (!isValidMove(from, to)) return false;
        
        // Execute the move
        if (from == -1) { // From bar
            moveFromBar(to);
        } else { // Normal move
            makeNormalMove(from, to);
        }
        
        // Update dice used
        updateUsedDice(from, to);
        
        // Check bearing off
        checkBearingOff(to);
        
        // Switch turn if no moves left
        if ((die1 == 0 && die2 == 0) || !hasValidMoves()) {
            switchTurn();
        }
        
        return true;
    }

    private void moveFromBar(int to) {
        if (currentPlayer == 1) {
            bar[0]--;
            if (board[to] == -1) { // Hit opponent
                board[to] = 1;
                bar[1]++;
            } else {
                board[to]++;
            }
        } else {
            bar[1]--;
            if (board[to] == 1) { // Hit opponent
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
            if (board[to] == -1) { // Hit opponent
                board[to] = 1;
                bar[1]++;
            } else {
                board[to]++;
            }
        } else {
            board[from]++;
            if (board[to] == 1) { // Hit opponent
                board[to] = -1;
                bar[0]++;
            } else {
                board[to]--;
            }
        }
    }

    private void updateUsedDice(int from, int to) {
        if (from == -1) {
            int distance = (currentPlayer == 1) ? 24 - to : to + 1;
            if (distance == die1) die1 = 0;
            else if (distance == die2) die2 = 0;
            return;
        }
        
        int distance = (currentPlayer == 1) ? to - from : from - to;
        if (distance == die1) die1 = 0;
        else if (distance == die2) die2 = 0;
        else if (die1 != 0 && die2 != 0 && distance == die1 + die2) {
            die1 = 0;
            die2 = 0;
        }
    }

    private void checkBearingOff(int to) {
        if (canBearOff(currentPlayer)) {
            if ((currentPlayer == 1 && to >= 18) || (currentPlayer == 2 && to <= 5)) {
                if (currentPlayer == 1) {
                    board[to]--;
                    borneOff[0]++;
                } else {
                    board[to]++;
                    borneOff[1]++;
                }
            }
        }
    }

    public boolean hasValidMoves() {
        if ((currentPlayer == 1 && bar[0] > 0) || (currentPlayer == 2 && bar[1] > 0)) {
            return canEnterFromBar();
        }
        
        for (int from = 0; from < 24; from++) {
            if ((currentPlayer == 1 && board[from] > 0) || (currentPlayer == 2 && board[from] < 0)) {
                if (die1 > 0 && hasMoveWithDie(from, die1)) return true;
                if (die2 > 0 && hasMoveWithDie(from, die2)) return true;
                if (die1 > 0 && die2 > 0 && hasMoveWithDie(from, die1 + die2)) return true;
            }
        }
        return false;
    }

    private boolean canEnterFromBar() {
        if (currentPlayer == 1 && bar[0] > 0) {
            int entry1 = 24 - die1;
            if (board[entry1] >= -1) return true;
            if (die2 > 0) {
                int entry2 = 24 - die2;
                if (board[entry2] >= -1) return true;
            }
        } else if (currentPlayer == 2 && bar[1] > 0) {
            int entry1 = die1 - 1;
            if (board[entry1] <= 1) return true;
            if (die2 > 0) {
                int entry2 = die2 - 1;
                if (board[entry2] <= 1) return true;
            }
        }
        return false;
    }

    private boolean hasMoveWithDie(int from, int die) {
        int to = (currentPlayer == 1) ? from + die : from - die;
        if (to < 0 || to > 23) {
            if (canBearOff(currentPlayer)) return true;
            return false;
        }
        return isValidMove(from, to);
    }

    public void switchTurn() {
        currentPlayer = 3 - currentPlayer;
        die1 = 0;
        die2 = 0;
        diceRolled = false;
        doubleRoll = false;
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

    public boolean isGameOver() {
        return borneOff[0] >= 15 || borneOff[1] >= 15;
    }

    public String boardToString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 24; i++) {
            sb.append(board[i]).append(" ");
        }
        sb.append(bar[0]).append(" ").append(bar[1]).append(" ");
        sb.append(borneOff[0]).append(" ").append(borneOff[1]);
        return sb.toString();
    }
}