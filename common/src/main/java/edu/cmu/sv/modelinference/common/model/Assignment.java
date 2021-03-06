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
package edu.cmu.sv.modelinference.common.model;

import com.google.common.base.Objects;


/**
 * @author Kasper Luckow
 */
public class Assignment<T extends Number> {
  
  private final String name;
  private T value;
  
  public Assignment(String name, T value) {
    this.name = name;
    this.value = value;
  }
  
  public Assignment(String name) {
    this(name, null);
  }
  
  public T getValue() {
    return this.value;
  }
  
  public void setValue(T value) {
    this.value = value;
  }
  
  public String getName() {
    return this.name;
  }
  
  @Override
  public int hashCode(){
    return Objects.hashCode(name, value);
  }

  @Override
  public boolean equals(final Object obj){
    if(obj instanceof Assignment<?>){
      @SuppressWarnings("unchecked")
	final Assignment<Number> other = (Assignment<Number>) obj;
      return Objects.equal(name, other.getName()) &&
          Objects.equal(value, other.getValue());
    } else{
      return false;
    }
  }

  @Override
  public String toString() {
    return this.name + "=" + this.value;
  }
}
