/**
 * Copyright 2016 Carnegie Mellon University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.cmu.sv.modelinference.features;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Range;

import edu.cmu.sv.modelinference.features.classification.AvgFeature;
import edu.cmu.sv.modelinference.features.classification.Event;

/**
 * @author Kasper Luckow
 *
 */
public class AvgEventGenerator implements EventGenerator {
  
  @Override
  public List<Event> computeEvents(List<Range<Integer>> eventIntervals, double[] ys) {
    List<Event> avgForEvents = new ArrayList<>();
    int idx = 0;
    for(Range<Integer> event : eventIntervals) {
      double[] yData = new double[event.upperEndpoint() - event.lowerEndpoint()];
      for(int i = 0, r = event.lowerEndpoint(); r < event.upperEndpoint(); r++, i++, idx++) {
        yData[i] = ys[idx];
      }
      avgForEvents.add(new Event(event, new AvgFeature(yData)));
    }    
    return avgForEvents;
  }
}