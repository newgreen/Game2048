package com.game.game2048;

import android.content.ContextWrapper;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class GameData implements Serializable {
    private static final String dataBasePath = "game2048.dat";

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

    void store(ContextWrapper context) {
        try {
            FileOutputStream fileStream = context.openFileOutput(dataBasePath, ContextWrapper.MODE_PRIVATE);
            ObjectOutputStream objStream = new ObjectOutputStream(fileStream);
            objStream.writeObject(this);
            objStream.flush();
            objStream.close();
        } catch (IOException e) {
            e.printStackTrace(); // TODO: how to handle all exceptions and close file
        }
    }

    static GameData load(ContextWrapper context) {
        FileInputStream fileStream = null;
        ObjectInputStream objStream = null;
        GameData serializer = null;

        try {
            fileStream = context.openFileInput(dataBasePath);
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
}
