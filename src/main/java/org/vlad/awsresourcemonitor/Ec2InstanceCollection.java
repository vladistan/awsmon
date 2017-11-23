/**
 *
 * Copyright 2017 Vlad Korolev
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/


package org.vlad.awsresourcemonitor;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Collection class for EC2 instances.
 */
public class Ec2InstanceCollection {

  private final AmazonEC2 ec2;
  private List<InstanceData> objList;

  public Ec2InstanceCollection(AmazonEC2 ec2) {

    this.ec2 = ec2;
    this.objList = new ArrayList<>(1000);

    getAllInstances();

  }

  public List<InstanceData> getObjList() {
    return objList;
  }


  /**
   * Get all instances in US_EAST_1 and US_WEST_1.
   *
   */
  public void getAllInstances() {
    // Find all running EC2 instances that match the regular expression

    Set<String> skipRegions = new HashSet<>();
    skipRegions.add("us-gov-west-1");
    skipRegions.add("cn-north-1");

    for (Regions reg : Regions.values()) {
      String regName = reg.getName();
      if (skipRegions.contains(regName)) {
        continue;
      }

      collectRegionInstances(reg);
    }

  }


  /**
   * Get all instances in the region.
   *
   * @param region   region
   */
  public void collectRegionInstances(Regions region) {
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
        objList.add(data);
      }
    }
  }


}
