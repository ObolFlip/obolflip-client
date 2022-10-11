package org.ergoplatform.obolflip

import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.text.DateFormat
import java.util.*
import kotlin.math.abs

fun millisecondsToRelativeTime(milliseconds: Long): String {
    val relTimeMinutes = abs((((System.currentTimeMillis() - milliseconds) / 1000) / 60).toInt())

    return if (relTimeMinutes < 120) {
        "$relTimeMinutes minutes"
    } else {
        val relTimeHours = relTimeMinutes / 60
        val minutes = relTimeMinutes % 60
        "$relTimeHours hours, $minutes minutes"
    }
}

fun formatErgAmount(rawAmount: Long): String =
    rawAmount.toBigDecimal().movePointLeft(9).toPlainString().trimEnd('0').trimEnd('.')

fun getLocalInetAddress(): InetAddress? {
    var savedAddress: InetAddress? = null
    try {
        val netinterfaces = NetworkInterface.getNetworkInterfaces()
        while (netinterfaces.hasMoreElements()) {
            val netinterface = netinterfaces.nextElement()
            val addresses = netinterface.inetAddresses
            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()

                // this is the condition that sometimes gives problems
                if (!address.isLoopbackAddress
                    && !address.isLinkLocalAddress
                ) {
                     if (address is Inet4Address)
                        return address
                    else
                        savedAddress = address
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return savedAddress
}