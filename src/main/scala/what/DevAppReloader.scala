package what

import java.io.IOException
import java.net.URLClassLoader
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{ Files , StandardWatchEventKinds, FileVisitor, FileVisitResult, Path , WatchEvent }
import scala.collection.JavaConverters._

/**
 * Main application used during developement for hot reloading of the application using classloaders magic.
 */
object DevAppReloader extends App {
  //install a monitor on the classes to detect a change
  val classesDir = new java.io.File(getClass.getResource("/").toURI).toPath
  val fileWatcher = classesDir.getFileSystem.newWatchService
  Files.walkFileTree(classesDir, new FileVisitor[Path] {
      override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes) = {
        dir.register(fileWatcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY)
        FileVisitResult.CONTINUE
      }
      override def postVisitDirectory(dir: Path, excec: IOException) = {
        FileVisitResult.CONTINUE
      }
      override def visitFile(file: Path, attrs: BasicFileAttributes) = {
        FileVisitResult.CONTINUE
      }
      override def visitFileFailed(file: Path, excec: IOException) = {
        FileVisitResult.TERMINATE
      }
    })
  classesDir.register(fileWatcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY)

  new Thread("ClassesChangesWatcher") {
    override def run(): Unit = {
      println("watching")
      var updateFound = false
      var lastUpdate = System.currentTimeMillis

      while(!isInterrupted()) {
        val now = System.currentTimeMillis
        val wk = fileWatcher.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS)
        if (wk != null) {
          wk.pollEvents.asScala foreach {
            case watchEvent: WatchEvent[Path @unchecked] =>
              val context = wk.watchable.asInstanceOf[Path].resolve(watchEvent.context)

              if (watchEvent.kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(context)) {
                context.register(fileWatcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY)
              }
          }
          wk.reset()
          updateFound = true
          lastUpdate = now
        }
        if (updateFound && (now - lastUpdate) > 300) { //if there was some time ago, trigger reloading
          updateFound = false
          reloadApp()
        }
      }
      println("watcher dying")
    }
  }.start()
  
  reloadApp()

  @volatile var recompiling = false
  var lastApplication: Application = _
  lazy val appContext = new collection.concurrent.TrieMap[String, Any]()
  def reloadApp(): Unit = {
    if (!recompiling) { // if I'm already recompiling, ignore the request. This might happen if the watcher thread detects many file changing in quick not not so quick intervals
      println(Console.CYAN + "RELOADING" + Console.RESET)
      recompiling = true

      //is there was an application, we need to dispose of it first
      if (lastApplication != null) {
        lastApplication.stop()
        val cl = lastApplication.getClass.getClassLoader.asInstanceOf[URLClassLoader]
        lastApplication = null
        System.gc()
        cl.close()
      }

      val loader = new URLClassLoader(Array(getClass.getResource("/"))) {
        //override default class loader behaviour to prioritize classes in this classloader
        val ApplicationClass = classOf[Application]
        override def loadClass(name: String, resolve: Boolean): Class[_] = {
          if (name == ApplicationClass.getName) return ApplicationClass
          var res: Class[_] = findLoadedClass(name)
          val startTime = System.currentTimeMillis
          while (res == null && System.currentTimeMillis - startTime < 5000) {//will some time before giving up finding the class
            try res = findClass(name)
            catch { case e: ClassNotFoundException =>
                try res = super.loadClass(name, false)
                catch { case e: ClassNotFoundException => Thread.sleep(50) } //sleep 50ms and retry
            }
          }
          if (res == null) throw new ClassNotFoundException(name)
          if (resolve) resolveClass(res)
          res
        }
      }

      try {
        lastApplication = loader.loadClass(args(0)).newInstance.asInstanceOf[Application]
        lastApplication.start(appContext)
      } catch {
        case e: Exception => e.printStackTrace()
      }
      recompiling = false
    }
  }

}
