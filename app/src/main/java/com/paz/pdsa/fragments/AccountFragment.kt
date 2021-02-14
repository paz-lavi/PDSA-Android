package com.paz.pdsa.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paz.logger.EZLog
import com.paz.pdsa.R
import com.paz.pdsa.databinding.FragmentAccountBinding
import com.paz.pdsa.dsa.ras.RSA
import com.paz.pdsa.utils.Constants
import com.paz.pdsa.utils.User
import com.paz.prefy_lib.Prefy
import lombok.Setter


@Setter
class AccountFragment : Fragment() {
    private lateinit var callback: MakeIntentCallback
    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!
    private val _auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var _showKeys = false
    private val ezLog = EZLog.getInstance()


    companion object {

        fun newInstance(): AccountFragment {
            return AccountFragment()
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindAuthData()
        hideKeysData()
        setOnClick()
    }

    private fun bindKeysData() {
        val prefy = Prefy.getInstance()
        val len = prefy.getLong(Constants.KEY_LENGTH, -1)
        val public = prefy.getLong(Constants.PUBLIC_KEY, -1)
        val private = prefy.getLong(Constants.PRIVATE_KEY, -1)
        binding.accountLBLKeyLen.text = getString(R.string.keyLength, len.toString())
        binding.accountLBLPublic.text = getString(R.string.publicKey, public.toString())
        binding.accountLBLPrivate.text = getString(R.string.privateKey, private.toString())

    }

    private fun hideKeysData() {


        binding.accountLBLKeyLen.text = getString(R.string.keyLength, "**********")
        binding.accountLBLPublic.text = getString(R.string.publicKey, "**********")
        binding.accountLBLPrivate.text = getString(R.string.privateKey, "**********")

    }

    private fun setOnClick() {
        binding.accountBTNSignOut.setOnClickListener {
            _auth.signOut()
            callback.intent()
        }

        binding.accountBTNReset.setOnClickListener { resetKeys() }
        binding.accountBTNShowKeys.setOnClickListener { showOrHide() }
    }

    private fun showOrHide() {
        _showKeys = !_showKeys
        if (_showKeys) {
            bindKeysData()
            binding.accountBTNShowKeys.text = getString(R.string.hideKeys)
            binding.accountBTNShowKeys.icon = ResourcesCompat.getDrawable(activity!!.resources, R.drawable.ic_visibility_off, null)

        }   else {
            hideKeysData()
            binding.accountBTNShowKeys.text = getString(R.string.showKey)
            binding.accountBTNShowKeys.icon = ResourcesCompat.getDrawable(activity!!.resources, R.drawable.ic_visibility, null)


        }
    }

    private fun resetKeys() {
        val keyPair = RSA().keysGenerator(64)
        val prefy = Prefy.getInstance()
        prefy.putLong(Constants.KEY_LENGTH, keyPair.keyLength)
        prefy.putLong(Constants.PUBLIC_KEY, keyPair.publicKey)
        prefy.putLong(Constants.PRIVATE_KEY, keyPair.privateKey)
        _showKeys = !_showKeys
        showOrHide()
    }


    private fun bindAuthData() {
        var user: User
        db.collection("users").document(_auth.currentUser?.uid!!).get().addOnSuccessListener { document ->
            if (document != null) {
                 ezLog.debug("DocumentSnapshot data: ${document.data}")
                user = document.toObject(User::class.java)!!
                 ezLog.debug("user data: $user")

                binding.accountLBLEmail.text = getString(R.string.emailVal, user.email)
                binding.accountLBLName.text = getString(R.string.nameVal, user.name)
                binding.accountLBLPhone.text = getString(R.string.phoneVal, user.phone)
            } else {
                 ezLog.debug("No such document")
            }
        }
                .addOnFailureListener { exception ->
                    ezLog.logException(exception.message, exception)

                }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    fun setCallback(callback: MakeIntentCallback) {
        this.callback = callback
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MakeIntentCallback) {
            callback = context
        } else {
            throw ClassCastException(
                    "$context must implement MakeIntentCallback.")
        }
    }
}