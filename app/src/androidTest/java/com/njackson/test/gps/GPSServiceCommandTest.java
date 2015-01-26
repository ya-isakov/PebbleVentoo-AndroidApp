package com.njackson.test.gps;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.njackson.application.modules.AndroidModule;
import com.njackson.application.modules.ForApplication;
import com.njackson.events.GPSServiceCommand.ChangeRefreshInterval;
import com.njackson.events.GPSServiceCommand.GPSChangeState;
import com.njackson.events.GPSServiceCommand.GPSStatus;
import com.njackson.events.GPSServiceCommand.ResetGPSState;
import com.njackson.events.GPSServiceCommand.NewLocation;
import com.njackson.events.base.BaseChangeState;
import com.njackson.events.base.BaseStatus;
import com.njackson.gps.GPSSensorEventListener;
import com.njackson.gps.GPSServiceCommand;
import com.njackson.gps.IForegroundServiceStarter;
import com.njackson.test.application.TestApplication;
import com.njackson.utils.time.ITime;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.mockito.ArgumentCaptor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyFloat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by server on 28/04/2014.
 */
public class GPSServiceCommandTest extends AndroidTestCase {

    private static final String TAG = "PB-GPSServiceTest";

    @Inject Bus _bus = new Bus();
    @Inject LocationManager _mockLocationManager;
    @Inject SensorManager _mockSensorManager;
    @Inject SharedPreferences _mockPreferences;
    private SharedPreferences.Editor _mockEditor;
    private static IForegroundServiceStarter _mockServiceStarter;
    private static ITime _mockTime;
    private static TestApplication _app;
    private GPSServiceCommand _serviceCommand;

    @Module(
            includes = AndroidModule.class,
            injects = GPSServiceCommandTest.class,
            overrides = true,
            complete = false
    )
    static class TestModule {
        @Provides
        @Singleton
        LocationManager provideLocationManager() {
            return mock(LocationManager.class);
        }

        @Provides
        @Singleton
        SensorManager provideSensorManager() { return mock(SensorManager.class); }

        @Provides
        @Singleton
        SharedPreferences provideSharedPreferences() {
            return mock(SharedPreferences.class);
        }

        @Provides
        IForegroundServiceStarter providesForegroundServiceStarter() { return _mockServiceStarter; }

        @Provides
        ITime providesTime() { return _mockTime; }

        @Provides @Singleton @ForApplication
        Context provideApplicationContext() {
            return _app;
        }

        @Provides @Singleton
        Bus providesBus() { return mock(Bus.class); }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());

        _app = spy(new TestApplication());
        _app.setObjectGraph(ObjectGraph.create(TestModule.class));
        _app.inject(this);

        setupMocks();

        _serviceCommand = new GPSServiceCommand();
    }

    private void setupMocks() {
        _mockTime = mock(ITime.class);
        _mockServiceStarter = mock(IForegroundServiceStarter.class);
        _mockEditor = mock(SharedPreferences.Editor.class, RETURNS_DEEP_STUBS);
        when(_mockPreferences.edit()).thenReturn(_mockEditor);
    }

    @SmallTest
    public void testRegistersWithInjectionOnCreate() throws Exception {
        _serviceCommand.execute(_app);

        verify(_app,times(1)).inject(_serviceCommand);
    }

    @SmallTest
    public void testBroadcastEventOnLocationDisabled() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(false);

        _serviceCommand.execute(_app);
        _serviceCommand.onGPSChangeState(new GPSChangeState(BaseChangeState.State.START));

        ArgumentCaptor<GPSStatus> captor = ArgumentCaptor.forClass(GPSStatus.class);
        verify(_bus,timeout(1000).times(1)).post(captor.capture());

        assertEquals(BaseStatus.Status.DISABLED, captor.getValue().getStatus());
    }

    @SmallTest
    public void testStartEvent() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);

        _serviceCommand.execute(_app);
        _serviceCommand.onGPSChangeState(new GPSChangeState(BaseChangeState.State.START));

        ArgumentCaptor<GPSStatus> captor = ArgumentCaptor.forClass(GPSStatus.class);
        verify(_bus,timeout(1000).times(1)).post(captor.capture());

        assertEquals(BaseStatus.Status.STARTED, captor.getValue().getStatus());
    }

    @SmallTest
    public void testStartEventOnlySentOnce() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);

        _serviceCommand.execute(_app);
        _serviceCommand.onGPSChangeState(new GPSChangeState(BaseChangeState.State.START));
        _serviceCommand.onGPSChangeState(new GPSChangeState(BaseChangeState.State.START));

        ArgumentCaptor<GPSStatus> captor = ArgumentCaptor.forClass(GPSStatus.class);
        verify(_bus,timeout(1000).times(1)).post(captor.capture());

        assertEquals(BaseStatus.Status.STARTED, captor.getValue().getStatus());
    }

    @SmallTest
    public void testStopEvent() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);

        _serviceCommand.execute(_app);
        _serviceCommand.onGPSChangeState(new GPSChangeState(BaseChangeState.State.STOP));

        ArgumentCaptor<GPSStatus> captor = ArgumentCaptor.forClass(GPSStatus.class);
        verify(_bus,timeout(1000).times(1)).post(captor.capture());

        assertEquals(BaseStatus.Status.STOPPED, captor.getValue().getStatus());
    }

    @SmallTest
    public void testStopEventOnlySentOnce() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);

        _serviceCommand.execute(_app);
        _serviceCommand.onGPSChangeState(new GPSChangeState(BaseChangeState.State.STOP));
        _serviceCommand.onGPSChangeState(new GPSChangeState(BaseChangeState.State.STOP));

        ArgumentCaptor<GPSStatus> captor = ArgumentCaptor.forClass(GPSStatus.class);
        verify(_bus,timeout(1000).times(1)).post(captor.capture());

        assertEquals(BaseStatus.Status.STOPPED, captor.getValue().getStatus());
    }


    @SmallTest
    public void testBroadcastStatusOnAnnounceEvent() throws Exception {
        _serviceCommand.execute(_app);
        _serviceCommand.onGPSChangeState(new GPSChangeState(BaseChangeState.State.ANNOUNCE_STATE));

        ArgumentCaptor<GPSStatus> captor = ArgumentCaptor.forClass(GPSStatus.class);
        verify(_bus,timeout(1000).times(1)).post(captor.capture());

        assertEquals(BaseStatus.Status.DISABLED, captor.getValue().getStatus());
    }

    @SmallTest
    public void testBroadcastEventOnLocationChange() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);

        _serviceCommand.execute(_app);
        _serviceCommand.onGPSChangeState(new GPSChangeState(BaseChangeState.State.START));

        ArgumentCaptor<LocationListener> locationListenerCaptor = ArgumentCaptor.forClass(LocationListener.class);
        verify(_mockLocationManager,timeout(1000).times(1)).requestLocationUpdates(
                anyString(),
                anyLong(),
                anyFloat(),
                locationListenerCaptor.capture());

        Location location = new Location("location");
        LocationListener listenerArgument = locationListenerCaptor.getValue();
        listenerArgument.onLocationChanged(location);

        verify(_bus,timeout(1000).times(2)).post(any(NewLocation.class));
    }

    @SmallTest
    public void testHandlesGPSLocationReset() throws Exception {
        _serviceCommand.execute(_app);

        _serviceCommand.onResetGPSStateEvent(new ResetGPSState());

        verify(_mockEditor, timeout(200).times(1)).putFloat("GPS_DISTANCE", 0.0f);
        verify(_mockEditor,timeout(200).times(1)).commit();
    }

    @SmallTest
    public void testHandlesGPSStartCommand() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);
        _serviceCommand.execute(_app);
        _serviceCommand.onGPSChangeState(new GPSChangeState(GPSChangeState.State.START));

        verify(_mockLocationManager,timeout(2000).times(1)).requestLocationUpdates(
                anyString(),
                anyLong(),
                anyFloat(),
                any(LocationListener.class));
    }

    @SmallTest
    public void testHandlesRefreshInterval() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);

        _serviceCommand.execute(_app);
        _serviceCommand.onGPSChangeState(new GPSChangeState(GPSChangeState.State.START));

        ArgumentCaptor<LocationListener> locationListenerCaptor = ArgumentCaptor.forClass(LocationListener.class);
        verify(_mockLocationManager,timeout(200).times(1)).requestLocationUpdates(
                anyString(),
                anyLong(),
                anyFloat(),
                locationListenerCaptor.capture());

        int refreshInterval = 200;
        _serviceCommand.onGPSRefreshChangeEvent(new ChangeRefreshInterval(refreshInterval));

        verify(_mockLocationManager, timeout(200).times(1)).removeUpdates((LocationListener) anyObject());
        verify(_mockLocationManager, timeout(200).times(1)).requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                refreshInterval,
                2,
                locationListenerCaptor.getValue()
        );
    }

    @SmallTest
    public void testSavesStateOnStop() throws Exception {
        _serviceCommand.execute(_app);

        _serviceCommand.execute(_app);
        _serviceCommand.onGPSChangeState(new GPSChangeState(GPSChangeState.State.START));
        _serviceCommand.onGPSChangeState(new GPSChangeState(GPSChangeState.State.STOP));

        verify(_mockEditor, timeout(200).times(1)).commit();
    }

    @SmallTest
    public void testRegistersNmeaListenerOnStart() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);

        _serviceCommand.execute(_app);
        _serviceCommand.onGPSChangeState(new GPSChangeState(GPSChangeState.State.START));

        verify(_mockLocationManager, timeout(2000).times(1)).addNmeaListener(any(GpsStatus.NmeaListener.class));
    }

    @SmallTest
    public void testRemovesNmeaListenerOnStop() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);

        _serviceCommand.execute(_app);
        _serviceCommand.onGPSChangeState(new GPSChangeState(GPSChangeState.State.START));
        _serviceCommand.onGPSChangeState(new GPSChangeState(GPSChangeState.State.STOP));

        verify(_mockLocationManager, timeout(2000).times(1)).removeNmeaListener(any(GpsStatus.NmeaListener.class));
    }

    @SmallTest
    public void testRegistersSensorOnStart() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);

        _serviceCommand.execute(_app);
        _serviceCommand.onGPSChangeState(new GPSChangeState(GPSChangeState.State.START));

        verify(_mockSensorManager,timeout(2000).times(1)).registerListener(any(GPSSensorEventListener.class),any(Sensor.class),anyInt());
    }

    @SmallTest
    public void testRemovesSensorOnStop() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);

        _serviceCommand.execute(_app);
        _serviceCommand.onGPSChangeState(new GPSChangeState(GPSChangeState.State.START));
        _serviceCommand.onGPSChangeState(new GPSChangeState(GPSChangeState.State.STOP));

        verify(_mockSensorManager,timeout(2000).times(1)).unregisterListener(any(GPSSensorEventListener.class));
    }

    @SmallTest
    public void testSetsPreferenceStartTimeOnStart() throws Exception {
        when(_mockTime.getCurrentTimeMilliseconds()).thenReturn((long)1000);
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);

        _serviceCommand.execute(_app);
        _serviceCommand.onGPSChangeState(new GPSChangeState(GPSChangeState.State.START));

        verify(_mockEditor,timeout(1000).times(1)).putLong("GPS_LAST_START", 1000);
    }

    @SmallTest
    public void testsSendStateChangeToPebbleOnStart() throws Exception {
        //throw new Exception("State change not implemented");
    }
}