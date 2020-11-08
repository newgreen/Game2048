package com.game.game2048;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements SoundEffect {
    private GameData data = null;
    private GameCore core;
    private int[][] history;
    private int[] historyScore;
    private Player player;
    private int soundDi;
    private int soundBeu;
    private int soundCher;
    private int soundUh;

    private void initSoundEffect() {
        player = new Player(this);
        soundDi = player.add(R.raw.di);
        soundBeu = player.add(R.raw.beu);
        soundCher = player.add(R.raw.cher);
        soundUh = player.add(R.raw.uh);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initSoundEffect();
        initData();
        showFrame();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.game_menu, menu);

        initData();
        prepareOptionsMenu(menu);
        return true;
    }

    private void initData() {
        if (data == null) {
            data = GameData.load(this);
            core = new GameCore(data);
        }
    }

    private TextView findTextViewById(@IdRes int id) {
        return findViewById(id);
    }

    private String toDateString(long timeMillis, String format){
        @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        return simpleDateFormat.format(new Date(timeMillis));
    }

    private boolean isPlaying() {
        return data.gameStatus == GameStatus.PLAY;
    }

    private boolean isReviewing() {
        return data.gameStatus == GameStatus.REPLAY;
    }

    private void refreshGrids() {
        findTextViewById(R.id.textViewMaxNumber).setText(String.valueOf(data.maxNumber));
        findTextViewById(R.id.textViewScore).setText(isPlaying() ?
                String.valueOf(data.score) : String.valueOf(historyScore[data.replayStep]));
        findTextViewById(R.id.textViewSteps).setText(isPlaying() ?
                String.valueOf(data.actionCount) : data.replayStep + "/" + data.actionCount);

        GridsLayout layout = findViewById(R.id.gridsLayout);
        layout.setGridNumbers(isPlaying() ? data.grids : history[data.replayStep]);
    }

    private void showFrame() {
        refreshGrids();

        String startTime = "Start@" + toDateString(data.startTime, "yyyy-MM-dd HH:mm:ss");
        findTextViewById(R.id.textViewStartTime).setText(startTime);

        findViewById(R.id.buttonContinue).setVisibility(isReviewing() ? View.VISIBLE : View.INVISIBLE);
        // length-1 is the last step which need not be rolled back to
        boolean canRollBack = isReviewing() && data.replayStep < history.length - 1;
        findViewById(R.id.buttonStartHere).setVisibility(canRollBack ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        prepareOptionsMenu(menu);
        return true;
    }

    private void prepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.shortcut_new_game).setVisible(isPlaying());
        menu.findItem(R.id.new_game).setVisible(isPlaying());

        menu.findItem(R.id.load_game).setVisible(isPlaying());

        boolean canGoBack = isPlaying() && data.actionCount > 0;
        menu.findItem(R.id.shortcut_go_back).setVisible(canGoBack);
        menu.findItem(R.id.go_back).setVisible(canGoBack);

        menu.findItem(R.id.shortcut_ctrl_voice)
                .setIcon(soundEffectEnabled() ? R.drawable.ic_voice_enabled : R.drawable.ic_voice_disabled)
                .setTitle(soundEffectEnabled() ? R.string.disable_sound_effect : R.string.enable_sound_effect);
        menu.findItem(R.id.ctrl_voice).setChecked(soundEffectEnabled());
    }

    public boolean soundEffectEnabled() {
        return data.gameSoundEffect;
    }

    private void switchVoiceCtrlStatus() {
        data.gameSoundEffect = !data.gameSoundEffect;
        invalidateOptionsMenu();
    }

    @Override
    protected void onPause() {
        super.onPause();
        data.store(this);
    }

    private void enterReviewMode() {
        historyScore = core.allocHistoryScore();
        history = core.getHistory(historyScore);

        if (data.replayStep == 0 || data.replayStep >= data.actionCount) {
            data.replayStep = data.actionCount - 1;
        }
        data.gameStatus = GameStatus.REPLAY;

        showFrame();
        invalidateOptionsMenu();
    }

    private void leaveReviewMode() {
        data.gameStatus = GameStatus.PLAY;
        history = null;
        historyScore = null;

        showFrame();
        invalidateOptionsMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.shortcut_new_game:
            case R.id.new_game:
                newGame();
                return true;

            case R.id.go_back:
                enterReviewMode();
                return true;

            case R.id.shortcut_ctrl_voice:
            case R.id.ctrl_voice:
                switchVoiceCtrlStatus();
                return true;

            case R.id.shortcut_go_back:
                if (data.actionCount > 0) {
                    core.backward(data.actionCount - 1);
                    showFrame();
                    invalidateOptionsMenu();
                }
                return true;

            case R.id.load_game:
                // TODO: load game from a data file
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onActionStartHere(View view) {
        core.backward(data.replayStep);
        leaveReviewMode();
    }

    public void onActionContinue(View view) {
        leaveReviewMode();
    }

    // used by GridsLayout
    @SuppressWarnings("unused")
    public void onFling(Direction dir) {
        if (isPlaying()) {
            onFlingDurPlaying(dir);
        } else if (isReviewing()) {
            onFlingDurReviewing(dir);
        }
    }

    private void onFlingDurPlaying(Direction dir) {
        int lastScore = data.score;
        boolean change = core.doAction(dir);
        if (change) {
            if (data.actionCount == 1) {
                invalidateOptionsMenu();
            }
            refreshGrids();
        }

        if (core.isGameOver()) {
            player.play(soundUh);
            new AlertDialog.Builder(this).setTitle("GAME OVER")
                    .setMessage("You have got " + data.score)
                    .setPositiveButton("Restart", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            newGame();
                        }
                    }).setNegativeButton("Review", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    enterReviewMode();
                }
            }).show();
        } else if (change){
            player.play(lastScore != data.score ? soundBeu : soundDi);
        }
    }

    private void onFlingDurReviewing(Direction dir) {
        if (dir == Direction.LEFT) {
            data.replayStep++;
            if (data.replayStep > data.actionCount) {
                data.replayStep = 0;
            }
        } else if (dir == Direction.RIGHT) {
            data.replayStep--;
            if (data.replayStep < 0) {
                data.replayStep = data.actionCount;
            }
        } else {
            return;
        }

        player.play(soundCher);
        showFrame();
    }

    private void newGame() {
        data = new GameData();
        core = new GameCore(data);

        showFrame();
        invalidateOptionsMenu();
    }
}
