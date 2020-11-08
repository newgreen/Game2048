package com.game.game2048;

import android.content.ContextWrapper;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

@SuppressWarnings("WeakerAccess")
public class GameData implements Serializable {
    private static final String dataBasePath = "game2048.dat";
    private static final String dbNameFormat = "yyyy-MM-dd_HH-mm-ss";

    double randomFactorOf2 = 0.75;
    int column = 4;

    int score;
    int[] grids;

    byte[] gridNumberHistory;
    int gridNumberCount;
    byte[] actionHistory;
    int actionCount;

    GameStatus gameStatus = GameStatus.PLAY;
    boolean gameSoundEffect = true;
    int replayStep = 0;

    int maxNumber;
    int historyMaxNumber;

    long startTime;

    boolean isInit = false;

    void copy(GameData data) {
        randomFactorOf2 = data.randomFactorOf2;
        column = data.column;

        score = data.score;
        grids = data.grids.clone();

        gridNumberHistory = data.gridNumberHistory.clone();
        gridNumberCount = data.gridNumberCount;
        actionHistory = data.actionHistory.clone();
        actionCount = data.actionCount;

        maxNumber = data.maxNumber;
        startTime = data.startTime;

        gameStatus = GameStatus.PLAY;
        replayStep = 0;
        isInit = true;
    }

    String dbName() {
        return Common.toDateString(startTime, dbNameFormat) + ".dat";
    }

    void store(ContextWrapper context) {
        store(context, dataBasePath);
    }

    void store(ContextWrapper context, String dataPath) {
        try {
            FileOutputStream fileStream = context.openFileOutput(dataPath, ContextWrapper.MODE_PRIVATE);
            ObjectOutputStream objStream = new ObjectOutputStream(fileStream);
            objStream.writeObject(this);
            objStream.flush();
            objStream.close();
        } catch (IOException e) {
            e.printStackTrace(); // TODO: how to handle all exceptions and close file
        }
    }

    static GameData load(ContextWrapper context) {
        return load(context, dataBasePath);
    }

    static GameData load(ContextWrapper context, String dataPath) {
        FileInputStream fileStream = null;
        ObjectInputStream objStream = null;
        GameData serializer = null;

        try {
            fileStream = context.openFileInput(dataPath);
            objStream = new ObjectInputStream(fileStream);
            serializer = (GameData)objStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (objStream != null) {
                    objStream.close();
                } else if (fileStream != null) {
                    fileStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return serializer != null ? serializer : new GameData();
    }

    static GameHistory[] getHistory(ContextWrapper context) {
        String[] fileList = context.fileList();
        GameHistory[] histories = new GameHistory[fileList.length];
        int count = 0;

        for (String dbName:fileList) {
            if (!dbName.equals(dataBasePath)) {
                GameHistory history = GameHistory.getInstance(context, count, dbName);
                if (history != null) {
                    histories[count++] = history;
                }
            }
        }

        if (count == 0) {
            return null;
        }

        GameHistory[] result = new GameHistory[count];
        System.arraycopy(histories, 0, result, 0, count);
        return result;
    }
}

class GameHistory {
    int orderIndex;
    int maxNumber;
    String dbName;
    String startTime;

    static GameHistory getInstance(ContextWrapper context, int theOrderIndex, String theDbName) {
        GameData data = GameData.load(context, theDbName);
        if (!data.isInit) {
            return null;
        }

        GameHistory history = new GameHistory();
        history.orderIndex = theOrderIndex;
        history.maxNumber = data.maxNumber;
        history.dbName = theDbName;
        history.startTime = Common.toDateString(data.startTime, "MM/dd HH:mm");

        return history;
    }
}
