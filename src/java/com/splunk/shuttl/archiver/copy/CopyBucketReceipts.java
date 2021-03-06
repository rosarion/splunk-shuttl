// Copyright (C) 2011 Splunk Inc.
//
// Splunk Inc. licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.splunk.shuttl.archiver.copy;

import java.io.File;

import com.splunk.shuttl.archiver.LocalFileSystemPaths;
import com.splunk.shuttl.archiver.model.LocalBucket;
import com.splunk.shuttl.archiver.util.UtilsFile;

/**
 * Managing receipts for bucket that has been copied.
 */
public class CopyBucketReceipts {

	private final LocalFileSystemPaths fileSystemPaths;

	public CopyBucketReceipts(LocalFileSystemPaths fileSystemPaths) {
		this.fileSystemPaths = fileSystemPaths;
	}

	/**
	 * @return receipt of that a bucket has been successfully copied to the
	 *         archive file system.
	 */
	public File createReceipt(LocalBucket bucket) {
		File receipt = getReceiptFileForBucket(bucket);
		UtilsFile.touch(receipt);
		return receipt;
	}

	private File getReceiptFileForBucket(LocalBucket bucket) {
		File receiptsDirectory = fileSystemPaths
				.getCopyBucketReceiptsDirectory(bucket);
		return new File(receiptsDirectory, "copy-receipt.file");
	}

	/**
	 * @return true if receipt has been created for bucket.
	 */
	public boolean hasReceipt(LocalBucket b) {
		return getReceiptFileForBucket(b).exists();
	}

}
