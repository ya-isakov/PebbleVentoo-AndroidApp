package com.njackson.test.fragments;

import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.MediumTest;
import android.widget.LinearLayout;

import com.njackson.R;
import com.njackson.application.PebbleBikeModule;
import com.njackson.events.GPSService.NewAltitiudeEvent;
import com.njackson.fragments.AltitudeFragment;
import com.njackson.test.application.TestApplication;
import com.squareup.otto.Bus;

import javax.inject.Inject;

import dagger.Module;
import dagger.ObjectGraph;

/**
 * Created by server on 04/04/2014.
 */
public class AltitudeFragmentTest extends ActivityInstrumentationTestCase2<AltitudeFragment> {

    private AltitudeFragment _activity;

    @Inject Bus _bus;

    @Module(
            includes = PebbleBikeModule.class,
            injects = AltitudeFragmentTest.class,
            overrides = true
    )
    static class TestModule {
        /*
        @Provides @Singleton Heater provideHeater() {
            return Mockito.mock(Heater.class);
        }
        */
    }

    public AltitudeFragmentTest() { super(AltitudeFragment.class); }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.getInstrumentation().waitForIdleSync(); // this is needed for emulator versions 2.3 as the application is instantiated on a separate thread.
        TestApplication app = (TestApplication)this.getInstrumentation().getTargetContext().getApplicationContext();

        app.setObjectGraph(ObjectGraph.create(new TestModule()));
        app.inject(this);

        setActivityInitialTouchMode(false);

        _activity = getActivity();
    }

    @MediumTest
    public void test_Element_Exists() {
        LinearLayout layout = (LinearLayout)_activity.findViewById(R.id.altitude_main_container);
        int elements = layout.getChildCount();

        assertEquals("Expected 14 altitude bars",14,elements);
    }

    @MediumTest
    public void test_Activity_Responds_To_NewAltitudeEvent() {

        final NewAltitiudeEvent event = new NewAltitiudeEvent();

        _activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _bus.post(event);
            }
        });

    }

}
