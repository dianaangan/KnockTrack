package com.knocktrack.knocktrack.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.knocktrack.knocktrack.R
import com.knocktrack.knocktrack.presenter.HistoryPresenter
import com.knocktrack.knocktrack.adapter.DoorbellEventAdapter
import com.knocktrack.knocktrack.model.DoorbellEvent

class HistoryActivity : BaseActivity(), HistoryView {
    
    private lateinit var presenter: HistoryPresenter
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DoorbellEventAdapter
    private lateinit var btnClearHistory: Button
    private lateinit var tvEmptyState: TextView
    private lateinit var progressBar: ProgressBar
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        
        initViews()
        initPresenter()
        setupListeners()
        
        // Load doorbell events
        presenter.loadDoorbellEvents()
    }
    
    private fun initViews() {
        // Initialize views
        recyclerView = findViewById(R.id.recyclerView)
        btnClearHistory = findViewById(R.id.btnClearHistory)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        progressBar = findViewById(R.id.progressBar)
        
        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = DoorbellEventAdapter(
            onDeleteClick = { event ->
                presenter.deleteEvent(event)
            }
        )
        recyclerView.adapter = adapter
        
        // Back button
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }
        
        // Bottom navigation
        val navHome = findViewById<LinearLayout>(R.id.bottomNavigation).getChildAt(0) as LinearLayout
        val navHistory = findViewById<LinearLayout>(R.id.bottomNavigation).getChildAt(1) as LinearLayout
        val navSettings = findViewById<LinearLayout>(R.id.bottomNavigation).getChildAt(2) as LinearLayout
        
        navHome.setOnClickListener {
            navigateToHome()
        }
        
        navHistory.setOnClickListener {
            // Already on history, do nothing
        }
        
        navSettings.setOnClickListener {
            navigateToSettings()
        }
    }
    
    private fun initPresenter() {
        presenter = HistoryPresenter()
        presenter.attachView(this, this)
    }
    
    private fun setupListeners() {
        btnClearHistory.setOnClickListener {
            presenter.clearHistory()
        }
    }
    
    
    // HistoryView interface methods
    override fun showDoorbellEvents(events: List<DoorbellEvent>) {
        adapter.updateEvents(events)
    }
    
    override fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
    
    override fun showEmptyState(show: Boolean) {
        tvEmptyState.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }
    
    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    override fun onHistoryCleared() {
        Toast.makeText(this, "History cleared successfully", Toast.LENGTH_SHORT).show()
    }
    
    override fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
    
    override fun navigateToSettings() {
        startActivity(Intent(this, SettingActivity::class.java))
    }
    
    override fun onDestroy() {
        presenter.cleanup()
        presenter.detachView()
        super.onDestroy()
    }
    
    /** Override from BaseActivity - handles global alert navigation */
    override fun navigateToHistory() {
        // Already in history, do nothing or refresh
        presenter.loadDoorbellEvents()
    }
    
    override fun onResume() {
        super.onResume()
        
        // Force refresh the listener to ensure it's working properly
        com.knocktrack.knocktrack.utils.GlobalFirebaseListener.forceRefresh(this)
        
        // Add test gesture for History screen
        // Long press anywhere on the screen to test alerts
        findViewById<android.view.View>(android.R.id.content).setOnLongClickListener {
            testDoorbellAlert()
            true
        }
    }
}