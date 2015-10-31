package com.fourv.internal.awsresourcemonitor.test; /**
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

import com.fourv.internal.awsresourcemonitor.Schemas;
import com.fourv.internal.awsresourcemonitor.Util;
import com.fourv.internal.awsresourcemonitor.test.TestUtil;
import com.jaxb.junit.ObjectFactory;
import com.jaxb.junit.Testcase;
import com.jaxb.junit.Testsuite;
import com.jaxb.junit.Testsuites;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This tests are to make sure we got all XML JAXB plumbing right.
 */
public class XmlWriter {


  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();
  private Marshaller ms;
  private ObjectFactory of;

  @Test
  public void writeXMLFile() throws JAXBException, SAXException, IOException, URISyntaxException {

    ms = Util.createMarshaller(Schemas.JUNIT_SCHEMA, Testsuites.class);
    of = new ObjectFactory();

    File reportOutput = testFolder.newFile();
    FileOutputStream os = new FileOutputStream(reportOutput);

    Testsuites report = of.createTestsuites();

    Testsuite ts = of.createTestsuite();
    ts.setName("TagsPresent");
    ts.setTests("5");


    Testcase tc = of.createTestcase();
    tc.setName("Bob");

    ts.getTestcase().add(tc);

    report.getTestsuite().add(ts);

    ms.marshal(report, os);

    os.close();

    assertThat(reportOutput).hasSameContentAs(TestUtil.getTestResource("testReportBasicReport.xml"));

  }

}
