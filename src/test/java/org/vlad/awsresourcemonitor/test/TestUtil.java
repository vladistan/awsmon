/**
 *
 * Copyright 2017 Vlad Korolev
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/

package org.vlad.awsresourcemonitor.test;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.Tag;

import org.joda.time.DateTime;
import org.joda.time.Period;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUtil {

  public static File getTestResource(String fileName) throws URISyntaxException {
    URL url = Thread.currentThread().getContextClassLoader().getResource(fileName);
    URI uri = new URI(url.toString()); //ARGH: Stupid bug in URL handling

    return new File(uri.getPath());
  }

  public static Instance getMockInstance(String stateName) {

    Instance inst = mock(Instance.class);
    InstanceState state = mock(InstanceState.class);

    when(inst.getState()).thenReturn(state);
    when(state.getName()).thenReturn(stateName);
    when(inst.getInstanceId()).thenReturn("i-12345");
    return inst;
  }

  public static Instance getMockInstance(String stateName, String instanceName) {

    Instance inst = getMockInstance(stateName);
    List<Tag> tagList = new ArrayList<Tag>();

    Tag nameTag = getMockTag("Name", instanceName);
    tagList.add(nameTag);
    when(inst.getTags()).thenReturn(tagList);


    return inst;
  }

  public static Tag getMockTag(String key, String value) {
    Tag nameTag = mock(Tag.class);
    when(nameTag.getKey()).thenReturn(key);
    when(nameTag.getValue()).thenReturn(value);
    return nameTag;
  }

  public static void addInstanceTag(Instance instance, String key, String value) {
    Tag tag = getMockTag(key, value);
    instance.getTags().add(tag);
  }

  public static Instance getMockInstance(String stateName, String instanceName, int runningTime) {

    Instance inst = getMockInstance(stateName, instanceName);

    DateTime launchTime = new DateTime().minus(new Period(runningTime, 0, 0, 0));

    when(inst.getLaunchTime()).thenReturn(launchTime.toDate());

    return inst;
  }


  public static Instance getMockRDS(String stateName, String instanceName, int runningTime) {

    Instance inst = getMockInstance(stateName, instanceName);

    DateTime launchTime = new DateTime().minus(new Period(runningTime, 0, 0, 0));

    when(inst.getLaunchTime()).thenReturn(launchTime.toDate());

    return inst;
  }
}
