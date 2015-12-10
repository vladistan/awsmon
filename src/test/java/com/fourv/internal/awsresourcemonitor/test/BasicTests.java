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
package com.fourv.internal.awsresourcemonitor.test;

import com.amazonaws.regions.Region;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.fourv.internal.awsresourcemonitor.AWSResourceMonitor;
import com.fourv.internal.awsresourcemonitor.InstanceData;
import com.jaxb.junit.Testcase;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class BasicTests {


  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @Test
  public void shouldGenerateCorrectReportGivenMockedEc2() throws SAXException, TransformerException, IOException, ParserConfigurationException, JAXBException, URISyntaxException {
    AWSResourceMonitor mon = new AWSResourceMonitor();

    mon.setjUnitFormatReportPath(testFolder.getRoot().toPath().toString());
    mon.setNamePattern("G.*");
    mon.setMaxAllowedHoursToRun("2");

    AmazonEC2 ec2 = mock(AmazonEC2.class);
    DescribeInstancesResult result = mock(DescribeInstancesResult.class);

    List<Reservation> reservations = new ArrayList<Reservation>();
    when(result.getReservations()).thenReturn(reservations).thenReturn(new ArrayList<Reservation>());
    when(ec2.describeInstances((DescribeInstancesRequest) notNull())).thenReturn(result);

    Reservation res1 = mock(Reservation.class);
    Reservation res2 = mock(Reservation.class);
    reservations.add(res1);
    reservations.add(res2);

    List<Instance> list1 = new ArrayList<Instance>();
    when(res1.getInstances()).thenReturn(list1);

    List<Instance> list2 = new ArrayList<Instance>();
    when(res2.getInstances()).thenReturn(list2);


    Instance inst = TestUtil.getMockInstance("running", "GreySparkCyberDev-gmartinDemo Master");
    list1.add(inst);
    TestUtil.addInstanceTag(inst, "Lifecycle", "POC");
    TestUtil.addInstanceTag(inst, "ChargeLine", "InternalDev");
    TestUtil.addInstanceTag(inst, "Owner", "gmartin@fourv.com");
    TestUtil.addInstanceTag(inst, "Service", "VPN");
    TestUtil.addInstanceTag(inst, "Project", "GreySpark (v1.0)");



    inst = TestUtil.getMockInstance("terminated", "GreySparkCyberDev-gmartinDemoServices SSO Server", 1);
    TestUtil.addInstanceTag(inst, "Service", "VPN");
    TestUtil.addInstanceTag(inst, "ChargeLine", "InternalDev");
    TestUtil.addInstanceTag(inst, "Owner", "jcamp@fourv.com");
    TestUtil.addInstanceTag(inst, "Lifecycle", "OnDemand");
    TestUtil.addInstanceTag(inst, "Project", "GreySpark (v1.0)");

    list1.add(inst);

    inst = TestUtil.getMockInstance("running", "GreySparkCyberDev-SweeneyDev1Services SSO Server", 4);
    TestUtil.addInstanceTag(inst, "Service", "VPN");
    TestUtil.addInstanceTag(inst, "ChargeLine", "InternalDev");
    TestUtil.addInstanceTag(inst, "Owner", "jcamp@fourv.com");
    TestUtil.addInstanceTag(inst, "Lifecycle", "OnDemand");
    TestUtil.addInstanceTag(inst, "Project", "GreySpark (v1.0)");
    list2.add(inst);

    inst = TestUtil.getMockInstance("running", "GreySparkCyberDev-SweeneyDev1Services WebApp", 3);
    TestUtil.addInstanceTag(inst, "Service", "VPN");
    TestUtil.addInstanceTag(inst, "ChargeLine", "InternalDev");
    TestUtil.addInstanceTag(inst, "Owner", "jcamp@fourv.com");
    TestUtil.addInstanceTag(inst, "Lifecycle", "Spot");
    TestUtil.addInstanceTag(inst, "Project", "GreySpark (v1.0)");
    list2.add(inst);

    mon.run(ec2);

    verify(ec2, times(2)).setRegion((Region) anyObject());
    verify(ec2, times(2)).describeInstances((DescribeInstancesRequest) anyObject());
    verifyNoMoreInteractions(ec2);

    File basicSamplereport = TestUtil.getTestResource("testReportRunningInstances.xml");
    assertThat(mon.getJunitReportFile()).hasSameContentAs(basicSamplereport);
  }

  @Test
  public void shouldNotifyAboutMissingTagsInTheReport() throws SAXException, TransformerException, IOException, ParserConfigurationException, JAXBException, URISyntaxException {
    AWSResourceMonitor mon = new AWSResourceMonitor();

    mon.setjUnitFormatReportPath(testFolder.getRoot().toPath().toString());
    mon.setNamePattern("G.*");
    mon.setMaxAllowedHoursToRun("2");

    AmazonEC2 ec2 = mock(AmazonEC2.class);
    DescribeInstancesResult result = mock(DescribeInstancesResult.class);

    List<Reservation> reservations = new ArrayList<Reservation>();
    when(result.getReservations()).thenReturn(reservations).thenReturn(new ArrayList<Reservation>());
    when(ec2.describeInstances((DescribeInstancesRequest) notNull())).thenReturn(result);

    Reservation res1 = mock(Reservation.class);
    reservations.add(res1);

    List<Instance> list1 = new ArrayList<Instance>();
    when(res1.getInstances()).thenReturn(list1);


    Instance inst = TestUtil.getMockInstance("running", "GreySparkCyberDev-gmartinDemo Master");
    TestUtil.addInstanceTag(inst, "Owner", "jcamp@fourv.com");
    TestUtil.addInstanceTag(inst, "Project", "GreySpark (v1.0)");
    TestUtil.addInstanceTag(inst, "Service", "VPN");
    TestUtil.addInstanceTag(inst, "ChargeLine", "InternalDev");
    list1.add(inst);


    inst = TestUtil.getMockInstance("terminated", "GreySparkCyberDev-gmartinDemoServices SSO Server", 1);
    TestUtil.addInstanceTag(inst, "Owner", "jcamp@fourv.com");
    TestUtil.addInstanceTag(inst, "Project", "GreySpark (v1.0)");
    TestUtil.addInstanceTag(inst, "Service", "Build");
    TestUtil.addInstanceTag(inst, "ChargeLine", "InternalDev");
    list1.add(inst);

    inst = TestUtil.getMockInstance("running", "GreySparkCyberDev-SweeneyDev1Services SSO Server", 4);
    list1.add(inst);

    mon.run(ec2);

    File basicSamplereport = TestUtil.getTestResource("testReportTagsMissing.xml");
    File tmpFileName = mon.getJunitReportFile();
    assertThat(tmpFileName).hasSameContentAs(basicSamplereport);
  }

  @Test
  public void instanceDataMustHaveRegionFieldFilled() throws SAXException, TransformerException, IOException, ParserConfigurationException, JAXBException {

    AWSResourceMonitor mon = new AWSResourceMonitor();
    mon.setNamePattern("G.*");
    mon.setMaxAllowedHoursToRun("2");

    AmazonEC2 ec2 = mock(AmazonEC2.class);

    DescribeInstancesResult result = mock(DescribeInstancesResult.class);

    List<Reservation> reservations = new ArrayList<Reservation>();
    when(result.getReservations()).thenReturn(reservations).thenReturn(new ArrayList<Reservation>());
    when(ec2.describeInstances((DescribeInstancesRequest) notNull())).thenReturn(result);

    Reservation res1 = mock(Reservation.class);
    reservations.add(res1);

    List<Instance> list1 = new ArrayList<Instance>();
    when(res1.getInstances()).thenReturn(list1);

    Instance inst = TestUtil.getMockInstance("running", "GreySparkCyberDev-gmartinDemo Master");
    list1.add(inst);


    List<InstanceData> list = mon.getAllInstances(ec2);

    InstanceData instData = list.get(0);

    assertThat(instData.getRegion()).isEqualTo("us-east-1");

  }

  @Test
  public void instanceDataMustHaveRegionFieldFilledWhenMultipleRegionsArePresent() throws SAXException, TransformerException, IOException, ParserConfigurationException, JAXBException {

    AWSResourceMonitor mon = new AWSResourceMonitor();
    mon.setNamePattern("G.*");
    mon.setMaxAllowedHoursToRun("2");

    AmazonEC2 ec2 = mock(AmazonEC2.class);

    DescribeInstancesResult east_result = mock(DescribeInstancesResult.class);

    List<Reservation> east_reservations = new ArrayList<Reservation>();

    when(east_result.getReservations()).thenReturn(east_reservations);


    when(ec2.describeInstances((DescribeInstancesRequest) notNull())).thenReturn(east_result);

    Reservation res1 = mock(Reservation.class);
    east_reservations.add(res1);

    List<Instance> list1 = new ArrayList<Instance>();
    when(res1.getInstances()).thenReturn(list1);

    Instance inst = TestUtil.getMockInstance("running", "GreySparkCyberDev-gmartinDemo Master");
    list1.add(inst);


    List<InstanceData> list = mon.getAllInstances(ec2);

    InstanceData instData = list.get(0);
    assertThat(instData.getRegion()).isEqualTo("us-east-1");

    instData = list.get(1);
    assertThat(instData.getRegion()).isEqualTo("us-west-1");

  }



  @Test
  public void shouldNotifyAboutIncorectValueForLifecycleTag() throws SAXException, TransformerException, IOException, ParserConfigurationException, JAXBException, URISyntaxException {
    AWSResourceMonitor mon = new AWSResourceMonitor();

    mon.setjUnitFormatReportPath(testFolder.getRoot().toPath().toString());
    mon.setNamePattern("G.*");
    mon.setMaxAllowedHoursToRun("2");

    AmazonEC2 ec2 = mock(AmazonEC2.class);
    DescribeInstancesResult result = mock(DescribeInstancesResult.class);

    List<Reservation> reservations = new ArrayList<Reservation>();
    when(result.getReservations()).thenReturn(reservations).thenReturn(new ArrayList<Reservation>());
    when(ec2.describeInstances((DescribeInstancesRequest) notNull())).thenReturn(result);

    Reservation res1 = mock(Reservation.class);
    reservations.add(res1);

    List<Instance> list1 = new ArrayList<Instance>();
    when(res1.getInstances()).thenReturn(list1);


    Instance inst = TestUtil.getMockInstance("running", "GreySparkCyberDev-gmartinDemo Master");
    list1.add(inst);
    TestUtil.addInstanceTag(inst, "Service", "VPN");
    TestUtil.addInstanceTag(inst, "Lifecycle", "Bob");
    TestUtil.addInstanceTag(inst, "ChargeLine", "InternalDev");
    TestUtil.addInstanceTag(inst, "Owner", "jcamp@fourv.com");
    TestUtil.addInstanceTag(inst, "Project", "GreySpark (v1.0)");

    list1.add(TestUtil.getMockInstance("terminated", "GreySparkCyberDev-gmartinDemoServices SSO Server", 1));

    // Should give error for instances that already have other violations
    inst = TestUtil.getMockInstance("running", "GreySparkCyberDev-SweeneyDev1Services SSO Server", 4);
    list1.add(inst);
    TestUtil.addInstanceTag(inst, "Lifecycle", "Bob");
    TestUtil.addInstanceTag(inst, "ChargeLine", "InternalDev");
    TestUtil.addInstanceTag(inst, "Service", "VPN");
    TestUtil.addInstanceTag(inst, "Owner", "jcamp@fourv.com");
    TestUtil.addInstanceTag(inst, "Project", "GreySpark (v1.0)");


    mon.run(ec2);

    File basicSamplereport = TestUtil.getTestResource("testReportInvalidLifeCycleTag.xml");
    File tmpFileName = mon.getJunitReportFile();
    assertThat(tmpFileName).hasSameContentAs(basicSamplereport);
  }

  @Test
  public void shouldNotFlagLongRunningInstancesWithLifeCyclePermanent() throws SAXException, TransformerException, IOException, ParserConfigurationException, JAXBException {

    AWSResourceMonitor mon = new AWSResourceMonitor();
    mon.setNamePattern("G.*");
    mon.setMaxAllowedHoursToRun("2");
    mon.setjUnitFormatReportPath(testFolder.getRoot().toPath().toString());


    AmazonEC2 ec2 = mock(AmazonEC2.class);
    DescribeInstancesResult result = mock(DescribeInstancesResult.class);

    List<Reservation> reservations = new ArrayList<Reservation>();
    when(result.getReservations()).thenReturn(reservations).thenReturn(new ArrayList<Reservation>());
    when(ec2.describeInstances((DescribeInstancesRequest) notNull())).thenReturn(result);

    Reservation res1 = mock(Reservation.class);
    reservations.add(res1);

    List<Instance> list1 = new ArrayList<Instance>();
    when(res1.getInstances()).thenReturn(list1);

    Instance inst = TestUtil.getMockInstance("running", "GreySparkCyberDev-gmartinDemo Master", 480);
    list1.add(inst);
    TestUtil.addInstanceTag(inst, "ChargeLine", "InternalDev");
    TestUtil.addInstanceTag(inst, "Service", "VPN");
    TestUtil.addInstanceTag(inst, "Owner", "jcamp@fourv.com");
    TestUtil.addInstanceTag(inst, "Project", "GreySpark (v1.0)");
    TestUtil.addInstanceTag(inst, "Lifecycle", "Permanent");
    TestUtil.addInstanceTag(inst, "ChargeLine", "InternalDev");
    TestUtil.addInstanceTag(inst, "Service", "VPN");
    TestUtil.addInstanceTag(inst, "Owner", "jcamp@fourv.com");
    TestUtil.addInstanceTag(inst, "Project", "GreySpark (v1.0)");


    mon.run(ec2);

    List<Testcase> res = mon.getTestResults();

    Testcase firstCase = res.get(0);

    assertThat(res).hasSize(1);
    assertThat(firstCase.getFailure()).hasSize(0);




  }


  @Test
  public void weCanWriteBasicXMLReport() throws JAXBException, SAXException, IOException, TransformerException, ParserConfigurationException, URISyntaxException {

    AWSResourceMonitor mon = new AWSResourceMonitor();
    final String message = "Instance '%s', has been running longer than the allowable time.";

    mon.setjUnitFormatReportPath(testFolder.getRoot().toPath().toString());

    mon.addResult(AWSResourceMonitor.getFailingTestCase("GreySparkCyberDev-gmartinDemo Master", "RunningTime",
      "Does not have required tag 'Lifecycle'"));


    mon.addResult(AWSResourceMonitor.getPassingTestCase("GreySparkCyberDev-gmartinDemoServices SSO Server", "RunningTime"));


    mon.addResult(AWSResourceMonitor.getFailingTestCase("GreySparkCyberDev-SweeneyDev1Services SSO Server", "RunningTime",
      String.format(message, "GreySparkCyberDev-SweeneyDev1Services SSO Server")));
    mon.addResult(AWSResourceMonitor.getFailingTestCase("GreySparkCyberDev-SweeneyDev1Services WebApp", "RunningTime",
      String.format(message, "GreySparkCyberDev-SweeneyDev1Services WebApp")));

    mon.writeJunitReport();

    File basicSamplereport = TestUtil.getTestResource("testReportBasicTest.xml");

    assertThat(mon.getJunitReportFile()).hasSameContentAs(basicSamplereport);

  }

  @Test
  public void shouldFlagInstancesThatWereRunningForYears() {
    AWSResourceMonitor mon = new AWSResourceMonitor();

    mon.setMaxAllowedHoursToRun("2");

    Date launchTime = new Date();
    launchTime.setYear(108);
    boolean rv = mon.beenRunningTooLong(launchTime, new Date());

    assertThat(rv).isTrue();
  }

  @Test
  public void shouldFlagInstancesOutsideOfUsEast1Region() {
    AWSResourceMonitor mon = new AWSResourceMonitor();


    List<InstanceData> instList = new ArrayList<InstanceData>();

    mon.assessInstances(instList);

    Instance inst = TestUtil.getMockInstance("running", "GreySparkCyberDev-SweeneyDev1Services SSO Server", 4);
    TestUtil.addInstanceTag(inst, "ChargeLine", "InternalDev");
    TestUtil.addInstanceTag(inst, "Service", "VPN");
    TestUtil.addInstanceTag(inst, "Project", "GreySpark (v1.0)");
    TestUtil.addInstanceTag(inst, "Lifecycle", "Permanent");
    TestUtil.addInstanceTag(inst, "ChargeLine", "InternalDev");
    TestUtil.addInstanceTag(inst, "Owner", "jcamp@fourv.com");


    InstanceData instData = new InstanceData(inst);
    instData.setRegion("us-west-9");
    instList.add(instData);

    mon.assessInstances(instList);
    List<Testcase> res = mon.getTestResults();

    assertThat(res).hasSize(1);

    Testcase case1 = res.get(0);

    assertThat(case1.getFailure()).hasSize(1);

    assertThat(case1.getFailure().get(0).getMessage()).isEqualTo("Found instance outside of US_EAST1 region");


  }


  @Test
  public void shouldNotFlagInstancesThatWasJustLaunched() {
    AWSResourceMonitor mon = new AWSResourceMonitor();

    mon.setMaxAllowedHoursToRun("2");

    Date launchTime = new Date();
    boolean rv = mon.beenRunningTooLong(launchTime, new Date());

    assertThat(rv).isFalse();
  }

  @Test
  public void shouldMarkInstanceThatWasOverHourLong() {
    Date now = new Date();
    DateTime launch = new DateTime(now).minus(new Period(3, 0, 0, 0));

    AWSResourceMonitor mon = new AWSResourceMonitor();
    mon.setMaxAllowedHoursToRun("1");

    boolean rv = mon.beenRunningTooLong(launch.toDate(), now);
    assertThat(rv).isTrue();
  }

  @Test
  public void shouldNotMarkInstanceThatIsOneHourLongWithAllowedFourHours() {
    Date now = new Date();
    DateTime launch = new DateTime(now).minus(new Period(1, 0, 0, 0));

    AWSResourceMonitor mon = new AWSResourceMonitor();
    mon.setMaxAllowedHoursToRun("4");

    boolean rv = mon.beenRunningTooLong(launch.toDate(), now);
    assertThat(rv).isFalse();
  }


}
