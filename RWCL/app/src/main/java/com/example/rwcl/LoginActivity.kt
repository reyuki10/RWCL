package com.example.rwcl

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthCredential
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    private val GOOGLE_SIGN_IN = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        //Setup
        setup()
        session()
    }

    private fun setup(){
        title = "Autenticación"

        //Registro con email y password
        registerButton.setOnClickListener{
            if (emailEditText.text.isNotEmpty() && passwordEditText.text.isNotEmpty()){
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(emailEditText.text.toString(), passwordEditText.text.toString()).addOnCompleteListener {
                    if (it.isSuccessful){
                        showHome(it.result?.user?.email ?: "", ProviderType.BASIC)
                    }else {
                        showAlert()
                    }
                }
            }else{
                showAlert()
            }
        }
        //Login con email y password
        loginButton.setOnClickListener{
            if (emailEditText.text.isNotEmpty() && passwordEditText.text.isNotEmpty()){
                FirebaseAuth.getInstance().signInWithEmailAndPassword(emailEditText.text.toString(), passwordEditText.text.toString()).addOnCompleteListener {
                    if (it.isSuccessful){
                        showHome(it.result?.user?.email ?: "", ProviderType.BASIC)
                    }else {
                        showAlert()
                    }
                }
            }else{
                showAlert()
            }
        }

        googleButton.setOnClickListener{
            //Configuracion

            val googleConf = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            val googleClient = GoogleSignIn.getClient(this, googleConf)
            googleClient.signOut()
            startActivityForResult(googleClient.signInIntent, GOOGLE_SIGN_IN)
        }
    }


    //Funcion guardado de datos del login
    private fun session() {
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email = prefs.getString("email", null)
        val proveedor = prefs.getString("proveedor", null)

        //Hacer invisible el login si hay datos
        if (email != null && proveedor != null) {
            loginLayout.visibility = View.INVISIBLE
            showHome(email, ProviderType.valueOf(proveedor))
        }
    }

    //Funcion volver a mostrar login si se hace log out
    override fun onStart() {
        super.onStart()

        loginLayout.visibility = View.VISIBLE
    }

    //Mostrar alerta de error en la autenticacion
    private fun showAlert(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage("Se ha producido un error autenticando al usuario")
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun showHome(email: String, proveedor: ProviderType){

        val homeIntent = Intent(this, HomeActivity::class.java).apply {
            putExtra("email",email)
            putExtra("proveedor", proveedor.name)
        }
        startActivity(homeIntent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Pasar credenciales de google a FireBase
                val account = task.getResult(ApiException::class.java)

                if (account != null){
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener{
                        if (it.isSuccessful){
                            showHome(account.email ?: "", ProviderType.GOOGLE)
                        }else {
                            showAlert()
                        }
                    }
                }
            }catch (e: ApiException) {
                showAlert()
            }


        }
    }
}