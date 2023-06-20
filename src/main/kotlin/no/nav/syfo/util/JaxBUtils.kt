package no.nav.syfo.util

import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import no.nav.helse.apprecV1.XMLAppRec
import no.nav.helse.eiFellesformat.XMLEIFellesformat

val apprecJaxbContext: JAXBContext = JAXBContext.newInstance(XMLAppRec::class.java)
val apprecJaxbMarshaller: Marshaller = apprecJaxbContext.createMarshaller()
val apprecFFJaxbContext: JAXBContext = JAXBContext.newInstance(XMLEIFellesformat::class.java)
val apprecFFJaxbMarshaller: Marshaller = apprecFFJaxbContext.createMarshaller()
