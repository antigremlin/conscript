package conscript

import dispatch._
import com.ning.http.client.RequestBuilder

trait Credentials {
  import scala.util.control.Exception.allCatch
  
  def withCredentials(req: RequestBuilder) =
    (oauth map { case token => req.addHeader("Authorization", "token %s".format(token)) }).orElse(
      credentials map { case (user, pass) => req as (user, pass) }) getOrElse req
  
  def oauth: Option[String] =
    Config.get("gh.oauth")
  
  lazy val credentials: Option[(String, String)] =
    gitConfig("github.user") flatMap { user =>
      gitConfig("github.password") map { password =>
        (user, password)
      }    
    }
  
  // https://github.com/defunkt/gist/blob/master/lib/gist.rb#L237
  def gitConfig(key: String): Option[String] =
    allCatch opt {
      Option(System.getenv(key.toUpperCase.replaceAll("""\.""", "_"))) map { Some(_) } getOrElse {
        val gitExec = windows map {_ => "git.exe"} getOrElse {"git"}
        val p = new java.lang.ProcessBuilder(gitExec, "config", "--global", key).start()
        val reader = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream))
        Option(reader.readLine)
      }
    } getOrElse {None}
  
  def windows =
    System.getProperty("os.name") match {
      case x: String if x contains "Windows" => Some(x)
      case _ => None
    }
}
