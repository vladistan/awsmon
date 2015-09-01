package com.fourvinternal;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class monitors the AWS resources for instances that match a specific pattern, and has been running for longer
 * than the allowable time. This class assumes that the following environment variables are set:
 *    AWS_ACCESS_KEY_ID - The access key for AWS
 *    AWS_SECRET_KEY - The secret key for AWS
 *    InstanceNamePattern - The regular expression pattern to match the instance name to
 *    MaxRunningTimeInHours - The maximum amount of time in hours.
 */
public class AWSResourceMonitor {
    public static void main(String args[]) {
        String namePattern = "(.*)DevCloud";
        int maxRunningTimeInMills = 12 * 60 * 60 * 1000;
        if (System.getenv("InstanceNamePattern") != null)
            namePattern = System.getenv("InstanceNamePattern");
        String maxRunningTimeInHours = System.getenv("MaxRunningTimeInHours");
        if (maxRunningTimeInHours != null)
            maxRunningTimeInMills = Integer.parseInt(maxRunningTimeInHours) * 60 * 60 *1000;

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
        List<String> values = new ArrayList<String>();
        values.add("running");
        Filter filter1 = new Filter("instance-state-name", values);
        DescribeInstancesResult result = ec2.describeInstances(request.withFilters(filter1));
        List<Reservation> reservations = result.getReservations();

        // loop through each running resource
        List<String> runningResources = new ArrayList<String>();
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
                    Date launchTime = instance.getLaunchTime();
                    System.out.println("*** Running Instances: " + name + " --- " + instance.getInstanceId() +
                            " - " + instance.getState().toString() +
                            " - " + instance.getLaunchTime().toString());

                    // check the time active...
                    long maxTime = launchTime.getTime() + maxRunningTimeInMills;
                    if (new Date().getTime() > maxTime) {
                        // been running too long
                        System.out.println("*** " + name + " has been running too long....");
                        runningResources.add(name);
                    }
                }
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
}
