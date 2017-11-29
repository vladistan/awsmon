/**
 *
 * Copyright 2017 Vlad Korolev
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/

package org.vlad.awsresourcemonitor;

/**
 * Utility class for displaying version.
 */
public class VersionInfo {

  /**
   * Get full version string.
   * @return version string
   */
  public static String getVersionString()
  {
    String version = getShortVersionString();

    version += "." + autorevision.BUILD_NUMBER;
    version += "-g" + autorevision.VCS_SHORT_HASH;

    return version;
  }

  /**
   * Get Short version string.
   * @return short version string
   */
  public static String getShortVersionString() {
    String version = autorevision.VCS_TAG;

    if ( version.startsWith("v/")) {
      version = version.substring(2);
    }
    return version;
  }


  /**
   * Display version.
   */
  public static void displayVersion() {

    //CHECKSTYLE:OFF
    System.out.println("v" + getVersionString());
    //CHECKSTYLE:ON

  }

  /**
   * Display build information.
   */
  public static void displayBuildInfo() {

    //CHECKSTYLE:OFF
    System.out.println("Build URL: " + autorevision.BUILD_URL);
    System.out.println("Build Time: " + autorevision.BUILD_TIME);

    System.out.println("VCS Branch: " + autorevision.VCS_BRANCH);
    System.out.println("VCS Tick: " + autorevision.VCS_TICK);
    System.out.println("VCS Date: " + autorevision.VCS_DATE);

    System.out.println("Version: " + getVersionString());
    //CHECKSTYLE:ON

  }

  /**
   * Get version numbers as array of components.
   * @return version numbers
     */
  public static int[] getVersionComponents() {

    int[] ver = new int[3];

    String vStr = getShortVersionString();
    int c = 0;
    for (String s : vStr.split("\\.")) {
        ver[c++] = Integer.parseInt(s);
    }

    return ver;
  }

  /**
   * Get major version number.
   * @return major version number
   */
  public static int getMajorVersion() {
    return getVersionComponents()[0];
  }

  /**
   * Get minor version number.
   * @return minor version number
   */
  public static int getMinorVersion() {
    return getVersionComponents()[1];
  }

  /**
   * Get point version number.
   * @return point version number
   */
  public static int getPointVersion() {
    return getVersionComponents()[2];
  }

}
