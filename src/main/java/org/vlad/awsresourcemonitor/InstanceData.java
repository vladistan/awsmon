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
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.ListTagsForResourceRequest;
import com.amazonaws.services.rds.model.ListTagsForResourceResult;
import org.vlad.awsresourcemonitor.exception.BadObjectAttributeKey;
import org.vlad.awsresourcemonitor.exception.BadObjectAttributeValue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Proxy object for EC2 instance data.
 */
public class InstanceData {

  private AmazonRDS rds;
  /** Is instance currently running. */
  public boolean running;
  /** Instance name. */
  public String name;
  /** Instance lifecycle. */
  public ObjectAttribute lifecycle;
  /** Instance project. */
  public ObjectAttribute project;
  /** Instance service. */
  public ObjectAttribute service;
  /** Instance owner. */
  public ObjectAttribute owner;
  /** Charge line. */
  public ObjectAttribute chargeLine;
  /** Environment. */
  public ObjectAttribute environment;


  /** List of errors found while processing instance tags. */
  private List<String> tagValueErrors = new ArrayList<>();
  private Date launchTime;

  private Set<String> allowedTags;

  private String region;

  /**
   * Initialize proxy object.
   * @param inst  - ec2 api instance
   */
  public InstanceData(final Instance inst) {

    Policy policy = Policy.getInstance();
    chargeLine = new ObjectAttribute("ChargeLine", policy.getChargeLines());
    environment = new ObjectAttribute("Environment", policy.getEnvironments());
    owner = new ObjectAttribute("Owner", policy.getOwners());
    lifecycle = new ObjectAttribute("Lifecycle", policy.getLifecycle());
    project = new ObjectAttribute("Project", policy.getProjects());
    service = new ObjectAttribute("Service", policy.getServices());

    allowedTags = policy.getAllowedTags();


    final String stateName = inst.getState().getName();
    running = "running".equals(stateName);
    launchTime = inst.getLaunchTime();

    processTags(inst);

    if ("".equals(name) || name == null) {
      name = inst.getInstanceId();
      tagValueErrors.add("Instance " + name + ": 'Name' tag is missing or empty");
    }

  }

  public InstanceData(DBInstance inst) {

    rds = AWSInfo.getRds();


    Policy policy = Policy.getInstance();
    chargeLine = new ObjectAttribute("ChargeLine", policy.getChargeLines());
    environment = new ObjectAttribute("Environment", policy.getEnvironments());
    owner = new ObjectAttribute("Owner", policy.getOwners());
    lifecycle = new ObjectAttribute("Lifecycle", policy.getLifecycle());
    project = new ObjectAttribute("Project", policy.getProjects());
    service = new ObjectAttribute("Service", policy.getServices());

    allowedTags = policy.getAllowedTags();



    final String stateName = inst.getDBInstanceStatus();
    running = "available".equals(stateName);
    launchTime = inst.getInstanceCreateTime();

    processTags(inst);

    if ("".equals(name) || name == null) {
      name = inst.getDBInstanceIdentifier();
      tagValueErrors.add("Instance " + name + ": 'Name' tag is missing or empty");
    }
  }

  private void processTags(DBInstance inst) {


    String arn = getInstanceARN(inst);
    ListTagsForResourceRequest tagReq = new ListTagsForResourceRequest().withResourceName(arn);
    ListTagsForResourceResult tagRes = rds.listTagsForResource(tagReq);

    final List<com.amazonaws.services.rds.model.Tag> tags = tagRes.getTagList();
    for (final com.amazonaws.services.rds.model.Tag tag : tags) {

      final String tagKey = tag.getKey();
      final String tagValue = tag.getValue();

      try {

        checkAllowedTag(tagKey);

        if ("Name".equals(tagKey)) {
          this.name = tagValue;
        } else if ("Lifecycle".equals(tagKey)) {
          lifecycle.setValue(tagValue);
        } else if ("Project".equals(tagKey)) {
          project.setValue(tagValue);
        } else if ("Service".equals(tagKey)) {
          service.setValue(tagValue);
        } else if ("Owner".equals(tagKey)) {
          owner.setValue(tagValue);
        } else if ("ChargeLine".equals(tagKey)) {
          chargeLine.setValue(tagValue);
        } else if ("Environment".equals(tagKey)) {
          environment.setValue(tagValue);
        } else if ("Env".equals(tagKey)) {
          environment.setValue(tagValue);
        }
      } catch (BadObjectAttributeValue | BadObjectAttributeKey badAttr) {
        tagValueErrors.add(badAttr.getMessage());
      }

    }

  }

  private String getInstanceARN(DBInstance inst) {

     String azone = inst.getAvailabilityZone();
     String region = azone.substring(0, azone.length() - 1);

     String arn = "arn:aws:rds:" + region + ":" + AWSInfo.getAcc() + ":db:" + inst.getDBInstanceIdentifier();

     return arn;

  }


  private void processTags(final Instance inst) {

    final List<Tag> tags = inst.getTags();
    for (final Tag tag : tags) {

      final String tagKey = tag.getKey();
      final String tagValue = tag.getValue();

      try {

        checkAllowedTag(tagKey);

        if ("Name".equals(tagKey)) {
          this.name = tagValue;
        } else if ("Lifecycle".equals(tagKey)) {
          lifecycle.setValue(tagValue);
        } else if ("Project".equals(tagKey)) {
          project.setValue(tagValue);
        } else if ("Service".equals(tagKey)) {
          service.setValue(tagValue);
        } else if ("Owner".equals(tagKey)) {
          owner.setValue(tagValue);
        } else if ("ChargeLine".equals(tagKey)) {
          chargeLine.setValue(tagValue);
        } else if ("Environment".equals(tagKey)) {
          environment.setValue(tagValue);
        } else if ("Env".equals(tagKey)) {
          environment.setValue(tagValue);
        }
      } catch (BadObjectAttributeValue | BadObjectAttributeKey badAttr) {
        tagValueErrors.add(badAttr.getMessage());
      }

    }
  }

  private void checkAllowedTag(String tagKey) throws BadObjectAttributeKey {
    if (!allowedTags.contains(tagKey)) {
      throw new BadObjectAttributeKey("Invalid tag key '" + tagKey + "'");
    }
  }

  /**
   * Getter for instance running state.
   *
   * @return true if instance is currently running
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
  public void setRegion(String region) {
    this.region = region;
  }

  /**
   * Getter for tag value errors.
   *
   * @return list that contains tag value errors
   */
  public List<String> getTagValueErrors() {
    return tagValueErrors;
  }
}
