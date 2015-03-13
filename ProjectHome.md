# Maven SVN Revision Number Plugin  #

This maven plugin retrieves the revision number and the status of the Subversion working copy directory and sets project properties that can be used later in the build process, for example to filter resource files or to activate additional profiles.

The latest plugin version is 1.13 and it has the following GAV:
```
<groupId>com.google.code.maven-svn-revision-number-plugin</groupId>
<artifactId>svn-revision-number-maven-plugin</artifactId>
<version>1.13</version>
```

If used without configuration, the plugin will set `"${project.artifactId}.revision"` and `"${project.artifactId}.status"` properties to the revision and status of the `${project.basedir}` directory correspondingly. The full instructions on how to use the plugin can be found in the [documentation](http://maven-svn-revision-number-plugin.googlecode.com/svn/site/index.html).

The [SVNKit](http://svnkit.com) library is used for all Subversion-related operations.

The plugin is available from the Maven Central, thanks to the Sonatype [OSS Repository Hosting](https://docs.sonatype.org/display/Repository/Sonatype+OSS+Maven+Repository+Usage+Guide).