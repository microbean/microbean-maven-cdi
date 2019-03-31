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

import java.io.IOException;
import java.io.InputStream;
import java.io.File;

import java.lang.annotation.Annotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Alternative;

import javax.inject.Inject;

import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.EnvarBasedValueSource;

import org.codehaus.plexus.util.xml.Xpp3Dom;

import org.apache.maven.building.FileSource;
import org.apache.maven.building.Source;

import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.TrackableBase;

import org.apache.maven.settings.building.DefaultSettingsBuilder; // for javadoc only
import org.apache.maven.settings.building.DefaultSettingsProblem;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.building.SettingsProblemCollector;

import org.apache.maven.settings.io.SettingsParseException;

import org.apache.maven.settings.merge.MavenSettingsMerger;

import org.apache.maven.settings.validation.SettingsValidator;
import org.apache.maven.settings.validation.DefaultSettingsValidator;

import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;

import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.constructor.SafeConstructor.ConstructYamlOmap;

import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.error.YAMLException;

import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;

import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;

import org.yaml.snakeyaml.reader.ReaderException;

/**
 * A {@link SettingsBuilder} implementation that behaves like a {@link
 * DefaultSettingsBuilder} implementation but without needlessly
 * requiring round-trip serialization and deserialization of the
 * underlying settings, and that reads YAML files instead of XML
 * files.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see #build(SettingsBuildingRequest)
 *
 * @see DefaultSettingsBuilder
 */
@Alternative
@ApplicationScoped
public class YamlSettingsBuilder implements SettingsBuilder {


  /*
   * Instance fields.
   */

  
  private final SettingsValidator settingsValidator;

  private final MavenSettingsMerger merger;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link YamlSettingsBuilder} with a {@link
   * DefaultSettingsValidator} and a {@link MavenSettingsMerger}.
   *
   * @see #YamlSettingsBuilder(SettingsValidator, MavenSettingsMerger)
   */
  public YamlSettingsBuilder() {
    this(new DefaultSettingsValidator(), new MavenSettingsMerger());
  }

  /**
   * Creates a new {@link YamlSettingsBuilder}.
   *
   * @param settingsValidator a {@link SettingsValidator}
   * implementation that will validate the {@link Settings} object
   * once it has been successfully deserialized; if {@code null} then
   * a {@link DefaultSettingsValidator} implementation will be used
   * instead
   *
   * @param merger a {@link MavenSettingsMerger} that will be used to
   * merge global and user-specific {@link Settings} objects; if
   * {@code null}, then a new {@link MavenSettingsMerger} will be used
   * instead
   */
  @Inject
  public YamlSettingsBuilder(final SettingsValidator settingsValidator,
                             final MavenSettingsMerger merger) {
    super();
    if (settingsValidator == null) {
      this.settingsValidator = new DefaultSettingsValidator();
    } else {
      this.settingsValidator = settingsValidator;
    }
    if (merger == null) {
      this.merger = new MavenSettingsMerger();
    } else {
      this.merger = merger;
    }
  }


  /*
   * Instance methods.
   */


  /**
   * Deserializes a {@link Settings} from a stream-based source
   * represented by the supplied {@link SettingsBuildingRequest} and
   * returns it wrapped by a {@link SettingsBuildingResult} in much
   * the same manner as the {@link
   * DefaultSettingsBuilder#build(SettingsBuildingRequest)} method,
   * but using YAML instead of XML, and performing interpolation in a
   * more efficient manner.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * <p>Overrides of this method must not return {@code null}.</p>
   *
   * @param request the {@link SettingsBuildingRequest} representing
   * the settings location; must not be {@code null}
   *
   * @return a non-{@code null} {@link SettingsBuildingResult}
   *
   * @see #read(Source, boolean, Interpolator,
   * SettingsProblemCollector)
   *
   * @exception NullPointerException if {@code request} is {@code null}
   *
   * @exception SettingsBuildingException if an error occurred, but
   * also see the return value of the {@link
   * SettingsBuildingResult#getProblems()} method
   */
  @Override
  public SettingsBuildingResult build(final SettingsBuildingRequest request) throws SettingsBuildingException {
    Objects.requireNonNull(request);
    final List<SettingsProblem> problems = new ArrayList<>();

    Source globalSettingsSource = request.getGlobalSettingsSource();
    if (globalSettingsSource == null) {
      final File file = request.getGlobalSettingsFile();
      if (file != null && file.exists()) {
        globalSettingsSource = new FileSource(file);
      }
    }
    final Settings globalSettings;
    if (globalSettingsSource == null) {
      globalSettings = null;
    } else {
      globalSettings = this.readSettings(globalSettingsSource, request, problems);
    }

    Source userSettingsSource = request.getUserSettingsSource();
    if (userSettingsSource == null) {
      final File file = request.getUserSettingsFile();
      if (file != null && file.exists()) {
        userSettingsSource = new FileSource(file);
      }
    }
    Settings settings = null;
    if (userSettingsSource != null) {
      settings = this.readSettings(userSettingsSource, request, problems);
    }
    
    if (settings == null) {
      if (globalSettings != null) {
        settings = globalSettings;
      }
    } else if (globalSettings != null) {
      this.merger.merge(settings, globalSettings, TrackableBase.GLOBAL_LEVEL);
    }

    if (hasErrors(problems)) {
      throw new SettingsBuildingException(problems);
    }

    final SettingsBuildingResult returnValue = new DefaultSettingsBuildingResult(settings, problems);
    return returnValue;
  }

  /**
   * Given a {@link Source}, reads settings information from it and
   * creates a new {@link Settings} object and returns it.
   *
   * <p>This method may return {@code null}.</p>
   *
   * <p>Overrides of this method are permitted to return {@code null}.</p>
   *
   * @param source the {@link Source} representing the location of the
   * settings; may be {@code null} in which case {@code null} will be
   * returned
   *
   * @param strict whether or not strictness should be in effect
   *
   * @param interpolator an {@link Interpolator} for interpolating
   * values within the settings information; may be {@code null}
   *
   * @param problemCollector a {@link SettingsProblemCollector} to
   * accumulate error information; must not be {@code null}
   *
   * @return a {@link Settings}, or {@code null}
   *
   * @exception NullPointerException if {@code problemCollector} is
   * {@code null}
   *
   * @exception IOException if an input or output error occurred
   *
   * @exception SettingsParseException if the settings information
   * could not be parsed as a YAML 1.1 document
   */
  public Settings read(final Source source,
                       final boolean strict,
                       final Interpolator interpolator,
                       final SettingsProblemCollector problemCollector)
    throws IOException, SettingsParseException {
    Objects.requireNonNull(problemCollector);
    Settings returnValue = null;
    if (source != null) {
      try (final InputStream stream = source.getInputStream()) {
        if (stream != null) {
          final Constructor constructor = new InterpolatingConstructor(interpolator, problemCollector);
          final Yaml yaml = new Yaml(constructor);
          yaml.addTypeDescription(new TypeDescription(Server.class) {

              @Override
              public final boolean setupPropertyType(final String key, final Node valueNode) {
                final boolean returnValue;
                if ("configuration".equals(key) && valueNode != null) {
                  valueNode.setType(Xpp3Dom.class);
                  returnValue = true;
                } else {
                  returnValue = super.setupPropertyType(key, valueNode);
                }
                return returnValue;
              }

              @Override
              public final Object newInstance(final String propertyName, final Node node) {
                final Object returnValue;
                if ("configuration".equals(propertyName) && (node instanceof MappingNode || node instanceof SequenceNode)) {
                  Xpp3Dom dom = new Xpp3Dom("configuration");
                  returnValue = dom;
                  final Object contents = new HackyConstructor().construct(node);
                  if (contents instanceof Map) {
                    handleConfigurationMap(interpolator, problemCollector, dom, (Map<?, ?>)contents);
                  } else if (contents instanceof Collection) {
                    handleConfigurationCollection(interpolator, problemCollector, dom, (Collection<?>)contents);
                  } else {
                    throw new MarkedYAMLException("after deserializing configuration",
                                                  node.getStartMark(),
                                                  "found invalid scalar configuration: " + contents,
                                                  node.getStartMark()) {
                      private static final long serialVersionUID = 1L;
                    };
                  }
                } else {
                  returnValue = super.newInstance(propertyName, node);
                }
                return returnValue;
              }
            });
          returnValue = yaml.loadAs(stream, Settings.class);
        }
      } catch (final MarkedYAMLException e) {
        final Mark mark = e.getProblemMark();
        final int line;
        final int column;
        if (mark == null) {
          line = -1;
          column = -1;
        } else {
          line = mark.getLine() + 1;
          column = mark.getColumn() + 1;
        }
        throw new SettingsParseException(e.getMessage(), line, column, e);
      } catch (final ReaderException e) {
        throw new SettingsParseException(e.getMessage(), -1, e.getPosition() + 1, e);
      } catch (final YAMLException e) {
        throw new SettingsParseException(e.getMessage(), -1, -1, e);
      }
    }
    return returnValue;
  }

  private static final void handleConfigurationScalar(final Interpolator interpolator,
                                                      final SettingsProblemCollector problemCollector,
                                                      final Xpp3Dom element,
                                                      final Object scalarItem) {
    Objects.requireNonNull(element);
    Objects.requireNonNull(problemCollector);
    if (scalarItem != null) {
      assert !(scalarItem instanceof Collection);
      assert !(scalarItem instanceof Map);
      String value = scalarItem.toString();
      if (interpolator != null) {
        try {
          value = interpolator.interpolate(value, "settings");
        } catch (final InterpolationException interpolationException) {
          problemCollector.add(SettingsProblem.Severity.ERROR,
                               "Failed to interpolate settings: " +
                               interpolationException.getMessage(),
                               -1,
                               -1,
                               interpolationException);
        }
      }
      element.setValue(value);
    }
  }
    
  private static final void handleConfigurationCollection(final Interpolator interpolator,
                                                          final SettingsProblemCollector problemCollector,
                                                          final Xpp3Dom rootElement,
                                                          final Collection<?> items) {
    Objects.requireNonNull(rootElement);
    if (items != null && !items.isEmpty()) {
      for (final Object item : items) {
        if (item instanceof Map) {
          handleConfigurationMap(interpolator, problemCollector, rootElement, (Map<?, ?>)item);
        } else if (item instanceof Collection) {
          throw new YAMLException("Invalid configuration element (after deserialization): " + item);
        } else {
          handleConfigurationScalar(interpolator, problemCollector, rootElement, item);
        }
      }
    }
  }

  private static final void handleConfigurationMap(final Interpolator interpolator,
                                                   final SettingsProblemCollector problemCollector,
                                                   final Xpp3Dom rootElement,
                                                   final Map<?, ?> items) {
    Objects.requireNonNull(rootElement);
    if (items != null && !items.isEmpty()) {
      final Set<? extends Entry<?, ?>> entrySet = items.entrySet();
      if (entrySet != null && !entrySet.isEmpty()) {
        for (final Entry<?, ?> entry : entrySet) {
          if (entry != null) {
            final Object key = entry.getKey();
            final Xpp3Dom element = new Xpp3Dom(String.valueOf(key));
            element.setParent(rootElement);
            rootElement.addChild(element);
            final Object value = entry.getValue();
            if (value != null) {
              if (value instanceof Collection) {
                handleConfigurationCollection(interpolator, problemCollector, element, (Collection<?>)value);
              } else if (value instanceof Map) {
                handleConfigurationMap(interpolator, problemCollector, element, (Map<?, ?>)value); // RECURSIVE
              } else {
                handleConfigurationScalar(interpolator, problemCollector, element, value);
              }
            }
          }
        }
      }
    }
  }

  private final Settings readSettings(final Source source,
                                      final SettingsBuildingRequest request,
                                      final Collection<SettingsProblem> problems) {
    Objects.requireNonNull(problems);
    Settings returnValue = null;
    if (source != null) {
      SettingsProblemCollector collector =
        (severity, message, line, column, cause) ->
        add(problems, severity, message, source.getLocation(), line, column, cause);
      final Interpolator interpolator = new RegexBasedInterpolator();
      interpolator.addValueSource(new PropertiesBasedValueSource(request.getUserProperties()));
      interpolator.addValueSource(new PropertiesBasedValueSource(request.getSystemProperties()));
      try {
        interpolator.addValueSource(new EnvarBasedValueSource());
      } catch (final IOException ioException) {
        collector.add(SettingsProblem.Severity.WARNING,
                      "Failed to use environment variables for interpolation: " +
                      ioException.getMessage(),
                      -1,
                      -1,
                      ioException);
      }
      try {
        try {
          returnValue = this.read(source, true, interpolator, collector);
          this.settingsValidator.validate(returnValue, collector);
        } catch (final SettingsParseException strictParsingFailed) {
          returnValue = this.read(source, false, interpolator, collector);
          // Record the warning only if lenient reading succeeded.
          collector.add(SettingsProblem.Severity.WARNING,
                        strictParsingFailed.getMessage(),
                        strictParsingFailed.getLineNumber(),
                        strictParsingFailed.getColumnNumber(),
                        strictParsingFailed);
          this.settingsValidator.validate(returnValue, collector);
        }
      } catch (final SettingsParseException lenientParsingFailed) {
        collector.add(SettingsProblem.Severity.FATAL,
                      "Non-parseable settings " +
                      source.getLocation() +
                      ": " +
                      lenientParsingFailed.getMessage(),
                      lenientParsingFailed.getLineNumber(),
                      lenientParsingFailed.getColumnNumber(),
                      lenientParsingFailed);
      } catch (final IOException ioException) {
        collector.add(SettingsProblem.Severity.FATAL,
                      "Non-readable settings " +
                      source.getLocation() +
                      ": " +
                      ioException.getMessage(),
                      -1,
                      -1,
                      ioException);
      }
    }
    return returnValue;
  }


  /*
   * Static methods.
   */

  
  private static final boolean hasErrors(Collection<? extends SettingsProblem> problems) {
    boolean returnValue = false;
    if (problems != null && !problems.isEmpty()) {
      for (final SettingsProblem problem : problems) {
        if (problem != null && SettingsProblem.Severity.ERROR.compareTo(problem.getSeverity()) >= 0) {
          returnValue = true;
          break;
        }
      }
    }
    return returnValue;
  }
  
  private static final void add(final Collection<SettingsProblem> problems,
                                final SettingsProblem.Severity severity,
                                final String message,
                                final String source,
                                int line,
                                int column,
                                final Exception cause) {
    Objects.requireNonNull(problems);
    if (cause instanceof SettingsParseException && line <= 0 && column <= 0) {
      final SettingsParseException e = (SettingsParseException)cause;
      line = e.getLineNumber();
      column = e.getColumnNumber();
    }
    problems.add(new DefaultSettingsProblem(message, severity, source, line, column, cause));
  }


  /*
   * Inner and nested calsses.
   */

  
  private static final class DefaultSettingsBuildingResult implements SettingsBuildingResult {

    private final Settings settings;

    private final List<SettingsProblem> problems;
    
    private DefaultSettingsBuildingResult(final Settings settings, final List<SettingsProblem> problems) {
      super();
      if (settings == null) {
        this.settings = new Settings();
      } else {
        this.settings = settings;
      }
      if (problems == null) {
        this.problems = Collections.emptyList();
      } else {
        this.problems = Collections.unmodifiableList(problems);
      }
    }

    @Override
    public final Settings getEffectiveSettings() {
      return this.settings;
    }

    @Override
    public final List<SettingsProblem> getProblems() {
      return this.problems;
    }
    
  }

  private static final class InterpolatingConstructor extends Constructor {

    private final Interpolator interpolator;

    private final SettingsProblemCollector problemCollector;
    
    private InterpolatingConstructor(final Interpolator interpolator,
                                     final SettingsProblemCollector problemCollector) {
      super(Settings.class);
      this.interpolator = interpolator;
      if (interpolator == null) {
        this.problemCollector = problemCollector;
      } else {
        this.problemCollector = Objects.requireNonNull(problemCollector);
      }
    }
    
    @Override
    protected final String constructScalar(final ScalarNode node) {
      String returnValue = super.constructScalar(node);
      if (this.interpolator != null && returnValue != null) {
        try {
          returnValue = this.interpolator.interpolate(returnValue, "settings");
        } catch (final InterpolationException interpolationException) {
          assert this.problemCollector != null;
          this.problemCollector.add(SettingsProblem.Severity.ERROR,
                                    "Failed to interpolate settings: " +
                                    interpolationException.getMessage(),
                                    -1,
                                    -1,
                                    interpolationException);
        }
      }
      return returnValue;
    }

  }

  private static final class HackyConstructor extends SafeConstructor {

    private HackyConstructor() {
      super();
    }

    private final Object construct(final Node node) {
      return this.yamlConstructors.get(node.getTag()).construct(node);
    }
    
  }

}
