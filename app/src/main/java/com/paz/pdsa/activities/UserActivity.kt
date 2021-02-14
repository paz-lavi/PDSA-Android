package com.paz.pdsa.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.paz.pdsa.databinding.ActivityUserBinding
import com.paz.pdsa.fragments.AccountFragment
import com.paz.pdsa.fragments.MakeIntentCallback
import com.paz.pdsa.fragments.NewUserFragment
import com.paz.pdsa.utils.Constants

class UserActivity : AppCompatActivity(), MakeIntentCallback {
    private var binding: ActivityUserBinding? = null
    private var newUserFragment: NewUserFragment? = null
    private var accountFragment: AccountFragment? = null
    private var b = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserBinding.inflate(layoutInflater)
        val view: View = binding!!.root
        setContentView(view)
        setFragment()
    }

    private fun setFragment() {
        b = intent.getBooleanExtra(Constants.NEW_USER, false)
        newUserFragment = NewUserFragment()
        accountFragment = AccountFragment()

        supportFragmentManager.beginTransaction().setReorderingAllowed(true)
                .add(binding!!.userFRAGContent.id, (if (b) NewUserFragment::class.java else AccountFragment::class.java), null)
                .commit()
    }

    override fun intent() {

        val i = Intent(this@UserActivity, if (b)  MainActivity::class.java else LoginActivity::class.java)
        i.data = intent.data
        startActivity(i)
        finish()
    }
}