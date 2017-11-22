/**
 *
 * Copyright 2017 Vlad Korolev
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/

package org.vlad.awsresourcemonitor;


import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.net.URL;

/**
 * Various utility methods to make working with JAXB bit easier.
 */

public final class JaxbUtil {

  private JaxbUtil() {

  }

  /**
   * Create XML marshaller for schema and class.
   * @param schema  schema to use
   * @param clazz   class for marshaller
   * @return  marshaller
   */
  public static Marshaller createMarshaller(final String schema, final Class<?> clazz)
    throws JAXBException, SAXException {
    return createMarshaller(getDefaultClassLoader(), schema, clazz);
  }

  /**
   * Get defaut class loader for current thread.
   * @return class loader
   */
  public static ClassLoader getDefaultClassLoader() {
    return Thread.currentThread().getContextClassLoader();
  }

  /**
   * Create marshaller for specified class loader, schema and class.
   * @param cL  class loader to use
   * @param schema schema
   * @param clazz  class
   * @return marshaller
   */
  public static Marshaller createMarshaller(final ClassLoader cL, final String schema, final Class<?> clazz)
    throws JAXBException, SAXException {

    final JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
    final Marshaller marshaller = jaxbContext.createMarshaller();

    marshaller.setSchema(getSchema(cL, schema));
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);


    return marshaller;
  }

  /**
   * Load schema from app resources
   * @param cL   class loader to use
   * @param schema schema file
   * @return  loaded JAXB schema
   * @throws SAXException
   */
  public static Schema getSchema(final ClassLoader cL, final String schema) throws SAXException {

    final SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
    final URL schemaFile = cL.getResource(schema);
    return schemaFactory.newSchema(schemaFile);
  }

}
