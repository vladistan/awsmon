/**
 *
 * Copyright 2017 Vlad Korolev
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/


package org.vlad.awsresourcemonitor;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.jaxb.junit.Testcase;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.vlad.awsresourcemonitor.exception.XmlException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;


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

  @Parameter(names = "--help", description = "Print help and exit", help = true)
  private boolean help;

  @Parameter(names = "--version", description = "Print version and exit")
  private boolean version;

  @Parameter(names = "--buildInfo", description = "Print build information")
  private boolean buildInfo;

  @Parameter(names = "--namePattern", description = "Name pattern to filter")
  private String namePattern = ".*";

  @Parameter(names = {"--reportPath"}, description = "Junit report path", required = true)
  public String jUnitFormatReportPath;

  @Parameter(names = {"--maxTime"}, description = "Max allowed time in hours")
  private Period maxAllowedHoursToRun = new Period(12, 0, 0, 0);

  @Parameter(names = {"--policyFile"}, description = "Policy file name", required = true)
  private File policyFile;


  public List<Testcase> testResults = new ArrayList<>();
  public int numFailing;
  private PolicyReport pReport;
  private Policy pol;


  /**
   * Main method.
   *
   * @param args command line arguments
   */
  public static void main(final String[] args) {

    final AWSResourceMonitor mon = new AWSResourceMonitor();

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
      System.out.println("Use AWSResource --help ");
      System.exit(2);
      //CHECKSTYLE:ON

    }


    try {
      final AmazonEC2Client ec2 = new AmazonEC2Client(new DefaultAWSCredentialsProviderChain());
      Policy.load(mon.policyFile);
      mon.run(ec2);
    } catch (ParseException | IOException | XmlException e) {
      System.out.println(e.getLocalizedMessage());
      e.printStackTrace();
    }

    System.exit(mon.exitCode());

  }

  private int exitCode() {
    return numFailing == 0 ? 0 : 1;
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
    List<InstanceData> instList = new Ec2InstanceCollection(ec2).getObjList();

    this.assessInstances(instList);
    pReport.writeJunitReport(this.numFailing, this.testResults);

  }

  /**
   * Go through the list of instances and assess that they don't violate the policy.
   *
   * @param instList list of instances
   */
  public void assessInstances(List<InstanceData> instList) {


    for (InstanceData objData : instList) {

      Policy pol = Policy.getInstance();

      final String instName = objData.name;

      if (!instName.matches(namePattern)) {
        continue;
      }

      boolean failure = false;


      if (objData.isRunning()) {

        final Date launchTime = objData.getLaunchTime();

        if (isSubjectToMaxRuntime(objData) && beenRunningTooLong(launchTime, new Date())) {
          // been running too long
          final String errMsg = "has been running longer than the allowable time.";
          addResult(PolicyReport.getFailingTestCase(instName, "RunningTime", errMsg));
          failure |= true;
        }

        if (!objData.getTagValueErrors().isEmpty()) {
          String errMsg = composeTagErrorMsg(objData);
          addResult(PolicyReport.getFailingTestCase(instName, "InvalidTagValue", errMsg));
          failure |= true;
        }

        failure |= checkTag(instName, "Lifecycle", objData.lifecycle.getValue());
        failure |= checkTag(instName, "Project", objData.project.getValue());
        failure |= checkTag(instName, "Service", objData.service.getValue());
        if ( pol.getOwners() != null ) {
          failure |= checkTag(instName, "Owner", objData.owner.getValue());
        }
        failure |= checkTag(instName, "ChargeLine", objData.chargeLine.getValue());
        failure |= checkTag(instName, "Environment", objData.environment.getValue());


        final Set<String> allowedRegions = pol.getAllowedRegion();
        if (!allowedRegions.contains(objData.getRegion())) {
          final String errMsg = "Found instance outside of " +  allowedRegions.toString() + " region";
          addResult(PolicyReport.getFailingTestCase(instName, "WrongRegion", errMsg));
          failure |= true;
        }

      }

      if (!failure) {
        addResult(PolicyReport.getPassingTestCase(instName, "RunningTime"));
      }

    }
  }

  private String composeTagErrorMsg(InstanceData objData) {
    String errMsg = "Tag Errors Detected :";
    for (String msg : objData.getTagValueErrors()) {
      errMsg += " [" + msg + "] ";
    }
    return errMsg;
  }

  private boolean isSubjectToMaxRuntime(InstanceData objData) {
    String lifeCycle = objData.lifecycle.getValue();
    return ! ("Permanent".equals(lifeCycle) || "Spot".equals(lifeCycle));
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

    if (!testCase.getFailure().isEmpty()) {
      numFailing++;
    }

    testResults.add(testCase);
  }


  private boolean checkTag(String name, String tagName, Object tagValue) {
    if (tagValue == null) {
      String errMsg = "Does not have required tag '" + tagName + "'";
      addResult(PolicyReport.getFailingTestCase(name, "MissingTag", errMsg));
      return true;
    }
    return false;
  }

  /**
   * Getter for testResults.
   *
   */
  public List<Testcase> getTestResults() {
    return new ArrayList<>(testResults);
  }

  public void initialize() {

    setMaxAllowedHoursToRun(System.getenv("MaxRunningTimeInHours"));

    pol = Policy.getInstance();
    pReport = new PolicyReport(jUnitFormatReportPath);

  }


  public void loadPolicy(String policyFile) throws FileNotFoundException, ParseException {

    final File polFile = new File(policyFile);
    Policy.load(polFile);

  }
}
