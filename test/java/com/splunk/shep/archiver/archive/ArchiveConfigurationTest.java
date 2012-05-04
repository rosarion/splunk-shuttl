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
package com.splunk.shep.archiver.archive;

import static org.mockito.Mockito.*;
import static org.testng.AssertJUnit.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.splunk.shep.server.mbeans.ShepArchiverMBean;

@Test(groups = { "fast-unit" })
public class ArchiveConfigurationTest {

    private ShepArchiverMBean mBean;

    @BeforeMethod
    public void setUp() {
	mBean = mock(ShepArchiverMBean.class);
    }

    private ArchiveConfiguration createConfiguration() {
	return ArchiveConfiguration.createConfigurationWithMBean(mBean);
    }

    @Test(groups = { "fast-unit" })
    public void getArchiveFormat_givenAnyFormatAsStringInMBean_returnsBucketFormat() {
	when(mBean.getArchiveFormat()).thenReturn(
		BucketFormat.SPLUNK_BUCKET.name());
	BucketFormat archiveFormat = createConfiguration().getArchiveFormat();
	assertNotNull(archiveFormat);
    }

    public void getArchiveFormat_givenNullFormat_null() {
	when(mBean.getArchiveFormat()).thenReturn(null);
	assertNull(createConfiguration().getArchiveFormat());
    }

    public void getArchivingRoot_givenNullUri_null() {
	when(mBean.getArchiverRootURI()).thenReturn(null);
	assertNull(createConfiguration().getArchivingRoot());
    }

    public void getArchivingRoot_givenUriInMBean_returnSameUriAsInMBean() {
	String uriString = "valid:/uri";
	URI expectedUri = URI.create(uriString);
	when(mBean.getArchiverRootURI()).thenReturn(uriString);
	URI actualUri = createConfiguration().getArchivingRoot();
	assertEquals(expectedUri, actualUri);
    }

    public void getClusterName_stubbedMBeanClusterName_sameAsInMBean() {
	String expected = "clusterName";
	when(mBean.getClusterName()).thenReturn(expected);
	String actual = createConfiguration().getClusterName();
	assertEquals(expected, actual);
    }

    public void getServerName_stubbedMBeanServerName_sameAsInMBean() {
	String expected = "serverName";
	when(mBean.getServerName()).thenReturn(expected);
	String actual = createConfiguration().getServerName();
	assertEquals(expected, actual);
    }

    public void getBucketFormatPriority_noFormats_emptyList() {
	when(mBean.getBucketFormatPriority()).thenReturn(
		new ArrayList<String>());
	List<BucketFormat> priorityList = createConfiguration()
		.getBucketFormatPriority();
	assertEquals(new ArrayList<BucketFormat>(), priorityList);
    }

    public void getBucketFormatPriority_oneFormat_listWithThatOneFormat() {
	List<String> format = Arrays.asList(BucketFormat.SPLUNK_BUCKET.name());
	when(mBean.getBucketFormatPriority()).thenReturn(format);
	List<BucketFormat> priorityList = createConfiguration()
		.getBucketFormatPriority();
	assertEquals(1, priorityList.size());
	assertEquals(BucketFormat.SPLUNK_BUCKET, priorityList.get(0));
    }

    public void getBucketFormatPriority_twoFormats_listWithThoseTwoFormats() {
	List<String> formats = Arrays.asList(BucketFormat.SPLUNK_BUCKET.name(),
		BucketFormat.UNKNOWN.name());
	when(mBean.getBucketFormatPriority()).thenReturn(formats);
	List<BucketFormat> priorityList = createConfiguration()
		.getBucketFormatPriority();
	assertEquals(2, priorityList.size());
	assertEquals(BucketFormat.SPLUNK_BUCKET, priorityList.get(0));
	assertEquals(BucketFormat.UNKNOWN, priorityList.get(1));
    }

    public void getTmpDirectory_givenNullArchivingRoot_null() {
	when(mBean.getArchiverRootURI()).thenReturn(null);
	assertNull(createConfiguration().getTmpDirectory());
    }

    public void getTmpDirectory_givenArchivingRootUriAndTmpDirectoryString_combineForTmpDirectoryUri() {
	when(mBean.getArchiverRootURI()).thenReturn("valid:/uri");
	when(mBean.getTmpDirectory()).thenReturn("/tmp");
	URI tmpDirectory = createConfiguration().getTmpDirectory();
	assertEquals(URI.create("valid:/tmp"), tmpDirectory);
    }

    public void getTmpDirectory_givenUriWithHostAndPort_keepingHostAndPortInUri() {
	when(mBean.getArchiverRootURI())
		.thenReturn("hdfz://localhost:8000/uri");
	when(mBean.getTmpDirectory()).thenReturn("/tmp");
	URI tmpDirectory = createConfiguration().getTmpDirectory();
	assertEquals(URI.create("hdfz://localhost:8000/tmp"), tmpDirectory);
    }

}
