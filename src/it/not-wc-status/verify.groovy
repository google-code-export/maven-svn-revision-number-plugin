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

assert properties.repository == ""
assert properties.path == ""
assert properties.revision == "-1"
assert properties.mixedRevisions == "false"
assert properties.committedRevision == "-1"
assert properties.committedDate == ""
assert properties.status == "?"
assert properties.specialStatus == "u"

return true
