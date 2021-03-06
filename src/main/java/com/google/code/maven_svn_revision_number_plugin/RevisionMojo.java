/*-
 * Copyright (c) 2009-2012, Oleg Estekhin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.google.code.maven_svn_revision_number_plugin;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver;
import org.tmatesoft.svn.core.wc2.SvnGetInfo;
import org.tmatesoft.svn.core.wc2.SvnGetStatus;
import org.tmatesoft.svn.core.wc2.SvnInfo;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnStatus;
import org.tmatesoft.svn.core.wc2.SvnTarget;

import static java.lang.String.format;

/**
 * Retrieves the revision number and the status of a file or a directory under the Subversion version control.
 *
 * @goal revision
 * @phase initialize
 * @threadSafe
 * @requiresProject
 */
public class RevisionMojo extends AbstractMojo {

    static {
        DAVRepositoryFactory.setup(); // http, https
        SVNRepositoryFactoryImpl.setup(); // svn, svn+xxx
        FSRepositoryFactory.setup(); // file
    }


    /**
     * The maven project.
     *
     * @parameter property="project"
     * @readonly
     */
    private MavenProject project;

    /**
     * Specifies the list of entries to inspect. Each entry has a separate configuration consisting of the local path,
     * report options and the prefix for the output properties.
     * <p/>
     * The following example shows the entry configured with the default properties:
     * <pre>
     * &lt;entries&gt;
     *   &lt;entry&gt;
     *     &lt;path&gt;${project.basedir}&lt;/path&gt;
     *     &lt;prefix&gt;${project.artifactId}&lt;/prefix&gt;
     *     &lt;depth&gt;infinity&lt;/depth&gt;
     *     &lt;reportUnversioned&gt;true&lt;/reportUnversioned&gt;
     *     &lt;reportIgnored&gt;false&lt;/reportIgnored&gt;
     *     &lt;reportOutOfDate&gt;false&lt;/reportOutOfDate&gt;
     *   &lt;/entry&gt;
     * &lt;/entries&gt;
     * </pre>
     * <p/>
     * If the entries configuration is not specified then the goal will operate on the default entry with the entry path
     * equal to the project basedir and the properties prefix equal to the project artifactId.
     *
     * @parameter
     */
    private Entry[] entries;

    /**
     * Specifies whether the goal runs in the verbose mode.
     *
     * @parameter property="svn-revision-number.verbose" default-value="false"
     */
    private boolean verbose;


    /**
     * Specifies whether the build should stop or continue if there are errors related to obtaining the svn revision.
     *
     * @parameter property="svn-revision-number.failOnError" default-value="true"
     */
    private boolean failOnError;


    public void execute() throws MojoExecutionException, MojoFailureException {
        if ( entries == null || entries.length == 0 ) {
            if ( getLog().isDebugEnabled() ) {
                getLog().debug( "configuration/entries section is not specified or is empty, defaulting to ${project.basedir}" );
            }
            entries = new Entry[] {
                    new Entry( project.getBasedir(), project.getArtifactId() ),
            };
        }

        SvnOperationFactory operationFactory = new SvnOperationFactory();
        try {
            for ( Entry entry : entries ) {
                if ( entry.getPath() == null ) {
                    entry.setPath( project.getBasedir() );
                }
                if ( entry.getPrefix() == null ) {
                    entry.setPrefix( project.getArtifactId() );
                }
                processEntry( operationFactory, entry );
            }
        } finally {
            operationFactory.dispose();
        }
    }


    private void processEntry( SvnOperationFactory operationFactory, Entry entry ) throws MojoExecutionException {
        if ( getLog().isInfoEnabled() ) {
            getLog().info( format( "inspecting %s %s", entry.getPath().isFile() ? "file" : entry.getPath().isDirectory() ? "directory" : "path", entry.getPath() ) );
        }
        logDebugInfo( format( "  prefix = %s", entry.getPrefix() ) );
        logDebugInfo( format( "  depth = %s", entry.getDepth() ) );
        logDebugInfo( format( "  report unversioned = %s", entry.reportUnversioned() ) );
        logDebugInfo( format( "  report ignored = %s", entry.reportIgnored() ) );
        logDebugInfo( format( "  report out-of-date = %s", entry.reportOutOfDate() ) );

        logDebugInfo( "calculating properties" );
        StatusHandler statusHandler = new StatusHandler( entry );
        try {
            logDebugInfo( format( "  wc format = %s", SvnOperationFactory.detectWcGeneration( entry.getPath(), true ) ) );
            fillStatus( entry, operationFactory, statusHandler );
            fillInfo( entry, operationFactory, statusHandler );
        } catch ( SVNException e ) {
            if ( e.getErrorMessage() != null && ( SVNErrorCode.WC_NOT_WORKING_COPY.equals( e.getErrorMessage().getErrorCode() ) || SVNErrorCode.WC_PATH_NOT_FOUND.equals( e.getErrorMessage().getErrorCode() ) ) ) {
                statusHandler.resetProperties( true );
            } else if ( failOnError ) {
                throw new MojoExecutionException( e.getMessage(), e );
            } else {
                if ( getLog().isErrorEnabled() ) {
                    getLog().error( e );
                }
                statusHandler.resetProperties();
            }
        }
        setProjectProperties( entry.getPrefix(), statusHandler.createProperties() );
    }


    private void fillStatus( Entry entry, SvnOperationFactory operationFactory, StatusHandler statusHandler ) throws SVNException {
        SvnGetStatus statusOperation = operationFactory.createGetStatus();
        statusOperation.setSingleTarget( SvnTarget.fromFile( entry.getPath() ) );
        statusOperation.setDepth( SVNDepth.fromString( entry.getDepth() ) );
        statusOperation.setRevision( SVNRevision.WORKING );
        statusOperation.setReportAll( true );
        statusOperation.setReportIgnored( entry.reportIgnored() );
        statusOperation.setRemote( entry.reportOutOfDate() );
        statusOperation.setReceiver( statusHandler );
        statusOperation.run();
    }

    private void fillInfo( Entry entry, SvnOperationFactory operationFactory, StatusHandler statusHandler ) throws SVNException {
        if ( statusHandler.repositoryPath == null || statusHandler.repositoryPath.length() == 0 ) {
            SvnGetInfo infoOperation = operationFactory.createGetInfo();
            infoOperation.setSingleTarget( SvnTarget.fromFile( entry.getPath() ) );
            SvnInfo infoResult = infoOperation.run();
            statusHandler.repositoryRoot = infoResult.getRepositoryRootUrl().toString();
            statusHandler.repositoryPath = infoResult.getUrl().toString().substring( statusHandler.repositoryRoot.length() + 1 );
        }
    }


    private void setProjectProperties( String prefix, Map<String, Object> properties ) {
        logDebugInfo( "setting properties" );
        for ( Map.Entry<String, Object> entryProperty : properties.entrySet() ) {
            setProjectProperty( prefix + '.' + entryProperty.getKey(), String.valueOf( entryProperty.getValue() ) );
        }
    }

    private void setProjectProperty( String name, String value ) {
        Properties projectProperties = project.getProperties();
        if ( projectProperties.getProperty( name ) != null ) {
            logDebugWarning( format( "the \"%s\" property is already defined and will be overwritten. The possible causes for this are:%n" +
                    "  - the plugin configuration contains two or more entries with the same prefix.%n" +
                    "  - the plugin runs multiple times with the same configuration.%n" +
                    "  - the property is already defined in the POM or by some other plugin.",
                    name ) );
        }
        projectProperties.setProperty( name, value );
        logDebugInfo( format( "  %s = %s", name, value ) );
    }


    private void logDebugInfo( CharSequence message ) {
        if ( verbose ) {
            if ( getLog().isInfoEnabled() ) {
                getLog().info( message );
            }
        } else {
            if ( getLog().isDebugEnabled() ) {
                getLog().debug( message );
            }
        }
    }

    private void logDebugWarning( CharSequence message ) {
        if ( verbose ) {
            if ( getLog().isWarnEnabled() ) {
                getLog().warn( message );
            }
        } else {
            if ( getLog().isDebugEnabled() ) {
                getLog().debug( message );
            }
        }
    }


    private final class StatusHandler implements ISvnObjectReceiver<SvnStatus> {

        private final Entry entry;

        private String repositoryRoot;

        private String repositoryPath;

        private long maximumRevision;

        private long minimumRevision;

        private long committedRevision;

        private Date committedDate;

        private Set<SVNStatusType> localStatusTypes;

        private boolean outOfDate;


        private StatusHandler( Entry entry ) {
            if ( entry == null ) {
                throw new IllegalArgumentException( "{entry} is null" );
            }
            this.entry = entry;
            resetProperties();
        }


        public void receive( SvnTarget target, SvnStatus status ) throws SVNException {
            if ( verbose && getLog().isDebugEnabled() ) {
                getLog().debug( format( "  %s%s%s %s%s%s  %6s %6s %6s  %s (%s %s)",
                        status.getNodeStatus().getCode(), status.getPropertiesStatus().getCode(), status.getTextStatus().getCode(),
                        status.getRepositoryNodeStatus().getCode(), status.getRepositoryPropertiesStatus().getCode(), status.getRepositoryTextStatus().getCode(),
                        status.getRevision(), status.getChangedRevision(), status.getRepositoryChangedRevision(),
                        target.getPathOrUrlString(),
                        status.getRepositoryRootUrl(), status.getRepositoryRelativePath()
                ) );
            }

            if ( repositoryRoot == null ) {
                repositoryRoot = status.getRepositoryRootUrl() == null ? "" : status.getRepositoryRootUrl().toString();
                repositoryPath = status.getRepositoryRelativePath();
            }

            long targetRevision = status.getRevision();
            if ( targetRevision >= 0L ) {
                if ( maximumRevision < targetRevision ) {
                    maximumRevision = targetRevision;
                }
                if ( targetRevision > 0L && minimumRevision > targetRevision ) {
                    minimumRevision = targetRevision;
                }
            }
            long targetCommittedRevision = status.getChangedRevision();
            if ( targetCommittedRevision >= 0L && committedRevision < targetCommittedRevision ) {
                committedRevision = targetCommittedRevision;
                committedDate = status.getChangedDate();
            }

            localStatusTypes.add( status.getNodeStatus() );
            if ( SVNStatusType.STATUS_NORMAL.equals( status.getNodeStatus() ) ) {
                localStatusTypes.add( status.getPropertiesStatus() );
            }

            if ( status.getRepositoryChangedRevision() > targetCommittedRevision ) {
                outOfDate = true;
            }
        }

        public void resetProperties() {
            resetProperties( false );
        }

        public void resetProperties( boolean forceUnversioned ) {
            repositoryRoot = null;
            repositoryPath = null;

            maximumRevision = Long.MIN_VALUE;
            minimumRevision = Long.MAX_VALUE;

            committedRevision = Long.MIN_VALUE;
            committedDate = null;

            localStatusTypes = new HashSet<SVNStatusType>();
            outOfDate = false;

            if ( forceUnversioned ) {
                localStatusTypes.add( SVNStatusType.STATUS_UNVERSIONED );
            }
        }

        public Map<String, Object> createProperties() {
            Map<String, Object> properties = new LinkedHashMap<String, Object>();
            properties.put( "repository", repositoryRoot == null ? "" : repositoryRoot );
            properties.put( "path", repositoryPath == null ? "" : repositoryPath );
            properties.put( "revision", maximumRevision == Long.MIN_VALUE ? -1L : maximumRevision );
            properties.put( "mixedRevisions", maximumRevision > 0L && minimumRevision > 0L && maximumRevision != minimumRevision );
            properties.put( "committedRevision", committedRevision == Long.MIN_VALUE ? -1L : committedRevision );
            properties.put( "committedDate", committedDate == null ? "" : format( Locale.ENGLISH, "%tF %<tT %<tz (%<ta, %<td %<tb %<tY)", committedDate ) );
            properties.put( "status", createStatusString( EntryStatusSymbols.DEFAULT ) );
            properties.put( "specialStatus", createStatusString( EntryStatusSymbols.SPECIAL ) );
            return properties;
        }

        private String createStatusString( EntryStatusSymbols symbols ) {
            StringBuilder status = new StringBuilder();

            Set<SVNStatusType> statusTypes = new HashSet<SVNStatusType>( localStatusTypes );
            statusTypes.remove( SVNStatusType.STATUS_NONE );
            statusTypes.remove( SVNStatusType.STATUS_NORMAL );
            if ( statusTypes.remove( SVNStatusType.STATUS_ADDED ) ) {
                status.append( symbols.getStatusSymbol( SVNStatusType.STATUS_ADDED ) );
            }
            if ( statusTypes.remove( SVNStatusType.STATUS_CONFLICTED ) ) {
                status.append( symbols.getStatusSymbol( SVNStatusType.STATUS_CONFLICTED ) );
            }
            if ( statusTypes.remove( SVNStatusType.STATUS_DELETED ) ) {
                status.append( symbols.getStatusSymbol( SVNStatusType.STATUS_DELETED ) );
            }
            if ( statusTypes.remove( SVNStatusType.STATUS_IGNORED ) && entry.reportIgnored() ) {
                status.append( symbols.getStatusSymbol( SVNStatusType.STATUS_IGNORED ) );
            }
            if ( statusTypes.remove( SVNStatusType.STATUS_MODIFIED ) ) {
                status.append( symbols.getStatusSymbol( SVNStatusType.STATUS_MODIFIED ) );
            }
            if ( statusTypes.remove( SVNStatusType.STATUS_REPLACED ) ) {
                status.append( symbols.getStatusSymbol( SVNStatusType.STATUS_REPLACED ) );
            }
            if ( statusTypes.remove( SVNStatusType.STATUS_EXTERNAL ) ) {
                status.append( symbols.getStatusSymbol( SVNStatusType.STATUS_EXTERNAL ) );
            }
            if ( statusTypes.remove( SVNStatusType.STATUS_UNVERSIONED ) && entry.reportUnversioned() ) {
                status.append( symbols.getStatusSymbol( SVNStatusType.STATUS_UNVERSIONED ) );
            }
            if ( statusTypes.remove( SVNStatusType.STATUS_MISSING ) ) {
                status.append( symbols.getStatusSymbol( SVNStatusType.STATUS_MISSING ) );
            }
            if ( statusTypes.remove( SVNStatusType.STATUS_INCOMPLETE ) ) {
                status.append( symbols.getStatusSymbol( SVNStatusType.STATUS_INCOMPLETE ) );
            }
            if ( statusTypes.remove( SVNStatusType.STATUS_OBSTRUCTED ) ) {
                status.append( symbols.getStatusSymbol( SVNStatusType.STATUS_OBSTRUCTED ) );
            }
            if ( !statusTypes.isEmpty() && getLog().isWarnEnabled() ) {
                getLog().warn( format( "the following svn statuses are not taken into account: %s", statusTypes ) );
            }

            if ( outOfDate && entry.reportOutOfDate() ) {
                status.append( symbols.getOutOfDateSymbol() );
            }

            return status.toString();
        }

    }

}
