package net.ttrstudios.in2000_team_47_app.backend.network

import android.util.Xml
import net.ttrstudios.in2000_team_47_app.backend.models.AlertInfo
import net.ttrstudios.in2000_team_47_app.backend.models.AlertModel
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import kotlin.jvm.Throws

private val namespace: String? = null

class AlertsParser {
    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(inputStream: InputStream): AlertModel {
        inputStream.use {
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            parser.nextTag()
            return readAlert(parser)
        }
    }

    //Sets up an Alert-element. Checks the tag of the attributes from the API and adds used attributes
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readAlert(parser: XmlPullParser): AlertModel {
        var id: String? = null
        var sender: String? = null
        var sent: String? = null
        var status: String? = null
        var msgType: String? = null
        var scope: String? = null
        var info: AlertInfo? = null

        parser.require(XmlPullParser.START_TAG, namespace, "alert")

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                "identifier" -> id = readAttribute(parser, parser.name)
                "sender" -> sender = readAttribute(parser, parser.name)
                "sent" -> sent = readAttribute(parser, parser.name)
                "status" -> status = readAttribute(parser, parser.name)
                "scope" -> scope = readAttribute(parser, parser.name)
                "msgType" -> msgType = readAttribute(parser, parser.name)
                "info" -> {
                    info = readInfo(parser)
                    break
                }

                else -> skip(parser)
            }
        }
        return AlertModel(id, sender, sent, status, msgType, scope, info)
    }

    //Sets up an AlertInfo-element. Checks the tag of the attributes from the API and adds used attributes
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readInfo(parser: XmlPullParser): AlertInfo {
        val info: AlertInfo?
        var event: String? = null
        var severity: String? = null
        var urgency: String? = null
        var certainty: String? = null
        var effective: String? = null
        var onset: String? = null
        var expires: String? = null
        var headline: String? = null
        var description: String? = null
        var instruction: String? = null


        parser.require(XmlPullParser.START_TAG, namespace, "info")

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                "event" -> event = readAttribute(parser, parser.name)
                "severity" -> severity = readAttribute(parser, parser.name)
                "urgency" -> urgency = readAttribute(parser, parser.name)
                "certainty" -> certainty = readAttribute(parser, parser.name)
                "effective" -> effective = readAttribute(parser, parser.name)
                "onset" -> onset = readAttribute(parser, parser.name)
                "expires" -> expires = readAttribute(parser, parser.name)
                "headline" -> headline = readAttribute(parser, parser.name)
                "description" -> description = readAttribute(parser, parser.name)
                "instruction" -> instruction = readAttribute(parser, parser.name)

                else -> skip(parser)
            }
        }
        info = AlertInfo(
            event,
            severity,
            urgency,
            certainty,
            effective,
            onset,
            expires,
            headline,
            description,
            instruction
        )
        return info
    }

    //Extract the attribute
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readAttribute(parser: XmlPullParser, tag: String): String {
        parser.require(XmlPullParser.START_TAG, namespace, tag)
        val value = readText(parser)
        parser.require(XmlPullParser.END_TAG, namespace, tag)
        return value
    }

    //Extract attribute-value
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    //Skips attribute
    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }
}
