/**
 *
 * Copyright 2017 Vlad Korolev
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/

package org.vlad.awsresourcemonitor.test;

import com.jaxb.junit.ObjectFactory;
import com.jaxb.junit.Testcase;
import com.jaxb.junit.Testsuite;
import com.jaxb.junit.Testsuites;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.vlad.awsresourcemonitor.Schemas;
import org.vlad.awsresourcemonitor.JaxbUtil;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;

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

    ms = JaxbUtil.createMarshaller(Schemas.JUNIT_SCHEMA, Testsuites.class);
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
