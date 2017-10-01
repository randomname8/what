package what.net

import io.aeron.Aeron
import io.aeron.ImageFragmentAssembler
import io.aeron.driver.MediaDriver
import java.net.InetSocketAddress

class RemoteCoordinator(val mediaDriver: MediaDriver, val local: InetSocketAddress, val remote: InetSocketAddress) {
  
  private[this] val aeron = 
    Aeron.connect(new Aeron.Context().aeronDirectoryName(mediaDriver.aeronDirectoryName).
                  availableImageHandler(image => println(s"Image ${image.sourceIdentity} is avialable")).
                  unavailableImageHandler(image => println(s"Image ${image.sourceIdentity} is unavialable")))
  
  private def aeron(addr: InetSocketAddress) = s"aeron:udp?endpoint=${addr.getAddress.getHostAddress}:${addr.getPort}"
  private[this] val responsesStream = aeron.addSubscription(aeron(remote), 0)
  private[this] val remoteUpdatesStream = aeron.addSubscription(aeron(remote), 1)
  private[this] val queriesStream = aeron.addPublication(aeron(remote), 2)
  
  private[this] val myResponsesStream = aeron.addPublication(aeron(local), 0)
  private[this] val myStatesStream = aeron.addPublication(aeron(local), 1)
  private[this] val myQueriesStream = aeron.addSubscription(aeron(local), 2)
  
  private[this] val fragmentHandler = new ImageFragmentAssembler((buffer, offset, length, header) => {})
}

object RemoteCoordinatorTest extends App {
  val punch1 = StunClient.discover()
  val punch2 = StunClient.discover()
  println(punch1)
  println(punch2)

  val mediaDriver = MediaDriver.launchEmbedded()
  println(s"Aeron directory: ${mediaDriver.aeronDirectoryName}")
  new RemoteCoordinator(mediaDriver, punch1.local, punch2.external)
  new RemoteCoordinator(mediaDriver, punch2.local, punch1.external)
  new RemoteCoordinator(mediaDriver, new InetSocketAddress("127.0.0.1", 30123), new InetSocketAddress("127.0.0.1", 30124))
  new RemoteCoordinator(mediaDriver, new InetSocketAddress("127.0.0.1", 30124), new InetSocketAddress("127.0.0.1", 30123))
}