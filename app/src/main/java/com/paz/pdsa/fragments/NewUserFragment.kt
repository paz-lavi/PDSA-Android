package com.paz.pdsa.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.paz.pdsa.databinding.FragmentNewUserBinding
import com.paz.pdsa.utils.User
import com.paz.prefy_lib.Prefy
import lombok.Setter

@Setter
class NewUserFragment : Fragment() {
    private lateinit var callback: MakeIntentCallback
    private var _binding: FragmentNewUserBinding? = null
    private val binding get() = _binding!!
    private val _auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()


    companion object {

        fun newInstance(): NewUserFragment {
            return NewUserFragment()
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNewUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindAuthData()
        setOnClick()
    }

    private fun setOnClick() {
        binding.signupBTNOk.setOnClickListener { buttonClicked() }

    }

    private fun buttonClicked() {
        val email = binding.signupEDTEmail.editText?.text.toString()
        val name = binding.signupEDTName.editText?.text.toString()
        val phone = binding.signupEDTPhone.editText?.text.toString()
        if (isAllNotEmpty(email, name, phone)) {
            _auth.currentUser?.updateEmail(email)
            val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(name).build()
            _auth.currentUser?.updateProfile(profileUpdates)

            val user = User(name, email, phone)
            Prefy.getInstance().putBoolean(_auth.currentUser?.uid, true)

            db.collection("users").document(_auth.currentUser?.uid!!).set(user)
            callback.intent()

        } else {
            activity?.let {
                MaterialAlertDialogBuilder(it).setTitle("Error").setMessage("Please fill all filed").setIcon(android.R.drawable.ic_dialog_alert)
                        .setNeutralButton("OK", null).show()
            }
        }

    }

    private fun isAllNotEmpty(email: String, name: String, phone: String): Boolean {
        return email.isNotEmpty() && name.isNotEmpty() && phone.isNotEmpty();
    }

    private fun bindAuthData() {
        binding.signupEDTEmail.editText?.setText(_auth.currentUser?.email)
        binding.signupEDTName.editText?.setText(_auth.currentUser?.displayName)
        binding.signupEDTPhone.editText?.setText(_auth.currentUser?.phoneNumber)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

     fun setCallback(callback: MakeIntentCallback){
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