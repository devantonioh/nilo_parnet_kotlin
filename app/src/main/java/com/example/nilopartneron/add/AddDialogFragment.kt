package com.example.nilopartneron.add

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.nilopartneron.Constants
import com.example.nilopartneron.entities.EventPost
import com.example.nilopartneron.entities.Product
import com.example.nilopartneron.databinding.FragmentDialogAddBinding
import com.example.nilopartneron.product.MainAux
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

class AddDialogFragment : DialogFragment(), DialogInterface.OnShowListener {

    private var binding: FragmentDialogAddBinding? = null

    private var positiveButton: Button? = null
    private var negativeButton: Button? = null

    private var product: Product? = null

    private var photoSelectedUri: Uri? = null

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                photoSelectedUri = it.data?.data

                //binding?.imgProductPreview?.setImageURI(photoSelectedUri)
                binding?.let {
                    Glide.with(this)
                        .load(photoSelectedUri)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .into(it.imgProductPreview)
                }
            }
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            binding = FragmentDialogAddBinding.inflate(LayoutInflater.from(context))

            binding?.let {
                val builder = AlertDialog.Builder(activity)
                    .setTitle("Agregar Producto")
                    .setPositiveButton("Agregar", null)
                    .setNegativeButton("Cancelar", null)
                    .setView(it.root)

                val dialog = builder.create()
                dialog.setOnShowListener(this)

                return dialog
            }
        }
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onShow(dialogInterface: DialogInterface?) {

        initProduct()

        configButtons()

        val dialog = dialog as? AlertDialog
        dialog?.let {
            positiveButton = it.getButton(Dialog.BUTTON_POSITIVE)
            negativeButton = it.getButton(Dialog.BUTTON_NEGATIVE)

            product?.let {  positiveButton?.setText("Actualizar")}

            positiveButton?.setOnClickListener {
                binding?.let {
                    enableUI(false)

                    //uploadImage(product?.id) { eventPost ->
                    uploadReducedImage(product?.id, product?.imgUrl) { eventPost ->
                        if (eventPost.isSuccess) {
                            if (product == null) {
                                val product = Product(
                                    name = it.etName.text.toString().trim(),
                                    description = it.etDescription.text.toString().trim(),
                                    imgUrl = eventPost.photoUrl,
                                    quantity = it.etQuantity.text.toString().toInt(),
                                    price = it.etPrice.text.toString().toDouble(),
                                    sellerId= eventPost.sellerId
                                )

                                save(product, eventPost.documentId!!)
                            } else {
                                product?.apply {
                                    name = it.etName.text.toString().trim()
                                    description = it.etDescription.text.toString().trim()
                                    imgUrl = eventPost.photoUrl
                                    quantity = it.etQuantity.text.toString().toInt()
                                    price = it.etPrice.text.toString().toDouble()

                                    update(this)
                                }
                            }

                        }
                    }

                }

            }
            negativeButton?.setOnClickListener {
                dismiss()
            }
        }
    }

    private fun initProduct() {
        product = (activity as? MainAux)?.getProductSelected()
        product?.let { product ->
            binding?.let {
                dialog?.setTitle("Actualizar Producto")

                it.etName.setText(product.name)
                it.etDescription.setText(product.description)
                it.etQuantity.setText(product.quantity.toString())
                it.etPrice.setText(product.price.toString())

                Glide.with(this)
                    .load(product.imgUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(it.imgProductPreview)
            }
        }
    }

    private fun configButtons() {
        binding?.let {
            it.ibProduct.setOnClickListener {
                openGallery()
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultLauncher.launch(intent)
    }

    private fun uploadImage(productId: String?, callback: (EventPost) -> Unit) {
        val eventPost = EventPost()
        eventPost.documentId =
            productId ?: FirebaseFirestore.getInstance()
                .collection(Constants.COLL_PRODUCTS).document().id
        val storageRef =
            FirebaseStorage.getInstance().reference.child(Constants.PATH_PRODUCT_IMAGES)

        photoSelectedUri?.let { uri ->
            binding?.let { binding ->
                binding.progressBar.visibility = View.VISIBLE
                val photoRef = storageRef.child(eventPost.documentId!!)

                photoRef.putFile(uri)
                    .addOnProgressListener {
                        val progress = (100 * it.bytesTransferred / it.totalByteCount).toInt()
                        it.run {
                            binding.progressBar.progress = progress
                            binding.tvProgress.text = String.format("%s%%", progress)
                        }
                    }
                    .addOnSuccessListener {
                        it.storage.downloadUrl.addOnSuccessListener { downloadUrl ->
                            Log.i("URL", downloadUrl.toString())
                            eventPost.isSuccess = true
                            eventPost.photoUrl = downloadUrl.toString()
                            callback(eventPost)
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(activity, "Error al Subir imagen", Toast.LENGTH_SHORT).show()
                        enableUI(true)

                        eventPost.isSuccess = false
                        callback(eventPost)
                    }
            }
        }
    }

    private fun uploadReducedImage(productId: String?, imageUrl: String?, callback: (EventPost) -> Unit) {
        val eventPost = EventPost()
        imageUrl?.let { eventPost.photoUrl= it }
        eventPost.documentId =
            productId ?: FirebaseFirestore.getInstance()
                .collection(Constants.COLL_PRODUCTS).document().id

        FirebaseAuth.getInstance().currentUser?.let { user ->
            val  imageRef =
                FirebaseStorage.getInstance().reference.child(user.uid)
                    .child(Constants.PATH_PRODUCT_IMAGES)
            val photoRef = imageRef.child(eventPost.documentId!!).child("image0")

            eventPost.sellerId = user.uid

            //photoSelectedUri?.let { uri ->
            if (photoSelectedUri == null){
                eventPost.isSuccess = true
                callback(eventPost)
            }else {
                binding?.let { binding ->
                    getBitmapFromUri(photoSelectedUri!!)?.let {bitmap ->
                        binding.progressBar.visibility = View.VISIBLE

                        val baos = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)

                        photoRef.putBytes(baos.toByteArray())
                            .addOnProgressListener {
                                val progress = (100 * it.bytesTransferred / it.totalByteCount).toInt()
                                it.run {
                                    binding.progressBar.progress = progress
                                    binding.tvProgress.text = String.format("%s%%", progress)
                                }
                            }
                            .addOnSuccessListener {
                                it.storage.downloadUrl.addOnSuccessListener { downloadUrl ->
                                    Log.i("URL", downloadUrl.toString())
                                    eventPost.isSuccess = true
                                    eventPost.photoUrl = downloadUrl.toString()
                                    callback(eventPost)
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(activity, "Error al Subir imagen", Toast.LENGTH_SHORT).show()
                                enableUI(true)

                                eventPost.isSuccess = false
                                callback(eventPost)
                            }

                    }
                }
            }
        }
    }

    private  fun getBitmapFromUri(uri: Uri): Bitmap?{
        activity?.let {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(it.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else{
                MediaStore.Images.Media.getBitmap(it.contentResolver, uri)
            }

            return  getResizedImage(bitmap, 320)

        }
        return null
    }

    private fun getResizedImage(image: Bitmap, maxSize: Int): Bitmap{
        var width = image.width
        var height = image.height
        if (width <= maxSize && height <= maxSize) return  image

        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1){
            width = maxSize
            height = (width / bitmapRatio).toInt()
        }else{
            height = maxSize
            width  =  (height / bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(image, width, height, true)
    }

    private fun save(product: Product, documentId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection(Constants.COLL_PRODUCTS)
            .document(documentId)
            .set(product)
            //.add(product)
            .addOnSuccessListener {
                Toast.makeText(activity, "Producto añadido", Toast.LENGTH_SHORT).show()
                dismiss()
            }
            .addOnFailureListener {
                Toast.makeText(activity, "Error al insertar", Toast.LENGTH_SHORT).show()
                enableUI(true)
            }
            .addOnCompleteListener {
                binding?.progressBar?.visibility = View.INVISIBLE
            }

    }

    private fun update(product: Product) {
        val db = FirebaseFirestore.getInstance()

        product.id?.let { id ->
            db.collection(Constants.COLL_PRODUCTS)
                .document(id)
                .set(product)
                .addOnSuccessListener {
                    Toast.makeText(activity, "Producto actualizado", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(activity, "Error al actualizar", Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener {
                    enableUI(true)
                    binding?.progressBar?.visibility = View.INVISIBLE
                    dismiss()
                }
        }


    }

    private fun enableUI(enable: Boolean) {
        positiveButton?.isEnabled = enable
        negativeButton?.isEnabled = enable
        binding?.let {
            with(it) {
                etName.isEnabled = enable
                etDescription.isEnabled = enable
                etQuantity.isEnabled = enable
                etPrice.isEnabled = enable
                progressBar.visibility = if (enable)View.INVISIBLE else View.VISIBLE
                tvProgress.visibility = if (enable)View.INVISIBLE else View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}