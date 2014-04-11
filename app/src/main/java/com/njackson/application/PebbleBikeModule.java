package com.njackson.application;

/**
 * Created by server on 30/03/2014.
 */
import com.njackson.activities.MainActivity;
import com.njackson.fragments.AltitudeFragment;
import com.njackson.fragments.SpeedFragment;
import com.squareup.otto.Bus;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
        injects = {MainActivity.class, SpeedFragment.class, AltitudeFragment.class},
        library = true
)
public class PebbleBikeModule {
    // TODO put your application-specific providers here!

    @Provides @Singleton Bus providesBus() {
        return new Bus();
    }

}
