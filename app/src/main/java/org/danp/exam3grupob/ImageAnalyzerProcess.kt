package org.danp.exam3grupob

import android.annotation.SuppressLint
import android.graphics.*
import android.media.Image
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.experimental.and

class ImageAnalyzerProcess(
) : ImageAnalysis.Analyzer {

    var listener: ((result: Bitmap?) -> Unit)? = null

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array
        return data // Return the byte array
    }

    fun Image.toBitmap(): Bitmap {
        val yBuffer = planes[0].buffer // Y
        val uBuffer = planes[1].buffer // U
        val vBuffer = planes[2].buffer // VU

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun rotateImage(bitmap: Bitmap): Bitmap {
        val matrix = Matrix();
        matrix.postRotate(90f);

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width, bitmap.height, true);

        val rotatedBitmap = Bitmap.createBitmap(
            scaledBitmap,
            0,
            0,
            scaledBitmap.getWidth(),
            scaledBitmap.getHeight(),
            matrix,
            true
        );

        return rotatedBitmap
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {

//        val buffer = imageProxy.planes[0].buffer
//        val data = buffer.toByteArray()
//        val pixels = data.map { it.toInt() and 0xFF }

        //option 1: YUV_420_888 format
        var bitmap = imageProxy.image?.toBitmap()
        bitmap = bitmap?.let { rotateImage(it) }
        val width = bitmap!!.width - 1;
        val height = bitmap!!.height -1 ;

        var histogramMap: HashMap<Int, Int> = hashMapOf(0 to 0,1 to 0)

        for (x in 0..width) {
            for (y in 0..height) {
                var pixel = bitmap?.getPixel(x, y)
                var red = pixel?.let { Color.red(it) }
//                var green = pixel?.let { Color.green(it) }
//                var blue = pixel?.let { Color.blue(it) }

                red?.let {
                    if (histogramMap.containsKey(it)){
                        var count = histogramMap.get(it)!!.toInt()
                        count = count + 1
                        histogramMap.put(it,count)
                    }
                    else{
                        histogramMap.put(it,1)
                    }
                }


            }
        }

        Log.d("TAG",histogramMap.toString())


//        if (imageProxy.format == PixelFormat.RGBA_8888) {
//            val buffer = imageProxy.planes[0].buffer
//            val data=buffer.toByteArray()
//            val pixels = data.map { it.toInt() and 0xFF }
//
//            val R = buffer.get(0).toInt() and 0xFF
//            val G = buffer.get(1).toInt() and 0xFF
//            val B = buffer.get(2).toInt() and 0xFF
//        }


        listener?.invoke(bitmap)


        imageProxy.close()

    }

}
