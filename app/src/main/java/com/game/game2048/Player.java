package com.game.game2048;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;

class Player {
    private static final int MAX_STREAM_CNT = 8;
    private static final int INVALID_LOCAL_IDX = MAX_STREAM_CNT;

    private Context context;
    private SoundPool soundPool;
    private int[] sampleIdArray;
    private boolean[] sampleStatusArray;
    private int sampleCnt;

    Player(Context context) {
        this.context = context;
        sampleCnt = 0;
        sampleIdArray = new int[MAX_STREAM_CNT];
        sampleStatusArray = new boolean[MAX_STREAM_CNT];

        SoundPool.Builder builder = new SoundPool.Builder();
        builder.setMaxStreams(MAX_STREAM_CNT);
        AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
        attrBuilder.setLegacyStreamType(AudioManager.STREAM_MUSIC);
        builder.setAudioAttributes(attrBuilder.build());
        soundPool = builder.build();
    }

    private int getLocalIdx(int sampleId) {
        for (int i = 0; i < sampleCnt; i++) {
            if (sampleIdArray[i] == sampleId) {
                return i;
            }
        }
        return INVALID_LOCAL_IDX;
    }

    int add(int rawId) {
        if (sampleCnt >= MAX_STREAM_CNT) {
            return INVALID_LOCAL_IDX;
        }

        int localIdx = sampleCnt++;

        sampleIdArray[localIdx] = soundPool.load(context, rawId, 1);
        sampleStatusArray[localIdx] = false;

        if (localIdx == 0) {
            soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                @Override
                public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                    if (status == 0) {
                        int localIdx = getLocalIdx(sampleId);
                        if (localIdx != INVALID_LOCAL_IDX) {
                            sampleStatusArray[localIdx] = true;
                        }
                    }
                }
            });
        }

        return localIdx;
    }

    void play(int localIdx) {
        SoundEffect soundEffect = (SoundEffect)context;
        if (soundEffect.soundEffectEnabled() && localIdx >= 0 && localIdx < sampleCnt && sampleStatusArray[localIdx]) {
            soundPool.play(sampleIdArray[localIdx], 1, 1, 1, 0, 1);
        }
    }
}

interface SoundEffect {
    boolean soundEffectEnabled();
}
