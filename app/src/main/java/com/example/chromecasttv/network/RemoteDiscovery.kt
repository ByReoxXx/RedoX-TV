package com.example.chromecasttv

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Yerel ağda (mDNS / NSD) TV'lerin duyurulması ve telefon/tabletlerin bulması.
 * TV tarafı [register], kumanda tarafı [startDiscovery] kullanır.
 */
class RemoteDiscovery(context: Context) {
    private val appContext = context.applicationContext
    private val nsd: NsdManager? = appContext.getSystemService(Context.NSD_SERVICE) as? NsdManager

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private val foundState = MutableStateFlow<List<DiscoveredTv>>(emptyList())
    val found: StateFlow<List<DiscoveredTv>> = foundState.asStateFlow()

    private val resolveQueue = ArrayDeque<NsdServiceInfo>()
    private var resolving = false

    // ---------------------------------------------------------------- TV tarafı

    fun register(serviceName: String, port: Int) {
        val manager = nsd ?: return
        unregister()
        val info = NsdServiceInfo()
        info.serviceName = serviceName
        info.serviceType = RemoteProtocol.SERVICE_TYPE
        info.port = port
        val listener = object : NsdManager.RegistrationListener {
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {}
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {}
        }
        try {
            manager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
            registrationListener = listener
        } catch (_: Exception) {
            registrationListener = null
        }
    }

    fun unregister() {
        val manager = nsd ?: return
        val listener = registrationListener ?: return
        try {
            manager.unregisterService(listener)
        } catch (_: Exception) {
        }
        registrationListener = null
    }

    // ---------------------------------------------------------------- Kumanda tarafı

    fun startDiscovery() {
        val manager = nsd ?: return
        stopDiscovery()
        foundState.value = emptyList()
        val listener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onDiscoveryStopped(serviceType: String) {}

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType?.contains("_redoxtv2") == true) {
                    enqueueResolve(serviceInfo)
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                val lostName = serviceInfo.serviceName
                foundState.value = foundState.value.filterNot { it.name == lostName }
            }
        }
        try {
            manager.discoverServices(RemoteProtocol.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
            discoveryListener = listener
        } catch (_: Exception) {
            discoveryListener = null
        }
    }

    fun stopDiscovery() {
        val manager = nsd ?: return
        val listener = discoveryListener ?: return
        try {
            manager.stopServiceDiscovery(listener)
        } catch (_: Exception) {
        }
        discoveryListener = null
    }

    private fun enqueueResolve(info: NsdServiceInfo) {
        synchronized(this) {
            resolveQueue.addLast(info)
        }
        pumpResolve()
    }

    private fun finishResolve() {
        synchronized(this) {
            resolving = false
        }
        pumpResolve()
    }

    private fun pumpResolve() {
        val manager = nsd ?: return
        var next: NsdServiceInfo? = null
        synchronized(this) {
            if (!resolving) {
                next = resolveQueue.removeFirstOrNull()
                if (next != null) resolving = true
            }
        }
        val target = next ?: return
        try {
            manager.resolveService(target, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    finishResolve()
                }

                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    val host = serviceInfo.host?.hostAddress
                    if (host != null) {
                        val name = serviceInfo.serviceName ?: "RedoX TV 2"
                        val tv = DiscoveredTv(name, host, serviceInfo.port)
                        val others = foundState.value.filterNot { it.host == tv.host && it.port == tv.port }
                        foundState.value = others + tv
                    }
                    finishResolve()
                }
            })
        } catch (_: Exception) {
            finishResolve()
        }
    }
}
