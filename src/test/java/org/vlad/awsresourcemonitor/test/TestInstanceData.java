/**
 *
 * Copyright 2017 Vlad Korolev
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/
package org.vlad.awsresourcemonitor.test;


import com.amazonaws.services.ec2.model.Instance;
import org.junit.BeforeClass;
import org.vlad.awsresourcemonitor.InstanceData;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.ReadablePeriod;
import org.joda.time.Seconds;
import org.junit.Test;
import org.vlad.awsresourcemonitor.Policy;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.text.ParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class TestInstanceData {

  @BeforeClass
  public static void setUp() throws URISyntaxException, FileNotFoundException, ParseException {

    File yamlFile = TestUtil.getTestResource("policy.yaml");
    Policy.load( yamlFile );

  }

  @Test
  public void WhenStateIsRunningShouldSetRunningFlagToTrue() {
    Instance inst = TestUtil.getMockInstance("running");

    InstanceData iData = new InstanceData(inst);

    assertThat(iData.isRunning()).isTrue();

  }

  @Test
  public void WhenStateIsStoppedShouldSetRunningFlagToTrue() {
    Instance inst = TestUtil.getMockInstance("stopped");

    InstanceData iData = new InstanceData(inst);

    assertThat(iData.isRunning()).isFalse();
  }

  @Test
  public void ShouldSetNameAttributeFromNameTag() {
    Instance inst = TestUtil.getMockInstance("running", "My EC2 Instance");

    InstanceData iData = new InstanceData(inst);

    assertThat(iData.name).isEqualTo("My EC2 Instance");
  }

  @Test
  public void ShouldSetLifeCycleAttributeFromTag() {

    Instance inst = TestUtil.getMockInstance("running", "My EC2 Instance");

    TestUtil.addInstanceTag(inst, "Lifecycle", "Permanent");
    InstanceData iData = new InstanceData(inst);

    assertThat(iData.lifecycle.getValue()).isEqualTo("Permanent");
  }

  @Test
  public void ShouldSetProjectAttributeFromTag() {

    Instance inst = TestUtil.getMockInstance("running", "My EC2 Instance");

    TestUtil.addInstanceTag(inst, "Project", "Dev-Infra");
    InstanceData iData = new InstanceData(inst);

    assertThat(iData.project.getValue()).isEqualTo("Dev-Infra");

  }

  @Test
  public void ShouldRecordTagValueErrorWhenProjectIsNotAllowedValue() {

    Instance inst = TestUtil.getMockInstance("running", "My EC2 Instance");

    TestUtil.addInstanceTag(inst, "Project", "bob");
    InstanceData iData = new InstanceData(inst);

    assertThat(iData.getTagValueErrors()).contains("Invalid Project tag value 'bob'");

  }

  @Test
  public void ShouldSetServiceAttributeFromTag() {

    Instance inst = TestUtil.getMockInstance("running", "My EC2 Instance");

    TestUtil.addInstanceTag(inst, "Service", "VPN");
    InstanceData iData = new InstanceData(inst);

    assertThat(iData.service.getValue()).isEqualTo("VPN");

  }

  @Test
  public void ShouldRecordTagValueErrorWhenChargeLineIsNotAllowedValue() {

    Instance inst = TestUtil.getMockInstance("running", "My EC2 Instance");

    TestUtil.addInstanceTag(inst, "ChargeLine", "bob");
    InstanceData iData = new InstanceData(inst);

    assertThat(iData.getTagValueErrors()).contains("Invalid ChargeLine tag value 'bob'");

  }

  @Test
  public void ShouldSetChargeLineAttributeFromTag() {

    Instance inst = TestUtil.getMockInstance("running", "My EC2 Instance");

    TestUtil.addInstanceTag(inst, "ChargeLine", "InternalQA");
    InstanceData iData = new InstanceData(inst);

    assertThat(iData.chargeLine.getValue()).isEqualTo("InternalQA");

  }


  @Test
  public void ShouldRecordTagValueErrorWhenServiceIsNotAllowedValue() {

    Instance inst = TestUtil.getMockInstance("running", "My EC2 Instance");

    TestUtil.addInstanceTag(inst, "Service", "bob");
    InstanceData iData = new InstanceData(inst);

    assertThat(iData.getTagValueErrors()).contains("Invalid Service tag value 'bob'");

  }

  @Test
  public void ShouldSetOwnerAttributeFromTag() {

    Instance inst = TestUtil.getMockInstance("running", "My EC2 Instance");

    TestUtil.addInstanceTag(inst, "Owner", "Vlad");
    InstanceData iData = new InstanceData(inst);

    assertThat(iData.owner.getValue()).isEqualTo("Vlad");

  }

  @Test
  public void ShouldAcceptBobAsOwner() {

    Instance inst = TestUtil.getMockInstance("running", "My EC2 Instance");

    TestUtil.addInstanceTag(inst, "Owner", "Greg");
    InstanceData iData = new InstanceData(inst);

    assertThat(iData.owner.getValue()).isEqualTo("Greg");

  }


  @Test
  public void ShouldRecordTagValueErrorWhenOwnerIsNotAllowedValue() {

    Instance inst = TestUtil.getMockInstance("running", "My EC2 Instance");

    TestUtil.addInstanceTag(inst, "Owner", "doop@hello.com");
    InstanceData iData = new InstanceData(inst);

    assertThat(iData.getTagValueErrors()).contains("Invalid Owner tag value 'doop@hello.com'");

  }

  @Test
  public void ShouldCorrectlyHandleNullNameTag() {

    Instance inst = TestUtil.getMockInstance("running", null);

    InstanceData iData = new InstanceData(inst);

    assertThat(iData.getTagValueErrors()).contains("Instance i-12345: 'Name' tag is missing or empty");

  }

  @Test
  public void ShouldCorrectlyHandleEmptyNameTag() {

    Instance inst = TestUtil.getMockInstance("running", "");

    InstanceData iData = new InstanceData(inst);

    assertThat(iData.getTagValueErrors()).contains("Instance i-12345: 'Name' tag is missing or empty");

  }


  @Test
  public void ShouldRecordTagValueErrorWhenLifeCycleIsNotAllowedValue() {

    Instance inst = TestUtil.getMockInstance("running", "My EC2 Instance");

    TestUtil.addInstanceTag(inst, "Lifecycle", "bob");
    InstanceData iData = new InstanceData(inst);

    assertThat(iData.getTagValueErrors()).contains("Invalid Lifecycle tag value 'bob'");

  }

  @Test
  public void ShouldDetectInstancesWithMissingNameTags() {

    Instance inst = TestUtil.getMockInstance("running", "");
    when(inst.getInstanceId()).thenReturn("i-333444");

    InstanceData iData = new InstanceData(inst);

    assertThat(iData.getTagValueErrors()).contains("Instance i-333444: 'Name' tag is missing or empty");

  }


  @Test
  public void ShouldSetLaunchTime() {

    Instance inst = TestUtil.getMockInstance("running", "My EC2 Instance", 13);

    InstanceData iData = new InstanceData(inst);
    DateTime launchTime = new DateTime(iData.getLaunchTime());

    ReadablePeriod thirteenHours = new Period(13, 0, 0, 0);

    assertThat(Seconds.secondsBetween(
        launchTime,
        new DateTime().minus(thirteenHours))
        .getSeconds()
    ).isLessThan(5);

  }

}
