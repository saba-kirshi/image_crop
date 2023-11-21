package com.example.imagecrop

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.camera2.*
import android.media.ImageReader
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Surface
import android.view.SurfaceHolder
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.ContextCompat.getExternalFilesDirs
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.imagecrop.databinding.FragmentCameraBinding
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropFragment
import com.yalantis.ucrop.UCropFragment.UCropResult
import com.yalantis.ucrop.UCropFragmentCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CameraFragment : Fragment() {

    var _binding: FragmentCameraBinding? = null
    val binding get() = _binding!!

    lateinit var cameraManager: CameraManager
    lateinit var textureView: TextureView
    lateinit var cameraCaptureSession: CameraCaptureSession
    lateinit var cameraDevice: CameraDevice
    lateinit var captureRequest: CaptureRequest.Builder
    lateinit var handler: Handler
    lateinit var handlerThread: HandlerThread
    lateinit var imageReader: ImageReader



    companion object {
        const val PICK_IMAGE_REQUEST_CODE = 123
        const val CROP_IMAGE_REQUEST_CODE = UCrop.REQUEST_CROP
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding=FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
       }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        textureView= binding.textureView
        textureView.surfaceTextureListener = object: TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
            openCamera()
            }

            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
            return false
            }

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
            }

        }
        val dir=requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageUrl="${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpeg"

        imageReader = ImageReader.newInstance(1080,1920, ImageFormat.JPEG,1)
        imageReader.setOnImageAvailableListener(object : ImageReader.OnImageAvailableListener{
            override fun onImageAvailable(p0: ImageReader?) {
                val image = p0?.acquireLatestImage()
                val buffer = image!!.planes[0].buffer
                val bytes=ByteArray(buffer.remaining())
                buffer.get(bytes)
                val file= File(dir,imageUrl)
                val opStream= FileOutputStream(file)
                opStream.write(bytes)
                opStream.close()
                image.close()
                val url= Uri.fromFile(File(dir,imageUrl))
                startUCrop(url)
            }
        },handler)

        binding.captureButton.setOnClickListener {
            captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureRequest.addTarget(imageReader.surface)
            cameraCaptureSession.capture(captureRequest.build(), null, null)

            val url= Uri.fromFile(File(dir,imageUrl))
            Log.d("camerafragment",url.toString())


        }
    }

    private fun startUCrop(sourceUri: Uri) {
        var dir=requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val destinationUri = Uri.fromFile(File(dir, "${
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(
                Date()
            )}.jpeg"))

        Log.d("destination", destinationUri.toString())

        val options = UCrop.Options().apply {
            setCompressionQuality(80) // Adjust compression quality
            setHideBottomControls(true)
            setFreeStyleCropEnabled(false)
        }

        UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1F, 1F)
            .withOptions(options)
            .start(requireContext(), this, CameraFragment.CROP_IMAGE_REQUEST_CODE)

    }


    @SuppressLint("MissingPermission")
    private fun openCamera() {
        cameraManager.openCamera(cameraManager.cameraIdList[0], object: CameraDevice.StateCallback(){
            override fun onOpened(p0: CameraDevice) {
                cameraDevice=p0
                captureRequest= cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                var surface=Surface(textureView.surfaceTexture)
                captureRequest.addTarget(surface)
                cameraDevice.createCaptureSession(listOf(surface,imageReader.surface), object: CameraCaptureSession.StateCallback(){
                    override fun onConfigured(p0: CameraCaptureSession) {
                        cameraCaptureSession=p0
                        cameraCaptureSession.setRepeatingRequest(captureRequest.build(),null,null)
                    }
                    override fun onConfigureFailed(p0: CameraCaptureSession) {
                    }
                },handler)

            }

            override fun onDisconnected(p0: CameraDevice) {
            }

            override fun onError(p0: CameraDevice, p1: Int) {
             }
        }, handler)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CameraFragment.PICK_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val selectedImageUri: Uri? = data?.data
            selectedImageUri?.let { uri ->
                startUCrop(uri)
            }
        } else if (requestCode == CameraFragment.CROP_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Handle the cropped image result
            val resultUri: Uri? = UCrop.getOutput(data!!)
            var action = CameraFragmentDirections.actionCameraFragmentToShowImageFragment(resultUri.toString())
            findNavController().navigate(action)
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        _binding=null
    }
}












