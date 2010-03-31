/*-
 * Copyright (c) 2010, Oleg Estekhin
 * All rights reserved.
 */

package oe.maven.plugins.revision;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;

/** todo write javadoc for Entry. */
public class Entry {

    private File path;

    private String prefix;

    private boolean recursive = true;

    private boolean reportUnversioned = false;

    private boolean reportIgnored = false;

    private boolean reportOutOfDate = false;


    public File getPath() {
        return path;
    }

    public String getPrefix() {
        return prefix;
    }

    public boolean isRecursive() {
        return recursive;
    }


    public boolean reportUnversioned() {
        return reportUnversioned;
    }

    public boolean reportIgnored() {
        return reportIgnored;
    }

    public boolean reportOutOfDate() {
        return reportOutOfDate;
    }


    public void validate() throws MojoExecutionException {
        if ( path == null ) {
            throw new MojoExecutionException( "entry path is not specified" );
        }
        if ( !path.exists() ) {
            throw new MojoExecutionException( "entry path does not exist: " + path );
        }
        if ( prefix == null ) {
            throw new MojoExecutionException( "entry prefix is not specified" );
        }
    }

}
