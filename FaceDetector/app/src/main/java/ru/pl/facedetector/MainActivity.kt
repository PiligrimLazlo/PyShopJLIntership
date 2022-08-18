package ru.pl.facedetector

import android.Manifest
import android.app.AlertDialog
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.frame.Frame
import ru.pl.facedetector.databinding.ActivityMainBinding
import kotlin.math.log

private const val TAG = "MainActivityTag"

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }

    private lateinit var bitmapSource: Bitmap
    private var effect = Effect.CONTOUR


    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.forEach {
                Toast.makeText(
                    this,
                    "${getString(R.string.permissions_granted)} ${it.key} : ${it.value}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestCameraPermission()

        bitmapSource = BitmapFactory.decodeResource(
            resources, R.drawable.glasses
        )

        val camera = setUpCamera()
        camera.previewFrameRate = 15f;

        binding.switchCamBtn.setOnClickListener {
            camera.facing = if (camera.facing == Facing.FRONT) Facing.BACK else Facing.FRONT
        }

        binding.changeEffectBtn.setOnClickListener {
            effect = if (effect == Effect.CONTOUR) Effect.GLASSES else Effect.CONTOUR
        }

    }

    private fun setUpCamera(): CameraView {
        return binding.cameraView.apply {
            setLifecycleOwner(this@MainActivity)
            addFrameProcessor { frame ->
                setupFrameProcessor(frame, facing)
            }
        }
    }

    private fun requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            showRationaleDialog(
                getString(R.string.error),
                getString(R.string.permission_did_not_granted)
            )
        } else {
            requestPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }

    private fun showRationaleDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(message)
            .setTitle(title)
            .setCancelable(false)
            .setPositiveButton("ОК") { dialog, _ ->
                dialog.dismiss()
            }
            .create().show()
    }

    private fun setupFrameProcessor(frame: Frame, facing: Facing) {
        val width = frame.size.width
        val height = frame.size.height

        val image = InputImage.fromByteArray(
            frame.getData(), width, height,
            if (facing == Facing.FRONT) 270 else 90,
            ImageFormat.NV21
        )
        val detectorOptions = FaceDetectorOptions.Builder()
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()
        val detector = FaceDetection.getClient(detectorOptions)

        val result = detector.process(image)
            .addOnSuccessListener {
                for (face in it) {
                    //for contour

                    if (effect == Effect.CONTOUR) {
                        val points = face.getContour(FaceContour.FACE)?.points ?: emptyList()
                        drawFaceContour(points, facing, height, width)
                    } else {
                        val leftEyeContours =
                            face.getContour(FaceContour.LEFT_EYE)?.points ?: emptyList()
                        val rightEyeContours =
                            face.getContour(FaceContour.RIGHT_EYE)?.points ?: emptyList()

                        drawGlasses(leftEyeContours, rightEyeContours, facing, height, width)
                    }
                }
            }
            .addOnFailureListener {
                Log.d(TAG, it.cause.toString())
            }
    }


    private fun drawGlasses(
        leftEyePoints: List<PointF>,
        rightEyePoints: List<PointF>,
        facing: Facing,
        heightFrame: Int,
        widthFrame: Int
    ) {
        val leftEyeX = leftEyePoints.first().x
        val leftEyeY = rightEyePoints.first().y
        val rightEyeX = rightEyePoints.first().x
        val rightEyeY = rightEyePoints.first().y

        val betweenEyes = rightEyeX - leftEyeX


        val bitmap = Bitmap.createBitmap(heightFrame, widthFrame, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val matrix = Matrix()
        if (facing == Facing.FRONT) {
            //simple variant without dynamic scale
            matrix.postScale(SCALE_FACTOR, SCALE_FACTOR)
            matrix.postTranslate(-leftEyeX + TRANSLATE_FACTOR, leftEyeY - TRANSLATE_FACTOR)
        } else {
            matrix.postScale(betweenEyes / TRANSLATE_FACTOR, betweenEyes / TRANSLATE_FACTOR)
            matrix.postTranslate(leftEyeX - (betweenEyes * 2.2f), leftEyeY - (betweenEyes * 2.2f))
        }

        canvas.drawBitmap(bitmapSource, matrix, Paint())

        val flippedBitmap = Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, bitmap.height,
            matrix, true
        )
        binding.faceImageView.setImageBitmap(flippedBitmap)
    }


    private fun drawFaceContour(
        points: List<PointF>,
        facing: Facing,
        heightFrame: Int,
        widthFrame: Int
    ) {

        val bitmap = Bitmap.createBitmap(heightFrame, widthFrame, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val dotPaint = Paint()
        dotPaint.color = Color.RED
        dotPaint.style = Paint.Style.FILL
        dotPaint.strokeWidth = 4F
        val linePaint = Paint()
        linePaint.color = Color.GREEN
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = 2F

        for ((i, contour) in points.withIndex()) {
            if (i != points.lastIndex)
                canvas.drawLine(contour.x, contour.y, points[i + 1].x, points[i + 1].y, linePaint)
            else
                canvas.drawLine(contour.x, contour.y, points[0].x, points[0].y, linePaint)
            canvas.drawCircle(contour.x, contour.y, 4F, dotPaint)
        }

        if (facing == Facing.FRONT) {
            val matrix = Matrix()
            //иначе неверно соотносится с лицом
            matrix.preScale(-1F, 1F)
            val flippedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0,
                bitmap.width, bitmap.height,
                matrix, true
            )
            binding.faceImageView.setImageBitmap(flippedBitmap)
        } else {
            binding.faceImageView.setImageBitmap(bitmap)
        }
    }


    companion object {
        private const val SCALE_FACTOR = 0.5f
        private const val TRANSLATE_FACTOR = 400
    }

    private enum class Effect {
        CONTOUR, GLASSES
    }

}