package com.game.game2048;

import android.annotation.SuppressLint;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

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

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    static int max(int[] array) {
        return Arrays.stream(array).max().getAsInt();
    }

    static String toDateString(long timeMillis, String format){
        @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        return simpleDateFormat.format(new Date(timeMillis));
}

    static void assertCheck(boolean condition) {
        if (BuildConfig.DEBUG) {
            if (!condition) {
                throw new AssertionError();
            }
        }
    }
}
