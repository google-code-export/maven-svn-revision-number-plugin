def tempDir = new File( basedir, "svn" )
println "temp directory is " + tempDir

def repositoryDir = new File( tempDir, "repo" )
def workingCopyDir = new File( tempDir, "wc" )

def dir = new File( workingCopyDir, "directory" )
def file = new File( dir, "file.txt" )

println "reading properties"
def propertiesFile = new File( basedir, "target/classes/properties.txt" )

def properties = new Properties();
def reader = propertiesFile.newReader( "UTF-8" )
try {
    properties.load( reader );
} finally {
    try {
        reader.close();
    } catch ( IOException ignored ) {
    }
}

println "checking properties"

assert properties.repository ==~ /^$/
assert properties.path ==~ /^$/
assert properties.revision == "1"
assert properties.mixedRevisions == "false"
assert properties.committedRevision == "1"
assert properties.committedDate ==~ /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2} .*/
assert properties.status == "M"
assert properties.specialStatus == "M"

return true
