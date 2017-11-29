/**
 *
 * Copyright 2017 Vlad Korolev
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/


package org.vlad.awsresourcemonitor;

import org.vlad.awsresourcemonitor.exception.BadObjectAttributeValue;

import java.util.Set;

/**
 * AWS policy attribute.
 */
public class ObjectAttribute {
  private String value;
  private final Set<String> allowedValues;
  private final String name;


  public ObjectAttribute(String name, Set<String> allowedValues) {
    this.allowedValues = allowedValues;
    this.name = name;
  }

  public final void setValue(final String value) throws BadObjectAttributeValue {
    if (!allowedValues.contains(value)) {
      throw new BadObjectAttributeValue("Invalid " + name + " tag value '" + value + '\'');
    }

    this.value = value;

  }

  public final String getValue() {
    return value;
  }

}
