/**
 *
 * Copyright 2017 Vlad Korolev
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/

package org.vlad.awsresourcemonitor.exception;

/**
 * Exception to indicate bad object attribute key.
 */
public class BadObjectAttributeKey extends Throwable {
  public BadObjectAttributeKey(final String msg) {
    super(msg);
  }
}
