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

package com.splunk.shuttl.archiver.archive;

import static com.splunk.shuttl.archiver.LogFormatter.*;

import java.io.FileNotFoundException;
import java.util.Arrays;

import org.apache.log4j.Logger;

import com.splunk.shuttl.archiver.LogFormatter;
import com.splunk.shuttl.archiver.archive.recovery.IndexPreservingBucketMover;
import com.splunk.shuttl.archiver.archive.recovery.FailedBucketsArchiver;
import com.splunk.shuttl.archiver.bucketlock.BucketLocker;
import com.splunk.shuttl.archiver.model.Bucket;
import com.splunk.shuttl.archiver.model.FileNotDirectoryException;
import com.splunk.shuttl.server.mbeans.ShuttlArchiver;
import com.splunk.shuttl.server.mbeans.ShuttlArchiverMBean;
import com.splunk.shuttl.server.mbeans.ShuttlMBeanException;
import com.splunk.shuttl.server.mbeans.util.RegistersMBeans;

/**
 * Takes a bucket that froze and archives it. <br/>
 * The {@link BucketFreezer} also recovers any failed archiving attempts by
 * other {@link BucketFreezer}s.
 */
public class BucketFreezer {

	private static Logger logger = Logger.getLogger(BucketFreezer.class);

	public static final int EXIT_OK = 0;
	public static final int EXIT_INCORRECT_ARGUMENTS = 11;
	public static final int EXIT_FILE_NOT_A_DIRECTORY = 12;
	public static final int EXIT_FILE_NOT_FOUND = 13;
	public static final int EXIT_COULD_NOT_CONFIGURE_BUCKET_FREEZER = 14;

	private final IndexPreservingBucketMover bucketMover;
	private final BucketLocker bucketLocker;
	private final FailedBucketsArchiver failedBucketsArchiver;
	private final ArchiveRestHandler archiveRestHandler;

	public BucketFreezer(IndexPreservingBucketMover bucketMover, BucketLocker bucketLocker,
			ArchiveRestHandler archiveRestHandler,
			FailedBucketsArchiver failedBucketsArchiver) {
		this.bucketMover = bucketMover;
		this.bucketLocker = bucketLocker;
		this.archiveRestHandler = archiveRestHandler;
		this.failedBucketsArchiver = failedBucketsArchiver;
	}

	/**
	 * Freezes the bucket on the specified path and belonging to specified index.
	 * 
	 * @param indexName
	 *          The name of the index that this bucket belongs to
	 * 
	 * @param path
	 *          The path of the bucket on the local file system
	 * 
	 * @return An exit code depending on the outcome.
	 */
	public int freezeBucket(String indexName, String path) {
		try {
			moveAndArchiveBucket(indexName, path);
			return EXIT_OK;
		} catch (FileNotDirectoryException e) {
			String error = LogFormatter.did("Attempted to archive bucket",
					"the provided path was not a directory", "path to a directory");
			logger.error(error);
			return EXIT_FILE_NOT_A_DIRECTORY;
		} catch (FileNotFoundException e) {
			String error = LogFormatter.did("Attempted to archive bucket",
					"the provided path was not found", "a valid path");
			logger.error(error);
			return EXIT_FILE_NOT_FOUND;
		}
	}

	private void moveAndArchiveBucket(String indexName, String path)
			throws FileNotFoundException, FileNotDirectoryException {
		Bucket bucket = new Bucket(indexName, path);

		bucketLocker.callBucketHandlerUnderSharedLock(bucket,
				new MoveAndArchiveBucketUnderLock(bucketMover, archiveRestHandler));

		failedBucketsArchiver.archiveFailedBuckets(archiveRestHandler);
	}

	/**
	 * The main.
	 */
	public static void main(String... args) {
		runMainWithDependencies(Runtime.getRuntime(), new BucketFreezerProvider(),
				RegistersMBeans.create(), args);
	}

	/**
	 * This method is used by the real main and only exists so that it can be
	 * tested using test doubles.
	 */
	/* package-private */static void runMainWithDependencies(Runtime runtime,
			BucketFreezerProvider bucketFreezerProvider,
			RegistersMBeans registersMBeans, String... args) {
		if (args.length != 2) {
			logIncorrectArguments(args);
			runtime.exit(EXIT_INCORRECT_ARGUMENTS);
		} else {
			archiveBucketWhileRegisteringMBean(runtime, bucketFreezerProvider,
					registersMBeans, args);
		}
	}

	private static void logIncorrectArguments(String[] args) {
		logger.error(did("Attempted to archive bucket", "insufficient arguments",
				"both index name and path", "nr_args", args.length, "args",
				Arrays.toString(args)));
	}

	private static void archiveBucketWhileRegisteringMBean(Runtime runtime,
			BucketFreezerProvider bucketFreezerProvider,
			RegistersMBeans registersMBeans, String... args) {
		String name = ShuttlArchiverMBean.OBJECT_NAME;
		String index = args[0];
		String path = args[1];
		Class<ShuttlArchiver> clazz = ShuttlArchiver.class;

		logger.info(will("Attempting to archive bucket", "index", index, "path",
				path));
		try {
			registersMBeans.registerMBean(name, clazz);
			runtime.exit(bucketFreezerProvider.getConfiguredBucketFreezer()
					.freezeBucket(index, path));
		} catch (ShuttlMBeanException e) {
			logException(name, index, path, clazz, e);
			runtime.exit(EXIT_COULD_NOT_CONFIGURE_BUCKET_FREEZER);
		} finally {
			registersMBeans.unregisterMBean(name);
		}
	}

	private static void logException(String name, String index, String path,
			Class<ShuttlArchiver> clazz, ShuttlMBeanException e) {
		logger.error(did("Registered MBean in BucketFreezer", e,
				"To configure the BucketFreezer with MBeans", "name", name, "class",
				clazz, "index", index, "bucket_path", path));
	}
}
