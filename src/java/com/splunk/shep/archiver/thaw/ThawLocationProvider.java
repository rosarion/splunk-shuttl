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
package com.splunk.shep.archiver.thaw;

import java.io.File;

import com.splunk.shep.archiver.model.Bucket;

/**
 * Provides location for where to thaw {@link Bucket}s.
 */
public class ThawLocationProvider {

    private final SplunkSettings splunkSettings;

    /**
     * @param splunkSettings
     *            for looking up the thaw directory.
     */
    public ThawLocationProvider(SplunkSettings splunkSettings) {
	this.splunkSettings = splunkSettings;
    }

    /**
     * @param bucket
     *            to get location in thaw for.
     */
    public File getLocationInThawForBucket(Bucket bucket) {
	File thawLocation = splunkSettings.getThawLocation(bucket.getIndex());
	return new File(thawLocation, bucket.getName());
    }

}