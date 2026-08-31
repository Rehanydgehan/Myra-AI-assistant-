package com.myra.assistant.ai

import com.myra.assistant.model.AppCommand
import java.util.Locale

class CommandParser {
    fun parse(input: String): AppCommand? {
        val text = input.lowercase(Locale.getDefault())

        if (text.contains("youtube kholo") || text.contains("open youtube")) return AppCommand("OPEN_APP", mapOf("app" to "youtube"))
        if (text.contains("whatsapp kholo") || text.contains("open whatsapp")) return AppCommand("OPEN_APP", mapOf("app" to "whatsapp"))
        if (text.contains("instagram kholo") || text.contains("open instagram")) return AppCommand("OPEN_APP", mapOf("app" to "instagram"))
        if (text.contains("facebook kholo") || text.contains("open facebook")) return AppCommand("OPEN_APP", mapOf("app" to "facebook"))
        if (text.contains("chrome kholo") || text.contains("open chrome")) return AppCommand("OPEN_APP", mapOf("app" to "chrome"))
        if (text.contains("gmail kholo") || text.contains("open gmail")) return AppCommand("OPEN_APP", mapOf("app" to "gmail"))
        if (text.contains("maps kholo") || text.contains("open maps") || text.contains("open google maps")) return AppCommand("OPEN_APP", mapOf("app" to "maps"))
        if (text.contains("spotify kholo") || text.contains("open spotify")) return AppCommand("OPEN_APP", mapOf("app" to "spotify"))
        if (text.contains("netflix kholo") || text.contains("open netflix")) return AppCommand("OPEN_APP", mapOf("app" to "netflix"))
        
        if (text.contains("close app") || text.contains("app band karo") || text.contains("close this app")) return AppCommand("CLOSE_APP")
        
        if (text.contains("volume up") || text.contains("awaaz badao")) return AppCommand("VOLUME_UP")
        if (text.contains("volume down") || text.contains("awaaz kam karo")) return AppCommand("VOLUME_DOWN")
        
        if (text.contains("flashlight on") || text.contains("torch on")) return AppCommand("FLASHLIGHT_ON")
        if (text.contains("flashlight off") || text.contains("torch off")) return AppCommand("FLASHLIGHT_OFF")
        
        if (text.contains("wifi on") || text.contains("wi-fi on")) return AppCommand("WIFI_ON")
        if (text.contains("wifi off") || text.contains("wi-fi off")) return AppCommand("WIFI_OFF")
        
        if (text.contains("bluetooth on")) return AppCommand("BLUETOOTH_ON")
        if (text.contains("bluetooth off")) return AppCommand("BLUETOOTH_OFF")
        
        // Contacts & Calls parsing
        val callRegex = Regex("(?:call|phone) (.*)")
        val callMatch = callRegex.find(text)
        if (callMatch != null && !text.contains("whatsapp") && !text.contains("prime")) {
            return AppCommand("CALL", mapOf("contact" to callMatch.groupValues[1].trim()))
        }
        
        if (text.contains("ko call karo")) {
            val contact = text.substringBefore("ko call karo").trim()
            if (contact.isNotEmpty() && !contact.contains("close friend") && !contact.contains("second contact")) {
                 return AppCommand("CALL", mapOf("contact" to contact))
            }
        }

        // WhatsApp
        if (text.contains("whatsapp call")) {
            val contact = text.replace("whatsapp call", "").replace("ko", "").trim()
            return AppCommand("WHATSAPP_CALL", mapOf("contact" to contact))
        }

        // Prime Contacts
        if (text.contains("close friend ko call karo") || text.contains("first contact ko call karo")) {
            return AppCommand("PRIME_CALL", mapOf("index" to "0"))
        }
        if (text.contains("second contact ko call karo")) {
            return AppCommand("PRIME_CALL", mapOf("index" to "1"))
        }
        if (text.contains("meri jaan ko message karo") || text.contains("first contact ko message karo")) {
            return AppCommand("PRIME_MSG", mapOf("index" to "0"))
        }
        
        if (text.contains("read screen") || text.contains("what is on my screen") || text.contains("screen par kya hai") || text.contains("read my screen")) {
            return AppCommand("READ_SCREEN")
        }
        
        return null
    }
}
