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
package edu.cmu.sv.modelinference.tools.charting;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.UnknownKeyException;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RefineryUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Range;

import edu.cmu.sv.modelinference.features.EWMASmoothingFilter;
import edu.cmu.sv.modelinference.features.EventDetector;
import edu.cmu.sv.modelinference.features.FeatureExtractor;
import edu.cmu.sv.modelinference.features.MovingAverageEventDetector;
import edu.cmu.sv.modelinference.features.PredictionModel;
import edu.cmu.sv.modelinference.features.RoCExtractor;
import edu.cmu.sv.modelinference.features.UnweightedRectangularSmoothingFilter;
import edu.cmu.sv.modelinference.features.classification.AvgFeature;
import edu.cmu.sv.modelinference.features.classification.ClassificationResult;
import edu.cmu.sv.modelinference.features.classification.Clusterer1D;
import edu.cmu.sv.modelinference.features.classification.Event;
import edu.cmu.sv.modelinference.features.classification.EventClass;
import edu.cmu.sv.modelinference.features.classification.EventClassifier;
import edu.cmu.sv.modelinference.features.classification.EventUtils;
import edu.cmu.sv.modelinference.generators.LogEntryFilter;
import edu.cmu.sv.modelinference.generators.parser.reader.LogReader;
import edu.cmu.sv.modelinference.generators.parser.reader.SequentialLogReader;
import edu.cmu.sv.modelinference.generators.parser.st.STEntry;
import edu.cmu.sv.modelinference.generators.parser.st.STParser;
import edu.cmu.sv.modelinference.tools.charting.STValueTracker.FIELD;

/**
 * @author Kasper Luckow
 */
public class Log2EventChart {
  private static final String HELP_ARG = "help";
  private static final String INPUT_ARG = "i";
  private static final String INPUT_TYPE_ARG = "t";
  private static final String ADD_OPTS_ARG = "a";
  
  private static final Logger logger = LoggerFactory.getLogger(Log2EventChart.class.getName());
      
  
  public static void main(String[] args) throws IOException {    
    Options options = createCmdOptions();
    
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = null;
    try {
      cmd = parser.parse(options, args);
    } catch(ParseException exp) {
      printHelpAndExit(options);
    }
    
    if(cmd.hasOption(HELP_ARG)) {
      printHelpAndExit(options);
    }
    
    String logFilePath = null;
    if(cmd.hasOption(INPUT_ARG)) {
      logFilePath = cmd.getOptionValue(INPUT_ARG);
    } else {
      printHelpAndExit(options);
    }
    
    //use enums instead
    String logType = "";
    
    //When options are required (required() called in option builder), do
    //we need to check hasValue, or will the parser simply throw an exception
    //if the option is not provided?
    if(cmd.hasOption(INPUT_TYPE_ARG)) {
      logType = cmd.getOptionValue(INPUT_TYPE_ARG);
    } else
      printHelpAndExit(options);
    
    long start = System.currentTimeMillis();
    
    //Expand this to be more pluggable. ugly
    switch(logType.toLowerCase()) {
    case "st":
      
      Set<STValueTracker.FIELD> trackedFields = new HashSet<>();
      
      if(cmd.hasOption(ADD_OPTS_ARG)) {
        String fieldsStr = cmd.getOptionValue(ADD_OPTS_ARG);
        for(String f : fieldsStr.split(",")) {
          trackedFields.add(FIELD.valueOf(f.toUpperCase()));
        }
      } else {
        logger.error("Must be supplied a comma-separated list of fields to be tracked (e.g., pos_x,pos_y) to additional arg option");
        System.exit(-1);
      }
      
      LogEntryFilter<STEntry> filter =
      
      new LogEntryFilter<STEntry>() {
        String tracked = "";
        @Override
        public boolean submitForProcessing(STEntry entry) {
          if(entry.getCallSign().equals("USA5596")) {
            tracked = entry.getCallSign();
            return true;
          }          
          return entry.getCallSign().equals(tracked);
        }
      };
      
      LogReader<STEntry> reader = new SequentialLogReader<>(new STParser(), filter);
      STValueTracker valTracker = new STValueTracker(trackedFields, reader);
      
      
      //Compute all the series
      XYSeriesCollection seriesCol = new XYSeriesCollection();
      XYSeriesCollection rawCollection = valTracker.getSeries(logFilePath);
      mergeSeries(rawCollection, seriesCol);
      
      //We'll just get the first series in the collection.. kind of arbitrary
      XYSeries ser = (XYSeries) seriesCol.getSeries().get(0);
      ser.toArray();
      double[] xsd = ser.toArray()[0];
      final int[] xs = new int[xsd.length];
      for (int i=0; i<xs.length; ++i)
          xs[i] = (int) xsd[i];
      double[] ys = ser.toArray()[1];
      
      
      //Get features
      //Feature: raw data
      UnweightedRectangularSmoothingFilter unweightedFilter = new UnweightedRectangularSmoothingFilter(3);
      EWMASmoothingFilter ewmaFilter = new EWMASmoothingFilter(3, 0.1);
      double[] unweightedYs = unweightedFilter.smoothen(xs, ys);
      double[] ewmaYs = ewmaFilter.smoothen(xs, ys);

      //Plot raw data
   //   addDataToXYSeries(xs, ys, "data", seriesCol);
      addDataToXYSeries(xs, unweightedYs, "data (EWMA smooth)", seriesCol);

      //Feature: slope/rate-of-change
      FeatureExtractor slopeExtractor = new RoCExtractor();
      double[] ySlopeRaw = slopeExtractor.computeFeature(xs, ewmaYs);
      double[] unweightedYSlope = unweightedFilter.smoothen(xs, ySlopeRaw);
      double[] ewmaYSlope = ewmaFilter.smoothen(xs, ySlopeRaw);
      
      //Plot slopes
     // addDataToXYSeries(xs, ySlopeRaw, "slope", seriesCol);
      addDataToXYSeries(xs, ewmaYSlope, "slope (EWMA smooth)", seriesCol);
      
      
      //use event detector on features
      EventDetector movingAvg = new MovingAverageEventDetector(2, 3);
      
      //Get prediction model based on moving average on slope
      PredictionModel predictionModelSlope = movingAvg.computePredictionModel(xs, ewmaYSlope);
      XYSeriesCollection serSlopeCol = predictionModelSlope.getSeries();
      mergeSeries(serSlopeCol, seriesCol);

      //Find violations of prediction model
      List<Range<Integer>> violations = predictionModelSlope.findThresholdViolations(xs, ewmaYSlope);
      
      
      logger.info("Number of violations: " + violations.size());
      logger.info("Points in raw xs: " + xs.length);
      logger.info("Points in raw slope: " + ySlopeRaw.length);
      logger.info("Points in eqmaYSlope: " + ewmaYSlope.length);
      
      List<Range<Integer>> eventIntervals = EventUtils.computeEventSequence(xs[0], xs[xs.length - 1], violations);
      
      List<Event> events = AvgFeature.computeAvgEvents(eventIntervals, ewmaYSlope);
      logger.info("events computed: " + events.size());
      
      //Start clustering
      EventClassifier clusterer = new Clusterer1D(7, -1, 3000);
      ClassificationResult clusters = clusterer.classify(events);
      logger.info("Number of clusters: " + clusters.getEventClasses().size());
      Set<Map.Entry<Event, EventClass>> evtClassSequence = clusters.getEvtSeqWithClassifiers();
      DecimalFormat df = new DecimalFormat("#.0000"); 
      for(Map.Entry<Event, EventClass> evtWithClass : evtClassSequence) {
        Range<Integer> evtPeriod = evtWithClass.getKey().getRange();
        System.out.println(evtPeriod + " : class " + 
            evtWithClass.getValue().getClassId() + 
            " ([" + df.format(evtWithClass.getValue().getMinFeatureVal()) + ";" + df.format(evtWithClass.getValue().getMaxFeatureVal()) + "], " +
            "avg: " + df.format(evtWithClass.getValue().getAvgFeatureVal()) + 
            ", stddev: " + df.format(Math.sqrt(evtWithClass.getValue().getFeatureVariance())) + ")");
      }
      
      //Chart clusters
      DataChart clustersDataChart = new DataChart();      
      clustersDataChart.chart(clusters.getSeries(), "avg rate");
      clustersDataChart.pack();
      RefineryUtilities.centerFrameOnScreen(clustersDataChart);
      clustersDataChart.setVisible(true);
      
      //Get chart on which we will plot the features and violations
      DataChart c = new DataChart();
      String yLbl = trackedFields.iterator().next().getUnit(); // very random.....
      JFreeChart chart = c.chart(seriesCol, yLbl);

      //Plot violations
      XYPlot plot = (XYPlot) chart.getPlot();
      plot.setRenderer(new ClassificationXYRenderer(clusters));

      setViolationMarkers(violations, plot);

      c.pack();
      RefineryUtilities.centerFrameOnScreen(c);
      c.setVisible(true);
      break;
    case "sierra":
    case "autoresolver":
    case "rp":
      default:
        logger.error("Unsupported input type");
        printHelpAndExit(options);
    }
    
    logger.info("Done constructing chart");
    logger.info("Took: " + (System.currentTimeMillis() - start));
  }
  
  private static void setViolationMarkers(Collection<Range<Integer>> violations, XYPlot plot) {
    for(Range<Integer> violation : violations) {
      
      ValueMarker startViolationMarker = new ValueMarker(violation.lowerEndpoint());  // position is the value on the axis
      startViolationMarker.setPaint(Color.BLACK);
      startViolationMarker.setStroke(new BasicStroke(1.0f));
      plot.addDomainMarker(startViolationMarker);
      
      ValueMarker endViolationMarker = new ValueMarker(violation.upperEndpoint());  // position is the value on the axis
      endViolationMarker.setPaint(Color.GREEN);
      endViolationMarker.setStroke(new BasicStroke(0.5f));
      plot.addDomainMarker(endViolationMarker);
    }
  }
  
  private static void mergeSeries(XYSeriesCollection inputCol, XYSeriesCollection outputCol) {
    for(Object s : inputCol.getSeries()) {
      XYSeries inputSeries = (XYSeries) s;
      XYSeries existing = null;
      try {
        existing = outputCol.getSeries(inputSeries.getKey());
      } catch(UnknownKeyException e) { }
      if(existing != null) {
        inputSeries.setKey(inputSeries.getKey() + "2");
      }
      outputCol.addSeries(inputSeries);
    }
  }
  
  private static XYSeries computeSeries(int[] xs, double[] ys, String name) {
    XYSeries series = new XYSeries(name);
    for(int i = 0; i < ys.length; i++) {
      series.add(xs[i], ys[i]);
    }
    return series;
  }
  
  private static void addDataToXYSeries(int[] xs, double[] ys, String name, XYSeriesCollection outputCol) {
    XYSeries series = computeSeries(xs,ys,name);
    outputCol.addSeries(series);
  }
  
  private static void printHelpAndExit(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(Log2EventChart.class.getName(), options);
    System.exit(0);
  }
  
  public static Options createCmdOptions() {
    Options options = new Options();
    
    Option help = new Option(HELP_ARG, "print this message");

    Option inputType = Option.builder(INPUT_TYPE_ARG).argName("st | autoresolver")
        .hasArg()
        .desc("Specify log type")
        .required()
        .build();
    
    Option addOpts = Option.builder(ADD_OPTS_ARG).argName("Additional options").hasArg()
        .desc("Additional input type options").build();
    
    Option input = Option.builder(INPUT_ARG).argName("file")
                                .hasArg()
                                .desc("Specify input file.")
                                .required()
                                .build();


    options.addOption(inputType);
    options.addOption(addOpts);
    options.addOption(help);
    options.addOption(input);
    return options;
  }
}