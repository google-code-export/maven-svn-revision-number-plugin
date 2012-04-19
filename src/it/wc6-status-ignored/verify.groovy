def tempDir = new File( basedir, "svn" )
println "temp directory is " + tempDir

def repositoryDir = new File( tempDir, "repo" )
def workingCopyDir = new File( tempDir, "wc" )

def dir = new File( workingCopyDir, "directory" )
def file = new File( dir, "file.txt" )

println "reading properties"
def propertiesFile = new File( basedir, "target/classes/properties.txt" )
def properties = new Properties();
propertiesFile.withReader( "UTF-8" ) {
    properties.load( it )
}

println "checking properties"

//assert properties[ "ignored.repository" ] =~ /\/repo$/
//assert properties[ "ignored.path" ] == "directory"
assert properties[ "ignored.revision" ] == "2"
assert properties[ "ignored.mixedRevisions" ] == "true"
assert properties[ "ignored.committedRevision" ] == "2"
assert properties[ "ignored.committedDate" ] ==~ /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2} .*/
assert properties[ "ignored.status" ] == ""
assert properties[ "ignored.specialStatus" ] == ""

//assert properties[ "reported.repository" ] =~ /\/repo$/
//assert properties[ "reported.path" ] == "directory"
assert properties[ "reported.revision" ] == "2"
assert properties[ "reported.mixedRevisions" ] == "true"
assert properties[ "reported.committedRevision" ] == "2"
assert properties[ "reported.committedDate" ] ==~ /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2} .*/
assert properties[ "reported.status" ] == "I"
assert properties[ "reported.specialStatus" ] == "I"

return true
