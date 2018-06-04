/**
 * Copyright 2017 Vlad Korolev
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 **/


package org.vlad.awsresourcemonitor;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Collection class for EC2 instances.
 */
public class RDSInstanceCollection {

  private final AmazonRDS rds;
  private List<InstanceData> objList;

  public RDSInstanceCollection(AmazonRDS rds) {
    this.rds = rds;

    this.objList = new ArrayList<>(1000);

    getAllInstances();

  }

  public List<InstanceData> getObjList() {
    return objList;
  }


  /**
   * Get all EC2 instances in all regions
   *
   */
  public final void getAllInstances() {
    // Find all running EC2 instances that match the regular expression

    final Set<String> skipRegions = new HashSet<>();
    skipRegions.add("us-gov-west-1");
    skipRegions.add("cn-north-1");

    for (final Regions reg : Regions.values()) {
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
  public final void collectRegionInstances(Regions region) {

    rds.setRegion(Region.getRegion(region));
    // Collect a list of running instances
    final DescribeDBInstancesRequest request = new DescribeDBInstancesRequest();
    final DescribeDBInstancesResult result = rds.describeDBInstances(request);


    // loop through each running resource
    for (DBInstance instance : result.getDBInstances()) {
      final InstanceData data = new InstanceData(instance);
      data.setRegion(region.getName());
      objList.add(data);
    }
  }


}
