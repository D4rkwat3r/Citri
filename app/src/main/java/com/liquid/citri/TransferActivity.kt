package com.liquid.citri

import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.liquid.citri.databinding.ActivityTransferBinding
import kamino.Amino
import kamino.exception.AminoException
import kamino.internal.model.Community
import kamino.internal.model.LinkInfoV2
import kamino.internal.model.UserProfile
import kotlinx.coroutines.*

class TransferActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransferBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransferBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val app = application as Citri
        initUI(intent.extras!!)
        binding.start.setOnClickListener {
            startProcess(
                app.amino,
                app.scope,
                app.ioScope,
                binding.link.text.toString(),
                binding.countAtSend.text.toString().toInt(),
                binding.count.text.toString().toInt()
            )
        }
    }

    private fun startProcess(amino: Amino,
                             scope: CoroutineScope,
                             ioScope: CoroutineScope,
                             target: String,
                             countAtSend: Int,
                             count: Int) {
        if (count < countAtSend) {
            return Toast.makeText(this, getString(R.string.incorrect_values), Toast.LENGTH_SHORT).show()
        }
        scope.launch {
            val (user, community) = try {
                retrieve(target, amino)
            } catch (e: Exception) {
                Log.e("Retrieve", e.stackTraceToString())
                return@launch uiToastInvalidLink()
            }
            amino.community = community.ndcId.toString()
            val userId = user.extensions.linkInfo.objectId
            uiSendProcess()
            try {
                prepareVips(userId, countAtSend, community, amino)
            } catch (e: AminoException) {
                uiSendDone()
                return@launch uiToastNoPrepare(e.apiMessage)
            }
            sendProcess(count, countAtSend, scope, amino, userId)
        }
    }

    private suspend fun retrieve(target: String, amino: Amino): Pair<LinkInfoV2, Community> {
        val userInfo = amino.getLinkInfo(target)
        val communityInfo = amino.getCommunityInfo(userInfo.extensions.linkInfo.ndcId.toString())
        return Pair(userInfo, communityInfo)
    }

    private suspend fun prepareVips(targetId: String,
                                    countAtSend: Int,
                                    community: Community,
                                    amino: Amino) {
        if (community.influencerList.size > 2)
            amino.deleteFanClub(community.influencerList[1].uid)
        if (community.influencerList.map { it.uid }.contains(targetId))
            amino.deleteFanClub(targetId)
        amino.createFanClub(targetId, countAtSend)
    }

    private suspend fun sendCoins(amino: Amino, userId: String) {
        amino.joinFanClub(userId)
        amino.leaveFanClub(userId)
    }

    private suspend fun sendProcess(count: Int,
                                    countAtSend: Int,
                                    scope: CoroutineScope,
                                    amino: Amino,
                                    userId: String) {
        val results = mutableListOf<Deferred<Boolean>>()
        repeat(count / countAtSend) {
            results += scope.async {
                try {
                    sendCoins(amino, userId)
                    true
                } catch (e: AminoException) {
                    false
                }
            }
        }
        results.awaitAll().filter { !it }.forEach {
            try { sendCoins(amino, userId) }
            catch (ignored: AminoException) {}
        }
        if (count % countAtSend != 0) {
            amino.deleteFanClub(userId)
            amino.createFanClub(userId, count % countAtSend)
            sendCoins(amino, userId)
        }
        amino.deleteFanClub(userId)
        binding.coins.text = getString(R.string.coins_available, amino.getWalletInfo().totalCoins.toInt())
        uiSendDone()
    }

    private fun uiSendProcess() {
        binding.start.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun uiSendDone() {
        binding.start.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
    }

    private fun uiToastNoPrepare(message: String) {
        Toast.makeText(
            this,
            getString(R.string.unable_to_prepare_vips, message),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun uiToastInvalidLink() {
        Toast.makeText(
            this,
            getString(R.string.unable_to_get_user_id),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun initUI(extras: Bundle) {
        val avatar = extras.getString("avatar")
        val nickname = extras.getString("nickname")
        val coins = extras.getInt("coins")
        load(avatar ?: "")
        binding.nickname.text = nickname
        binding.coins.text = getString(R.string.coins_available, coins)
    }

    private fun load(url: String) {
        Glide.with(this)
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .error(R.drawable.empty)
            .circleCrop()
            .into(binding.avatar)
    }
}