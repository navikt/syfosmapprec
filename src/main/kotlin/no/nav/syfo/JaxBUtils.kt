package no.nav.syfo

import no.kith.xmlstds.apprec._2004_11_21.XMLAppRec
import no.trygdeetaten.xml.eiff._1.XMLEIFellesformat
import no.trygdeetaten.xml.eiff._1.XMLMottakenhetBlokk

import javax.xml.bind.JAXBContext
import javax.xml.bind.Unmarshaller

val fellesformatJaxBContext: JAXBContext = JAXBContext.newInstance(XMLEIFellesformat::class.java,
        XMLMottakenhetBlokk::class.java, XMLAppRec::class.java)
val fellesformatUnmarshaller: Unmarshaller = fellesformatJaxBContext.createUnmarshaller()
