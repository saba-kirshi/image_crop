package com.example.imagecrop

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.example.imagecrop.databinding.FragmentFirstBinding
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropFragment
import com.yalantis.ucrop.UCropFragmentCallback
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get()  = _binding!!

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
        _binding=FragmentFirstBinding.inflate(inflater, container,false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getPermission()
        val action= FirstFragmentDirections.actionFirstFragmentToCameraFragment("")
        binding.camera.setOnClickListener {
            view.findNavController().navigate(action)
        }
        binding.gallery.setOnClickListener {
            pickImage()
        }
    }
    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val selectedImageUri: Uri? = data?.data
            selectedImageUri?.let { uri ->
                startUCrop(uri)
            }
        } else if (requestCode == CROP_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Handle the cropped image result
            val resultUri: Uri? = UCrop.getOutput(data!!)
            var action = FirstFragmentDirections.actionFirstFragmentToShowImageFragment(resultUri.toString())
            findNavController().navigate(action)
            // Do something with the cropped image URI
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
            .start(requireContext(), this, CROP_IMAGE_REQUEST_CODE)    }

    private fun getPermission() {
        val permissionList = mutableListOf<String>()
        if(ContextCompat.checkSelfPermission(
                requireContext().applicationContext,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED) permissionList.add(android.Manifest.permission.CAMERA)
        if(ContextCompat.checkSelfPermission(
                requireContext().applicationContext,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED) permissionList.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        if(ContextCompat.checkSelfPermission(
                requireContext().applicationContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED) permissionList.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if(permissionList.size > 0) requestPermissions(permissionList.toTypedArray(), 101)

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        grantResults.forEach {
            if(it != PackageManager.PERMISSION_GRANTED) getPermission()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding=null
    }
}