package com.felipe.showeriocloud.Activities.ShowerIO;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.felipe.showeriocloud.Activities.Fragments.WalkthroughFragment;
import com.felipe.showeriocloud.R;
import com.github.paolorotolo.appintro.AppIntro;

/**
 * Created by HP on 10/23/2016.
 */
public class Walkthrough extends AppIntro {
    // Please DO NOT override onCreate. Use init
    @Override
    public void init(Bundle savedInstanceState) {

        //adding the three slides for introduction app you can ad as many you needed

        addSlide(WalkthroughFragment.newInstance(R.layout.walkthrough1));
        addSlide(WalkthroughFragment.newInstance(R.layout.walkthrough2));
        addSlide(WalkthroughFragment.newInstance(R.layout.walkthrough3));
        addSlide(WalkthroughFragment.newInstance(R.layout.walkthrough4));

        // Show and Hide Skip and Done buttons
        showStatusBar(false);
        showSkipButton(false);

        // Turn vibration on and set intensity
        // You will need to add VIBRATE permission in Manifest file
        setVibrate(false);
        setVibrateIntensity(30);

        //Add animation to the intro slider
        setDepthAnimation();
    }

    @Override
    public void onSkipPressed() {
        // Do something here when users click or tap on Skip button.
        Toast.makeText(getApplicationContext(),
                getString(R.string.app_intro_skip), Toast.LENGTH_SHORT).show();
        Intent i = new Intent(getApplicationContext(), ShowerNavigationDrawer.class);
        startActivity(i);
    }

    @Override
    public void onNextPressed() {
        // Do something here when users click or tap on Next button.
    }

    @Override
    public void onDonePressed() {
        // Do something here when users click or tap tap on Done button.
        Intent i = new Intent(getApplicationContext(), ShowerNavigationDrawer.class);
        startActivity(i);
    }

    @Override
    public void onBackPressed() {
        Intent i = new Intent(getApplicationContext(), ShowerNavigationDrawer.class);
        startActivity(i);
    }

    @Override
    public void onSlideChanged() {
        // Do something here when slide is changed
    }
}