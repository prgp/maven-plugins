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

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;

/**
 * Installs the project's main artifact, and any other artifacts attached by other plugins in the lifecycle, to the
 * local repository.
 * 
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 */
@Mojo( name = "install", defaultPhase = LifecyclePhase.INSTALL, threadSafe = true )
public class InstallMojo
    extends AbstractInstallMojo
{
    /**
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    @Parameter( defaultValue = "${reactorProjects}", required = true, readonly = true )
    private List<MavenProject> reactorProjects;

    /**
     * Whether every project should be installed during its own install-phase or at the end of the multimodule build. If
     * set to {@code true} and the build fails, none of the reactor projects is installed.
     * <strong>(experimental)</strong>
     * 
     * @since 2.5
     */
    @Parameter( defaultValue = "false", property = "installAtEnd" )
    private boolean installAtEnd;

    /**
     * @deprecated either use project.getPackaging() or reactorProjects.get(i).getPackaging()
     */
    @Parameter( defaultValue = "${project.packaging}", required = true, readonly = true )
    protected String packaging;

    /**
     * @deprecated either use project.getFile() or reactorProjects.get(i).getFile()
     */
    @Parameter( defaultValue = "${project.file}", required = true, readonly = true )
    private File pomFile;

    /**
     * Set this to <code>true</code> to bypass artifact installation. Use this for artifacts that does not need to be
     * installed in the local repository.
     * 
     * @since 2.4
     */
    @Parameter( property = "maven.install.skip", defaultValue = "false" )
    private boolean skip;

    /**
     * @deprecated either use project.getArtifact() or reactorProjects.get(i).getArtifact()
     */
    @Parameter( defaultValue = "${project.artifact}", required = true, readonly = true )
    private Artifact artifact;

    /**
     * @deprecated either use project.getAttachedArtifacts() or reactorProjects.get(i).getAttachedArtifacts()
     */
    @Parameter( defaultValue = "${project.attachedArtifacts}", required = true, readonly = true )
    private List<Artifact> attachedArtifacts;

    public void execute()
        throws MojoExecutionException
    {
        boolean addedInstallRequest = false;
        if ( skip )
        {
            getLog().info( "Skipping artifact installation" );
        }
        else
        {
            // CHECKSTYLE_OFF: LineLength
            InstallRequest currentExecutionInstallRequest =
                new InstallRequest().setProject( project ).setCreateChecksum( createChecksum ).setUpdateReleaseInfo( updateReleaseInfo );
            // CHECKSTYLE_ON: LineLength

            if ( !installAtEnd )
            {
                installProject( currentExecutionInstallRequest );
            }
            else
            {
                addedInstallRequest = true;
            }
        }

        if ( isLastProject() )
        {
            // NB the last project does not have to be deferred ...
            installDeferredProjects();
        }
        else if ( addedInstallRequest )
        {
            getLog().info( "Installing " + project.getGroupId() + ":" + project.getArtifactId() + ":"
                               + project.getVersion() + " at end" );
        }
    }

    private boolean isLastProject()
    {
        return project.equals( reactorProjects.get( reactorProjects.size() - 1 ) );
    }

    private void installDeferredProjects() throws MojoExecutionException
    {
        synchronized ( reactorProjects )
        {
            for ( MavenProject installProject : reactorProjects )
            {
                InstallConfiguration config = InstallConfiguration.from( installProject );

                if ( config.installAtEnd() && !config.skip() && !( installProject.equals( project ) && skip ) )
                {
                    getLog().info( "Installing " + project.getGroupId() + ":" + project.getArtifactId() + ":"
                                   + project.getVersion() + " now" );

                    installProject( new InstallRequest()
                                        .setProject( installProject )
                                        .setCreateChecksum( config.createChecksum() )
                                        .setUpdateReleaseInfo( config.updateReleaseInfo() ) );
                }
            }
        }
    }

    private void installProject( InstallRequest request )
        throws MojoExecutionException
    {
        MavenProject project = request.getProject();
        boolean createChecksum = request.isCreateChecksum();
        boolean updateReleaseInfo = request.isUpdateReleaseInfo();

        Artifact artifact = project.getArtifact();
        String packaging = project.getPackaging();
        File pomFile = project.getFile();
        @SuppressWarnings( "unchecked" )
        List<Artifact> attachedArtifacts = project.getAttachedArtifacts();

        // TODO: push into transformation
        boolean isPomArtifact = "pom".equals( packaging );

        ArtifactMetadata metadata;

        if ( updateReleaseInfo )
        {
            artifact.setRelease( true );
        }

        try
        {
            Collection<File> metadataFiles = new LinkedHashSet<File>();

            if ( isPomArtifact )
            {
                installer.install( pomFile, artifact, localRepository );
                installChecksums( artifact, createChecksum );
                addMetaDataFilesForArtifact( artifact, metadataFiles, createChecksum );
            }
            else
            {
                metadata = new ProjectArtifactMetadata( artifact, pomFile );
                artifact.addMetadata( metadata );

                File file = artifact.getFile();

                // Here, we have a temporary solution to MINSTALL-3 (isDirectory() is true if it went through compile
                // but not package). We are designing in a proper solution for Maven 2.1
                if ( file != null && file.isFile() )
                {
                    installer.install( file, artifact, localRepository );
                    installChecksums( artifact, createChecksum );
                    addMetaDataFilesForArtifact( artifact, metadataFiles, createChecksum );
                }
                else if ( !attachedArtifacts.isEmpty() )
                {
                    getLog().info( "No primary artifact to install, installing attached artifacts instead." );

                    Artifact pomArtifact =
                        artifactFactory.createProjectArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                                               artifact.getBaseVersion() );
                    pomArtifact.setFile( pomFile );
                    if ( updateReleaseInfo )
                    {
                        pomArtifact.setRelease( true );
                    }

                    installer.install( pomFile, pomArtifact, localRepository );
                    installChecksums( pomArtifact, createChecksum );
                    addMetaDataFilesForArtifact( pomArtifact, metadataFiles, createChecksum );
                }
                else
                {
                    // CHECKSTYLE_OFF: LineLength
                    throw new MojoExecutionException(
                                                      "The packaging for this project did not assign a file to the build artifact" );
                    // CHECKSTYLE_ON: LineLength
                }
            }

            for ( Artifact attached : attachedArtifacts )
            {
                installer.install( attached.getFile(), attached, localRepository );
                installChecksums( attached, createChecksum );
                addMetaDataFilesForArtifact( attached, metadataFiles, createChecksum );
            }

            installChecksums( metadataFiles );
        }
        catch ( ArtifactInstallationException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

    public void setSkip( boolean skip )
    {
        this.skip = skip;
    }

}
