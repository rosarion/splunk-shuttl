// Copyright (C) 2011 Splunk Inc.
//
// Splunk Inc. licenses this file
// to you under the Apache License, Version 2.0 (the
// License); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an AS IS BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.splunk.shuttl.archiver.fileSystem;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.splunk.shuttl.archiver.fileSystem.FileOverwriteException;
import com.splunk.shuttl.archiver.fileSystem.HadoopFileSystemArchive;
import com.splunk.shuttl.testutil.HadoopFileSystemPutter;
import com.splunk.shuttl.testutil.UtilsFile;
import com.splunk.shuttl.testutil.UtilsFileSystem;
import com.splunk.shuttl.testutil.UtilsPath;
import com.splunk.shuttl.testutil.UtilsTestNG;

/**
 * Using the method naming convention:
 * [metodNamn]_[stateUnderTest]_[expectedOutcome]
 */
@Test(groups = { "fast-unit" })
public class HadoopFileSystemArchiveTest {

    private FileSystem fileSystem;
    private HadoopFileSystemArchive hadoopFileSystemArchive;
    private HadoopFileSystemPutter hadoopFileSystemPutter;
    private Path tmpPath;

    @BeforeMethod
    public void beforeMethod() {
	fileSystem = UtilsFileSystem.getLocalFileSystem();
	tmpPath = new Path("/tmp/" + RandomUtils.nextInt() + "/");
	hadoopFileSystemArchive = new HadoopFileSystemArchive(fileSystem,
		tmpPath);
	hadoopFileSystemPutter = HadoopFileSystemPutter.create(fileSystem);
    }

    @AfterMethod
    public void afterMethod() throws IOException {
	hadoopFileSystemPutter.deleteMyFiles();
	fileSystem.delete(tmpPath, true);
    }

    @Test(groups = { "fast-unit" })
    public void HadoopFileSystemArchive_notInitialized_aNonNullInstanceIsCreated() {
	// Test done in before
	// Confirm
	assertNotNull(hadoopFileSystemArchive);
    }

    public void getFile_validInput_fileShouldBeRetrived() throws IOException {
	File testFile = UtilsFile.createTestFileWithRandomContent();
	hadoopFileSystemPutter.putFile(testFile);
	Path hadoopPath = hadoopFileSystemPutter.getPathForFile(testFile);
	URI fileSystemPath = hadoopPath.toUri();
	File retrivedFile = UtilsFile.createTestFilePath();

	// Test
	hadoopFileSystemArchive.getFile(retrivedFile, fileSystemPath);

	// Confirm
	UtilsTestNG.assertFileContentsEqual(testFile, retrivedFile);
    }

    @Test(expectedExceptions = FileNotFoundException.class)
    public void getFile_whenRemotefileDoNotExist_fileNotFoundException()
	    throws IOException, URISyntaxException {
	URI fileSystemPath = new URI("file:///random/path/to/non/existing/file");
	File retrivedFile = UtilsFile.createTestFilePath();

	// Test
	hadoopFileSystemArchive.getFile(retrivedFile, fileSystemPath);
    }

    @Test(expectedExceptions = FileOverwriteException.class)
    public void getFile_whenLocalFileAllreadyExist_fileOverwriteException()
	    throws IOException, URISyntaxException {
	File testFile = UtilsFile.createTestFileWithRandomContent();
	hadoopFileSystemPutter.putFile(testFile);
	Path hadoopPath = hadoopFileSystemPutter.getPathForFile(testFile);
	URI fileSystemPath = hadoopPath.toUri();
	File retrivedFile = UtilsFile.createTestFileWithRandomContent();

	// Test
	hadoopFileSystemArchive.getFile(retrivedFile, fileSystemPath);
    }

    public void getFile_whenLocalFileAllreadyExist_localFileIsNotOverwritten()
	    throws IOException, URISyntaxException {
	File testFile = UtilsFile.createTestFileWithRandomContent();
	hadoopFileSystemPutter.putFile(testFile);
	Path hadoopPath = hadoopFileSystemPutter.getPathForFile(testFile);
	URI fileSystemPath = hadoopPath.toUri();
	File fileThatCouldBeOverwritten = UtilsFile
		.createTestFileWithRandomContent();
	File originalFile = UtilsFile
		.createTestFileWithContentsOfFile(fileThatCouldBeOverwritten);

	try {
	    // Test
	    hadoopFileSystemArchive.getFile(fileThatCouldBeOverwritten,
		    fileSystemPath);
	} catch (Exception e) { // Intentionally ignoring.
	}

	// Confirm
	UtilsTestNG.assertFileContentsEqual(originalFile,
		fileThatCouldBeOverwritten);

    }

    public void putFile_validInput_fileShouldBePutToFilesSystem()
	    throws IOException {
	File testFile = UtilsFile.createTestFileWithRandomContent();
	Path hadoopPath = UtilsPath.getSafeDirectory(fileSystem);
	URI fileSystemPath = hadoopPath.toUri();

	// Test
	hadoopFileSystemArchive.putFile(testFile, fileSystemPath);

	// Confirm
	File retrivedFile = UtilsFileSystem.getFileFromFileSystem(fileSystem,
		hadoopPath);
	UtilsTestNG.assertFileContentsEqual(testFile, retrivedFile);

    }

    @Test(expectedExceptions = FileNotFoundException.class)
    public void putFile_whenLocalFileDoNotExist_fileNotFoundException()
	    throws IOException {
	File testFile = UtilsFile.createTestFilePath();
	Path hadoopPath = UtilsPath.getSafeDirectory(fileSystem);
	URI fileSystemPath = hadoopPath.toUri();

	// Test
	hadoopFileSystemArchive.putFile(testFile, fileSystemPath);
    }

    @Test(expectedExceptions = FileOverwriteException.class)
    public void putFile_whenRemoteFileExists_fileOverwriteException()
	    throws IOException {
	File fileThatWouldBeOwerwriten = UtilsFile
		.createTestFileWithRandomContent();
	hadoopFileSystemPutter.putFile(fileThatWouldBeOwerwriten);
	Path hadoopPath = hadoopFileSystemPutter
		.getPathForFile(fileThatWouldBeOwerwriten);
	URI pathToRemoteFile = hadoopPath.toUri();
	File testFile = UtilsFile.createTestFileWithRandomContent();

	// Test
	hadoopFileSystemArchive.putFile(testFile, pathToRemoteFile);
    }

    public void putFile_whenRemoteFileExists_remoteFileShouldNotBeOverwriten()
	    throws IOException {
	File fileThatWouldBeOwerwriten = UtilsFile
		.createTestFileWithRandomContent();
	hadoopFileSystemPutter.putFile(fileThatWouldBeOwerwriten);
	Path hadoopPath = hadoopFileSystemPutter
		.getPathForFile(fileThatWouldBeOwerwriten);
	URI pathToRemoteFile = hadoopPath.toUri();
	File testFile = UtilsFile.createTestFileWithRandomContent();

	boolean didGetExeption = false;
	try {
	    // Test
	    hadoopFileSystemArchive.putFile(testFile, pathToRemoteFile);
	} catch (FileOverwriteException e) {
	    didGetExeption = true;
	}


	// Confirm
	assertTrue(didGetExeption);
	File fileAfterPut = UtilsFile.createTestFilePath();
	hadoopFileSystemArchive.getFile(fileAfterPut, pathToRemoteFile);
	UtilsTestNG.assertFileContentsEqual(
		"Put shouln't have overwritten the file.",
		fileThatWouldBeOwerwriten, fileAfterPut);

    }

    public void putFile_withDirectoryContainingAnotherDirectory_bothDirectoriesExistsInTheArchive()
	    throws URISyntaxException, FileNotFoundException,
	    FileOverwriteException, IOException {
	File parent = UtilsFile.createTempDirectory();
	String childFileName = "childDir";
	UtilsFile.createDirectoryInParent(parent, childFileName);
	Path parentPathOnHadoop = hadoopFileSystemPutter.getPathForFile(parent);
	hadoopFileSystemArchive.putFile(parent, parentPathOnHadoop.toUri());
	assertTrue(fileSystem.exists(parentPathOnHadoop));
	Path childPath = new Path(parentPathOnHadoop, childFileName);
	assertTrue(fileSystem.exists(childPath));
	FileUtils.deleteDirectory(parent);
    }

    public void putFileAtomically_validInput_fileShouldBePutToFilesSystem()
	    throws IOException {
	File testFile = UtilsFile.createTestFileWithRandomContent();
	Path hadoopPath = UtilsPath.getSafeDirectory(fileSystem);
	URI fileSystemPath = hadoopPath.toUri();

	// Test
	hadoopFileSystemArchive.putFileAtomically(testFile, fileSystemPath);

	// Confirm
	File retrivedFile = UtilsFileSystem.getFileFromFileSystem(fileSystem,
		hadoopPath);
	UtilsTestNG.assertFileContentsEqual(testFile, retrivedFile);

    }

    @Test(expectedExceptions = FileNotFoundException.class)
    public void putFileAtomically_whenLocalFileDoNotExist_fileNotFoundException()
	    throws IOException {
	File testFile = UtilsFile.createTestFilePath();
	Path hadoopPath = UtilsPath.getSafeDirectory(fileSystem);
	URI fileSystemPath = hadoopPath.toUri();

	// Test
	hadoopFileSystemArchive.putFileAtomically(testFile, fileSystemPath);
    }

    @Test(expectedExceptions = FileOverwriteException.class)
    public void putFileAtomically_whenRemoteFileExists_fileOverwriteException()
	    throws IOException {
	File fileThatWouldBeOwerwriten = UtilsFile
		.createTestFileWithRandomContent();
	hadoopFileSystemPutter.putFile(fileThatWouldBeOwerwriten);
	Path hadoopPath = hadoopFileSystemPutter
		.getPathForFile(fileThatWouldBeOwerwriten);
	URI pathToRemoteFile = hadoopPath.toUri();
	File testFile = UtilsFile.createTestFileWithRandomContent();

	// Test
	hadoopFileSystemArchive.putFileAtomically(testFile, pathToRemoteFile);
    }

    public void putFileAtomically_whenRemoteFileExists_remoteFileShouldNotBeOverwriten()
	    throws IOException {
	File fileThatWouldBeOwerwriten = UtilsFile
		.createTestFileWithRandomContent();
	hadoopFileSystemPutter.putFile(fileThatWouldBeOwerwriten);
	Path hadoopPath = hadoopFileSystemPutter
		.getPathForFile(fileThatWouldBeOwerwriten);
	URI pathToRemoteFile = hadoopPath.toUri();
	File testFile = UtilsFile.createTestFileWithRandomContent();

	boolean didGetExeption = false;
	try {
	    // Test
	    hadoopFileSystemArchive.putFileAtomically(testFile,
		    pathToRemoteFile);
	} catch (FileOverwriteException e) {
	    didGetExeption = true;
	}

	// Make sure there was an exception
	assertTrue(didGetExeption);


	// Confirm
	File fileAfterPut = UtilsFile.createTestFilePath();
	hadoopFileSystemArchive.getFile(fileAfterPut, pathToRemoteFile);
	UtilsTestNG.assertFileContentsEqual(
		"Put shouln't have overwritten the file.",
		fileThatWouldBeOwerwriten, fileAfterPut);

    }

    public void putFileAtomically_withDirectoryContainingAnotherDirectory_bothDirectoriesExistsInTheArchive()
	    throws URISyntaxException, FileNotFoundException,
	    FileOverwriteException, IOException {
	File parent = UtilsFile.createTempDirectory();
	String childFileName = "childDir";
	UtilsFile.createDirectoryInParent(parent, childFileName);
	Path parentPathOnHadoop = hadoopFileSystemPutter.getPathForFile(parent);
	hadoopFileSystemArchive.putFileAtomically(parent,
		parentPathOnHadoop.toUri());
	assertTrue(fileSystem.exists(parentPathOnHadoop));
	Path childPath = new Path(parentPathOnHadoop, childFileName);
	assertTrue(fileSystem.exists(childPath));
	FileUtils.deleteDirectory(parent);
    }

    public void putFileAtomically_withFileAllreadyInTmpFolder_theFilesinTmpFolderDoesNotAffectTheTrasfer()
	    throws FileNotFoundException, FileOverwriteException, IOException {
	File fileToTransfer = UtilsFile.createTestFileWithRandomContent();
	File fileToPutOnTempThatShouldNotAffectTheTransfer = UtilsFile.createTestFileWithRandomContent();
	hadoopFileSystemPutter
		.putFile(fileToPutOnTempThatShouldNotAffectTheTransfer);
	Path hadoopPath = UtilsPath.getSafeDirectory(fileSystem);
	hadoopPath = new Path(hadoopPath, "fileName");
	
	URI fileSystemPath = hadoopPath.toUri();

	// Test
	hadoopFileSystemArchive.putFileAtomically(fileToTransfer,
		fileSystemPath);

	// Confirm
	File retrivedFile = UtilsFileSystem.getFileFromFileSystem(fileSystem,
		hadoopPath);
	UtilsTestNG.assertFileContentsEqual(fileToTransfer, retrivedFile);

    }

    public void listPath_listingAPathThatPointsToADirectory_aListThatContainsThePathsInsideSpecifiedDirectory()
	    throws URISyntaxException, IOException {
	File file1 = UtilsFile.createTestFileWithRandomContent();
	File file2 = UtilsFile.createTestFileWithRandomContent();
	File file3 = UtilsFile.createTestFileWithRandomContent();
	hadoopFileSystemPutter.putFile(file1);
	hadoopFileSystemPutter.putFile(file2);
	hadoopFileSystemPutter.putFile(file3);
	URI baseURI = hadoopFileSystemPutter.getPathOfMyFiles().toUri();
	URI uri1 = new URI(baseURI + "/" + file1.getName());
	URI uri2 = new URI(baseURI + "/" + file2.getName());
	URI uri3 = new URI(baseURI + "/" + file3.getName());

	// Test
	List<URI> contents = hadoopFileSystemArchive.listPath(baseURI);

	// Confirm
	assertTrue(contents.contains(uri1));
	assertTrue(contents.contains(uri2));
	assertTrue(contents.contains(uri3));
    }

    public void listPath_listingAnEmptyDirectory_emptyList() throws IOException {
	File testDirectory = UtilsFile.createTempDirectory();
	hadoopFileSystemPutter.putFile(testDirectory);
	URI hadoopPathToTheDirectory = hadoopFileSystemPutter.getPathForFile(
		testDirectory).toUri();

	// Test
	List<URI> contents = hadoopFileSystemArchive
		.listPath(hadoopPathToTheDirectory);

	// Confirm
	assertEquals(0, contents.size());
    }

    public void listPath_listingAPathThatPointsToAFile_aListOnlyContainingThePathToTheFile()
	    throws IOException {
	File file = UtilsFile.createTestFileWithRandomContent();
	hadoopFileSystemPutter.putFile(file);
	URI uri = hadoopFileSystemPutter.getPathForFile(file).toUri();

	// Test
	List<URI> contents = hadoopFileSystemArchive.listPath(uri);

	// Confirm
	assertTrue(contents.contains(uri));
    }

    public void listPath_listingAPathThatDoNotExist_emptyList()
	    throws IOException, URISyntaxException {
	URI hadoopPathToTheDirectory = new URI(
		"file:///This/path/should/not/exist");

	// Test
	List<URI> contents = hadoopFileSystemArchive
		.listPath(hadoopPathToTheDirectory);

	// Confirm
	assertEquals(0, contents.size());
    }

    public void deletePathRecursivly_givenAFile_thePathShouldBeDeleted()
	    throws IOException {
	File testFile = UtilsFile.createTestFileWithRandomContent();
	hadoopFileSystemPutter.putFile(testFile);
	Path testFilePath = hadoopFileSystemPutter.getPathForFile(testFile);
	
	// Make sure setup was correct
	assertTrue(fileSystem.exists(testFilePath));

	// Test
	hadoopFileSystemArchive.deletePathRecursivly(testFilePath);

	// Verify
	assertFalse(fileSystem.exists(testFilePath));
    }

    public void deletePathRecursivly_givenADirectory_thePathShouldBeDeleted()
	    throws IOException {
	File testDirectory = UtilsFile.createTempDirectory();
	hadoopFileSystemPutter.putFile(testDirectory);
	Path testFilePath = hadoopFileSystemPutter
		.getPathForFile(testDirectory);

	// Make sure setup was correct
	assertTrue(fileSystem.exists(testFilePath));

	// Test
	hadoopFileSystemArchive.deletePathRecursivly(testFilePath);

	// Verify
	assertFalse(fileSystem.exists(testFilePath));
    }

    public void deletePathRecursivly_givenADirectoryWithFilesInIt_thePathShouldBeDeleted()
	    throws IOException {
	File testDirectory = UtilsFile.createTempDirectory();
	File testFile = UtilsFile.createFileInParent(testDirectory, "STUFF");
	UtilsFile.populateFileWithRandomContent(testFile);
	hadoopFileSystemPutter.putFile(testDirectory);
	Path testFilePath = hadoopFileSystemPutter.getPathForFile(testDirectory);

	// Make sure setup was correct
	assertTrue(fileSystem.exists(testFilePath));
	assertTrue(fileSystem.exists(testFilePath.suffix("/STUFF")));

	// Test
	hadoopFileSystemArchive.deletePathRecursivly(testFilePath);

	// Verify
	assertFalse(fileSystem.exists(testFilePath));
	assertFalse(fileSystem.exists(testFilePath.suffix("STUFF")));

    }
    
    public void putFileToTmpDirectoryAppendingPath_existingFile_fileIsCopiedToTheTmpDirectory()
	    throws IOException {
	File testFile = UtilsFile.createTestFileWithRandomContent();
	Path testFilePath = new Path("/just/a/random/path");
	Path whereTestFileShouldGo = new Path(tmpPath.toUri().getPath()
		+ testFilePath.toUri().getPath());

	// Make sure setup was correct
	assertFalse(fileSystem.exists(whereTestFileShouldGo));
	assertFalse(fileSystem.exists(testFilePath));

	// Test
	Path pathWhereTestFilePut = hadoopFileSystemArchive
		.putFileToTmpDirectoryOverwirtingOldFilesAppendingPath(testFile,
		testFilePath.toUri());

	// Verify
	assertEquals(whereTestFileShouldGo, pathWhereTestFilePut);
	assertTrue(fileSystem.exists(whereTestFileShouldGo));
	assertFalse(fileSystem.exists(testFilePath));
    }

    public void move_existingFileOnHadoop_fileIsMoved() throws IOException {
	File testFile = UtilsFile.createTestFileWithRandomContent();
	hadoopFileSystemPutter.putFile(testFile);
	Path testFilePath = hadoopFileSystemPutter.getPathForFile(testFile);
	Path testFilePathAfterMoving = new Path(tmpPath.toUri().getPath()
		+ testFilePath.toUri().getPath());
	
	// Make sure setup was correct
	assertTrue(fileSystem.exists(testFilePath));
	assertFalse(fileSystem.exists(testFilePathAfterMoving));

	
	// Test
	hadoopFileSystemArchive.move(testFilePath, testFilePathAfterMoving);
	
	// Verify
	assertFalse(fileSystem.exists(testFilePath));
	assertTrue(fileSystem.exists(testFilePathAfterMoving));

    }
}
