package com.example.english_app;

import android.app.Activity;
import com.example.english_app.api.AuthRepository;

/** Reports deliberate foreground interaction, throttled to one request per minute. */
public abstract class UserSessionActivity extends Activity {
    @Override public void onUserInteraction() {
        super.onUserInteraction();
        AuthRepository.get(this).reportActivity();
    }
}
