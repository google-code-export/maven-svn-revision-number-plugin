# Artifact ID is svn-revision-number-maven-plugin from now on #

The Apache/Maven guidelines reserve plugin names in the form of maven-X-plugin. The third-party plugins should use the X-maven-plugin scheme instead.

The svn-revision-number plugin was initially named `maven-svn-revision-number-plugin`, and this is clearly my fault for not following the guidelines in the first place.

In order to comply with the naming guidelines the plugin's artifact id has been changed from `maven-svn-revision-number-plugin` to `svn-revision-number-maven-plugin`, while the group id has been left as `com.google.code.maven-svn-revision-number-plugin`.

So, the plugin GAV for versions from 1.8 is:
```
<groupId>com.google.code.maven-svn-revision-number-plugin</groupId>
<artifactId>svn-revision-number-maven-plugin</artifactId>
<version>1.8+</version>
```

The plugin GAV for versions up to 1.7 was:
```
<groupId>com.google.code.maven-svn-revision-number-plugin</groupId>
<artifactId>maven-svn-revision-number-plugin</artifactId>
<version>1.1-1.7</version>
```

The Maven does not support relocation feature for plugins, so there will be no relocation pom in the Maven Central that points from the old GAV to the new GAV, which means that plugin users will have to manually discover that the GAV was changed, and this page serves exactly this purpose.