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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.IplImage;
import static org.bytedeco.opencv.helper.opencv_imgcodecs.cvSaveImage;
import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.IplImage;
import static org.bytedeco.opencv.helper.opencv_imgcodecs.cvSaveImage;

import java.awt.image.DataBufferByte;
import  java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

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
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import javax.imageio.ImageIO;


@EventDriven
@SupportsBatching
@SideEffectFree
@Tags({ "webcam", "computer vision", "camera", "image", "web camera", "usb web camera" })
@CapabilityDescription("Ingest an image from a camera")
@SeeAlso({})
@ReadsAttributes({ @ReadsAttribute(attribute = "imagefilename", description = "imagefilename") })
@WritesAttributes({ @WritesAttribute(attribute = "mime_type", description = "image") })
/**
 * 
 * @author tspann
 *
 */
public class GetWebCameraProcessor extends AbstractProcessor {

	private static final String IMAGE_TYPE_USED = "jpg";
	private static final String IMAGEFILENAME = "imagefilename";
	public static final String PROPERTY_NAME_EXTRA = "Extra Resources";

	public static final PropertyDescriptor FILE_NAME = new PropertyDescriptor.Builder().name(IMAGEFILENAME)
			.expressionLanguageSupported( ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
			.description("File Name for the output image").required(false).defaultValue("image.jpg")
			.addValidator(StandardValidators.NON_BLANK_VALIDATOR)
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

	public static byte[] IplImageToByteArray2(IplImage src) {
		byte[] barray = new byte[src.imageSize()];
		src.getByteBuffer().get(barray);


		return barray;
	}

	public static BufferedImage toBufferedImage(IplImage src) {
		OpenCVFrameConverter.ToIplImage iplConverter = new OpenCVFrameConverter.ToIplImage();
		Java2DFrameConverter bimConverter = new Java2DFrameConverter();
		Frame frame = iplConverter.convert(src);
		BufferedImage img = bimConverter. convert(frame);
		img.flush();
		return img;
	}
	public static byte[] toByteArray(BufferedImage bi, String format)
			throws IOException {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(bi, format, baos);
		byte[] bytes = baos.toByteArray();
		return bytes;

	}

	/**
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

			try {
				final AtomicReference<Boolean> wasError = new AtomicReference<>(false);

				flowFile = session.write(flowFile, new OutputStreamCallback() {

					@Override
					public void process(OutputStream out) throws IOException {

						try {
							FrameGrabber grabber = new OpenCVFrameGrabber(0);
							OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
							grabber.start();
							Frame frame = grabber.grab();
							grabber.stop();
							grabber.close();
							IplImage img = converter.convert(frame);
							frame.close();
							//byte[] bytes = IplImageToByteArray2(img);

							BufferedImage bufImage = toBufferedImage(img);
							byte[] bytes = toByteArray(bufImage, "jpg");

							//cvSaveImage("image.jpg", img);

							if (bytes != null) {
									getLogger().debug(String.format("read %d bytes from captured image file",
											new Object[] { bytes.length }));
									IOUtils.write(bytes, out);
							}
						} catch (Throwable e) {
							e.printStackTrace();
						}
					}
				});

				flowFile = session.putAttribute(flowFile, CoreAttributes.MIME_TYPE.key(), "image/jpeg");
				flowFile = session.putAttribute(flowFile, CoreAttributes.FILENAME.key(), fileNameToWrite);
				flowFile = session.putAttribute(flowFile, "imageoutputname", fileNameToWrite);

				if (wasError.get()) {
					session.transfer(flowFile, REL_FAILURE);
				} else {
					session.transfer(flowFile, REL_SUCCESS);
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new ProcessException(e);
			}

			session.commitAsync();
		} catch (

		final Throwable t) {
			System.out.println("Error");
			getLogger().error("Unable to process GetWebCamera Processor file " + t.getLocalizedMessage());
			throw new ProcessException(t);
		}
	}
}