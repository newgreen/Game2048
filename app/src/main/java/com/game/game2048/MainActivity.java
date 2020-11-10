package com.game.game2048;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements SoundEffect {
    private GameData data = null;
    private GameCore core;

    private int[][] history;
    private int[] historyScore;

    private static final int GROUP_ID_LOAD = 0;
    private static final int GROUP_ID_DEL = 1;

    private boolean needRefreshLoadGameList = true;
    private GameHistory[] gameHistories = null;

    private Player player;
    private int soundDi;
    private int soundBeu;
    private int soundCher;
    private int soundUh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initSoundEffect();
        initData();
        showFrame();
    }

    private void initSoundEffect() {
        player = new Player(this);
        soundDi = player.add(R.raw.di);
        soundBeu = player.add(R.raw.beu);
        soundCher = player.add(R.raw.cher);
        soundUh = player.add(R.raw.uh);
    }

    private void initData() {
        if (data == null) {
            data = GameData.load(this);
            core = new GameCore(data);
            data.store(this);
            if (isReviewing()) {
                enterReviewMode();
            }
        }
    }

    private void showFrame() {
        refreshGrids();

        String startTime = "Start@" + Common.toDateString(data.startTime, "yyyy-MM-dd HH:mm:ss");
        findTextViewById(R.id.textViewStartTime).setText(startTime);

        findViewById(R.id.buttonContinue).setVisibility(isReviewing() ? View.VISIBLE : View.INVISIBLE);
        // length-1 is the last step which need not be rolled back to
        boolean canRollBack = isReviewing() && data.replayStep < history.length - 1;
        findViewById(R.id.buttonStartHere).setVisibility(canRollBack ? View.VISIBLE : View.INVISIBLE);
    }

    private boolean isReviewing() {
        return data.gameStatus == GameStatus.REPLAY;
    }

    private boolean isPlaying() {
        return data.gameStatus == GameStatus.PLAY;
    }

    private void enterReviewMode() {
        historyScore = core.allocHistoryScore();
        history = core.getHistory(historyScore);

        if (data.replayStep == 0 || data.replayStep >= data.actionCount) {
            data.replayStep = data.actionCount - 1;
        }
        data.gameStatus = GameStatus.REPLAY;
    }

    private void leaveReviewMode() {
        data.gameStatus = GameStatus.PLAY;
        history = null;
        historyScore = null;

        showFrame();
        invalidateOptionsMenu();
    }

    private void refreshGrids() {
        findTextViewById(R.id.textViewMaxNumber).setText(String.valueOf(data.historyMaxNumber));
        findTextViewById(R.id.textViewScore).setText(isPlaying() ?
                String.valueOf(data.score) : String.valueOf(historyScore[data.replayStep]));
        findTextViewById(R.id.textViewSteps).setText(isPlaying() ?
                String.valueOf(data.actionCount) : data.replayStep + "/" + data.actionCount);

        GridsLayout layout = findViewById(R.id.gridsLayout);
        layout.setGridNumbers(isPlaying() ? data.grids : history[data.replayStep]);
    }

    private TextView findTextViewById(@IdRes int id) {
        return findViewById(id);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.game_menu, menu);

        initData();
        prepareOptionsMenu(menu);
        return true;
    }

    private void prepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.shortcut_new_game).setVisible(isPlaying());
        menu.findItem(R.id.new_game).setVisible(isPlaying());

        refreshLoadGameList(menu);

        boolean canGoBack = isPlaying() && data.actionCount > 0;
        menu.findItem(R.id.shortcut_go_back).setVisible(canGoBack);
        menu.findItem(R.id.go_back).setVisible(canGoBack);

        menu.findItem(R.id.shortcut_ctrl_voice)
                .setIcon(soundEffectEnabled() ? R.drawable.ic_voice_enabled : R.drawable.ic_voice_disabled)
                .setTitle(soundEffectEnabled() ? R.string.disable_sound_effect : R.string.enable_sound_effect);
        menu.findItem(R.id.ctrl_voice).setChecked(soundEffectEnabled());
    }

    private void refreshLoadGameList(Menu menu) {
        if (needRefreshLoadGameList) {
            needRefreshLoadGameList = false;

            SubMenu loadSubMenu = menu.findItem(R.id.load_game).getSubMenu();
            SubMenu delSubMenu = menu.findItem(R.id.del_game).getSubMenu();

            loadSubMenu.removeGroup(GROUP_ID_LOAD);
            delSubMenu.removeGroup(GROUP_ID_DEL);

            gameHistories = GameData.getHistory(this);
            if (gameHistories != null) {
                for (GameHistory history : gameHistories) {
                    String title = "[" + history.startTime + "]" + history.maxNumber;
                    loadSubMenu.add(GROUP_ID_LOAD, history.orderIndex, history.orderIndex, title);
                    delSubMenu.add(GROUP_ID_DEL, history.orderIndex, history.orderIndex, title);
                }
            }
        }

        menu.findItem(R.id.load_game).setVisible(isPlaying() && gameHistories != null);
        menu.findItem(R.id.del_game).setVisible(isPlaying() && gameHistories != null);
    }

    public boolean soundEffectEnabled() {
        return data.gameSoundEffect;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        prepareOptionsMenu(menu);
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        data.store(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (gameHistories != null && itemId >= 0 && itemId < gameHistories.length) {
            if (item.getGroupId() == GROUP_ID_LOAD) {
                loadGame(gameHistories[itemId].dbName);
            } else if (item.getGroupId() == GROUP_ID_DEL) {
                deleteGame(gameHistories[itemId].dbName);
            }
            return true;
        }

        switch (itemId) {
            case R.id.shortcut_new_game:
            case R.id.new_game:
                newGame();
                return true;

            case R.id.go_back:
                enterReviewMode();
                showFrame();
                invalidateOptionsMenu();
                return true;

            case R.id.shortcut_ctrl_voice:
            case R.id.ctrl_voice:
                switchVoiceCtrlStatus();
                return true;

            case R.id.shortcut_go_back:
                if (data.actionCount > 0) {
                    core.backward(data.actionCount - 1);
                    data.replayStep = 0;
                    showFrame();
                    invalidateOptionsMenu();
                }
                return true;

            case R.id.show_version:
                Toast.makeText(this, getResources().getString(R.string.software_version), Toast.LENGTH_SHORT).show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void loadGame(String theDbName) {
        conditionBackupGame(data);

        GameData theLoadData = GameData.load(this, theDbName);
        deleteGame(theDbName);

        if (theLoadData != null) {
            data.copy(theLoadData);
            core = new GameCore(data);
            data.store(this);

            showFrame();
        } else {
            Toast.makeText(this, "Fail to load the game", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteGame(String theDbName) {
        deleteFile(theDbName);
        refreshLoadGameList();
    }

    private void newGame() {
        conditionBackupGame(data);

        data.isInit = false;
        core = new GameCore(data);
        data.store(this);

        showFrame();
        invalidateOptionsMenu();
    }

    private void conditionBackupGame(GameData game) {
        if (game.actionCount > 0) {
            game.store(this, game.dbName());
            refreshLoadGameList();
        }
    }

    private void refreshLoadGameList() {
        needRefreshLoadGameList = true;
        invalidateOptionsMenu();
    }

    private void switchVoiceCtrlStatus() {
        data.gameSoundEffect = !data.gameSoundEffect;
        invalidateOptionsMenu();
    }

    public void onActionStartHere(View view) {
        core.backward(data.replayStep);
        data.replayStep = 0;
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
            data.replayStep = 0;
            refreshGrids();
            data.store(this);
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
                    showFrame();
                    invalidateOptionsMenu();
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

        data.store(this);
        player.play(soundCher);
        showFrame();
    }
}
