/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2017-2018 microBean.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.microbean.maven.cdi;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import org.apache.maven.settings.Settings;

import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingResult;

import org.apache.maven.settings.io.SettingsParseException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

public class TestYamlSettingsBuilder {

  public TestYamlSettingsBuilder() {
    super();
  }

  @Test
  public void testRead() throws IOException, SettingsBuildingException, SettingsParseException {
    final YamlSettingsBuilder builder = new YamlSettingsBuilder();
    final SettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
    request.setSystemProperties(System.getProperties());
    request.setUserSettingsFile(new File(Thread.currentThread().getContextClassLoader().getResource(this.getClass().getSimpleName() + "/settings.yaml").getPath()));
    final SettingsBuildingResult result = builder.build(request);
    assertNotNull(result);
    final Settings settings = result.getEffectiveSettings();
    assertNotNull(settings);
    assertEquals("fred", settings.getLocalRepository());
  }
  
}
