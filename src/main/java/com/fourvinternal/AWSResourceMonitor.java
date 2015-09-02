package com.fourvinternal;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class monitors the AWS resources for instances that match a specific pattern, and has been running for longer
 * than the allowable time. This class assumes that the following environment variables are set:
 * AWS_ACCESS_KEY_ID - The access key for AWS
 * AWS_SECRET_KEY - The secret key for AWS
 * InstanceNamePattern - The regular expression pattern to match the instance name to
 * MaxRunningTimeInHours - The maximum amount of time in hours.
 */
public class AWSResourceMonitor {
    public static void main(String args[]) {
        String namePattern = "(.*)DevCloud";
        int maxRunningTimeInMills = 12 * 60 * 60 * 1000;
        String jUnitFormatReportPath = null;
        if (System.getenv("InstanceNamePattern") != null)
            namePattern = System.getenv("InstanceNamePattern");
        String maxRunningTimeInHours = System.getenv("MaxRunningTimeInHours");
        if (maxRunningTimeInHours != null)
            maxRunningTimeInMills = Integer.parseInt(maxRunningTimeInHours) * 60 * 60 * 1000;
        if (System.getenv("JUnitFormatReportPath") != null)
            jUnitFormatReportPath = System.getenv("JUnitFormatReportPath");

        // Find all running EC2 instances that match the regular expression
        AmazonEC2 ec2 = new AmazonEC2Client(new DefaultAWSCredentialsProviderChain());
        ec2.setRegion(Region.getRegion(Regions.US_EAST_1));

        System.out.println("*************************************");
        System.out.println("Getting List of Running EC2 Instances");
        System.out.println("*************************************");
        System.out.println("*** System Environment Variable InstanceNamePattern: " + namePattern);
        System.out.println("*** System Environment Variable MaxRunningTimeInHours: " + maxRunningTimeInHours);

        // Collect a list of running instances
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        DescribeInstancesResult result = ec2.describeInstances(request);
        List<Reservation> reservations = result.getReservations();

        // loop through each running resource
        List<String> runningResources = new ArrayList<String>();
        List<String> passedInstanceNames = new ArrayList<String>();
        for (Reservation reservation : reservations) {
            List<Instance> instances = reservation.getInstances();

            for (Instance instance : instances) {
                boolean foundName = false;
                List<Tag> tags = instance.getTags();
                String name = null;
                for (Tag tag : tags) {
                    if (tag.getKey().equals("Name") && tag.getValue().matches(namePattern)) {
                        foundName = true;
                        name = tag.getValue();
                        break;
                    }
                }
                if (foundName) {
                    if (instance.getState().equals("running")) {
                        Date launchTime = instance.getLaunchTime();
                        System.out.println("*** Running Instances: " + name + " --- " + instance.getInstanceId() +
                                " - " + instance.getState().toString() +
                                " - " + instance.getLaunchTime().toString());

                        // check the time active...
                        long maxTime = launchTime.getTime() + maxRunningTimeInMills;
                        if (new Date().getTime() > maxTime) {
                            // been running too long
                            runningResources.add(name);
                        } else {
                            passedInstanceNames.add(name);
                        }
                    } else {
                        passedInstanceNames.add(name);
                    }
                }
            }
        }

        // Print the JUnit XML Format
        if (jUnitFormatReportPath != null) {
            String xmlReport = outputJunitReportFormat(passedInstanceNames, runningResources);
            StringBuffer sbOut = new StringBuffer(System.lineSeparator());
            sbOut.append("JUNitXMLOutStart");
            sbOut.append(System.lineSeparator());
            sbOut.append(xmlReport);
            sbOut.append(System.lineSeparator());
            sbOut.append("JUNitXMLOutEnd");
            System.out.println(sbOut.toString());

            try {
                // if the directory does not exist, create it
                File reportDir = new File(jUnitFormatReportPath);
                if (!reportDir.exists()) reportDir.mkdir();
                FileUtils.writeStringToFile(new File(jUnitFormatReportPath + "AWSResourceMonitorReport.xml"), xmlReport);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // if there are instances that have been found to be running in excess of the allowed time,
        // throw an exception
        if (runningResources.size() > 0) {
            StringBuffer sb = new StringBuffer(System.lineSeparator());
            sb.append("ErrorMsgStart");
            sb.append(System.lineSeparator());
            sb.append("The following resources have been running longer than the allowed time: ");
            sb.append(System.lineSeparator());
            for (String name : runningResources)
                sb.append(name).append(System.lineSeparator());
            sb.append("Please stop, or terminate these resources.");
            sb.append(System.lineSeparator());
            sb.append("ErrorMsgEnd");
            sb.append(System.lineSeparator());
            throw new RuntimeException(sb.toString());
        }
    }

    /**
     * This method creates a String output in the format of JUnit Report XML. The format will be
     * similar to the following:
     * <?xml version="1.0" encoding="UTF-8"?>
     * <testsuite failures="4" tests="4">
     * <testcase name="passedInstanceName" classname="classname"/>
     * <testcase name="failedInstanceName" classname="classname">
     * <failure message="....."/>
     * </testcase>
     * </testsuite>
     *
     * @param passedInstanceNames
     * @param failedInstanceNames
     */
    private static String outputJunitReportFormat(List<String> passedInstanceNames, List<String> failedInstanceNames) {
        String xml = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = factory.newDocumentBuilder();

            // root elements
            Document doc = docBuilder.newDocument();
            Element testsuite = doc.createElement("testsuite");
            doc.appendChild(testsuite);
            testsuite.setAttribute("failures", "" + failedInstanceNames.size());
            testsuite.setAttribute("tests", "" + (failedInstanceNames.size() + passedInstanceNames.size()));

            for (String pass : passedInstanceNames) {
                Element testcase = doc.createElement("testcase");
                testsuite.appendChild(testcase);
                testcase.setAttribute("name", pass);
                testcase.setAttribute("classname", AWSResourceMonitor.class.getName());
            }

            for (String fail : failedInstanceNames) {
                Element testcase = doc.createElement("testcase");
                testsuite.appendChild(testcase);
                testcase.setAttribute("name", fail);
                testcase.setAttribute("classname", AWSResourceMonitor.class.getName());

                Element failure = doc.createElement("failure");
                testcase.appendChild(failure);
                failure.setAttribute("message", String.format("Instance '%s', has been running longer than the allowable time.", fail));
            }

            // write the content to a String
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            transformer.transform(source, result);

            xml = writer.toString();
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
        }
        return xml;
    }
}