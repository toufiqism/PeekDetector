package com.tofiq.peekdetector

import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

/**
 * Optimized face detection analyzer with frame skipping for battery efficiency
 * 
 * Battery optimizations:
 * - Frame skipping: Processes every 3rd frame (~10fps effective vs 30fps)
 * - Reduced CPU processing by ~66% while maintaining detection accuracy
 * - Proper image proxy cleanup prevents memory leaks
 */
class PeekDetectorAnalyzer(private val listener: (Int) -> Unit) : ImageAnalysis.Analyzer {

    // Battery optimization: Process every 3rd frame (SKIP_FRAMES = 3)
    // This reduces CPU usage by ~66% while maintaining detection responsiveness
    // Face detection doesn't need 30fps - 10fps is sufficient for security monitoring
    private var frameCounter = 0
    private companion object {
        private const val SKIP_FRAMES = 3 // Process every 3rd frame
    }

    private val highAccuracyOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST) // Already optimized for speed
        .setMinFaceSize(0.15f) // Minimum face size for detection (optimized threshold)
        .build()

    private val detector = FaceDetection.getClient(highAccuracyOpts)

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        frameCounter++
        
        // Battery optimization: Skip frames to reduce processing
        // Close skipped frames immediately to prevent memory buildup
        if (frameCounter % SKIP_FRAMES != 0) {
            imageProxy.close()
            return
        }
        
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            detector.process(image)
                .addOnSuccessListener { faces ->
                    listener(faces.size)
                    Log.d("PeekDetectorAnalyzer", "Face detected: ${faces.size}")
                }
                .addOnFailureListener { e ->
                    Log.e("PeekDetectorAnalyzer", "Face detection failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            // If mediaImage is null, close the proxy to prevent leaks
            imageProxy.close()
        }
    }
}