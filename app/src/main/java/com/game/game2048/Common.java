package com.game.game2048;

import java.util.Arrays;

enum Direction {
    LEFT, RIGHT, UP, DOWN
}

enum GameStatus {
    PLAY, REPLAY
}

class Common {
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    static int min(int[] array) {
        return Arrays.stream(array).min().getAsInt();
    }

    static int max(int[] array) {
        return Arrays.stream(array).max().getAsInt();
    }

    // ON means play, OFF means replay
    static boolean gameStatusToBool(GameStatus gameStatus) {
        return (gameStatus == GameStatus.PLAY);
    }

    static GameStatus boolToGameStatus(boolean switchOn) {
        return switchOn ? GameStatus.PLAY : GameStatus.REPLAY;
    }

    static void assertCheck(boolean condition) {
        if (BuildConfig.DEBUG) {
            if (!condition) {
                throw new AssertionError();
            }
        }
    }
}
