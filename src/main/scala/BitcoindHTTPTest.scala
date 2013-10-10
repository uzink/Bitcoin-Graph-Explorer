/**
 * Created with IntelliJ IDEA.
 * User: stefan
 * Date: 9/11/13
 * Time: 6:37 PM
 * To change this template use File | Settings | File Templates.
 */

import bddb.BlocksReader
import scala.slick.driver.MySQLDriver.simple._
import Database.threadLocalSession
import java.sql.DriverManager;
import play.api.libs.json.JsValue
import scala.concurrent._
import ExecutionContext.Implicits.global
import dispatch._
import com.sagesex.JsonRPCProxy

object BitcoindHTTPTest extends App {

  BlocksReader.user ="user"
  BlocksReader.pass ="pass"
  BlocksReader.url = "http://127.0.0.1:8332"


  Database.forURL(
      url = "jdbc:mysql://localhost/bitcoin",
      driver = "com.mysql.jdbc.Driver",
      user = "root",
      password = "12345"

  )  withSession { BlocksReader.run }


}