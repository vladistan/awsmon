/**
 *
 * Copyright 2017 Vlad Korolev
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/

package org.vlad.awsresourcemonitor.exception;

/**
 * Generic exception related to XML parsing or generation problem.
 */
public class XmlException extends Exception {
  public XmlException(final Throwable e) {
    super("XML handling problem", e);
  }
}
