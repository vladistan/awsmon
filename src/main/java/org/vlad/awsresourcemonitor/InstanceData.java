/**
 *
 * Copyright 2017 Vlad Korolev
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/

package org.vlad.awsresourcemonitor;


import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Proxy object for EC2 instance data.
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
  /** Environment */
  public String environment;


  /**
   * List of errors found while processing instance tags
   */
  public List<String> tagValueErrors = new ArrayList<String>();
  private Date launchTime;
  final private Set<String> AllowedLifeCycleValues = new HashSet<>();
  final private Set<String> AllowedProjectValues = new HashSet<>();
  final private Set<String> AllowedServiceValues = new HashSet<>();
  final private Set<String> AllowedOwnerValues = new HashSet<>();
  final private Set<String> AllowedChargeLineValues = new HashSet<>();
  final private Set<String> AllowedEnvironmentValues = new HashSet<>();
  final private Set<String> AllowedTag = new HashSet<>();

  private String region;

  /**
   * Initialize proxy object.
   * @param inst  - ec2 api instance
   */
  public InstanceData(final Instance inst) {

    AllowedLifeCycleValues.add("Permanent");
    AllowedLifeCycleValues.add("OnDemand");
    AllowedLifeCycleValues.add("POC");
    AllowedLifeCycleValues.add("Spot");

    AllowedProjectValues.add("Development Infrastructure");
    AllowedProjectValues.add("App1 (v1.0)");

    AllowedServiceValues.add("VPN");
    AllowedServiceValues.add("QA");
    AllowedServiceValues.add("Scheduler");
    AllowedServiceValues.add("Build");
    AllowedServiceValues.add("Analytics");
    AllowedServiceValues.add("Authentication");
    AllowedServiceValues.add("Auth");
    AllowedServiceValues.add("Microservices");

    AllowedOwnerValues.add("vlad@myorg.org");
    AllowedOwnerValues.add("bob@myorg.org");
    AllowedOwnerValues.add("greg@myorg.org");

    AllowedChargeLineValues.add("InternalDevops");
    AllowedChargeLineValues.add("InternalDev");
    AllowedChargeLineValues.add("InternalQA");
    AllowedChargeLineValues.add("Cust1");

    AllowedEnvironmentValues.add("Common");
    AllowedEnvironmentValues.add("Admin");
    AllowedEnvironmentValues.add("Iso");
    AllowedEnvironmentValues.add("Dev");
    AllowedEnvironmentValues.add("Test");
    AllowedEnvironmentValues.add("Union");
    AllowedEnvironmentValues.add("Staging");
    AllowedEnvironmentValues.add("Prod");

    AllowedTag.add("aws:autoscaling:groupName");
    AllowedTag.add("aws:cloudformation:logical-id");
    AllowedTag.add("VPCStackPrefix");

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
      } else if ("Environment".equals(tagKey)) {
        addEnvironmentTag(tagValue);
      } else {
        checkAllowedTag(tagKey);
      }

    }
  }

  private void checkAllowedTag(String tagKey) {
    if(!AllowedTag.contains(tagKey)) {
      tagValueErrors.add("Invalid tag key '" + tagKey + "'");
    }


  }

  private void addEnvironmentTag(String tagValue) {
    if(AllowedEnvironmentValues.contains(tagValue)) {
      environment = tagValue;
    } else {
      tagValueErrors.add("Invalid Environment tag value '" + tagValue + "'");
    }
  }

  private void addChargeLineTag(String tagValue) {
    if (AllowedChargeLineValues.contains(tagValue)) {
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
   * Getter for instance running state.
   *
   * @return  true if instance is currently running
   */
  public final boolean isRunning() {
    return running;
  }

  /**
   * Getter for launch time.
   *
   * @return launch time
   */
  public final Date getLaunchTime() {
    if (launchTime == null) {
      return null;
    }
    return (Date) launchTime.clone();
  }

  /**
   * Getter for region.
   *
   * @return region
   */
  public String getRegion() {
    return region;
  }

  /**
   * Setter for region.
   *
   * @param region region
   */
  public void setRegion(String region)
  {
    this.region = region;
  }
}
