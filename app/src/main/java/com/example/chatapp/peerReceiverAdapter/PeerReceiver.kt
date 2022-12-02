package com.example.chatapp.peerReceiverAdapter

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pManager
import android.widget.Toast
import com.example.chatapp.ConversationStatusActivity

class PeerReceiver(
    val manager: WifiP2pManager,
    val channel: WifiP2pManager.Channel,
    val activity: ConversationStatusActivity,
    val peerListListener: WifiP2pManager.PeerListListener?
) : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        var action: String? = intent.action

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            var state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Toast.makeText(context, "Wifi is ON", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Wifi is OFF", Toast.LENGTH_SHORT).show()
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            if ( manager != null) {
                manager.requestPeers(channel, activity.peerListListener)
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            if (manager == null) {
                return
            }

            var networkInfo: NetworkInfo? =
                intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)

            if (networkInfo!!.isConnected) {//todo debug this line and all lines of code like this
                manager.requestConnectionInfo(channel, activity.connectionInfoListener)
            }else{
                activity.csBinding.connectionStatus.text = "Device Disconnected"
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            if (manager != null) {
                manager.requestPeers(channel, activity.peerListListener)
            }
        }
    }
}

//companion object {
//    val intentFilter = IntentFilter().apply {
//        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
//        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
//    }
//}
