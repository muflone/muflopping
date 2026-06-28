package com.muflone.muflopping.ui.detail.fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.muflone.muflopping.R
import com.muflone.muflopping.data.api.RetrofitClient
import com.muflone.muflopping.data.model.Product
import com.muflone.muflopping.data.model.ProductCategory
import com.muflone.muflopping.data.repository.ShoppingRepository
import com.muflone.muflopping.databinding.FragmentProductPickerBinding
import com.muflone.muflopping.ui.detail.ListDetailViewModel
import com.muflone.muflopping.ui.product.ProductAdapter
import com.muflone.muflopping.ui.product.ProductListItem
import com.muflone.muflopping.ui.product.ProductViewModel
import com.muflone.muflopping.util.SettingsManager
import com.muflone.muflopping.util.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ProductPickerFragment : Fragment() {

    private var _binding: FragmentProductPickerBinding? = null
    private val binding get() = _binding!!
    private lateinit var settingsManager: SettingsManager

    private val viewModel: ProductViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val context = requireContext().applicationContext
                val tokenManager = TokenManager(context)
                val settingsManager = SettingsManager(context)
                val repository = ShoppingRepository(tokenManager, settingsManager)
                return ProductViewModel(repository) as T
            }
        }
    }

    private val listViewModel: ListDetailViewModel by activityViewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val context = requireContext().applicationContext
                val tokenManager = TokenManager(context)
                val settingsManager = SettingsManager(context)
                val repository = ShoppingRepository(tokenManager, settingsManager)
                return ListDetailViewModel(repository) as T
            }
        }
    }

    private lateinit var adapter: ProductAdapter
    private var listId: Int = -1
    private var productToItemMap: Map<Int, Int> = emptyMap()
    private var allCategories: List<ProductCategory> = emptyList()
    private var allProducts: List<Product> = emptyList()
    private var isGridView: Boolean = false
    private var selectedCategoryName: String? = null

    private var selectedImageUri: Uri? = null
    private var tempCameraUri: Uri? = null
    private var dialogProductImageView: ImageView? = null

    private val backPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            showCategories()
        }
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            dialogProductImageView?.apply {
                setImageURI(it)
                visibility = View.VISIBLE
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempCameraUri?.let { uri ->
                selectedImageUri = uri
                dialogProductImageView?.apply {
                    setImageURI(uri)
                    visibility = View.VISIBLE
                }
            }
        }
    }

    private val imageSearchLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUrl = result.data?.getStringExtra(com.muflone.muflopping.ui.product.ImageSearchActivity.EXTRA_IMAGE_URL)
            if (imageUrl != null) {
                downloadImage(imageUrl)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settingsManager = SettingsManager(requireContext())

        listId = arguments?.getInt(ARG_LIST_ID, -1) ?: -1

        setupRecyclerView()
        observeViewModel()

        binding.llCategoryHeader.setOnClickListener {
            showCategories()
        }

        binding.btnBackToCategories.setOnClickListener {
            showCategories()
        }

        binding.fabAddProduct.setOnClickListener {
            showProductDialog(null)
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.fetchProducts()
            viewModel.fetchGlobalCategories()
        }

        viewModel.fetchProducts()
        viewModel.fetchGlobalCategories()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)

        binding.rvProducts.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                if (!isGridView) {
                    updateVisibleCategory()
                }
            }
        })
    }

    private fun updateVisibleCategory() {
        val layoutManager = binding.rvProducts.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager ?: return
        val firstVisiblePos = layoutManager.findFirstVisibleItemPosition()

        if (firstVisiblePos != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
            for (i in firstVisiblePos downTo 0) {
                val item = adapter.currentList.getOrNull(i)
                if (item is ProductListItem.CategoryItem) {
                    selectedCategoryName = item.categoryName
                    break
                }
            }
        }
    }

    fun toggleViewMode() {
        isGridView = !isGridView
        if (isGridView) {
            selectedCategoryName = null
            binding.llCategoryHeader.visibility = View.GONE
            backPressedCallback.isEnabled = false
        }
        updateLayoutManager()
        adapter.setViewMode(isGridView)
        updateDisplayList()
    }

    fun isGridView() = isGridView

    fun reload() {
        viewModel.fetchProducts()
        viewModel.fetchGlobalCategories()
    }

    private var lastClickedProductId: Int = -1

    private fun setupRecyclerView() {
        adapter = ProductAdapter(
            onCategoryClick = { categoryName ->
                if (isGridView) {
                    showProductsForCategory(categoryName)
                }
            },
            onProductClick = { product ->
                lastClickedProductId = product.id
                val itemId = productToItemMap[product.id]
                if (itemId != null) {
                    listViewModel.deleteItemFromList(listId, itemId)
                } else {
                    viewModel.addProductToList(listId, product.id)
                }
            },
            onProductLongClick = { product ->
                showProductOptionsDialog(product)
            }
        )
        adapter.setViewMode(isGridView)
        binding.rvProducts.adapter = adapter
        binding.rvProducts.itemAnimator = null
        updateLayoutManager()
    }

    private fun updateLayoutManager() {
        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        val spanCount = (screenWidthDp / 80).toInt().coerceAtMost(4).coerceAtLeast(3)

        if (isGridView) {
            binding.rvProducts.layoutManager = GridLayoutManager(context, spanCount)
        } else {
            binding.rvProducts.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        }
    }

    private fun showCategories() {
        selectedCategoryName = null
        binding.llCategoryHeader.visibility = View.GONE
        val categoryNames = allProducts.map { it.categoryName }.distinct().sorted()
        adapter.submitList(categoryNames.map { ProductListItem.CategoryItem(it) })
        backPressedCallback.isEnabled = false
    }

    private fun showProductsForCategory(categoryName: String) {
        selectedCategoryName = categoryName
        binding.tvCurrentCategory.text = categoryName
        binding.llCategoryHeader.visibility = View.VISIBLE
        val filteredProducts = allProducts.filter { it.categoryName == categoryName }.sortedBy { it.name }
        adapter.submitList(filteredProducts.map { ProductListItem.ProductItem(it) })
        backPressedCallback.isEnabled = true
    }

    private fun updateDisplayList() {
        if (isGridView) {
            if (selectedCategoryName != null) {
                showProductsForCategory(selectedCategoryName!!)
            } else {
                showCategories()
            }
        } else {
            selectedCategoryName = null
            binding.llCategoryHeader.visibility = View.GONE
            backPressedCallback.isEnabled = false
            val grouped = allProducts.groupBy { it.categoryName }.toSortedMap()
            val listItems = mutableListOf<ProductListItem>()
            grouped.forEach { (categoryName, productList) ->
                listItems.add(ProductListItem.CategoryItem(categoryName))
                productList.sortedBy { it.name }.forEach { product ->
                    listItems.add(ProductListItem.ProductItem(product))
                }
            }
            adapter.submitList(listItems)
        }
    }

    private fun showProductOptionsDialog(product: Product) {
        val options = arrayOf("Edit", "Delete")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(product.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showProductDialog(product)
                    1 -> viewModel.deleteProduct(product.id)
                }
            }
            .show()
    }

    private fun showProductDialog(product: Product?) {
        selectedImageUri = null
        tempCameraUri = null
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_product_edit, null)
        val etName = dialogView.findViewById<EditText>(R.id.etProductName)
        val actvCategory = dialogView.findViewById<AutoCompleteTextView>(R.id.actvCategory)
        dialogProductImageView = dialogView.findViewById(R.id.ivProductImage)
        val btnGallery = dialogView.findViewById<Button>(R.id.btnGallery)
        val btnCamera = dialogView.findViewById<Button>(R.id.btnCamera)
        val btnSearch = dialogView.findViewById<Button>(R.id.btnSearch)

        etName.setText(product?.name)

        if (product?.image != null) {
            dialogProductImageView?.load(settingsManager.getFullImageUrl(product.image))
        }
        
        val categoryNames = allCategories.map { it.name }
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames)
        actvCategory.setAdapter(categoryAdapter)
        
        if (product != null) {
            val currentCategory = allCategories.find { it.id == product.category }
            actvCategory.setText(currentCategory?.name, false)
        } else if (selectedCategoryName != null) {
            actvCategory.setText(selectedCategoryName, false)
        }

        val isGlobal = product?.isGlobal ?: false
        if (isGlobal) {
            btnGallery.isEnabled = false
            btnCamera.isEnabled = false
            btnSearch.isEnabled = false
            etName.isEnabled = false
            actvCategory.isEnabled = false
        }

        btnGallery.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        btnCamera.setOnClickListener {
            val file = File(requireContext().cacheDir, "camera_temp_${UUID.randomUUID()}.jpg")
            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        }

        btnSearch.setOnClickListener {
            val query = etName.text.toString().ifBlank { product?.name ?: "" }
            if (query.isNotBlank()) {
                val intent = Intent(requireContext(), com.muflone.muflopping.ui.product.ImageSearchActivity::class.java).apply {
                    putExtra(com.muflone.muflopping.ui.product.ImageSearchActivity.EXTRA_QUERY, query)
                }
                imageSearchLauncher.launch(intent)
            } else {
                Toast.makeText(context, "Please enter a product name to search", Toast.LENGTH_SHORT).show()
            }
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString()
                val categoryName = actvCategory.text.toString()
                val selectedCategory = allCategories.find { it.name == categoryName }

                if (name.isNotBlank() && selectedCategory != null) {
                    val imagePart = prepareImagePart()
                    if (product == null) {
                        viewModel.createProduct(name, selectedCategory.id, imagePart)
                    } else {
                        viewModel.updateProduct(product.id, name, selectedCategory.id, imagePart)
                    }
                } else {
                    Toast.makeText(context, "Name and category are required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        dialog.show()

        etName.requestFocus()
        val text = etName.text
        if (!text.isNullOrEmpty()) {
            etName.setSelection(text.length)
        }
    }

    private fun downloadImage(imageUrl: String) {
        if (!imageUrl.startsWith("http")) {
            Toast.makeText(context, "Invalid image URL", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(imageUrl).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val file = File(requireContext().cacheDir, "downloaded_product_image_${UUID.randomUUID()}.jpg")
                    val outputStream = FileOutputStream(file)
                    response.body?.byteStream()?.use { input ->
                        input.copyTo(outputStream)
                    }
                    outputStream.close()
                    
                    withContext(Dispatchers.Main) {
                        val uri = Uri.fromFile(file)
                        selectedImageUri = uri
                        dialogProductImageView?.apply {
                            setImageURI(uri)
                            visibility = View.VISIBLE
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to download image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun prepareImagePart(): MultipartBody.Part? {
        val uri = selectedImageUri ?: return null
        
        return try {
            val file = File(requireContext().cacheDir, "temp_product_image.jpg")
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()

            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("image", file.name, requestFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun observeViewModel() {
        viewModel.products.observe(viewLifecycleOwner) { result ->
            binding.progressBar.visibility = View.GONE
            binding.swipeRefresh.isRefreshing = false
            if (result.isSuccess) {
                allProducts = result.getOrNull() ?: emptyList()
                updateDisplayList()
            } else {
                Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.categories.observe(viewLifecycleOwner) { result ->
            if (result.isSuccess) {
                allCategories = result.getOrNull() ?: emptyList()
            }
        }

        viewModel.itemAdded.observe(viewLifecycleOwner) { result ->
            if (result.isSuccess) {
                Toast.makeText(context, "Product added to list", Toast.LENGTH_SHORT).show()
                if (lastClickedProductId != -1) {
                    adapter.markAsAdded(lastClickedProductId)
                    lastClickedProductId = -1
                }
                listViewModel.fetchListDetail(listId)
            } else {
                Toast.makeText(context, "Failed to add product: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }

        listViewModel.operationResult.observe(viewLifecycleOwner) { result ->
            if (result.isSuccess) {
                if (lastClickedProductId != -1) {
                    Toast.makeText(context, "Product removed from list", Toast.LENGTH_SHORT).show()
                    lastClickedProductId = -1
                }
            } else {
                Toast.makeText(context, "Operation failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.productOperationResult.observe(viewLifecycleOwner) { result ->
            if (result.isSuccess) {
                Toast.makeText(context, "Operation successful", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Operation failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }

        listViewModel.listDetail.observe(viewLifecycleOwner) { result ->
            if (result.isSuccess) {
                val detail = result.getOrNull()
                productToItemMap = detail?.items?.associate { it.product to it.id } ?: emptyMap()
                adapter.setInitialAddedProducts(productToItemMap.keys.toList())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_LIST_ID = "arg_list_id"

        fun newInstance(listId: Int) = ProductPickerFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_LIST_ID, listId)
            }
        }
    }
}
