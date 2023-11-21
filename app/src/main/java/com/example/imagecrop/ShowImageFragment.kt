package com.example.imagecrop

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.imagecrop.databinding.FragmentShowImageBinding
import java.io.InputStream

class ShowImageFragment : Fragment() {
    private lateinit var imageUrl: String

    private lateinit var _binding: FragmentShowImageBinding
    val binding get() = _binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            imageUrl = it.getString("image_url").toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding= FragmentShowImageBinding.inflate(inflater, container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("imagefragment",imageUrl)
        binding.imageUrl.text=imageUrl
        binding.croppedImage.setImageURI(Uri.parse(imageUrl))
        val lengthOfFile= getFileSize(requireContext(),Uri.parse(imageUrl))

        binding.lengthOfFile.text= "${ (lengthOfFile / 1024f).toString() } KB"
    }


}


fun getFileSize(context: Context, uri: Uri): Long {
    var inputStream: InputStream? = null
    try {
        val contentResolver = context.contentResolver
        inputStream = contentResolver.openInputStream(uri)
        return inputStream?.available()?.toLong() ?: 0L
    } finally {
        inputStream?.close()
    }
}



//file:///storage/emulated/0/Android/data/com.example.imagecrop/files/Pictures/20231120_194044.jpeg