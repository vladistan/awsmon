/**
 *
 * Project: GreySpark Core
 * (c) 2015 FourV Systems, LLC.
 * Unpublished-rights reserved under the copyrights laws of the United States.
 * All use of this commercial software is subject to the terms and conditions of
 * the manufacturer's End User License Agreement.
 *
 * Manufacturer is:
 * FourV Systems, LLC, 8 Market Place,  Baltimore, MD 21202.
 *
 */

package com.fourv.internal.awsresourcemonitor;


import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Proxy object for EC2 instance data
 */
public class InstanceData {

  /** Is instance currently running */
  public boolean running;
  /** Instance name */
  public String name;
  /** Instance lifecycle */
  public String lifecycle;
  /** Instance project */
  public String project;
  /** Instance service */
  public String service;
  /** Instance owner */
  public String owner;
  /** Charge line */
  public String chargeLine;

  /**
   * List of errors found while processing instance tags
   */
  public List<String> tagValueErrors = new ArrayList<String>();
  private Date launchTime;
  final private Set<String> AllowedLifeCycleValues = new HashSet<String>();
  final private Set<String> AllowedProjectValues = new HashSet<String>();
  final private Set<String> AllowedServiceValues = new HashSet<String>();
  final private Set<String> AllowedOwnerValues = new HashSet<String>();
  final private Set<String> AllowChargeLineValues = new HashSet<String>();
  private String region;

  /**
   * Initialize proxy object
   * @param inst  - ec2 api instance
   */
  public InstanceData(final Instance inst) {

    AllowedLifeCycleValues.add("Permanent");
    AllowedLifeCycleValues.add("OnDemand");
    AllowedLifeCycleValues.add("POC");
    AllowedLifeCycleValues.add("Spot");

    AllowedProjectValues.add("Development Infrastructure");
    AllowedProjectValues.add("GreySpark (v1.0)");
    AllowedProjectValues.add("GreySpark Cyber (v1.0)");

    AllowedServiceValues.add("VPN");
    AllowedServiceValues.add("QA");
    AllowedServiceValues.add("Scheduler");
    AllowedServiceValues.add("Build");
    AllowedServiceValues.add("Analytics");
    AllowedServiceValues.add("Authentication");
    AllowedServiceValues.add("Web Services");

    AllowedOwnerValues.add("gmartin@fourv.com");
    AllowedOwnerValues.add("msweeney@fourv.com");
    AllowedOwnerValues.add("vkorolev@fourv.com");
    AllowedOwnerValues.add("jcamp@fourv.com");
    AllowedOwnerValues.add("rsamson@fourv.com");
    AllowedOwnerValues.add("roman_glova@epam.com");

    AllowChargeLineValues.add("InternalDevops");
    AllowChargeLineValues.add("InternalDev");
    AllowChargeLineValues.add("InternalQA");
    AllowChargeLineValues.add("SalesOps");
    AllowChargeLineValues.add("AP");

    final String stateName = inst.getState().getName();
    running = "running".equals(stateName);
    launchTime = inst.getLaunchTime();

    processTags(inst);

    if (name == null || name.equals("")  ) {
      name = inst.getInstanceId();
      tagValueErrors.add("Instance " + name + ": 'Name' tag is missing or empty");
    }

  }


  private void processTags(final Instance inst) {
    final List<Tag> tags = inst.getTags();
    for (final Tag tag : tags) {

      final String tagKey = tag.getKey();
      final String tagValue = tag.getValue();

      if ("Name".equals(tagKey)) {
        this.name = tagValue;
      } else if ("Lifecycle".equals(tagKey)) {
        addLifeCycleTag(tagValue);
      } else if ("Project".equals(tagKey)) {
        addProjectTag(tagValue);
      } else if ("Service".equals(tagKey)) {
        addServiceTag(tagValue);
      } else if ("Owner".equals(tagKey)) {
        addOwnerTag(tagValue);
      } else if ("ChargeLine".equals(tagKey)) {
        addChargeLineTag(tagValue);
      }

    }
  }

  private void addChargeLineTag(String tagValue) {
    if (AllowChargeLineValues.contains(tagValue)) {
      chargeLine = tagValue;
    } else {
      tagValueErrors.add("Invalid ChargeLine tag value '" + tagValue + "'");
    }

  }

  private void addOwnerTag(String tagValue) {
    if (AllowedOwnerValues.contains(tagValue)) {
      owner = tagValue;
    } else {
      tagValueErrors.add("Invalid Owner tag value '" + tagValue + "'");
    }
  }

  private void addServiceTag(String tagValue) {
    if (AllowedServiceValues.contains(tagValue)) {
      service = tagValue;
    } else {
      tagValueErrors.add("Invalid Service tag value '" + tagValue + "'");
    }
  }

  private void addProjectTag(String tagValue) {

    if (AllowedProjectValues.contains(tagValue)) {
      project = tagValue;
    } else {
      tagValueErrors.add("Invalid Project tag value '" + tagValue + "'");
    }

  }

  private void addLifeCycleTag(String tagValue) {
    if (AllowedLifeCycleValues.contains(tagValue)) {
      lifecycle = tagValue;
    } else {
      tagValueErrors.add("Invalid Lifecycle tag value '" + tagValue + "'");
    }
  }


  /**
   * Getter for instance running state
   * @return  true if instance is currently running
   */
  public final boolean isRunning() {
    return running;
  }

  /**
   * Getter for launch time
   * @return launch time
   */
  public final Date getLaunchTime() {
    if (launchTime == null) {
      return null;
    }
    return (Date) launchTime.clone();
  }

  /**
   * Getter for region
   * @return region
   */
  public String getRegion() {
    return region;
  }

  /**
   * Setter for region
   * @param region region
   */
  public void setRegion(String region)
  {
    this.region = region;
  }
}
