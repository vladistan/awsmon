package org.vlad.awsresourcemonitor.test;


import com.amazonaws.services.ec2.model.Instance;
import org.junit.BeforeClass;
import org.junit.Test;
import org.vlad.awsresourcemonitor.InstanceData;
import org.vlad.awsresourcemonitor.Policy;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.text.ParseException;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRDSData {

  @BeforeClass
  public static void setUp() throws URISyntaxException, FileNotFoundException, ParseException {

    File yamlFile = TestUtil.getTestResource("policy.yaml");
    Policy.load( yamlFile );

  }

  @Test
  public void WhenStateIsRunningShouldSetRunningFlagToTrue() {

    Instance inst = TestUtil.getMockRDS("running", "RDS-01", 10);

    InstanceData iData = new InstanceData(inst);

    assertThat(iData.isRunning()).isTrue();

  }
}
