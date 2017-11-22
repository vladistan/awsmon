/**
 *
 * Copyright 2017 Vlad Korolev
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/


package org.vlad.awsresourcemonitor;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.jaxb.junit.*;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.vlad.awsresourcemonitor.exception.XmlException;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.util.*;


/**
 * This class monitors the AWS resources for instances that match a specific
 * pattern, and has been running for longerthan the allowable time. This class
 * assumes that the following environment variables are set:
 * AWS_ACCESS_KEY_ID - The access key for AWS
 * AWS_SECRET_KEY - The secret key for AWS
 * InstanceNamePattern - The regular expression pattern for instance name
 * MaxRunningTimeInHours - The maximum amount of time in hours.
 */
public class AWSResourceMonitor {

  @Parameter(names = "-help", description = "Print help and exit", help = true)
  private boolean help;

  @Parameter(names = "-version", description = "Print version and exit")
  private boolean version;

  @Parameter(names = "-buildInfo", description = "Print build information")
  private boolean buildInfo;

  @Parameter(names = "-namePattern", description = "Name pattern to filter")
  private String namePattern = ".*";

  @Parameter(names = {"-reportPath"}, description = "Junit report path")
  public String jUnitFormatReportPath;

  @Parameter(names = {"-maxTime"}, description = "Max allowed time in hours")
  private Period maxAllowedHoursToRun = new Period(12, 0, 0, 0);


  private List<Testcase> testResults = new ArrayList<>();
  private int numFailing = 0;


  /**
   * Main method.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {

    final AWSResourceMonitor mon = new AWSResourceMonitor();
    final AmazonEC2Client ec2 = new AmazonEC2Client(new DefaultAWSCredentialsProviderChain());

    try {
      JCommander jc = new JCommander(mon, args);
      jc.setProgramName("AWSResourceMonitor");

      if (mon.help) {
        jc.usage();
        return;
      }

      if (mon.version) {
        VersionInfo.displayVersion();
        return;
      }

      if (mon.buildInfo) {
        VersionInfo.displayBuildInfo();
        return;
      }

    } catch (ParameterException exc) {

      //CHECKSTYLE:OFF
      System.out.println(exc.getMessage());
      System.out.println("Use AWSResource -help ");
      System.exit(2);
      //CHECKSTYLE:ON

    }

    try {
      mon.run(ec2);
    } catch (IOException e) {
      System.out.println(e.getLocalizedMessage());
      e.printStackTrace();
    } catch (XmlException e) {
      System.out.println(e.getLocalizedMessage());
      e.printStackTrace();
    }

  }

  /**
   * Generate passing test case for report insertion.
   *
   * @param name     - instance name
   * @param testType - test name
   * @return test case
   */
  public static Testcase getPassingTestCase(final String name, String testType) {
    final Testcase testCase = PolicyReport.of.createTestcase();
    testCase.setName(testType);
    testCase.setClassname(name);
    return testCase;
  }

  /**
   * Generate failing testcase for report insertion.
   *
   * @param instName - instance name
   * @param testName - test name
   * @param message  - error message
   * @return test case
   */
  public static Testcase getFailingTestCase(String instName, String testName, String message) {

    Testcase testCase = getPassingTestCase(instName, testName);
    Failure failure = PolicyReport.of.createFailure();
    failure.setMessage(message);
    testCase.getFailure().add(failure);

    return testCase;
  }

  /**
   * Set path to JUnit output.
   *
   * @param reportPath - directory to place output in
   */
  public void setjUnitFormatReportPath(String reportPath) {
    if (reportPath != null) {
      this.jUnitFormatReportPath = reportPath;
    }
  }

  /**
   * Set maximum allowed time to run.
   *
   * @param maxTime - maximum allowed hours
   */
  public void setMaxAllowedHoursToRun(String maxTime) {
    if (maxTime != null) {
      maxAllowedHoursToRun = new Period(Integer.parseInt(maxTime), 0, 0, 0);
    }
  }

  /**
   * Set name pattern to use.
   *
   * @param namePattern - regular expression of the pattern
   */
  public void setNamePattern(String namePattern) {
    if (namePattern != null) {
      this.namePattern = namePattern;
    }
  }

  /**
   * Run resource monitoring job.
   *
   * @param ec2 reference to EC2 API object
   */
  public void run(AmazonEC2 ec2) throws IOException, XmlException {

    this.initialize();
    List<InstanceData> instList = this.getAllInstances(ec2);
    this.assessInstances(instList);
    this.writeJunitReport();

  }

  /**
   * Go through the list of instances and assess that they don't violate the policy.
   *
   * @param instList list of instances
   */
  public void assessInstances(List<InstanceData> instList) {
    for (InstanceData objData : instList) {


      final String name = objData.name;

      if (!name.matches(namePattern)) {
        continue;
      }

      boolean failure = false;


      if (objData.isRunning()) {

        Date launchTime = objData.getLaunchTime();

        if ((!"Permanent".equals(objData.lifecycle))
          && beenRunningTooLong(launchTime, new Date())) {
          // been running too long
          String errMsg = "has been running longer than the allowable time.";
          addResult(getFailingTestCase(name, "RunningTime", errMsg));
          failure |= true;
        }

        if (!objData.getTagValueErrors().isEmpty()) {
          String errMsg = "Tag Errors Detected :";
          for (String msg : objData.getTagValueErrors()) {
            errMsg += " [" + msg + "] ";
          }

          addResult(getFailingTestCase(name, "InvalidTagValue", errMsg));
          failure |= true;
        }

        if (objData.lifecycle == null) {
          String errMsg = "Does not have required tag 'Lifecycle'";
          addResult(getFailingTestCase(name, "MissingTag", errMsg));
          failure |= true;
        }

        if (objData.project == null) {
          String errMsg = "Does not have required tag 'Project'";
          addResult(getFailingTestCase(name, "MissingTag", errMsg));
          failure |= true;
        }

        if (objData.service == null) {
          String errMsg = "Does not have required tag 'Service'";
          addResult(getFailingTestCase(name, "MissingTag", errMsg));
          failure |= true;
        }

        if (objData.owner == null) {
          String errMsg = "Does not have required tag 'Owner'";
          addResult(getFailingTestCase(name, "MissingTag", errMsg));
          failure |= true;
        }

        if (objData.chargeLine == null) {
          String errMsg = "Does not have required tag 'ChargeLine'";
          addResult(getFailingTestCase(name, "MissingTag", errMsg));
          failure |= true;
        }

        if (!objData.getRegion().equals("us-east-1")) {
          String errMsg = "Found instance outside of US_EAST1 region";
          addResult(getFailingTestCase(name, "WrongRegion", errMsg));
          failure |= true;
        }

      }

      if (!failure) {
        addResult(getPassingTestCase(name, "RunningTime"));
      }

    }
  }

  /**
   * Get all instances in US_EAST_1 and US_WEST_1.
   *
   * @param ec2 ec2 API object
   * @return list of instance proxy objects
   */
  public List<InstanceData> getAllInstances(AmazonEC2 ec2) {
    // Find all running EC2 instances that match the regular expression
    List<InstanceData> instList = new ArrayList<>(1000);

    Set<String> skipRegions = new HashSet<>();

    skipRegions.add("us-gov-west-1");
    skipRegions.add("cn-north-1");

    for (Regions reg : Regions.values()) {
      String regName = reg.getName();
      if (skipRegions.contains(regName)) {
        continue;
      }

      collectRegionInstances(ec2, instList, reg);
    }


    return instList;
  }

  /**
   * Get all instances in the region.
   *
   * @param ec2      ec2 object
   * @param instList instance list
   * @param region   region
   */
  public void collectRegionInstances(AmazonEC2 ec2, List<InstanceData> instList, Regions region) {
    ec2.setRegion(Region.getRegion(region));
    // Collect a list of running instances
    DescribeInstancesRequest request = new DescribeInstancesRequest();
    DescribeInstancesResult result = ec2.describeInstances(request);
    List<Reservation> reservations = result.getReservations();


    // loop through each running resource
    for (Reservation reservation : reservations) {
      for (Instance instance : reservation.getInstances()) {
        InstanceData data = new InstanceData(instance);
        data.setRegion(region.getName());
        instList.add(data);
      }
    }
  }


  /**
   * Check if instance been running to long.
   *
   * @param juLaunchTime instance launch time
   * @param now          current time
   * @return true if instance is overdue
   */
  public boolean beenRunningTooLong(Date juLaunchTime, Date now) {
    // check the time active...
    DateTime launchTime = new DateTime(juLaunchTime);
    return launchTime.plus(maxAllowedHoursToRun).isBefore(new DateTime(now));
  }

  /**
   * Add testcase to the list of results.
   *
   * @param testCase test case to add
   */
  public void addResult(Testcase testCase) {

    if (testCase.getFailure().size() != 0) {
      numFailing++;
    }

    testResults.add(testCase);
  }


  /**
   * Getter for testResults.
   *
   * @return accumulated test results
   */
  public List<Testcase> getTestResults() {
    return new ArrayList<Testcase>(testResults);
  }

  /**
   * Verify whether JUnit output is desired and write it to the file.
   */
  public void writeJunitReport() throws IOException, XmlException {
    if (jUnitFormatReportPath != null) {

      String xmlReport = null;
      try {
        xmlReport = outputJunitReportFormat();
      } catch (JAXBException | SAXException | TransformerException | ParserConfigurationException e) {
        throw new XmlException(e);
      }

      FileUtils.writeStringToFile(getJunitReportFile(jUnitFormatReportPath), xmlReport);

    }
  }

  /**
   * Get file object for writing report file.
   *
   * @return report file obj
   * @param jUnitFormatReportPath
   */
  public static File getJunitReportFile(String jUnitFormatReportPath) {
    // if the directory does not exist, create it
    File reportDir = new File(jUnitFormatReportPath);
    if (!reportDir.exists()) {
      reportDir.mkdir();
    }
    return new File(jUnitFormatReportPath + "AWSResourceMonitorReport.xml");
  }

  private void initialize() {

    setMaxAllowedHoursToRun(System.getenv("MaxRunningTimeInHours"));

  }

  /**
   * This method creates a String output in the format of JUnit Report XML.
   * <p/>
   * The format will be similar to the following:
   * <?xml version="1.0" encoding="UTF-8"?>
   * <testsuite failures="4" tests="4">
   * <testcase name="passedInstanceName" classname="classname"/>
   * <testcase name="failedInstanceName" classname="classname"/>
   * <failure message="....."/>
   *
   * @return Junit report as string
   */
  public String outputJunitReportFormat()
    throws JAXBException, SAXException,
    TransformerException, ParserConfigurationException {

    Testsuite runningTimeSuite = PolicyReport.of.createTestsuite();
    runningTimeSuite.setName("AwsResources");
    runningTimeSuite.setFailures(String.valueOf(numFailing));
    runningTimeSuite.setTests(String.valueOf(testResults.size()));

    Testsuites report = PolicyReport.of.createTestsuites();

    report.getTestsuite().add(runningTimeSuite);

    for (Testcase pass : testResults) {
      runningTimeSuite.getTestcase().add(pass);
    }

    PolicyReport policyReport = new PolicyReport();
    return policyReport.getXml(report);

  }


}
