// SPDX-FileCopyrightText: 2019, Andras Veres-Szentkiaalyi <vsza@vsza.hu>
// SPDX-License-Identifier: MIT

package org.hsbp.androsphinx

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import android.widget.SearchView
import androidx.core.content.getSystemService
import kotlinx.android.synthetic.main.activity_main.*
import java.net.URL

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab_settings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val i = intent
        if (i.action == Intent.ACTION_SEND && i.type == "text/plain") {
            val sharedText = i.getStringExtra(Intent.EXTRA_TEXT) ?: return
            val url = try {
                URL(sharedText)
            } catch (e: Exception) {
                return
            }
            startActivity(with(Intent(this, AccountsActivity::class.java)) {
                action = Intent.ACTION_SEARCH
                putExtra(SearchManager.QUERY, url.toString())
            })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        val searchManager = getSystemService<SearchManager>()
        (menu.findItem(R.id.app_bar_search).actionView as SearchView).apply {
            setSearchableInfo(searchManager?.getSearchableInfo(componentName))
        }

        return true
    }
}
