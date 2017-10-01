package what

import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.stage.Stage
import scala.collection.concurrent.TrieMap
import scala.util.control.NonFatal

class TestApp extends Application {
  
  class JfxApp extends javafx.application.Application {
    var primaryStage: Stage = _
    override def start(primaryStage: Stage): Unit = {
      this.primaryStage = primaryStage
      primaryStage setTitle "Test JavafxApplication"
      primaryStage.setScene(new Scene(new Label("Hi, Javafx!"), 500, 50))
      primaryStage.sizeToScene()
      primaryStage.show()
    }
  }
  
  @volatile var jfxApp: JfxApp = _
  override def start(context: TrieMap[String, Any]) = {
    sys.props("prism.lcdtext") = "false"
    sys.props("prism.text") = "t2k"
    
    context.get(getClass.getName + ".jfxstage") match {
      case None =>
        try {
          new JFXPanel() //trigger javafx initialization if necessary
          Platform runLater {() =>
            Platform.setImplicitExit(false)
            jfxApp = new JfxApp()
            val stage = new Stage()
            context(getClass.getName + ".jfxstage") = stage
            jfxApp.start(stage)
          }
        } catch { case NonFatal(e) => e.printStackTrace() }
      case Some(stage: Stage) => Platform runLater {() => jfxApp = new JfxApp(); jfxApp.start(stage) }
    }
  }
  override def stop() = {
  }
}
