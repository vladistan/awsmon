/**
 *
 * Copyright 2017 Vlad Korolev
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/

package org.vlad.awsresourcemonitor;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Contains AWS resource policy.
 */
public final class Policy {

  private static Policy instance;
  private String envTagName;

  private Set<String> allowedRegions;
  private Set<String> environments;
  private Set<String> allowedTags;
  private Set<String> chargeLines;
  private Set<String> owners;
  private Set<String> lifecycle;
  private Set<String> projects;
  private Set<String> services;


  private Policy() {

    allowedTags = new HashSet<>();
    lifecycle = new HashSet<>();
    allowedRegions = new HashSet<>();

    lifecycle.add("Permanent");
    lifecycle.add("OnDemand");
    lifecycle.add("POC");
    lifecycle.add("Spot");

    allowedTags.add("aws:autoscaling:groupName");
    allowedTags.add("aws:cloudformation:logical-id");
    allowedTags.add("VPCStackPrefix");

    allowedTags.add("Name");
    allowedTags.add("Lifecycle");

    envTagName = "Environment";

  }

  public static Policy getInstance() {

    if (instance.projects == null) {
      throw new IllegalStateException("Policy must be loaded first");
    }

    return instance;
  }

  public static void load(File yamlFile) throws FileNotFoundException, ParseException {

    Yaml yaml = new Yaml();

    HashMap<String, List<String>> map = yaml.load(new FileReader(yamlFile));

    instance.owners = null;

    instance.allowedTags.addAll(map.get("AllowedTags"));
    List<String> regions = map.get("Region");
    instance.allowedRegions.addAll(regions);

    instance.projects     = loadTag(map, "Project");
    instance.environments = loadTag(map, "Environment");
    instance.chargeLines  = loadTag(map, "ChargeLine");
    if ( map.containsKey("Owner")) {
      instance.owners = loadTag(map, "Owner");
    }
    instance.services     = loadTag(map, "Service");


  }

  private static Set<String> loadTag(HashMap<String, List<String>> map, String name) throws ParseException {

    if ((!map.containsKey(name)) && name.equals("Environment"))
    {
      name = "Env";
      instance.envTagName = "Env";
    }

    List<String> valueList = map.get(name);

    if (valueList == null) {
      throw new ParseException("Missing section '" + name + "' in the policy file", 0);
    }

    Set<String> set = new HashSet<>();
    set.addAll(valueList);
    instance.allowedTags.add(name);
    return set;
  }


  static {
    instance = new Policy();
  }



  public Set<String> getAllowedRegion() {
    return allowedRegions;
  }

  public Set<String> getEnvironments() {
    return environments;
  }

  public Set<String> getAllowedTags() {
    return allowedTags;
  }

  public Set<String> getChargeLines() {
    return chargeLines;
  }

  public Set<String> getOwners() {
    return owners;
  }

  public Set<String> getServices() {
    return services;
  }

  public Set<String> getProjects() {
    return projects;
  }

  public Set<String> getLifecycle() {
    return lifecycle;
  }

}
