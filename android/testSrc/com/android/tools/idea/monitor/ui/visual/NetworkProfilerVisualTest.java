/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.idea.monitor.ui.visual;

import com.android.annotations.NonNull;
import com.android.tools.adtui.Animatable;
import com.android.tools.adtui.AnimatedTimeRange;
import com.android.tools.adtui.Range;
import com.android.tools.adtui.model.RangedDiscreteSeries;
import com.android.tools.adtui.visual.VisualTest;
import com.android.tools.idea.monitor.datastore.SeriesDataStore;
import com.android.tools.idea.monitor.ui.ProfilerEventListener;
import com.android.tools.idea.monitor.ui.network.view.NetworkCaptureSegment;
import com.android.tools.idea.monitor.ui.network.view.NetworkSegment;
import com.intellij.util.EventDispatcher;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NetworkProfilerVisualTest extends VisualTest {

  private static final String NETWORK_PROFILER_NAME = "Network Profiler";

  // Number of fake network capture data
  private static final int CAPTURE_SIZE = 10;

  private SeriesDataStore mDataStore;

  private NetworkSegment mSegment;

  private NetworkCaptureSegment mCaptureSegment;

  private long mStartTimeMs;

  private List<RangedDiscreteSeries<NetworkCaptureSegment.NetworkState>> mCaptureData;

  private Thread mSimulateTestDataThread;

  @Override
  protected void initialize() {
    mDataStore = new VisualTestSeriesDataStore();
    super.initialize();
  }

  @Override
  protected void reset() {
    if (mDataStore != null) {
      mDataStore.reset();
    }
    if (mSimulateTestDataThread != null) {
      mSimulateTestDataThread.interrupt();
    }
    super.reset();
  }

  @Override
  public String getName() {
    return NETWORK_PROFILER_NAME;
  }

  @Override
  protected List<Animatable> createComponentsList() {
    mStartTimeMs = System.currentTimeMillis();
    Range timeRange = new Range();
    AnimatedTimeRange animatedTimeRange = new AnimatedTimeRange(timeRange, mStartTimeMs);

    EventDispatcher<ProfilerEventListener> dummyDispatcher = EventDispatcher.create(ProfilerEventListener.class);
    mSegment = new NetworkSegment(timeRange, mDataStore, dummyDispatcher);

    mCaptureData = new ArrayList<>();
    for (int i = 0; i < CAPTURE_SIZE; ++i) {
      mCaptureData.add(new RangedDiscreteSeries<>(NetworkCaptureSegment.NetworkState.class, timeRange));
    }
    mCaptureSegment = new NetworkCaptureSegment(timeRange, mCaptureData, dummyDispatcher);

    List<Animatable> animatables = new ArrayList<>();

    animatables.add(animatedTimeRange);
    mSegment.createComponentsList(animatables);
    mCaptureSegment.createComponentsList(animatables);
    return animatables;
  }

  @Override
  protected void populateUi(@NonNull JPanel panel) {
    panel.setLayout(new GridBagLayout());
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.fill = GridBagConstraints.BOTH;
    constraints.weighty = .5;
    constraints.weightx = 1;
    constraints.gridy = 0;
    mSegment.initializeComponents();
    panel.add(mSegment, constraints);

    constraints.gridy = 1;
    mCaptureSegment.initializeComponents();
    panel.add(mCaptureSegment, constraints);
    simulateTestData();
  }

  private void simulateTestData() {
    mSimulateTestDataThread = new Thread() {
      @Override
      public void run() {
        try {
          Random rnd = new Random();
          while (true) {
            //  Insert new data point at now.
            long now = System.currentTimeMillis() - mStartTimeMs;
            for (RangedDiscreteSeries series : mCaptureData) {
              NetworkCaptureSegment.NetworkState[] states = NetworkCaptureSegment.NetworkState.values();
              // Hard coded value 10 to make the 'NONE' state more frequent
              int index = rnd.nextInt(10);
              series.getSeries().add(now, (index < states.length) ? states[index] : NetworkCaptureSegment.NetworkState.NONE);
            }
            Thread.sleep(1000);
          }
        }
        catch (InterruptedException e) {
        }
      }
    };
    mSimulateTestDataThread.start();
  }
}
