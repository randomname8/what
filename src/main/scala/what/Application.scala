package what

import scala.collection.concurrent.TrieMap

/**
 * Base application definition. Contains lifecycle methods.
 */
trait Application {

  def start(context: TrieMap[String, Any]): Unit
  def stop(): Unit
}
