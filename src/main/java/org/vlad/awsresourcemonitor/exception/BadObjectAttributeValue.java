/**
 *
 * Copyright 2017 Vlad Korolev
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/

package org.vlad.awsresourcemonitor.exception;

/**
 * Exception to indicate bad object attribute value.
 */
public class BadObjectAttributeValue extends Throwable {
  public BadObjectAttributeValue(final String msg) {
    super(msg);
  }
}
