package com.dataflowdeveloper.processors.GetWebCamera;

import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.IplImage;
import static org.bytedeco.opencv.helper.opencv_imgcodecs.cvSaveImage;
import java.awt.image.DataBufferByte;
import  java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

//import java.awt.event.WindowEvent;
//import java.awt.event.WindowEvent;
//import javax.swing.JFrame;


public class CVLoad {

    public static byte[] IplImageToByteArray(IplImage src) {
        ByteBuffer byteBuffer = src.getByteBuffer();
        byte[] barray = new byte[byteBuffer.remaining()];
        byteBuffer.get(barray);
        return barray;
    }

    public static byte[] IplImageToByteArray2(IplImage src) {
        byte[] barray = new byte[src.imageSize()];
        src.getByteBuffer().get(barray);
        return barray;
    }

    /*

    -Djavacpp.platform=macosx-arm64

     */
    public static void main(String args[]) {

        try {
            //CanvasFrame canvas = new CanvasFrame("Web Cam");
            //canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            FrameGrabber grabber = new OpenCVFrameGrabber(0);
            OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();

            grabber.start();
            Frame frame = grabber.grab();

            IplImage img = converter.convert(frame);
            cvSaveImage("selfie1.jpg", img);
            frame.close();

            //IplImage
            //canvas.showImage(frame);

            //Thread.sleep(2000);
            //canvas.dispatchEvent(new WindowEvent(canvas, WindowEvent.WINDOW_CLOSING));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
