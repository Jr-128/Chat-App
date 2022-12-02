package com.example.chatapp.peerReceiverAdapter

import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.databinding.UserBinding

class PeersAdapter(
    private val users: MutableList<WifiP2pDevice> = mutableListOf(),
    private val userClicked: (WifiP2pDevice) -> Unit
) : RecyclerView.Adapter<PeerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeerViewHolder =
        PeerViewHolder(
            UserBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: PeerViewHolder, position: Int) =
        holder.bind(users[position], userClicked)

    override fun getItemCount(): Int = users.size
}

class PeerViewHolder(
    private val mBinding: UserBinding
) : RecyclerView.ViewHolder(mBinding.root) {

    fun bind(user: WifiP2pDevice, onDeviceClick: (WifiP2pDevice) -> Unit) {
        mBinding.userItem.text = user.deviceName

        mBinding.chatBtn.setOnClickListener {
            onDeviceClick(user)
        }
    }
}