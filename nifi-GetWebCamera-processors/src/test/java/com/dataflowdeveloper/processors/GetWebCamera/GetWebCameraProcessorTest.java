/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dataflowdeveloper.processors.GetWebCamera;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Before;
import org.junit.Test;

import com.dataflowdeveloper.processors.GetWebCamera.GetWebCameraProcessor;

/**
 * Support this tool http://webcam-capture.sarxos.pl/
 * 
 * @author tspann
 *
 */
public class GetWebCameraProcessorTest {

	private TestRunner testRunner;

	@Before
	public void init() {
		testRunner = TestRunners.newTestRunner(GetWebCameraProcessor.class);
	}

	@Test
	public void testProcessor() {

	testRunner.setProperty("imagefilename", "initialimag343434.jpg");
//
//		try {
//			testRunner.enqueue(new FileInputStream(new File("src/test/resources/test.csv")));
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}

		testRunner.setValidateExpressionUsage(true);
		testRunner.run();
		testRunner.assertValid();
		List<MockFlowFile> successFiles = testRunner.getFlowFilesForRelationship(GetWebCameraProcessor.REL_SUCCESS);

		System.out.println("SF:" + successFiles.size());
		for (MockFlowFile mockFile : successFiles) {
			try {
				System.out.println("Size: " + mockFile.getSize());

				Map<String, String> attributes = mockFile.getAttributes();

				for (String attribute : attributes.keySet()) {
					System.out.println("Attribute:" + attribute + " = " + mockFile.getAttribute(attribute));
				}
				System.out.println("File size:" + mockFile.getData().length ) ;

			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

}
