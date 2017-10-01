package what.net

import java.net._
import org.ice4j.{Transport, TransportAddress}
import org.ice4j.socket.IceUdpSocketWrapper
import org.ice4j.stunclient.SimpleAddressDetector

object StunClient extends App {
  case class Discovered(local: InetSocketAddress, external: InetSocketAddress)
  
  def discover(): Discovered = {
    val detector = new SimpleAddressDetector(new TransportAddress("stun.l.google.com", 19302, Transport.UDP))
    detector.start()
    val socket = new DatagramSocket()
    val localAddress = new InetSocketAddress(socket.getLocalAddress, socket.getLocalPort)
    try {
      val mapped = detector.getMappingFor(new IceUdpSocketWrapper(socket))
      Discovered(localAddress,
                 new InetSocketAddress(mapped.getAddress, mapped.getPort))
    } finally {
      socket.close()
      detector.shutDown()
    }
  }
  
  println(discover())
}
