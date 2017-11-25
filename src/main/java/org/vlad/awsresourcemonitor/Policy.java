/**
 *
 * Copyright 2017 Vlad Korolev
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/

package org.vlad.awsresourcemonitor;

import java.util.HashSet;
import java.util.Set;

/**
 * Contains AWS resource policy.
 */
public final class Policy {

  private static Policy instance;

  private final String allowedRegion;
  private Set<String> environments;


  private Policy() {

    allowedRegion = "us-east-1";
    environments = new HashSet<>();

    environments.add("Common");
    environments.add("Admin");
    environments.add("Iso");
    environments.add("Dev");
    environments.add("Test");
    environments.add("Union");
    environments.add("Staging");
    environments.add("Prod");

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

  public Set<String> getEnvironments() {
    return environments;
  }
}
