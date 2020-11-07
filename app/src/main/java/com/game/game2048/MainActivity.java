package com.game.game2048;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {
    private boolean voiceEnabled = true;
    private boolean reviewing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.game_menu, menu);
        showMenu(menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        showMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.shortcut_new_game:
            case R.id.new_game:
                return true;

            case R.id.go_back:
            case R.id.shortcut_start_here:
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

    private void switchVoiceCtrlStatus() {
        voiceEnabled = !voiceEnabled;
        invalidateOptionsMenu();
    }

    private void switchGameMode() {
        reviewing = !reviewing;
        invalidateOptionsMenu();
    }

    private void showVoiceCtrlMenu(Menu menu) {
        MenuItem shortCtrlVoice = menu.findItem(R.id.shortcut_ctrl_voice);
        shortCtrlVoice.setIcon(voiceEnabled ? R.drawable.ic_voice_enabled : R.drawable.ic_voice_disabled);
        shortCtrlVoice.setTitle(voiceEnabled ? R.string.disable_sound_effect : R.string.enable_sound_effect);

        MenuItem ctrlVoice = menu.findItem(R.id.ctrl_voice);
        ctrlVoice.setChecked(voiceEnabled);
    }

    private void showMenuByMode(Menu menu) {
        MenuItem startHere = menu.findItem(R.id.shortcut_start_here);
        startHere.setVisible(reviewing);

        MenuItem newGame = menu.findItem(R.id.shortcut_new_game);
        newGame.setVisible(!reviewing);

        MenuItem goBack = menu.findItem(R.id.shortcut_go_back);
        goBack.setVisible(!reviewing);

        MenuItem ctrlVoice = menu.findItem(R.id.shortcut_ctrl_voice);
        ctrlVoice.setVisible(!reviewing);
    }

    private void showMenu(Menu menu) {
        showVoiceCtrlMenu(menu);
        showMenuByMode(menu);
    }
}
