/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2017 MicroBean.
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
package org.microbean.maven.cdi.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

import org.eclipse.aether.RepositorySystem; // for javadoc only
import org.eclipse.aether.RepositorySystemSession; // for javadoc only

import org.eclipse.aether.deployment.DeployRequest; // for javadoc only

import org.eclipse.aether.resolution.DependencyRequest; // for javadoc only

/**
 * A {@link Qualifier} annotation that indicates that whatever it
 * annotates is affiliated somehow with {@linkplain
 * RepositorySystem#resolveDependencies(RepositorySystemSession,
 * DependencyRequest) artifact or dependency <em>resolution</em>}, as
 * opposed to, say, {@linkplain
 * RepositorySystem#deploy(RepositorySystemSession, DeployRequest)
 * artifact <em>deployment</em>}.
 *
 * @author <a href="http://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 */
@Documented
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE })
public @interface Resolution {

}
