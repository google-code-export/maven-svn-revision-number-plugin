def tempDir = new File( basedir, "svn" )
println "temp directory is " + tempDir

def repositoryDir = new File( tempDir, "repo" )
def workingCopyDir = new File( tempDir, "wc" )

def dir = new File( workingCopyDir, "directory" )
def file = new File( dir, "file.txt" )

println "cleaning temp directory"
tempDir.deleteDir()
tempDir.mkdirs()

println "  creating generic content"
dir.mkdirs()
file << "content"

println "  creating test content"

return true
