/**
 *
 * Copyright 2017 Vlad Korolev
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/

package org.vlad.awsresourcemonitor;

/**
 * Contains AWS resource policy.
 */
public class Policy {

  private static Policy instance;

  private String allowedRegion;


  private Policy() {

    allowedRegion = "us-east-1";
  }

  public static Policy getInstance() {
    return instance;
  }

  static {

    instance = new Policy();

  }


  public String getAllowedRegion() {
    return allowedRegion;
  }
}
