package org.apache.maven.plugin.install;

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
public class InstallConfiguration
{
  public static final InstallConfiguration DEFAULT = new InstallConfiguration( false, false, false, false );

  @SuppressWarnings( "unchecked" )
  static InstallConfiguration from( MavenProject project )
  {
    InstallConfiguration config = InstallConfiguration.DEFAULT;

    for ( Plugin p : (List<Plugin>) project.getBuildPlugins() )
    {
      if ( "org.apache.maven.plugins:maven-install-plugin".equals( p.getKey() ) )
      {
        Object cfg = p.getConfiguration();
        if ( cfg != null )
        {
          config = InstallConfiguration.from( cfg, config );
        }
        for ( PluginExecution e : p.getExecutions() )
        {
          if ( "default-install".equals( e.getId() ) )
          {
            config = InstallConfiguration.from( e.getConfiguration(), config );
            break;
          }
        }
        break;
      }
    }
    return config;
  }

  static InstallConfiguration from( Object configurationObject, InstallConfiguration previous )
  {
    if ( configurationObject instanceof Xpp3Dom )
    {
      return from( (Xpp3Dom) configurationObject, previous );
    }
    throw new IllegalStateException( "Unknown configuration type : " + configurationObject.getClass().getName() );
  }

  private static InstallConfiguration from( Xpp3Dom cfg, InstallConfiguration previous )
  {
    Boolean installAtEnd = readBooleanValue( cfg, "installAtEnd" );
    Boolean createChecksum = readBooleanValue( cfg, "createChecksum" );
    Boolean updateReleaseInfo = readBooleanValue( cfg, "updateReleaseInfo" );
    Boolean skip = readBooleanValue( cfg, "skip" );
    return new InstallConfiguration( previous, installAtEnd, createChecksum, updateReleaseInfo, skip );
  }

  @Nullable
  private static Boolean readBooleanValue( Xpp3Dom element, String name )
  {
    Xpp3Dom[] children = element.getChildren( name );
    return ( children.length > 0 ) ? Boolean.parseBoolean( children[0].getValue() ) : null;
  }

  @Nullable
  private final InstallConfiguration previous;
  @Nullable
  private final Boolean installAtEnd;
  @Nullable
  private final Boolean createChecksum;
  @Nullable
  private final Boolean updateReleaseInfo;
  @Nullable
  private final Boolean skip;

  private InstallConfiguration( boolean installAtEnd, boolean createChecksum, boolean updateReleaseInfo, boolean skip )
  {
    this.previous = null;
    this.installAtEnd = installAtEnd;
    this.createChecksum = createChecksum;
    this.updateReleaseInfo = updateReleaseInfo;
    this.skip = skip;
  }

  private InstallConfiguration( InstallConfiguration previous,
                                @Nullable Boolean installAtEnd,
                                @Nullable Boolean createChecksum,
                                @Nullable Boolean updateReleaseInfo,
                                @Nullable Boolean skip )
  {
    this.previous = previous;
    this.installAtEnd = installAtEnd;
    this.createChecksum = createChecksum;
    this.updateReleaseInfo = updateReleaseInfo;
    this.skip = skip;
  }

  public boolean installAtEnd()
  {
    return installAtEnd != null ? installAtEnd : previous.installAtEnd();
  }

  public boolean createChecksum()
  {
    return createChecksum != null ? createChecksum : previous.createChecksum();
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
