package com.muflone.muflopping.ui.product

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.muflone.muflopping.R
import com.muflone.muflopping.data.api.RetrofitClient
import com.muflone.muflopping.data.model.Product
import com.muflone.muflopping.data.model.ProductCategory
import com.muflone.muflopping.data.repository.ShoppingRepository
import com.muflone.muflopping.databinding.ActivityProductBinding
import com.muflone.muflopping.util.SettingsManager
import com.muflone.muflopping.util.ThemeUtils
import com.muflone.muflopping.util.TokenManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductBinding
    private lateinit var settingsManager: SettingsManager
    private val viewModel: ProductViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val tokenManager = TokenManager(applicationContext)
                val settingsManager = SettingsManager(applicationContext)
                val repository = ShoppingRepository(tokenManager, settingsManager)
                return ProductViewModel(repository) as T
            }
        }
    }

    private lateinit var adapter: ProductAdapter
    private var listId: Int = -1
    private var existingProductIds: List<Int> = emptyList()
    private var allCategories: List<ProductCategory> = emptyList()
    private var allProducts: List<Product> = emptyList()
    private var allUnits: List<com.muflone.muflopping.data.model.ProductUnit> = emptyList()
    private var isGridView: Boolean = false
    private var selectedCategoryName: String? = null

    private var selectedImageUri: Uri? = null
    private var tempCameraUri: Uri? = null
    private var dialogProductImageView: ImageView? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this, noActionBar = true)
        ThemeUtils.updateLastTheme(this)
        settingsManager = SettingsManager(this)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        ViewCompat.setOnApplyWindowInsetsListener(binding.productRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        listId = intent.getIntExtra(EXTRA_LIST_ID, -1)
        existingProductIds = intent.getIntegerArrayListExtra(EXTRA_EXISTING_PRODUCT_IDS) ?: emptyList()

        if (listId == -1) {
            Toast.makeText(this, "Invalid list ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        title = "Pick Product"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        observeViewModel()

        binding.fabAddProduct.setOnClickListener {
            showProductDialog(null)
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.fetchProducts()
            viewModel.fetchGlobalCategories()
            viewModel.fetchUnits()
        }

        viewModel.fetchProducts()
        viewModel.fetchGlobalCategories()
        viewModel.fetchUnits()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isGridView && selectedCategoryName != null) {
                    showCategories()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        binding.rvProducts.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                if (!isGridView) {
                    updateVisibleCategory()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (ThemeUtils.isThemeChanged(this)) {
            recreate()
        }
    }

    private fun updateVisibleCategory() {
        val layoutManager = binding.rvProducts.layoutManager as? LinearLayoutManager ?: return
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            if (isGridView && selectedCategoryName != null) {
                showCategories()
                return true
            }
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
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
                viewModel.addProductToList(listId, product.id)
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
            binding.rvProducts.layoutManager = GridLayoutManager(this, spanCount)
        } else {
            binding.rvProducts.layoutManager = LinearLayoutManager(this)
        }
    }

    private fun showCategories() {
        selectedCategoryName = null
        val categoryNames = allProducts.map { it.categoryName }.distinct().sorted()
        adapter.submitList(categoryNames.map { ProductListItem.CategoryItem(it) })
    }

    private fun showProductsForCategory(categoryName: String) {
        selectedCategoryName = categoryName
        val filteredProducts = allProducts.filter { it.categoryName == categoryName }.sortedBy { it.name }
        adapter.submitList(filteredProducts.map { ProductListItem.ProductItem(it) })
    }

    private fun updateDisplayList() {
        if (isGridView) {
            if (selectedCategoryName != null) {
                showProductsForCategory(selectedCategoryName!!)
            } else {
                showCategories()
            }
        } else {
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

    fun toggleViewMode() {
        isGridView = !isGridView
        if (isGridView) {
            selectedCategoryName = null
        }
        updateLayoutManager()
        adapter.setViewMode(isGridView)
        updateDisplayList()
    }

    private fun showProductOptionsDialog(product: Product) {
        val options = arrayOf("Edit", "Delete")
        MaterialAlertDialogBuilder(this)
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
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_product_edit, null)
        val etName = dialogView.findViewById<EditText>(R.id.etProductName)
        val actvCategory = dialogView.findViewById<AutoCompleteTextView>(R.id.actvCategory)
        val actvUnit = dialogView.findViewById<AutoCompleteTextView>(R.id.actvUnit)
        dialogProductImageView = dialogView.findViewById(R.id.ivProductImage)
        val btnGallery = dialogView.findViewById<Button>(R.id.btnGallery)
        val btnCamera = dialogView.findViewById<Button>(R.id.btnCamera)
        val btnSearch = dialogView.findViewById<Button>(R.id.btnSearch)

        etName.setText(product?.name)

        if (product?.image != null) {
            dialogProductImageView?.load(settingsManager.getFullImageUrl(product.image))
        }
        
        val categoryNames = allCategories.map { it.name }
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryNames)
        actvCategory.setAdapter(categoryAdapter)
        
        val unitNames = allUnits.map { it.name }
        val unitAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, unitNames)
        actvUnit.setAdapter(unitAdapter)

        if (product != null) {
            val currentCategory = allCategories.find { it.id == product.category }
            actvCategory.setText(currentCategory?.name, false)
            val currentUnit = allUnits.find { it.id == product.unit }
            actvUnit.setText(currentUnit?.name, false)
        } else {
            if (selectedCategoryName != null) {
                actvCategory.setText(selectedCategoryName, false)
            }
            if (unitNames.isNotEmpty()) {
                actvUnit.setText(unitNames[0], false)
            }
        }

        val isGlobal = product?.isGlobal ?: false
        if (isGlobal) {
            btnGallery.isEnabled = false
            btnCamera.isEnabled = false
            btnSearch.isEnabled = false
            etName.isEnabled = false
            actvCategory.isEnabled = false
            actvUnit.isEnabled = false
        }

        btnGallery.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        btnCamera.setOnClickListener {
            val file = File(cacheDir, "camera_temp_${UUID.randomUUID()}.jpg")
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        }

        btnSearch.setOnClickListener {
            val query = etName.text.toString().ifBlank { product?.name ?: "" }
            showImageSearchDialog(query)
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString()
                val categoryName = actvCategory.text.toString()
                val selectedCategory = allCategories.find { it.name == categoryName }
                val unitName = actvUnit.text.toString()
                val selectedUnit = allUnits.find { it.name == unitName }

                if (name.isNotBlank() && selectedCategory != null && selectedUnit != null) {
                    val imagePart = prepareImagePart()
                    if (product == null) {
                        viewModel.createProduct(name, selectedCategory.id, selectedUnit.id, imagePart)
                    } else {
                        viewModel.updateProduct(product.id, name, selectedCategory.id, selectedUnit.id, imagePart)
                    }
                } else {
                    Toast.makeText(this, "Name, category and unit are required", Toast.LENGTH_SHORT).show()
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

    private fun showImageSearchDialog(initialQuery: String) {
        val editText = EditText(this)
        editText.setText(initialQuery)
        editText.setHint("Search images for...")
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Google Images Search")
            .setView(editText)
            .setPositiveButton("Search") { _, _ ->
                val query = editText.text.toString()
                if (query.isNotBlank()) {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://www.google.com/search?q=$query&tbm=isch")
                    }
                    startActivity(intent)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun prepareImagePart(): MultipartBody.Part? {
        val uri = selectedImageUri ?: return null
        
        return try {
            val file = File(cacheDir, "temp_product_image.jpg")
            val inputStream = contentResolver.openInputStream(uri) ?: return null
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
        viewModel.products.observe(this) { result ->
            binding.progressBar.visibility = View.GONE
            binding.swipeRefresh.isRefreshing = false
            if (result.isSuccess) {
                allProducts = result.getOrNull() ?: emptyList()
                updateDisplayList()
            } else {
                Toast.makeText(this, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.categories.observe(this) { result ->
            if (result.isSuccess) {
                allCategories = result.getOrNull() ?: emptyList()
            }
        }

        viewModel.units.observe(this) { result ->
            if (result.isSuccess) {
                allUnits = result.getOrNull() ?: emptyList()
            }
        }

        viewModel.itemAdded.observe(this) { result ->
            if (result.isSuccess) {
                Toast.makeText(this, "Product added to list", Toast.LENGTH_SHORT).show()
                if (lastClickedProductId != -1) {
                    adapter.markAsAdded(lastClickedProductId)
                    lastClickedProductId = -1
                }
            } else {
                Toast.makeText(this, "Failed to add product: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.productOperationResult.observe(this) { result ->
            if (result.isSuccess) {
                Toast.makeText(this, "Operation successful", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Operation failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }
        binding.progressBar.visibility = View.VISIBLE
    }

    companion object {
        const val EXTRA_LIST_ID = "extra_list_id"
        const val EXTRA_EXISTING_PRODUCT_IDS = "extra_existing_product_ids"
    }
}
