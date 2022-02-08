package net.ttrstudios.in2000_team_47_app.backend.network


import android.util.Xml
import net.ttrstudios.in2000_team_47_app.backend.models.MetAlertModel
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import kotlin.jvm.Throws

private val namespace: String? = null

class MetAlertsParser {
    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(inputStream: InputStream): List<MetAlertModel> {
        inputStream.use {
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            parser.nextTag()
            return readFeed(parser)
        }
    }

    //Read the alertfeed
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readFeed(parser: XmlPullParser): List<MetAlertModel> {
        val itemList = mutableListOf<MetAlertModel>()

        parser.require(XmlPullParser.START_TAG, namespace, "rss")

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }

            // First navigate to channel-tag
            if (parser.name == "channel") {
                parser.require(XmlPullParser.START_TAG, namespace, "channel")

                while (parser.next() != XmlPullParser.END_TAG) {
                    if (parser.eventType != XmlPullParser.START_TAG) {
                        continue
                    }
                    // Look for item tags
                    if (parser.name == "item") {
                        itemList.add(readEntry(parser))
                    } else {
                        skip(parser)
                    }
                }
            }
        }
        return itemList
    }

    //Sets up a MetAlertModel-element, with used attributes
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readEntry(parser: XmlPullParser): MetAlertModel {
        parser.require(XmlPullParser.START_TAG, namespace, "item")
        var title: String? = null
        var description: String? = null
        var link: String? = null
        var author: String? = null
        var guid: String? = null
        var pubDate: String? = null

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                "title" -> title = readAttribute(parser, parser.name)
                "description" -> description = readAttribute(parser, parser.name)
                "link" -> link = readAttribute(parser, parser.name) // link til proxy
                "author" -> author = readAttribute(parser, parser.name)
                "guid" -> guid = readAttribute(parser, parser.name)
                "pubDate" -> pubDate = readAttribute(parser, parser.name)
                else -> skip(parser)
            }
        }
        return MetAlertModel(title, description, link, author, guid, pubDate)
    }

    //Exract attribute
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readAttribute(parser: XmlPullParser, tag: String): String {
        parser.require(XmlPullParser.START_TAG, namespace, tag)
        val value = readText(parser)
        parser.require(XmlPullParser.END_TAG, namespace, tag)
        return value
    }

    //extract attribute values
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    //Skips tag
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