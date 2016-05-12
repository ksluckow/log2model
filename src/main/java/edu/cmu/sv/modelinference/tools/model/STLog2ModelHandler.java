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
package edu.cmu.sv.modelinference.tools.model;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import edu.cmu.sv.modelinference.generators.ModelInferer;
import edu.cmu.sv.modelinference.generators.formats.st.GridState;
import edu.cmu.sv.modelinference.generators.formats.st.STConfig;
import edu.cmu.sv.modelinference.generators.formats.st.STModelInferer;
import edu.cmu.sv.modelinference.generators.model.Model;
import edu.cmu.sv.modelinference.tools.LogHandler;
import edu.cmu.sv.modelinference.tools.charting.LogProcessingException;
import edu.cmu.sv.modelinference.tools.cmdutil.Util;
import edu.cmu.sv.modelinference.tools.cmdutil.Util.GridPartitions;

/**
 * @author Kasper Luckow
 *
 */
public class STLog2ModelHandler implements LogHandler<Model<?>> {

  private static final String GRID_DIM = "dim";
  
  private static STLog2ModelHandler instance = null;
  
  public static STLog2ModelHandler getInstance() {
    if(instance == null) {
      instance = new STLog2ModelHandler();
    }
    return instance;
  }
  
  private Options cmdOpts;
  
  private STLog2ModelHandler() {
    this.cmdOpts = createCmdOptions();
  }
  
  private Options createCmdOptions() {
    Options options = new Options();
    
    Option addOpts = Option.builder(GRID_DIM).argName("Grid Dimensions").hasArg()
          .desc("Dimensions of the grid projected on the airfield. Format: NUMxNUM.").build();
    
    options.addOption(addOpts);
    return options;
  }

  @Override
  public String getHandlerName() {
    return STConfig.LOG_CONFIG_NAME;
  }
  
  @Override
  public Model<?> process(String logFile, String logType, String[] additionalCmdArgs) throws LogProcessingException {
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd;
    try {
      cmd = parser.parse(cmdOpts, additionalCmdArgs, false);
    } catch (ParseException e) {
      throw new LogProcessingException(e);
    }
    
    Model<?> model = null;
    ModelInferer<GridState> modelInferer = null;
    if(cmd.hasOption(GRID_DIM)) {
      String partStr = cmd.getOptionValue(GRID_DIM).trim();
      GridPartitions parts;
      try {
        parts = Util.extractGridPartitions(partStr);
      } catch (ParseException e) {
        throw new LogProcessingException(e);
      }
      modelInferer = new STModelInferer(parts.horiz, parts.vert);
    } else
      modelInferer = new STModelInferer(STModelInferer.DEF_PARTITIONS, STModelInferer.DEF_PARTITIONS);
    
    try {
      model = modelInferer.generateModel(logFile);
    } catch (IOException e) {
      throw new LogProcessingException(e);
    }
    return model;
  }
}
