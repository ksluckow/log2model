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
package edu.cmu.sv.modelinference.modeltool.mc.prism;

import edu.cmu.sv.modelinference.modeltool.mc.ModelGeneratorException;

/**
 * @author Kasper Luckow
 *
 */
public class PrismModelGeneratorException extends ModelGeneratorException {
	private static final long serialVersionUID = 1L;

	public PrismModelGeneratorException(String details) {
		super(details);
	}

	public PrismModelGeneratorException(Throwable t) {
		super(t);
	}
	
	public PrismModelGeneratorException(Exception e) {
		super(e);
	}
}
