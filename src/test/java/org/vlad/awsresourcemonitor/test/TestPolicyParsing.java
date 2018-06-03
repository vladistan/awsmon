/**
 *
 * Copyright 2017 Vlad Korolev
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/

package org.vlad.awsresourcemonitor.test;

import org.junit.Test;
import org.vlad.awsresourcemonitor.Policy;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class TestPolicyParsing {

  @Test
  public void weCanParseYamlPolicy() throws URISyntaxException, FileNotFoundException {

    File yamlFile = TestUtil.getTestResource("policy.yaml");
    Yaml yaml = new Yaml();
    Object obj = yaml.load(new FileReader(yamlFile));

    assertThat(obj).isInstanceOf(HashMap.class);

    HashMap<String, List<String>> map = (HashMap<String, List<String>>) obj;

    assertThat(map).containsKeys("Environment", "Project");

    List<String> projects = map.get("Project");

    assertThat(projects).contains("Zebra", "Build");

  }

  @Test
  public void weCanConstructPolicyObj() throws URISyntaxException, FileNotFoundException, ParseException {

    File yamlFile = TestUtil.getTestResource("policy.yaml");

    Policy.load( yamlFile );
    Policy pol = Policy.getInstance();

    assertThat(pol.getAllowedRegion()).contains("us-east-1");
    assertThat(pol.getAllowedRegion()).contains("us-west-2");

    assertThat(pol.getAllowedTags()).contains("Environment", "Puppet");
    assertThat(pol.getProjects()).contains("Zebra", "Build");
    assertThat(pol.getEnvironments()).contains("Common", "Prod");
    assertThat(pol.getChargeLines()).contains("InternalDev", "InternalQA");
    assertThat(pol.getLifecycle()).contains("Permanent", "Spot");
    assertThat(pol.getOwners()).contains("Vlad", "Greg");
    assertThat(pol.getServices()).contains("VPN", "MicroService");



  }


}
