package no.nav.syfo

import com.migesok.jaxb.adapter.javatime.LocalDateTimeXmlAdapter
import com.migesok.jaxb.adapter.javatime.LocalDateXmlAdapter
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.bind.Unmarshaller
import no.nav.helse.apprecV1.XMLAppRec
import no.nav.helse.eiFellesformat.XMLEIFellesformat
import no.nav.helse.eiFellesformat.XMLMottakenhetBlokk
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet

val fellesformatJaxBContext: JAXBContext = JAXBContext.newInstance(XMLEIFellesformat::class.java,
        XMLMsgHead::class.java, XMLMottakenhetBlokk::class.java, HelseOpplysningerArbeidsuforhet::class.java)
val fellesformatUnmarshaller: Unmarshaller = fellesformatJaxBContext.createUnmarshaller().apply {
    setAdapter(LocalDateTimeXmlAdapter::class.java, XMLDateTimeAdapter())
    setAdapter(LocalDateXmlAdapter::class.java, XMLDateAdapter())
}

val apprecJaxbContext: JAXBContext = JAXBContext.newInstance(XMLAppRec::class.java)
val apprecJaxbMarshaller: Marshaller = apprecJaxbContext.createMarshaller()
val apprecFFJaxbContext: JAXBContext = JAXBContext.newInstance(XMLEIFellesformat::class.java)
val apprecFFJaxbMarshaller: Marshaller = apprecFFJaxbContext.createMarshaller()
