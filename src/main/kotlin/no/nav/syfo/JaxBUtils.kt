package no.nav.syfo

import no.kith.xmlstds.apprec._2004_11_21.XMLAppRec
import no.kith.xmlstds.msghead._2006_05_24.XMLMsgHead
import no.nav.helse.sm2013.EIFellesformat
import no.nav.model.sm2013.HelseOpplysningerArbeidsuforhet
import no.trygdeetaten.xml.eiff._1.XMLEIFellesformat
import no.trygdeetaten.xml.eiff._1.XMLMottakenhetBlokk

import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.bind.Unmarshaller

val fellesformatJaxBContext: JAXBContext = JAXBContext.newInstance(EIFellesformat::class.java, XMLMsgHead::class.java,
        XMLMottakenhetBlokk::class.java, HelseOpplysningerArbeidsuforhet::class.java, XMLAppRec::class.java)
val fellesformatUnmarshaller: Unmarshaller = fellesformatJaxBContext.createUnmarshaller()

val apprecJaxBContext: JAXBContext = JAXBContext.newInstance(XMLEIFellesformat::class.java, XMLAppRec::class.java)
val apprecMarshaller: Marshaller = apprecJaxBContext.createMarshaller()
