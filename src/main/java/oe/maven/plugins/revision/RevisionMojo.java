/*-
 * Copyright (c) 2009, Oleg Estekhin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  * Neither the names of the copyright holders nor the names of their
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package oe.maven.plugins.revision;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.wc.admin.SVNEntry;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusClient;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

/**
 * Retrieves the status and revision number of the subversion working copy directory.
 *
 * @goal revision
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
     * @parameter expression="${project}"
     * @readonly
     */
    private MavenProject project;

    /**
     * The subversion working copy directory.
     *
     * @parameter default-value="${basedir}
     * @required
     */
    private File workingCopyDirectory;

    /**
     * The name of the property that will contain the root of the remote repository of the working copy directory
     * entry.
     *
     * @parameter default-value="workingCopyDirectory.repository"
     */
    private String repositoryPropertyName;

    /**
     * The name of the property that will contain the path of the working copy directory entry relative
     * to the root of the remote repository.
     *
     * @parameter default-value="workingCopyDirectory.path"
     */
    private String pathPropertyName;

    /**
     * The name of the property that will contain the aggregated status and revision number of the  working copy
     * directory.
     *
     * @parameter default-value="workingCopyDirectory.revision"
     */
    private String revisionPropertyName;


    /**
     * Whether to report the mixed revisions information. If set to {@code false} then only the maximum revision number
     * will be reported.
     *
     * @parameter default-value="true"
     */
    private boolean reportMixedRevisions;

    /**
     * Whether to report the status information. If set to {@code false} then only the revision number will be
     * reported.
     *
     * @parameter default-value="true"
     */
    private boolean reportStatus;

    /**
     * Whether to collect the status information on items that are not under version control.
     *
     * @parameter default-value="true"
     */
    private boolean reportUnversioned;

    /**
     * Whether to collect the status information on items that were set to be ignored.
     *
     * @parameter default-value="false"
     */
    private boolean reportIgnored;

    /**
     * Whether to check the remote repository and report if the local items are out-of-date.
     *
     * @parameter default-value="false"
     */
    private boolean reportOutOfDate;

    /**
     * Provides detailed messages while this goal is running.
     *
     * @parameter default-value="false"
     */
    private boolean verbose;


    public void execute() throws MojoExecutionException, MojoFailureException {
        if (verbose) {
            getLog().info("${workingCopyDirectory}: " + workingCopyDirectory);
            getLog().info("report mixed revisions: " + reportMixedRevisions);
            getLog().info("report status: " + reportStatus);
            getLog().info("report unversioned: " + reportUnversioned);
            getLog().info("report ignored: " + reportIgnored);
            getLog().info("report out-of-date: " + reportOutOfDate);
        }
        try {
            String repository;
            String path;
            String revision;
            if (SVNWCUtil.isVersionedDirectory(workingCopyDirectory)) {
                SVNClientManager clientManager = SVNClientManager.newInstance();
                SVNStatusClient statusClient = clientManager.getStatusClient();

                SVNEntry entry = statusClient.doStatus(workingCopyDirectory, false).getEntry();
                repository = entry.getRepositoryRoot();
                path = entry.getURL().substring(repository.length());
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }

                StatusCollector statusCollector = new StatusCollector();
                statusClient.doStatus(workingCopyDirectory,
                        SVNRevision.HEAD, SVNDepth.INFINITY,
                        reportOutOfDate, true, reportIgnored, false,
                        statusCollector,
                        null);
                revision = statusCollector.toString();
            } else {
                repository = "";
                path = "";
                revision = "unversioned";
            }
            project.getProperties().setProperty(repositoryPropertyName, repository);
            project.getProperties().setProperty(pathPropertyName, path);
            project.getProperties().setProperty(revisionPropertyName, revision);
            if (verbose) {
                getLog().info("${" + repositoryPropertyName + "} is set to \"" + repository + '\"');
                getLog().info("${" + pathPropertyName + "} is set to \"" + path + '\"');
                getLog().info("${" + revisionPropertyName + "} is set to \"" + revision + '\"');
            }
        } catch (SVNException e) {
            throw new MojoExecutionException("failed to obtain revision information", e);
        }
    }


    private final class StatusCollector implements ISVNStatusHandler {

        private long maximumRevisionNumber;

        private long minimumRevisionNumber;

        private Set<SVNStatusType> localStatusTypes;

        private boolean remoteChanges;

        private StatusCollector() {
            maximumRevisionNumber = Long.MIN_VALUE;
            minimumRevisionNumber = Long.MAX_VALUE;
            localStatusTypes = new HashSet<SVNStatusType>();
        }

        public void handleStatus(SVNStatus status) {
            SVNStatusType contentsStatusType = status.getContentsStatus();
            localStatusTypes.add(contentsStatusType);
            if (SVNStatusType.STATUS_NORMAL.equals(contentsStatusType)
                    || SVNStatusType.STATUS_MODIFIED.equals(contentsStatusType)) {
                long revisionNumber = status.getRevision().getNumber();
                maximumRevisionNumber = Math.max(maximumRevisionNumber, revisionNumber);
                minimumRevisionNumber = Math.min(minimumRevisionNumber, revisionNumber);
            }
            SVNStatusType propertiesStatusType = status.getPropertiesStatus();
            localStatusTypes.add(propertiesStatusType);
            boolean remoteStatusTypes = !SVNStatusType.STATUS_NONE.equals(status.getRemotePropertiesStatus())
                    || !SVNStatusType.STATUS_NONE.equals(status.getRemoteContentsStatus());
            remoteChanges = remoteChanges || remoteStatusTypes;
            if (verbose) {
                StringBuilder buffer = new StringBuilder();
                buffer.append(status.getContentsStatus().getCode()).append(status.getPropertiesStatus().getCode());
                buffer.append(remoteStatusTypes ? '*' : ' ');
                buffer.append(' ').append(String.format("%6d", status.getRevision().getNumber()));
                buffer.append(' ').append(status.getFile());
                getLog().info(buffer.toString());
            }
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            if (maximumRevisionNumber != Long.MIN_VALUE) {
                result.append('r').append(maximumRevisionNumber);
                if (minimumRevisionNumber != maximumRevisionNumber && reportMixedRevisions) {
                    result.append('-').append('r').append(maximumRevisionNumber);
                }
            }
            if (reportStatus) {
                localStatusTypes.remove(SVNStatusType.STATUS_NONE);
                localStatusTypes.remove(SVNStatusType.STATUS_NORMAL);
                if (!localStatusTypes.isEmpty()) {
                    result.append(' ');
                }
                if (localStatusTypes.remove(SVNStatusType.STATUS_MODIFIED)) {
                    result.append(SVNStatusType.STATUS_MODIFIED.getCode());
                }
                if (localStatusTypes.remove(SVNStatusType.STATUS_ADDED)) {
                    result.append(SVNStatusType.STATUS_ADDED.getCode());
                }
                if (localStatusTypes.remove(SVNStatusType.STATUS_DELETED)) {
                    result.append(SVNStatusType.STATUS_DELETED.getCode());
                }
                if (localStatusTypes.remove(SVNStatusType.STATUS_UNVERSIONED) && reportUnversioned) {
                    result.append(SVNStatusType.STATUS_UNVERSIONED.getCode());
                }
                if (localStatusTypes.remove(SVNStatusType.STATUS_MISSING)) {
                    result.append(SVNStatusType.STATUS_MISSING.getCode());
                    localStatusTypes.remove(SVNStatusType.STATUS_INCOMPLETE); // same status code '!'
                }
                if (localStatusTypes.remove(SVNStatusType.STATUS_REPLACED)) {
                    result.append(SVNStatusType.STATUS_REPLACED.getCode());
                }
                if (localStatusTypes.remove(SVNStatusType.STATUS_CONFLICTED)) {
                    result.append(SVNStatusType.STATUS_CONFLICTED.getCode());
                }
                if (localStatusTypes.remove(SVNStatusType.STATUS_OBSTRUCTED)) {
                    result.append(SVNStatusType.STATUS_OBSTRUCTED.getCode());
                }
                if (localStatusTypes.remove(SVNStatusType.STATUS_IGNORED) && reportIgnored) {
                    result.append(SVNStatusType.STATUS_IGNORED.getCode());
                }
                if (localStatusTypes.remove(SVNStatusType.STATUS_INCOMPLETE)) {
                    result.append(SVNStatusType.STATUS_CONFLICTED.getCode());
                }
                if (localStatusTypes.remove(SVNStatusType.STATUS_EXTERNAL)) {
                    result.append(SVNStatusType.STATUS_EXTERNAL.getCode());
                }
                if (!localStatusTypes.isEmpty()) {
                    getLog().warn("unprocessed svn statuses: " + localStatusTypes);
                }
                if (remoteChanges && reportOutOfDate) {
                    result.append('*');
                }
            }
            return result.toString();
        }

    }

}
