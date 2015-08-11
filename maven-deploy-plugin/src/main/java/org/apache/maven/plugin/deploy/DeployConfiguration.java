package org.apache.maven.plugin.deploy;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.List;
import javax.annotation.Nullable;

/**
 * Contains the configuration for this plugin.
 */
public class DeployConfiguration
{
  public static final DeployConfiguration DEFAULT = new DeployConfiguration( false, null, null, null, 1, false, false );

  @SuppressWarnings( "unchecked" )
  static DeployConfiguration from( MavenProject project )
  {
    DeployConfiguration config = DeployConfiguration.DEFAULT;

    for ( Plugin p : (List<Plugin>) project.getBuildPlugins() )
    {
      if ( "org.apache.maven.plugins:maven-deploy-plugin".equals( p.getKey() ) )
      {
        Object cfg = p.getConfiguration();
        if ( cfg != null )
        {
          config = DeployConfiguration.from( cfg, config );
        }
        for ( PluginExecution e : p.getExecutions() )
        {
          if ( "default-install".equals( e.getId() ) )
          {
            config = DeployConfiguration.from( e.getConfiguration(), config );
            break;
          }
        }
        break;
      }
    }
    return config;
  }

  static DeployConfiguration from( Object configurationObject, DeployConfiguration previous )
  {
    if ( configurationObject instanceof Xpp3Dom )
    {
      return from( (Xpp3Dom) configurationObject, previous );
    }
    throw new IllegalStateException( "Unknown configuration type : " + configurationObject.getClass().getName() );
  }

  private static DeployConfiguration from( Xpp3Dom cfg, DeployConfiguration previous )
  {
    Boolean deployAtEnd = readBooleanValue( cfg, "deployAtEnd" );
    String altDeploymentRepository = readStringValue( cfg, "altDeploymentRepository" );
    String altReleaseDeploymentRepository = readStringValue( cfg, "altReleaseDeploymentRepository" );
    String altSnapshotDeploymentRepository = readStringValue( cfg, "altSnapshotDeploymentRepository" );
    Integer retryFailedDeploymentCount = readIntegerValue( cfg, "retryFailedDeploymentCount" );

    Boolean updateReleaseInfo = readBooleanValue( cfg, "updateReleaseInfo" );
    Boolean skip = readBooleanValue( cfg, "skip" );
    return new DeployConfiguration( previous,
                                    deployAtEnd,
                                    altDeploymentRepository,
                                    altReleaseDeploymentRepository,
                                    altSnapshotDeploymentRepository,
                                    retryFailedDeploymentCount,
                                    updateReleaseInfo,
                                    skip );
  }

  @Nullable
  private static Boolean readBooleanValue( Xpp3Dom element, String name )
  {
    Xpp3Dom[] children = element.getChildren( name );
    return ( children.length > 0 ) ? Boolean.parseBoolean( children[0].getValue() ) : null;
  }

  @Nullable
  private static Integer readIntegerValue( Xpp3Dom element, String name )
  {
    Xpp3Dom[] children = element.getChildren( name );
    return ( children.length > 0 ) ? Integer.parseInt( children[0].getValue() ) : null;
  }

  @Nullable
  private static String readStringValue( Xpp3Dom element, String name )
  {
    Xpp3Dom[] children = element.getChildren( name );
    return ( children.length > 0 ) ? children[0].getValue() : null;
  }

  @Nullable
  private final DeployConfiguration previous;
  @Nullable
  private final Boolean deployAtEnd;
  @Nullable
  private final String altDeploymentRepository;
  @Nullable
  private final String altReleaseDeploymentRepository;
  @Nullable
  private final String altSnapshotDeploymentRepository;
  @Nullable
  private final Integer retryFailedDeploymentCount;
  @Nullable
  private final Boolean updateReleaseInfo;
  @Nullable
  private final Boolean skip;

  private DeployConfiguration( boolean deployAtEnd,
                               @Nullable String altDeploymentRepository,
                               @Nullable String altReleaseDeploymentRepository,
                               @Nullable String altSnapshotDeploymentRepository,
                               int retryFailedDeploymentCount,
                               boolean updateReleaseInfo,
                               boolean skip )
  {
    this.previous = null;
    this.deployAtEnd = deployAtEnd;
    this.altDeploymentRepository = altDeploymentRepository;
    this.altReleaseDeploymentRepository = altReleaseDeploymentRepository;
    this.altSnapshotDeploymentRepository = altSnapshotDeploymentRepository;
    this.retryFailedDeploymentCount = retryFailedDeploymentCount;
    this.updateReleaseInfo = updateReleaseInfo;
    this.skip = skip;
  }

  private DeployConfiguration( DeployConfiguration previous,
                               @Nullable Boolean deployAtEnd,
                               @Nullable String altDeploymentRepository,
                               @Nullable String altReleaseDeploymentRepository,
                               @Nullable String altSnapshotDeploymentRepository,
                               @Nullable Integer retryFailedDeploymentCount,
                               @Nullable Boolean updateReleaseInfo,
                               @Nullable Boolean skip )
  {
    this.previous = previous;
    this.deployAtEnd = deployAtEnd;
    this.altDeploymentRepository = altDeploymentRepository;
    this.altReleaseDeploymentRepository = altReleaseDeploymentRepository;
    this.altSnapshotDeploymentRepository = altSnapshotDeploymentRepository;
    this.retryFailedDeploymentCount = retryFailedDeploymentCount;
    this.updateReleaseInfo = updateReleaseInfo;
    this.skip = skip;
  }

  public boolean deployAtEnd()
  {
    return deployAtEnd != null ? deployAtEnd : previous.deployAtEnd();
  }

  @Nullable
  public String altDeploymentRepository()
  {
    return altDeploymentRepository != null ? altDeploymentRepository
                                           : previous != null ? previous.altDeploymentRepository() : null;
  }

  @Nullable
  public String altReleaseDeploymentRepository()
  {
    return altReleaseDeploymentRepository != null ? altReleaseDeploymentRepository
                                                  : previous != null ? previous.altReleaseDeploymentRepository()
                                                                     : null;
  }

  @Nullable
  public String altSnapshotDeploymentRepository()
  {
    return altSnapshotDeploymentRepository != null ? altSnapshotDeploymentRepository
                                                   : previous != null ? previous.altSnapshotDeploymentRepository()
                                                                      : null;
  }

  public int retryFailedDeploymentCount()
  {
    return retryFailedDeploymentCount != null ? retryFailedDeploymentCount : previous.retryFailedDeploymentCount();
  }

  public boolean updateReleaseInfo()
  {
    return updateReleaseInfo != null ? updateReleaseInfo : previous.updateReleaseInfo();
  }

  public boolean skip()
  {
    return skip != null ? skip : previous.skip();
  }
}
