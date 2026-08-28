package com.ce11kjw.junkclean;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * 人脸检测（OpenCV Haar 级联，纯本地）。
 * 用于 AI 智能保留策略的人脸加分项：
 *   含人脸的重复照片更可能被保留（人脸照片通常更珍贵）。
 */
public final class FaceDetector {

    private static volatile CascadeClassifier cascade;
    private static volatile boolean loaded;

    private FaceDetector() {}

    /** 从 res/raw 复制级联文件到私有目录并加载 */
    public static synchronized void init(android.content.Context c) {
        if (loaded) return;
        try {
            File dir = new File(c.getFilesDir(), "cv");
            if (!dir.exists()) dir.mkdirs();
            File cascadeFile = new File(dir, "frontalface.xml");
            if (!cascadeFile.exists()) {
                InputStream in = c.getResources().openRawResource(
                        c.getResources().getIdentifier("frontalface", "raw", c.getPackageName()));
                FileOutputStream out = new FileOutputStream(cascadeFile);
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                out.close();
                in.close();
            }
            cascade = new CascadeClassifier(cascadeFile.getAbsolutePath());
            loaded = cascade != null;
        } catch (Throwable t) {
            loaded = false;
        }
    }

    /** 统计图片中的人脸数（最多返回 3，够用） */
    public static int countFaces(android.content.Context c, Bitmap bmp) {
        if (!loaded) init(c);
        if (!loaded || cascade == null) return 0;
        if (bmp == null || bmp.getWidth() < 40 || bmp.getHeight() < 40) return 0;
        try {
            Mat mat = new Mat();
            Utils.bitmapToMat(bmp, mat);
            // 缩到最大 400px 宽，加速检测
            if (mat.cols() > 400) {
                double scale = 400.0 / mat.cols();
                Mat small = new Mat();
                org.opencv.core.Size sz = new Size(400, mat.rows() * scale);
                Imgproc.resize(mat, small, sz);
                mat.release();
                mat = small;
            }
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY);
            MatOfRect faces = new MatOfRect();
            cascade.detectMultiScale(mat, faces, 1.15, 3, 0,
                    new Size(30, 30), new Size(400, 400));
            Rect[] arr = faces.toArray();
            faces.release();
            mat.release();
            return Math.min(arr.length, 3);
        } catch (Throwable t) {
            return 0;
        }
    }
}
