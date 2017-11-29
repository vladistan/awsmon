/**
 *
 * Copyright 2017 Vlad Korolev
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/

package org.vlad.awsresourcemonitor;

import com.jaxb.junit.*;
import org.apache.commons.io.FileUtils;
import org.vlad.awsresourcemonitor.exception.XmlException;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

/**
 * Report describing objects and policy violations.
 */
public class PolicyReport {

  static ObjectFactory of = new ObjectFactory();
  private final String reportPath;

  public PolicyReport(String reportPath) {

    this.reportPath = reportPath;
  }

  /**
   * Generate passing test case for report insertion.
   *
   * @param name     - instance name
   * @param testType - test name
   * @return test case
   */
  public static Testcase getPassingTestCase(final String name, String testType) {
    final Testcase testCase = of.createTestcase();
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
    Failure failure = of.createFailure();
    failure.setMessage(message);
    testCase.getFailure().add(failure);

    return testCase;
  }

  /**
   * This method creates a String output in the format of JUnit Report XML.

   * The format will be similar to the following:
   * &lt;?xml version="1.0" encoding="UTF-8"?&gt;
   * &lt;testsuite failures="4" tests="4"&gt;
   * &lt;testcase name="passedInstanceName" classname="classname"/&gt;
   * &lt;testcase name="failedInstanceName" classname="classname"/&gt;
   * &lt;failure message="....."/&gt;
   *
   * @return Junit report as string
   * @param numFailing
   * @param testResults
   */
  public String outputJunitReportFormat(int numFailing, List<Testcase> testResults)
    throws JAXBException, SAXException,
    TransformerException, ParserConfigurationException {

    Testsuite runningTimeSuite = of.createTestsuite();
    runningTimeSuite.setName("AwsResources");
    runningTimeSuite.setFailures(String.valueOf(numFailing));
    runningTimeSuite.setTests(String.valueOf(testResults.size()));

    Testsuites report = of.createTestsuites();

    report.getTestsuite().add(runningTimeSuite);

    for (Testcase pass : testResults) {
      runningTimeSuite.getTestcase().add(pass);
    }

    return getXml(report);

  }

  /**
   * Get file object for writing report file.
   *
   * @return report file obj
   */
  public File getJunitReportFile() {
    // if the directory does not exist, create it
    File reportDir = new File(this.reportPath);
    if (!reportDir.exists()) {
      reportDir.mkdir();
    }
    return new File(this.reportPath + "AWSResourceMonitorReport.xml");
  }

  /**
   * Verify whether JUnit output is desired and write it to the file.
   * @param numFailing
   * @param testResults
   */
  public void writeJunitReport(int numFailing, List<Testcase> testResults) throws IOException, XmlException {

    String xmlReport = null;
    try {
      xmlReport = outputJunitReportFormat(numFailing, testResults);
    } catch (JAXBException | SAXException | TransformerException | ParserConfigurationException e) {
      throw new XmlException(e);
    }

    FileUtils.writeStringToFile(getJunitReportFile(), xmlReport);


  }

  public String getXml(Testsuites report) throws JAXBException, SAXException {
    Marshaller ms = JaxbUtil.createMarshaller(Schemas.JUNIT_SCHEMA, Testsuites.class);
    StringWriter sw = new StringWriter();
    ms.marshal(report, sw);
    return sw.toString();
  }


}
