package com.liquid.citri

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.liquid.citri.databinding.ActivityMainBinding
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import kamino.Amino
import kamino.exception.AminoException
import kamino.model.SessionInfo
import kamino.model.response.Wallet
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val app = application as Citri
        binding.submit.setOnClickListener {
            uiLoginProcess()
            app.scope.launch {
                login(
                    binding.login.text.toString(),
                    binding.password.text.toString(),
                    app.amino
                )
            }
        }
    }

    private suspend fun login(login: String,
                      password: String,
                      amino: Amino) {
        val (session, wallet) = try {
            val session = if (login.contains("@")) amino.loginByEmail(login, password)
            else amino.loginByPhoneNumber(login, password)
            Pair(session, amino.getWalletInfo())
        } catch (e: AminoException) {
            return uiErrorAlert(e.apiCode, e.apiMessage)
        } finally {
            uiLoginDone()
        }
        uiLoginSuccess(session, wallet)
    }

    private fun uiLoginSuccess(sessionInfo: SessionInfo, wallet: Wallet) {
        Toast.makeText(
            this@MainActivity,
            getString(R.string.successfully_logged_in, sessionInfo.userProfile.nickname),
            Toast.LENGTH_SHORT
        ).show()
        val intent = Intent(this, TransferActivity::class.java)
        intent.putExtra("nickname", sessionInfo.userProfile.nickname)
        intent.putExtra("avatar", sessionInfo.userProfile.icon)
        intent.putExtra("coins", wallet.totalCoins.toInt())
        startActivity(intent)
        finish()
    }

    private fun uiErrorAlert(code: Int, message: String) {
        AlertDialog.Builder(this)
            .setTitle(code.toString())
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok)) { view, _ -> view.dismiss() }
            .show()
    }

    private fun uiLoginProcess() {
        binding.ads.visibility = View.GONE
        binding.submit.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun uiLoginDone() {
        binding.ads.visibility = View.VISIBLE
        binding.submit.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
    }
}