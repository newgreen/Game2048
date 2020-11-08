package com.game.game2048;

import java.util.Arrays;
import java.util.Random;

class GameCore {
    private static final int INIT_GRID_CNT = 2;
    private static final int HISTORY_EXPAND_LENGTH = 1024;
    private GameData gameData;
    private Action action;

    GameCore(GameData gameDataInput) {
        gameData = gameDataInput;
        action = new Action(gameData.column);

        if (!gameData.isInit) {
            initGameData();
        }

        int initGridCnt = gameData.gridNumberCount - gameData.actionCount;
        while (initGridCnt < INIT_GRID_CNT) {
            addGridNumber();
            initGridCnt++;
        }
    }

    private void initGameData() {
        gameData.startTime = System.currentTimeMillis();

        gameData.score = 0;
        gameData.grids = new int[gameData.column * gameData.column];

        gameData.gridNumberHistory = new byte[HISTORY_EXPAND_LENGTH];
        gameData.gridNumberCount = 0;
        gameData.actionHistory = new byte[HISTORY_EXPAND_LENGTH];
        gameData.actionCount = 0;

        gameData.gameStatus = GameStatus.PLAY;
        gameData.replayStep = 0;

        gameData.maxNumber = 2; // auto-gen 2,4; assume 2 is the initial max
        gameData.isInit = true;
    }

    private void setMaxNumber(int number) {
        if (number > gameData.maxNumber) {
            gameData.maxNumber = number;
        }

        if (gameData.maxNumber > gameData.historyMaxNumber) {
            gameData.historyMaxNumber = gameData.maxNumber;
        }
    }

    boolean doAction(Direction dir) {
        ActionResult result = action.execute(gameData.grids, dir);
        if (result.changed) {
            recordAction(dir);
            addGridNumber();
            gameData.score += result.score;
            setMaxNumber(result.maxNumber);
        }
        return result.changed;
    }

    boolean isGameOver() {
        if (Common.min(gameData.grids) == 0) {
            return false;
        }

        int[] tmpGrids = gameData.grids.clone();
        for (int i = 0; i < Direction.values().length; i++) {
            if (action.execute(tmpGrids, Direction.values()[i]).changed) {
                return false;
            }
        }
        return true;
    }

    int[][] getHistory() {
        ActionResult result = new ActionResult();
        return getHistoryWithResult(result, true);
    }

    void backward(int stepIndex) {
        if (stepIndex < gameData.actionCount && stepIndex + 2 < gameData.gridNumberCount) {
            gameData.actionCount = stepIndex;
            gameData.gridNumberCount = stepIndex + 2;

            ActionResult result = new ActionResult();
            int[][] history = getHistoryWithResult(result, false);

            gameData.grids = history[gameData.actionCount].clone();
            gameData.score = result.score;
            gameData.maxNumber = Common.max(gameData.grids);
            gameData.gameStatus = GameStatus.PLAY;
        }
    }

    private int[][] getHistoryWithResult(ActionResult result, boolean needCheck) {
        // plus 1 because of first grids before any action
        int[][] history = new int[gameData.actionCount + 1][gameData.column * gameData.column];
        int historyIndex = 0;

        int[] replayGrids = new int[gameData.column * gameData.column];

        // generate the first grids before any action
        for (int i = 0; i < INIT_GRID_CNT; i++) {
            addGridNumber(replayGrids, GridNumber.decode(gameData.gridNumberHistory[i]));
        }
        System.arraycopy(replayGrids, 0, history[historyIndex++], 0, replayGrids.length);

        // replay actions
        for (int i = 0; i < gameData.actionCount; i++) {
            ActionResult curr = action.execute(replayGrids, Direction.values()[gameData.actionHistory[i]]);
            result.score += curr.score;
            addGridNumber(replayGrids, GridNumber.decode(gameData.gridNumberHistory[i + INIT_GRID_CNT]));
            System.arraycopy(replayGrids, 0, history[historyIndex++], 0, replayGrids.length);
        }

        // after replayed, replayGrids should be same with grids or bugs somewhere
        if (needCheck) {
            for (int i = 0; i < replayGrids.length; i++) {
                Common.assertCheck(replayGrids[i] == gameData.grids[i]);
            }
        }

        return history;
    }

    private void addGridNumber(int[] grids, GridNumber gridNum) {
        grids[gridNum.location] = gridNum.number;
    }

    private void addGridNumber() {
        GridNumber gridNum = new GridNumber(gameData.grids, gameData.randomFactorOf2);
        if (gridNum.number != 0) {
            Common.assertCheck(gridNum.location >= 0);
            Common.assertCheck(gridNum.location < gameData.column * gameData.column);
            Common.assertCheck(gameData.grids[gridNum.location] == 0);

            addGridNumber(gameData.grids, gridNum);
            recordGridNumber(gridNum);
        }
    }

    private void recordGridNumber(GridNumber gridNum) {
        if (gameData.gridNumberCount == gameData.gridNumberHistory.length) {
            gameData.gridNumberHistory = Arrays.copyOf(gameData.gridNumberHistory,
                    gameData.gridNumberHistory.length + HISTORY_EXPAND_LENGTH);
        }

        gameData.gridNumberHistory[gameData.gridNumberCount++] = gridNum.encode();
    }

    private void recordAction(Direction dir) {
        if (gameData.actionCount == gameData.actionHistory.length) {
            gameData.actionHistory = Arrays.copyOf(gameData.actionHistory, gameData.actionHistory.length + HISTORY_EXPAND_LENGTH);
        }

        gameData.actionHistory[gameData.actionCount++] = (byte)dir.ordinal();
    }
}

class GridNumber {
    int location;
    int number;

    GridNumber(int[] grids, double randomFactorOf2) {
        int[] idleLocation = new int[grids.length];
        int idleCnt = 0;

        for (int i = 0; i < grids.length; i++) {
            if (grids[i] == 0) {
                idleLocation[idleCnt++] = i;
            }
        }

        if (idleCnt > 0) {
            Random random = new Random();
            location = idleLocation[random.nextInt(idleCnt)];
            number = Math.random() > randomFactorOf2 ? 4 : 2;
            gridsLength = grids.length;
        }
    }

    private int gridsLength;

    private GridNumber(int location, int number) {
        this.location = location;
        this.number = number;
    }

    private static final int MAX_LOCATION = 0x3F;

    byte encode() {
        Common.assertCheck(gridsLength <= MAX_LOCATION + 1);
        return (byte)((((number == 4) ? 1 : 0) << 6) | (location & MAX_LOCATION));
    }

    static GridNumber decode(byte code) {
        return new GridNumber((int)code & MAX_LOCATION, ((((int)code >> 6) & 0x1) == 1) ? 4 : 2);
    }
}

class ActionResult {
    boolean changed;
    int score;
    int maxNumber;
}

class Action {
    Action(int column) {
        initIndexMatrix(column);
    }

    ActionResult execute(int[] grids, Direction dir) {
        ActionResult result = new ActionResult();

        int[][] indexMatrix = getIndexByDirection(dir);
        for (int[] index:indexMatrix) {
            executeOnSingleLine(grids, index, result);
        }

        return result;
    }

    private void executeOnSingleLine(int[] grids, int[] index, ActionResult result) {
        int[] line = new int[index.length];
        int cnt = 0;

        // fetch non-zero numbers
        for (int i:index) {
            if (grids[i] != 0) {
                line[cnt++] = grids[i];
            }
        }

        boolean moved = false;
        for (int i = 0; i < index.length; i++) {
            if (grids[index[i]] != line[i]) {
                moved = true;
                break;
            }
        }

        // merge adjacent same numbers and stat score
        int score = 0;
        boolean merged = false;

        for (int i = 1; i < cnt; i++) {
            if (line[i-1] == line[i]) {
                line[i-1] += line[i];
                score += line[i-1];
                merged = true;

                if (line[i - 1] > result.maxNumber) {
                    result.maxNumber = line[i - 1];
                }

                // remove line[i], and move one step every number behind line[i]
                System.arraycopy(line, i+1, line, i, cnt - i - 1);

                /* after merging, cnt should be reduced,
                 * and the last number should be cleared which has been moved */
                line[--cnt] = 0;
            }
        }

        // update grids
        for (int i = 0; i < index.length; i++) {
            grids[index[i]] = line[i];
        }

        if (moved || merged) {
            result.score += score;
            result.changed = true;
        }
    }

    private int[][] getIndexByDirection(Direction dir) {
        switch (dir) {
            case LEFT:
                return indexForLeft;
            case RIGHT:
                return indexForRight;
            case UP:
                return indexForUp;
            case DOWN:
            default:
                return indexForDown;
        }
    }

    private void initIndexMatrix(int column) {
        /*  index for left
         *   0,  1,  2,  3,
         *   4,  5,  6,  7,
         *   8,  9, 10, 11,
         *  12, 13, 14, 15,
         */
        indexForLeft = new int[column][column];
        for (int r = 0; r < column; r++) {
            for (int c = 0; c < column; c++) {
                indexForLeft[r][c] = r * column + c;
            }
        }

        /*  index for right
         *   3,  2,  1,  0,
         *   7,  6,  5,  4,
         *  11, 10,  9,  8,
         *  15, 14, 13, 12,
         *
         *  Note: 1st row from-left-to-right: (r+1)*column-(1,2,3,4)
         */
        indexForRight = new int[column][column];
        for (int r = 0; r < column; r++) {
            for (int c = 0; c < column; c++) {
                indexForRight[r][c] = (r + 1) * column - (c + 1);
            }
        }

        /*  index for up
         *  0, 4,  8, 12,
         *  1, 5,  9, 13,
         *  2, 6, 10, 14,
         *  3, 7, 11, 15,
         *
         * Note: 1st column from-up-to-down: c*column+(0,1,2,3)
         */
        indexForUp = new int[column][column];
        for (int r = 0; r < column; r++) {
            for (int c = 0; c < column; c++) {
                indexForUp[r][c] = c * column + r;
            }
        }

        /*  index for down
         *  12,  8, 4, 0,
         *  13,  9, 5, 1,
         *  14, 10, 6, 2,
         *  15, 11, 7, 3,
         *
         * Note: 4th column from-up-to-down: (column-c-1)*column+(0,1,2,3)
         */
        indexForDown = new int[column][column];
        for (int r = 0; r < column; r++) {
            for (int c = 0; c < column; c++) {
                indexForDown[r][c] = (column - c - 1) * column + r;
            }
        }
    }

    private int[][] indexForLeft  = null;
    private int[][] indexForRight = null;
    private int[][] indexForUp    = null;
    private int[][] indexForDown  = null;
}
