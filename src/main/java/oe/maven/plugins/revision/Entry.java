/*-
 * Copyright (c) 2010, Oleg Estekhin
 * All rights reserved.
 */

package oe.maven.plugins.revision;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;

/** Describes a file or a directory to be inspected. */
public class Entry {

    /** The entry local path. */
    private File path;

    /** The prefix for entry properties. */
    private String prefix;

    /** Specifies whether the status information should be collected recursively. */
    private boolean recursive = true;

    /** Specifies whether to report items that are not under version control. */
    private boolean reportUnversioned = true;

    /** Specifies whether to report items that were set to be ignored. */
    private boolean reportIgnored = false;

    /** Specifies whether to check the remote repository and report local out-of-date items. */
    private boolean reportOutOfDate = false;


    /**
     * Returns the entry local path.
     *
     * @return the local path
     */
    public File getPath() {
        return path;
    }

    /**
     * Returns the prefix for entry properties.
     *
     * @return the properties prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Specifies whether the status information should be collected recursively.
     *
     * @return {@code true} if status information should be collected and aggregated both for path entry and all entries
     *         below it; {@code false} is only the path entry should be inspected
     */
    public boolean isRecursive() {
        return recursive;
    }

    /**
     * Specifies whether to report items that are not under version control.
     *
     * @return {@code true} if unversioned items should be reported
     */
    public boolean reportUnversioned() {
        return reportUnversioned;
    }

    /**
     * Specifies whether to report items that were set to be ignored.
     *
     * @return {@code true} if ignored items should be reported
     */
    public boolean reportIgnored() {
        return reportIgnored;
    }

    /**
     * Specifies whether to check the remote repository and report local out-of-date items.
     *
     * @return {@code true} if out-of-date items should be reported
     */
    public boolean reportOutOfDate() {
        return reportOutOfDate;
    }


    /**
     * Validates the entry configuration.
     *
     * @throws MojoExecutionException if entry configuration is invalid
     */
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
