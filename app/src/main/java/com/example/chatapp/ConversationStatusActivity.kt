package com.example.chatapp

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ActionListener
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.chatapp.databinding.ActivityConversationStatusBinding
import com.example.chatapp.peerReceiverAdapter.PeerReceiver
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket


class ConversationStatusActivity : AppCompatActivity() {
    lateinit var csBinding: ActivityConversationStatusBinding

    private lateinit var wifiManager: WifiManager

    //TODO DELETE LATER
    //mManager
    private lateinit var wifip2pManager: WifiP2pManager


    private lateinit var wifiChannel: WifiP2pManager.Channel

    private lateinit var receiver: BroadcastReceiver
    private lateinit var intentFilter: IntentFilter
    private lateinit var deviceNameArray: Array<String>
    private lateinit var deviceArray: Array<WifiP2pDevice?>

    private var peers: MutableList<WifiP2pDevice> = mutableListOf()

    lateinit var peerListListener: WifiP2pManager.PeerListListener
    lateinit var connectionInfoListener: WifiP2pManager.ConnectionInfoListener
    lateinit var handler: Handler.Callback
    lateinit var msgString: String


    lateinit var server: ServerClass
    lateinit var client: ClientClass
    lateinit var sendReceive: SendReceive

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversation_status)
        csBinding = ActivityConversationStatusBinding.inflate(layoutInflater)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifip2pManager =
            applicationContext.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        wifiChannel = wifip2pManager.initialize(this, mainLooper, null)
        receiver = PeerReceiver(wifip2pManager, wifiChannel, this, null)

        intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        peerListListener = WifiP2pManager.PeerListListener { peerList ->
            if (!peerList.deviceList.equals(peers)) {
                peers.clear()
                peers.addAll(peerList.deviceList)
                deviceNameArray = Array(peerList.deviceList.size) { "$it" }
                deviceArray =
                    arrayOfNulls(peerList.deviceList.size) //todo initialize elements in array
                var index = 0

                for (device in peerList.deviceList) {
                    deviceNameArray[index] = device.deviceName
                    deviceArray[index] = device
                    index++
                }
                var adapter: ArrayAdapter<String> =
                    ArrayAdapter(
                        applicationContext,
                        R.layout.user,
                        deviceNameArray
                    )//todo may need to change to list item menu item, different xml
                csBinding.peerListView.adapter = adapter
            }
            if (peers.size == 0) {
                Toast.makeText(applicationContext, "No Device Found", Toast.LENGTH_SHORT).show()
                return@PeerListListener
            }
        }

        connectionInfoListener =
            WifiP2pManager.ConnectionInfoListener { wifiP2pInfo ->
                val groupOwnerAddress: InetAddress =
                    wifiP2pInfo!!.groupOwnerAddress //todo debug ALL app values

                if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
                    csBinding.connectionStatus.text = "Host"
                    server =  ServerClass()     //Starts the send/receive thread
                    server.start()
                } else if (wifiP2pInfo.groupFormed) {
                    csBinding.connectionStatus.text = "Client"
                    client = ClientClass(groupOwnerAddress)
                    client.start()
                }
            }

        handler = Handler.Callback { msg ->
            when (msg.what) {
                MESSAGE_READ -> {
                    val readBuff = msg.obj as ByteArray
                    val tempMsg = String(readBuff, 0, msg.arg1)
                    msgString = tempMsg
                    csBinding.readMsg.text = tempMsg
                    true
                }
                else -> {
                    false
                }
            }
        }

        exqListener()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    @SuppressLint("MissingPermission")
    private fun exqListener() {
        csBinding.onOff.setOnClickListener {
            if (wifiManager.isWifiEnabled) {
                wifiManager.isWifiEnabled = false
                csBinding.onOff.text = "ON"

            } else {
                wifiManager.isWifiEnabled = true
                csBinding.onOff.text = "OFF"
            }
        }

        csBinding.discover.setOnClickListener {
            wifip2pManager.discoverPeers(wifiChannel, object : ActionListener {
                override fun onSuccess() {
                    csBinding.connectionStatus.text = "Discovery Started"
                }

                override fun onFailure(p0: Int) {
                    csBinding.connectionStatus.text = "Discovery Starting Failed"
                }
            })
        }

        csBinding.peerListView.onItemClickListener = object : AdapterView.OnItemClickListener {
            override fun onItemClick(adv: AdapterView<*>?, v: View?, i: Int, l: Long) {
                val device = deviceArray[i]
                val config: WifiP2pConfig = WifiP2pConfig()
                config.deviceAddress =
                    device?.deviceAddress //todo might need to verify the deviceArray is getting the correct

                wifip2pManager.connect(wifiChannel, config, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Toast.makeText(
                            applicationContext,
                            "Connected to " + device?.deviceName,
                            Toast.LENGTH_SHORT
                        ).show() //todo
                    }

                    override fun onFailure(p0: Int) {
                        Toast.makeText(
                            applicationContext,
                            "Not Connected " + device?.deviceName,
                            Toast.LENGTH_SHORT
                        ).show() //todo
                    }

                })
            }
        }

        csBinding.sendButton.setOnClickListener {
            val msgBytes = csBinding.writeMsg.text.toString().toByteArray()
            sendReceive.write(msgBytes)
        }
    }

    inner class ServerClass : Thread() {
        var socket: Socket? = null
        var serverSocket: ServerSocket? = null

        override fun run() {
            super.run()
            try {
                serverSocket = ServerSocket(8888, 500)
                socket = serverSocket!!.accept()
                sendReceive = SendReceive()
                sendReceive.start()

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    inner class ClientClass() : Thread() {
        var socket: Socket? = null
        var hostAdd: String? = null

        constructor(hostAddress: InetAddress) : this() {
            hostAdd = hostAddress.hostAddress
            socket = Socket()
        }

        override fun run() {
            super.run()
            socket?.connect(InetSocketAddress(hostAdd, 8888), 500)
            sendReceive = SendReceive(socket!!)
            sendReceive.start()
        }
    }

    inner class SendReceive() : Thread() {
        var socket: Socket? = null
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null

        constructor(skt: Socket) : this() {
            socket = skt

            try {
                inputStream = socket!!.getInputStream()
                outputStream = socket!!.getOutputStream()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        override fun run() {
            super.run()
            var buffer: ByteArray = ByteArray(1024)
            var bytes: Int

            while (socket != null) {
                try {
                    bytes = inputStream!!.read(buffer)
                    if (bytes > 0) {
                        Handler().obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        fun write(bytes: ByteArray){
            outputStream?.write(bytes) //todo verify if outputStream null
        }
    }

    companion object {
        const val MESSAGE_READ = 1
    }
}