import org.tmatesoft.svn.core.SVNPropertyValue
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration
import org.tmatesoft.svn.core.io.SVNRepositoryFactory
import org.tmatesoft.svn.core.wc2.SvnOperationFactory
import org.tmatesoft.svn.core.wc2.SvnTarget

def tempDir = new File( basedir, "svn" )
println "temp directory is " + tempDir

def repositoryDir = new File( tempDir, "repo" )
def workingCopyDir = new File( tempDir, "wc" )

def dir = new File( workingCopyDir, "directory" )
def file = new File( dir, "file.txt" )

println "cleaning temp directory"
tempDir.deleteDir()
tempDir.mkdirs()

println "creating repository"
FSRepositoryFactory.setup()
def repositoryUrl = SVNRepositoryFactory.createLocalRepository( repositoryDir, null, false, false, false, false, false, false, true )

println "creating working copy"
def operationFactory = new SvnOperationFactory()
operationFactory.setPrimaryWcGeneration( SvnWcGeneration.V17 )

println "  checking out"
def genericCheckout = operationFactory.createCheckout()
genericCheckout.setSource( SvnTarget.fromURL( repositoryUrl ) )
genericCheckout.setSingleTarget( SvnTarget.fromFile( workingCopyDir ) )
genericCheckout.run()

println "  creating generic content"
dir.mkdirs()
file << "content"

def genericAdd = operationFactory.createScheduleForAddition()
genericAdd.addTarget( SvnTarget.fromFile( dir ) )
genericAdd.addTarget( SvnTarget.fromFile( file ) )
genericAdd.run()

def genericCommit = operationFactory.createCommit()
genericCommit.setSingleTarget( SvnTarget.fromFile( workingCopyDir ) )
genericCommit.setCommitMessage( "generic content" )
genericCommit.run()

def genericUpdate = operationFactory.createUpdate();
genericUpdate.setSingleTarget( SvnTarget.fromFile( workingCopyDir ) )
genericUpdate.run()

println "  creating test content"
def property = operationFactory.createSetProperty()
property.setSingleTarget( SvnTarget.fromFile( dir ) )
property.setPropertyName( "svn:ignore" )
property.setPropertyValue( SVNPropertyValue.create( "ignored.txt" ) )
property.run()

def testCommit = operationFactory.createCommit()
testCommit.setSingleTarget( SvnTarget.fromFile( workingCopyDir ) )
testCommit.setCommitMessage( "test content" )
testCommit.run()

def ignoredFile = new File( dir, "ignored.txt" )
ignoredFile << "ignored"

return true
