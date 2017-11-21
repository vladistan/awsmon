/**
 *
 * Copyright 2017 Vlad Korolev
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/

package org.vlad.awsresourcemonitor.test;


import org.junit.Ignore;
import org.junit.Test;
import org.vlad.awsresourcemonitor.VersionInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class TestVersionInfo {

  @Test
  @Ignore("Not valid on build server")
  public void getCorrectVersionString()
  {
     String ver = VersionInfo.getVersionString();

     assertThat(ver).isEqualTo("0.1.0.37-g6102233");

  }

  @Test
  @Ignore("Not valid on build server")
  public void getCorrectShortVersionString() {

    String ver = VersionInfo.getShortVersionString();
    assertThat(ver).isEqualTo("0.1.0");

  }

  @Test
  @Ignore("Not valid on build server")
  public void getVersionComponents() {

    int[] ver_cmp = VersionInfo.getVersionComponents();

    assertThat(ver_cmp[0]).isEqualTo(0);
    assertThat(ver_cmp[1]).isEqualTo(1);
    assertThat(ver_cmp[2]).isEqualTo(0);

  }

  @Test
  @Ignore("Not valid on build server")
  public void verComponentsAreOK() {

    assertEquals(VersionInfo.getMajorVersion(), 0);
    assertEquals(VersionInfo.getMinorVersion(), 1);
    assertEquals(VersionInfo.getPointVersion(), 0);

  }


}
