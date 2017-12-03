/**
 *
 * Copyright 2017 Vlad Korolev
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/
package org.vlad.awsresourcemonitor.test;

import com.amazonaws.regions.Region;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.jaxb.junit.Testcase;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.vlad.awsresourcemonitor.AWSResourceMonitor;
import org.vlad.awsresourcemonitor.Ec2InstanceCollection;
import org.vlad.awsresourcemonitor.InstanceData;
import org.vlad.awsresourcemonitor.PolicyReport;
import org.vlad.awsresourcemonitor.exception.XmlException;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.*;

public class BasicTests {

  private String policyFile;
  private String altPolicyFile;

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @Before
  public void setUp() throws URISyntaxException {
      policyFile = TestUtil.getTestResource("policy.yaml").getPath();
      altPolicyFile = TestUtil.getTestResource("alt-policy.yaml").getPath();
  }

  @Test
  public void shouldGenerateCorrectReportGivenMockedEc2()
    throws SAXException, TransformerException,
    IOException, ParserConfigurationException, JAXBException, URISyntaxException,
    XmlException, ParseException {

    AWSResourceMonitor mon = new AWSResourceMonitor();

    String reportPath = testFolder.getRoot().toPath().toString();
    mon.setjUnitFormatReportPath(reportPath);
    mon.setNamePattern("my.*");
    mon.setMaxAllowedHoursToRun("2");
    mon.loadPolicy(policyFile);


    AmazonEC2 ec2 = mock(AmazonEC2.class);
    DescribeInstancesResult result = mock(DescribeInstancesResult.class);

    List<Reservation> reservations = new ArrayList<Reservation>();
    when(result.getReservations()).thenReturn(reservations)
                                  .thenReturn(new ArrayList<Reservation>());
    when(ec2.describeInstances((DescribeInstancesRequest) notNull())).thenReturn(result);

    Reservation res1 = mock(Reservation.class);
    Reservation res2 = mock(Reservation.class);
    reservations.add(res1);
    reservations.add(res2);

    List<Instance> list1 = new ArrayList<Instance>();
    when(res1.getInstances()).thenReturn(list1);

    List<Instance> list2 = new ArrayList<Instance>();
    when(res2.getInstances()).thenReturn(list2);


    Instance inst = TestUtil.getMockInstance("running", "myOrg-app1 Master");
    list1.add(inst);
    TestUtil.addInstanceTag(inst, "Lifecycle", "POC");
    TestUtil.addInstanceTag(inst, "ChargeLine", "InternalDev");
    TestUtil.addInstanceTag(inst, "Owner", "Akshay");
    TestUtil.addInstanceTag(inst, "Service", "VPN");
    TestUtil.addInstanceTag(inst, "Environment", "Common");
    TestUtil.addInstanceTag(inst, "Project", "App1 (v1.0)");



    inst = TestUtil.getMockInstance("terminated", "myOrg-app1Services SSO Server", 1);
    TestUtil.addInstanceTag(inst, "Service", "Auth");
    TestUtil.addInstanceTag(inst, "ChargeLine", "InternalDev");
    TestUtil.addInstanceTag(inst, "Owner", "Vlad");
    TestUtil.addInstanceTag(inst, "Lifecycle", "OnDemand");
    TestUtil.addInstanceTag(inst, "Environment", "Common");
    TestUtil.addInstanceTag(inst, "Project", "App1 (v1.0)");

    list1.add(inst);

    inst = TestUtil.getMockInstance("running", "myOrg-app2Services SSO Server", 4);
    TestUtil.addInstanceTag(inst, "Service", "Authentication");
    TestUtil.addInstanceTag(inst, "ChargeLine", "InternalDev");
    TestUtil.addInstanceTag(inst, "Environment", "Common");
    TestUtil.addInstanceTag(inst, "Owner", "Vlad");
    TestUtil.addInstanceTag(inst, "Lifecycle", "OnDemand");
    TestUtil.addInstanceTag(inst, "Project", "App1 (v1.0)");
    TestUtil.addInstanceTag(inst, "Environment", "Invalid");
    list2.add(inst);

    inst = TestUtil.getMockInstance("running", "myOrg-app2Services WebApp", 3);
    TestUtil.addInstanceTag(inst, "Service", "VPN");
    TestUtil.addInstanceTag(inst, "ChargeLine", "InternalDev");
    TestUtil.addInstanceTag(inst, "Environment", "Common");
    TestUtil.addInstanceTag(inst, "Owner", "Vlad");
    TestUtil.addInstanceTag(inst, "Lifecycle", "POC");
    TestUtil.addInstanceTag(inst, "Project", "App1 (v1.0)");

    TestUtil.addInstanceTag(inst, "aws:autoscaling:groupName", "asg-1");
    TestUtil.addInstanceTag(inst, "aws:cloudformation:logical-id", "55555");
    TestUtil.addInstanceTag(inst, "VPCStackPrefix", "xx");
    list2.add(inst);

    mon.run(ec2);

    verify(ec2, times(9)).setRegion((Region) anyObject());
    verify(ec2, times(9)).describeInstances((DescribeInstancesRequest) anyObject());
    verifyNoMoreInteractions(ec2);


    File basicSamplereport = TestUtil.getTestResource("testReportRunningInstances.xml");
    PolicyReport pReport = new PolicyReport(reportPath);
    assertThat(pReport.getJunitReportFile()).hasSameContentAs(basicSamplereport);
  }

  @Test
  public void shouldNotifyAboutMissingTagsInTheReport() throws SAXException, TransformerException, IOException, ParserConfigurationException, JAXBException, URISyntaxException, XmlException, ParseException {
    AWSResourceMonitor mon = new AWSResourceMonitor();

    String reportPath = testFolder.getRoot().toPath().toString();
    mon.setjUnitFormatReportPath(reportPath);
    mon.setNamePattern("my.*");
    mon.setMaxAllowedHoursToRun("2");
    mon.loadPolicy(policyFile);


    AmazonEC2 ec2 = mock(AmazonEC2.class);
    DescribeInstancesResult result = mock(DescribeInstancesResult.class);

    List<Reservation> reservations = new ArrayList<Reservation>();
    when(result.getReservations())
      .thenReturn(reservations)
      .thenReturn(new ArrayList<Reservation>());
    when(ec2.describeInstances((DescribeInstancesRequest) notNull())).thenReturn(result);

    Reservation res1 = mock(Reservation.class);
    reservations.add(res1);

    List<Instance> list1 = new ArrayList<Instance>();
    when(res1.getInstances()).thenReturn(list1);


    Instance inst = TestUtil.getMockInstance("running", "myOrg-app1 Master");
    TestUtil.addInstanceTag(inst, "Owner", "Vlad");
    TestUtil.addInstanceTag(inst, "Project", "App1 (v1.0)");
    TestUtil.addInstanceTag(inst, "Environment", "Dev");
    TestUtil.addInstanceTag(inst, "Service", "VPN");
    TestUtil.addInstanceTag(inst, "ChargeLine", "InternalDev");
    list1.add(inst);


    inst = TestUtil.getMockInstance("terminated", "myOrg-app1Services SSO Server", 1);
    TestUtil.addInstanceTag(inst, "Owner", "Vlad");
    TestUtil.addInstanceTag(inst, "Project", "App1 (v1.0)");
    TestUtil.addInstanceTag(inst, "Service", "Build");
    TestUtil.addInstanceTag(inst, "ChargeLine", "InternalDev");
    list1.add(inst);

    inst = TestUtil.getMockInstance("running", "myOrg-app2Services SSO Server", 4);
    list1.add(inst);

    mon.run(ec2);

    File basicSamplereport = TestUtil.getTestResource("testReportTagsMissing.xml");

    PolicyReport pReport = new PolicyReport(reportPath);
    File tmpFileName = pReport.getJunitReportFile();

    assertThat(tmpFileName).hasSameContentAs(basicSamplereport);
  }

  @Test
  public void instanceDataMustHaveRegionFieldFilled() throws SAXException, TransformerException, IOException, ParserConfigurationException, JAXBException {

    AWSResourceMonitor mon = new AWSResourceMonitor();
    mon.setNamePattern("my.*");
    mon.setMaxAllowedHoursToRun("2");

    AmazonEC2 ec2 = mock(AmazonEC2.class);



    DescribeInstancesResult result = mock(DescribeInstancesResult.class);

    List<Reservation> reservations = new ArrayList<Reservation>();
    when(result.getReservations())
      .thenReturn(reservations)
      .thenReturn(new ArrayList<Reservation>());
    when(ec2.describeInstances((DescribeInstancesRequest) notNull())).thenReturn(result);

    Reservation res1 = mock(Reservation.class);
    reservations.add(res1);

    List<Instance> list1 = new ArrayList<Instance>();
    when(res1.getInstances()).thenReturn(list1);

    Instance inst = TestUtil.getMockInstance("running", "myOrg-App1 Master");
    list1.add(inst);


    List<InstanceData> list = new Ec2InstanceCollection(ec2).getObjList();
    InstanceData instData = list.get(0);

    assertThat(instData.getRegion()).isEqualTo("us-east-1");

  }

  @Test
  public void allowAlternativeEnvInPolicyFile() throws FileNotFoundException, ParseException {

    AWSResourceMonitor mon = new AWSResourceMonitor();
    mon.setNamePattern("my.*");
    mon.setMaxAllowedHoursToRun("2");
    mon.loadPolicy(altPolicyFile);


  }

  @Test
  public void instanceDataMustHaveRegionFieldFilledWhenMultipleRegionsArePresent()
    throws SAXException, TransformerException,
           IOException, ParserConfigurationException,
           JAXBException, ParseException {

    AWSResourceMonitor mon = new AWSResourceMonitor();
    mon.setNamePattern("my.*");
    mon.setMaxAllowedHoursToRun("2");
    mon.loadPolicy(policyFile);

    AmazonEC2 ec2 = mock(AmazonEC2.class);

    DescribeInstancesResult east_result = mock(DescribeInstancesResult.class);

    List<Reservation> east_reservations = new ArrayList<Reservation>();

    when(east_result.getReservations()).thenReturn(east_reservations);


    when(ec2.describeInstances((DescribeInstancesRequest) notNull())).thenReturn(east_result);

    Reservation res1 = mock(Reservation.class);
    east_reservations.add(res1);

    List<Instance> list1 = new ArrayList<Instance>();
    when(res1.getInstances()).thenReturn(list1);

    Instance inst = TestUtil.getMockInstance("running", "myOrg-App1 Master");
    list1.add(inst);


    List<InstanceData> list = new Ec2InstanceCollection(ec2).getObjList();

    InstanceData instData = list.get(0);
    assertThat(instData.getRegion()).isEqualTo("us-east-1");

    instData = list.get(1);
    assertThat(instData.getRegion()).isEqualTo("us-west-1");

  }


  @Test
  public void shouldHandlePoliciesWithNoOwnerTag() throws SAXException, TransformerException, IOException, ParserConfigurationException, JAXBException, URISyntaxException, XmlException, ParseException {
    AWSResourceMonitor mon = new AWSResourceMonitor();

    String reportPath = testFolder.getRoot().toPath().toString();
    mon.setjUnitFormatReportPath(reportPath);
    mon.setNamePattern("my.*");
    mon.setMaxAllowedHoursToRun("2");
    mon.loadPolicy(altPolicyFile);


    AmazonEC2 ec2 = mock(AmazonEC2.class);
    DescribeInstancesResult result = mock(DescribeInstancesResult.class);

    List<Reservation> reservations = new ArrayList<Reservation>();
    when(result.getReservations())
      .thenReturn(reservations)
      .thenReturn(new ArrayList<Reservation>());
    when(ec2.describeInstances((DescribeInstancesRequest) notNull())).thenReturn(result);

    Reservation res1 = mock(Reservation.class);
    reservations.add(res1);

    List<Instance> list1 = new ArrayList<Instance>();
    when(res1.getInstances()).thenReturn(list1);


    Instance inst = TestUtil.getMockInstance("running", "myOrg-app1 Master");
    list1.add(inst);
    TestUtil.addInstanceTag(inst, "Service", "VPN");
    TestUtil.addInstanceTag(inst, "Lifecycle", "Bob");
    TestUtil.addInstanceTag(inst, "ChargeLine", "InternalDev");
    TestUtil.addInstanceTag(inst, "Env", "common");
    TestUtil.addInstanceTag(inst, "Project", "App1 (v1.0)");

    list1.add(TestUtil.getMockInstance("terminated", "myOrg-app1Services SSO Server", 1));

    // Should give error for instances that already have other violations
    inst = TestUtil.getMockInstance("running", "myOrg-app2Services SSO Server", 4);
    list1.add(inst);
    TestUtil.addInstanceTag(inst, "Lifecycle", "Bob");
    TestUtil.addInstanceTag(inst, "ChargeLine", "InternalDev");
    TestUtil.addInstanceTag(inst, "Service", "VPN");
    TestUtil.addInstanceTag(inst, "Env", "prod");
    TestUtil.addInstanceTag(inst, "Project", "App1 (v1.0)");


    mon.run(ec2);

    File basicSamplereport = TestUtil.getTestResource("testReportInvalidLifeCycleTag.xml");
    PolicyReport pReport = new PolicyReport(reportPath);

    File tmpFileName = pReport.getJunitReportFile();
    assertThat(tmpFileName).hasSameContentAs(basicSamplereport);
  }



  @Test
  public void shouldNotifyAboutIncorectValueForLifecycleTag() throws SAXException, TransformerException, IOException, ParserConfigurationException, JAXBException, URISyntaxException, XmlException, ParseException {
    AWSResourceMonitor mon = new AWSResourceMonitor();

    String reportPath = testFolder.getRoot().toPath().toString();
    mon.setjUnitFormatReportPath(reportPath);
    mon.setNamePattern("my.*");
    mon.setMaxAllowedHoursToRun("2");
    mon.loadPolicy(policyFile);


    AmazonEC2 ec2 = mock(AmazonEC2.class);
    DescribeInstancesResult result = mock(DescribeInstancesResult.class);

    List<Reservation> reservations = new ArrayList<Reservation>();
    when(result.getReservations())
      .thenReturn(reservations)
      .thenReturn(new ArrayList<Reservation>());
    when(ec2.describeInstances((DescribeInstancesRequest) notNull())).thenReturn(result);

    Reservation res1 = mock(Reservation.class);
    reservations.add(res1);

    List<Instance> list1 = new ArrayList<Instance>();
    when(res1.getInstances()).thenReturn(list1);


    Instance inst = TestUtil.getMockInstance("running", "myOrg-app1 Master");
    list1.add(inst);
    TestUtil.addInstanceTag(inst, "Service", "VPN");
    TestUtil.addInstanceTag(inst, "Lifecycle", "Bob");
    TestUtil.addInstanceTag(inst, "ChargeLine", "InternalDev");
    TestUtil.addInstanceTag(inst, "Environment", "Common");
    TestUtil.addInstanceTag(inst, "Owner", "Vlad");
    TestUtil.addInstanceTag(inst, "Project", "App1 (v1.0)");

    list1.add(TestUtil.getMockInstance("terminated", "myOrg-app1Services SSO Server", 1));

    // Should give error for instances that already have other violations
    inst = TestUtil.getMockInstance("running", "myOrg-app2Services SSO Server", 4);
    list1.add(inst);
    TestUtil.addInstanceTag(inst, "Lifecycle", "Bob");
    TestUtil.addInstanceTag(inst, "ChargeLine", "InternalDev");
    TestUtil.addInstanceTag(inst, "Service", "VPN");
    TestUtil.addInstanceTag(inst, "Environment", "Prod");
    TestUtil.addInstanceTag(inst, "Owner", "Vlad");
    TestUtil.addInstanceTag(inst, "Project", "App1 (v1.0)");


    mon.run(ec2);

    File basicSamplereport = TestUtil.getTestResource("testReportInvalidLifeCycleTag.xml");
    PolicyReport pReport = new PolicyReport(reportPath);

    File tmpFileName = pReport.getJunitReportFile();
    assertThat(tmpFileName).hasSameContentAs(basicSamplereport);
  }

  @Test
  public void shouldNotFlagLongRunningInstancesWithLifeCyclePermanentOrSpot() throws SAXException, TransformerException, IOException, ParserConfigurationException, JAXBException, XmlException {

    AWSResourceMonitor mon = new AWSResourceMonitor();
    mon.setNamePattern("myOrg.*");
    mon.setMaxAllowedHoursToRun("2");
    mon.setjUnitFormatReportPath(testFolder.getRoot().toPath().toString());


    AmazonEC2 ec2 = mock(AmazonEC2.class);
    DescribeInstancesResult result = mock(DescribeInstancesResult.class);

    List<Reservation> reservations = new ArrayList<Reservation>();
    when(result.getReservations())
         .thenReturn(reservations)
         .thenReturn(new ArrayList<Reservation>());
    when(ec2.describeInstances((DescribeInstancesRequest) notNull())).thenReturn(result);

    Reservation res1 = mock(Reservation.class);
    reservations.add(res1);

    List<Instance> list1 = new ArrayList<Instance>();
    when(res1.getInstances()).thenReturn(list1);

    Instance inst = TestUtil.getMockInstance("running", "myOrg-app1 Master", 480);
    list1.add(inst);
    TestUtil.addInstanceTag(inst, "ChargeLine", "InternalDev");
    TestUtil.addInstanceTag(inst, "Lifecycle", "Permanent");
    TestUtil.addInstanceTag(inst, "Environment", "Prod");
    TestUtil.addInstanceTag(inst, "Service", "VPN");
    TestUtil.addInstanceTag(inst, "Owner", "Vlad");
    TestUtil.addInstanceTag(inst, "Project", "App1 (v1.0)");


    inst = TestUtil.getMockInstance("running", "myOrg-app1 Master-2", 480);
    list1.add(inst);
    TestUtil.addInstanceTag(inst, "ChargeLine", "InternalDev");
    TestUtil.addInstanceTag(inst, "Lifecycle", "Spot");
    TestUtil.addInstanceTag(inst, "Environment", "Prod");
    TestUtil.addInstanceTag(inst, "Service", "VPN");
    TestUtil.addInstanceTag(inst, "Owner", "Vlad");
    TestUtil.addInstanceTag(inst, "Project", "App1 (v1.0)");



    mon.run(ec2);

    List<Testcase> res = mon.getTestResults();

    assertThat(res).hasSize(2);

    Testcase testCase = res.get(0);
    assertThat(testCase.getFailure()).hasSize(0);

    testCase = res.get(1);
    assertThat(testCase.getFailure()).hasSize(0);

  }


  @Test
  public void weCanWriteBasicXMLReport() throws JAXBException, SAXException, IOException, TransformerException, ParserConfigurationException, URISyntaxException, XmlException {


    AWSResourceMonitor mon = new AWSResourceMonitor();
    final String message = "Instance '%s', has been running longer than the allowable time.";

    String reportPath = testFolder.getRoot().toPath().toString();
    mon.setjUnitFormatReportPath(reportPath);

    mon.addResult(PolicyReport.getFailingTestCase("myOrg-app1 Master", "RunningTime",
      "Does not have required tag 'Lifecycle'"));

    mon.addResult(PolicyReport.getPassingTestCase("myOrg-app1Services SSO Server", "RunningTime"));


    mon.addResult(PolicyReport.getFailingTestCase("myOrg-app2Services SSO Server", "RunningTime",
      String.format(message, "myOrg-app2Services SSO Server")));
    mon.addResult(PolicyReport.getFailingTestCase("myOrg-app2Services WebApp", "RunningTime",
      String.format(message, "myOrg-app2Services WebApp")));

    PolicyReport pReport = new PolicyReport(reportPath);
    pReport.writeJunitReport(mon.numFailing, mon.testResults);

    File basicSamplereport = TestUtil.getTestResource("testReportBasicTest.xml");

    assertThat(pReport.getJunitReportFile()).hasSameContentAs(basicSamplereport);

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

    Instance inst = TestUtil.getMockInstance("running", "myOrg-app2Services SSO Server", 4);
    TestUtil.addInstanceTag(inst, "ChargeLine", "InternalDev");
    TestUtil.addInstanceTag(inst, "Service", "VPN");
    TestUtil.addInstanceTag(inst, "Project", "App1 (v1.0)");
    TestUtil.addInstanceTag(inst, "Environment", "Common");
    TestUtil.addInstanceTag(inst, "Lifecycle", "Permanent");
    TestUtil.addInstanceTag(inst, "ChargeLine", "InternalDev");
    TestUtil.addInstanceTag(inst, "Owner", "Vlad");


    InstanceData instData = new InstanceData(inst);
    instData.setRegion("us-west-9");
    instList.add(instData);

    mon.initialize();
    mon.assessInstances(instList);
    List<Testcase> res = mon.getTestResults();

    assertThat(res).hasSize(1);

    Testcase case1 = res.get(0);

    assertThat(case1.getFailure()).hasSize(1);

    assertThat(case1.getFailure().get(0).getMessage()).isEqualTo("Found instance outside of us-east-1 region");


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
