package com.ipfssearch.ipfstika

import java.io.IOException

import java.net.URL
import java.net.URI
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.util.ArrayList

import fi.iki.elonen.NanoHTTPD

import org.apache.commons.lang.StringUtils

import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.sax.BodyContentHandler
import org.apache.tika.sax.LinkContentHandler
import org.apache.tika.language.detect.LanguageHandler
import org.apache.tika.sax.TeeContentHandler
import org.apache.tika.sax.Link
import org.apache.tika.exception.TikaException
import org.apache.tika.io.TikaInputStream

import com.google.gson.Gson
import com.google.gson.JsonObject

import org.xml.sax.SAXException

class App @Throws(IOException::class)
constructor() : NanoHTTPD("localhost", 8081) {

    init {
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
        println("\nipfs-tika accepting requests at: http://localhost:8081/ \n")
    }

    override fun serve(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val uri=session.uri

        try {
            return NanoHTTPD.newFixedLengthResponse(getResponse(uri))
        } catch (ioe: IOException) {
            System.err.println("Internal server error:\n" + ioe.toString())
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, ioe.message)
        }

    }

    private fun getAbsoluteLinks(parent_url: URL, links: List<Link>): List<String> {
        val links_out=ArrayList<String>()
        var uri: String

        for (link in links) {
            uri=link.uri

            if (StringUtils.isBlank(uri)) {
                continue
            }
            val abs_uri: String

            // build an absolute URL
            try {
                val tmpURL=URL(parent_url, uri)
                abs_uri=tmpURL.toExternalForm()
            } catch (e: MalformedURLException) {
                System.err.println("MalformedURLException:\n" + e.message)
                continue
            }

            links_out.add(abs_uri.toString())
        }

        return links_out
    }

    @Throws(IOException::class)
    private fun getResponse(path: String): String {
        // Generate properly escaped URL
        val uri: URI

        try {
            uri=URI(
                    "http",
                    null,
                    "localhost",
                    8080,
                    path, null, null
            )

        } catch (e: URISyntaxException) {
            System.err.println("URI syntax exception:\n" + e.message)
            throw IOException(e)
        }

        println("Fetching: " + uri.toString())

        // Turn URL into input stream
        var url=uri.toURL()
        url=URL("https://ipfs.io/ipns/QmQCb2GGcdC2Cm36X5RjjzabNPEfFaQvE97ETNb6f24KFp")
        val inputStream=TikaInputStream.get(url)

        val parser=AutoDetectParser()
        val link_handler=LinkContentHandler()
        val body_handler=BodyContentHandler(10 * 1024 * 1024)
        // This causes weird crashes
        // LanguageHandler language_handler = new LanguageHandler();
        val handler=TeeContentHandler(link_handler, body_handler)
        val metadata=Metadata()

        // Set filename from path string
        val filename=path.substring(path.lastIndexOf("/") + 1, path.length)
        metadata.set(Metadata.RESOURCE_NAME_KEY, filename)

        println("Parsing: " + uri.toString() + " (" + filename + ")")

        try {
            parser.parse(inputStream, handler, metadata)
        } catch (e: TikaException) {
            System.err.println("Tika exception:\n" + e.message)
            throw IOException(e)
        } catch (e: SAXException) {
            System.err.println("SAX exception:\n" + e.message)
            throw IOException(e)
        } finally {
            inputStream.close()
        }

        val links=getAbsoluteLinks(url, link_handler.links)

        /* Now return JSON with:
            {
                "language": language_handler.getLanguage(),
                "content": body_handler.toString(),
                "links": links,
                "metadata": metadata
            }
        */
        val gson=Gson()
        val output_json=gson.toJsonTree(metadata).asJsonObject
        output_json.add("content", gson.toJsonTree(body_handler.toString().trim { it <= ' ' }))
        output_json.add("urls", gson.toJsonTree(links))

        return output_json.toString()
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            try {
                App()
            } catch (ioe: IOException) {
                System.err.println("Couldn't start server:\n" + ioe)
            }

        }
    }
}
