/**
 *
 * Copyright 2017 Vlad Korolev
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/

package org.vlad.awsresourcemonitor;

import com.jaxb.junit.ObjectFactory;
import com.jaxb.junit.Testsuites;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;

/**
 * Report describing objects and policy violations.
 */
public class PolicyReport {

  static ObjectFactory of = new ObjectFactory();

  public String getXml(Testsuites report) throws JAXBException, SAXException {
    Marshaller ms = JaxbUtil.createMarshaller(Schemas.JUNIT_SCHEMA, Testsuites.class);
    StringWriter sw = new StringWriter();
    ms.marshal(report, sw);
    return sw.toString();
  }


}
