package com.suse.maven.obs;


import org.apache.commons.lang3.NotImplementedException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Goal which touches a timestamp file.
 */
@Mojo( name = "download-dependencies", defaultPhase = LifecyclePhase.INITIALIZE )
public class DownloadDependenciesMojo extends AbstractMojo
{
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    public void execute() throws MojoExecutionException {
        throw new NotImplementedException();
    }
}
