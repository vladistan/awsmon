/**
 *
 * Copyright 2017 Vlad Korolev
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/


package org.vlad.awsresourcemonitor;

import org.vlad.awsresourcemonitor.exception.BadObjectAttributeValue;

import java.util.HashSet;
import java.util.Set;

/**
 * AWS policy attribute.
 */
public class ObjectAttribute {
  private String value;
  private Set<String> allowedValues;

  public ObjectAttribute() {
    allowedValues = new HashSet<>();
  }

  public void setValue(String value) throws BadObjectAttributeValue {
    if(!allowedValues.contains(value)) {
      throw new BadObjectAttributeValue("Invalid Environment tag value '" + value + "'");
    }

    this.value = value;


  }

  public void add_allowed_value(String value) {
    allowedValues.add(value);
  }
}
