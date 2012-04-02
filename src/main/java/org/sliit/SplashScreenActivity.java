package org.sliit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import org.sliit.service.RepositoryController;
import org.sliit.service.SharedPreferencesHelper;


public class SplashScreenActivity extends Activity {

    private final Handler mHandler = new Handler();

    private final Runnable mPendingLauncherRunnable = new Runnable() {
        public void run() {
            RepositoryController mRepositoryController = new RepositoryController(SplashScreenActivity.this);
            mRepositoryController.open();

            Intent intent = new Intent(SplashScreenActivity.this, FeedTabActivity.class);
            startActivity(intent);

            mRepositoryController.close();
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        SharedPreferencesHelper.setPrefTabFeedId(this, SharedPreferencesHelper.getPrefStartChannel(this));

        setContentView(R.layout.splash_screen);

        Drawable backgroundDrawable = getResources().getDrawable(R.drawable.splash_background);
        backgroundDrawable.setDither(true);
        findViewById(android.R.id.content).setBackgroundDrawable(backgroundDrawable);
        mHandler.postDelayed(mPendingLauncherRunnable, SharedPreferencesHelper.getPrefSplashDuration(this));
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mPendingLauncherRunnable);
    }
}
