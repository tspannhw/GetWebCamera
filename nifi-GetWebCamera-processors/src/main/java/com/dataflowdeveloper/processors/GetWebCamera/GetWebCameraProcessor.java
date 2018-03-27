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

import java.io.IOException;
import java.io.OutputStream;

// see:   https://github.com/sarxos/webcam-capture/blob/master/webcam-capture/src/example/java/TakePictureExample.java

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.IOUtils;
import org.apache.nifi.annotation.behavior.EventDriven;
import org.apache.nifi.annotation.behavior.SideEffectFree;
import org.apache.nifi.annotation.behavior.SupportsBatching;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamException;
import com.github.sarxos.webcam.WebcamResolution;
import com.github.sarxos.webcam.WebcamUtils;
import com.github.sarxos.webcam.util.ImageUtils;

@EventDriven
@SupportsBatching
@SideEffectFree
@Tags({ "webcam", "computer vision", "image", "web camera", "usb web camera" })
@CapabilityDescription("Ingest an image from a camera")
@SeeAlso({})
@WritesAttributes({ @WritesAttribute(attribute = "probilities", description = "The probabilites and labels") })
/**
 * 
 * @author tspann
 *
 */
public class GetWebCameraProcessor extends AbstractProcessor {

	private static final String IMAGE_TYPE_USED = "png";
	private static final String IMAGEFILENAME = "imagefilename";
	private static final String CAMERANAME = "cameraname";
	public static final String PROPERTY_NAME_EXTRA = "Extra Resources";

	public static final PropertyDescriptor FILE_NAME = new PropertyDescriptor.Builder().name(IMAGEFILENAME)
			.description("File Name for the output image").required(true).expressionLanguageSupported(true)
			.addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();
	public static final PropertyDescriptor CAMERA_NAME = new PropertyDescriptor.Builder().name(CAMERANAME)
			.description("Full or partial name of the camera you want to use, leave blank if you have only one")
			.required(true).expressionLanguageSupported(true).addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
			.build();

	public static final Relationship REL_SUCCESS = new Relationship.Builder().name("success")
			.description("Successfully determined image.").build();
	public static final Relationship REL_FAILURE = new Relationship.Builder().name("failure")
			.description("Failed to determine image.").build();

	private List<PropertyDescriptor> descriptors;
	private Set<Relationship> relationships;

	@Override
	protected void init(final ProcessorInitializationContext context) {
		final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
		descriptors.add(FILE_NAME);
		descriptors.add(CAMERA_NAME);
		this.descriptors = Collections.unmodifiableList(descriptors);
		final Set<Relationship> relationships = new HashSet<Relationship>();
		relationships.add(REL_SUCCESS);
		relationships.add(REL_FAILURE);
		this.relationships = Collections.unmodifiableSet(relationships);
	}

	@Override
	public Set<Relationship> getRelationships() {
		return this.relationships;
	}

	@Override
	public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
		return descriptors;
	}

	@OnScheduled
	public void onScheduled(final ProcessContext context) {
		return;
	}

	/**
	 * https://github.com/sarxos/webcam-capture/blob/master/webcam-capture/src/example/java/DifferentFileFormatsExample.java
	 * 
	 */
	@Override
	public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
		FlowFile flowFile = session.get();
		if (flowFile == null) {
			flowFile = session.create();
		}
		try {
			flowFile.getAttributes();
			String fileName = flowFile.getAttribute(IMAGEFILENAME);
			if (fileName == null) {
				fileName = context.getProperty(IMAGEFILENAME).evaluateAttributeExpressions(flowFile).getValue();
			}
			if (fileName == null) {
				fileName = "image1";
			}
			final String fileNameToWrite = fileName;

			String cameraName = flowFile.getAttribute(CAMERANAME);
			if (cameraName == null) {
				cameraName = context.getProperty(CAMERANAME).evaluateAttributeExpressions(flowFile).getValue();
			}
			if (cameraName == null) {
				cameraName = "";
			}
			final String cameraNameFinal = cameraName;
			
			try {
				final AtomicReference<Boolean> wasError = new AtomicReference<>(false);

				flowFile = session.write(flowFile, new OutputStreamCallback() {

					@Override
					public void process(OutputStream out) throws IOException {
						try {
							if (!Webcam.getWebcams().isEmpty()) {
								if (Webcam.getWebcams().size() > 1) {
									for (Webcam wcam : Webcam.getWebcams()) {
										if (wcam != null && wcam.getName() != null
												&& wcam.getName().contains(cameraNameFinal)) {
											wcam.setViewSize(WebcamResolution.VGA.getSize());
											wcam.open();
											WebcamUtils.capture(wcam, fileNameToWrite, ImageUtils.FORMAT_PNG);
											byte[] bytes = WebcamUtils.getImageBytes(wcam, IMAGE_TYPE_USED);

											if (bytes != null) {
												getLogger().debug(String.format("webcamer name %s", wcam.getName()));
												getLogger()
														.debug(String.format("read %d bytes from captured image file",
																new Object[] { bytes.length }));
												IOUtils.write(bytes, out);
											}

											// BufferedImage image = wcam.getImage();
											//
											// // save image to PNG file
											// if ( image != null ) {
											// ImageIO.write(image, "PNG", new File("image2.png"));
											// }

											wcam.close();
										}
									}
								}
								else {
									// only have one webcam
									Webcam wcam = Webcam.getDefault();
									wcam.setViewSize(WebcamResolution.VGA.getSize());
									wcam.open();
									WebcamUtils.capture(wcam, fileNameToWrite, ImageUtils.FORMAT_PNG);
									byte[] bytes = WebcamUtils.getImageBytes(wcam, IMAGE_TYPE_USED);

									if (bytes != null) {
										getLogger().debug(String.format("webcamer name %s", wcam.getName()));
										getLogger().debug(String.format("read %d bytes from captured image file",
												new Object[] { bytes.length }));
										IOUtils.write(bytes, out);
									}

									wcam.close();
								}
							} 
							else {
								// No Webcameras found
								wasError.set(true);
							}
							// webcam.setViewSize(WebcamResolution.VGA.getSize());
							// WebcamUtils.capture(webcam, fileNameToWrite, ImageUtils.FORMAT_PNG);
							// byte[] bytes = WebcamUtils.getImageBytes(webcam, "jpg");
							//
							// getLogger().debug(
							// String.format("read %d bytes from captured image file", new Object[] {
							// bytes.length }));
							//
							// IOUtils.write(bytes, out);
							//
							// ImageIO.write(webcam.getImage(), "PNG", new File(fileNameToWrite + ".png"));
							// webcam.close();
						} catch (WebcamException e) {
							getLogger().error("Image failed to write " + e.getLocalizedMessage());
							wasError.set(true);
						}

					}
				});

				if (wasError.get()) {
					session.transfer(flowFile, REL_FAILURE);
				} else {
					session.transfer(flowFile, REL_SUCCESS);
				}
			} catch (Exception e) {
				throw new ProcessException(e);
			}

			session.commit();
		} catch (

		final Throwable t) {
			getLogger().error("Unable to process GetWebCamera Processor file " + t.getLocalizedMessage());
			throw new ProcessException(t);
		}
	}
}