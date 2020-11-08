package com.game.game2048;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends AppCompatActivity implements SoundEffect {
    private boolean voiceEnabled = true;
    private boolean reviewing = false;
    private int actionCnt = 0;

    public boolean soundEffectEnabled() {
        return voiceEnabled;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.game_menu, menu);
        prepareOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        prepareOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.shortcut_new_game:
            case R.id.new_game:
                return true;

            case R.id.go_back:
                switchGameMode();
                return true;

            case R.id.shortcut_ctrl_voice:
            case R.id.ctrl_voice:
                switchVoiceCtrlStatus();
                // fall-through

            case R.id.shortcut_go_back:
            case R.id.load_game:
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void actionStartHere(View view) {
        actionCnt = 0;
        reviewing = false;

        invalidateOptionsMenu();
        findViewById(R.id.buttonContinue).setVisibility(View.INVISIBLE);
        findViewById(R.id.buttonStartHere).setVisibility(View.INVISIBLE);
    }

    public void actionContinue(View view) {
        reviewing = false;

        invalidateOptionsMenu();
        findViewById(R.id.buttonContinue).setVisibility(View.INVISIBLE);
        findViewById(R.id.buttonStartHere).setVisibility(View.INVISIBLE);
    }

    // used by GridsLayout
    @SuppressWarnings("unused")
    public void onFling(Direction dir) {
        boolean needRefreshMenus = actionCnt == 0;

        Log.i("Fling", "Direction " + dir);
        actionCnt++;

        if (needRefreshMenus) {
            invalidateOptionsMenu();
        }
    }

    private void prepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.shortcut_new_game).setVisible(!reviewing);
        menu.findItem(R.id.new_game).setVisible(!reviewing);

        menu.findItem(R.id.load_game).setVisible(!reviewing);

        menu.findItem(R.id.shortcut_go_back).setVisible(!reviewing && actionCnt > 0);
        menu.findItem(R.id.go_back).setVisible(!reviewing && actionCnt > 0);

        menu.findItem(R.id.shortcut_ctrl_voice)
                .setIcon(voiceEnabled ? R.drawable.ic_voice_enabled : R.drawable.ic_voice_disabled)
                .setTitle(voiceEnabled ? R.string.disable_sound_effect : R.string.enable_sound_effect);
        menu.findItem(R.id.ctrl_voice).setChecked(voiceEnabled);
    }

    private void switchVoiceCtrlStatus() {
        voiceEnabled = !voiceEnabled;
        invalidateOptionsMenu();
    }

    private void switchGameMode() {
        reviewing = !reviewing;
        invalidateOptionsMenu();
        findViewById(R.id.buttonContinue).setVisibility(View.VISIBLE);
        findViewById(R.id.buttonStartHere).setVisibility(View.VISIBLE);
    }
}
