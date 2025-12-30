package com.tofiq.peekdetector

import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.tofiq.peekdetector.data.SensitivityLevel

/**
 * Optimized face detection analyzer with configurable frame skipping for battery efficiency.
 * 
 * Battery optimizations:
 * - Configurable frame skipping based on sensitivity level
 * - Reduced CPU processing while maintaining detection accuracy
 * - Proper image proxy cleanup prevents memory leaks
 * 
 * @param listener Callback invoked with the number of faces detected
 * @param getFrameSkip Lambda to get the current frame skip value from settings.
 *                     Defaults to MEDIUM sensitivity (every 3rd frame).
 * 
 * Requirements: 2.2, 2.3, 2.4
 */
class PeekDetectorAnalyzer(
    private val listener: (Int) -> Unit,
    private val getFrameSkip: () -> Int = { SensitivityLevel.MEDIUM.frameSkip }
) : ImageAnalysis.Analyzer {

    // Frame counter for skip logic
    private var frameCounter = 0

    private val highAccuracyOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST) // Already optimized for speed
        .setMinFaceSize(0.15f) // Minimum face size for detection (optimized threshold)
        .build()

    private val detector = FaceDetection.getClient(highAccuracyOpts)

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        frameCounter++
        
        // Get current frame skip value from settings
        val skipFrames = getFrameSkip()
        
        // Battery optimization: Skip frames to reduce processing
        // Close skipped frames immediately to prevent memory buildup
        if (frameCounter % skipFrames != 0) {
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