package com.github.kerberos.kdc

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kerby.kerberos.kdc.impl.NettyKdcServerImpl
import org.apache.kerby.kerberos.kerb.server.SimpleKdcServer
import org.apache.kerby.kerberos.kerb.server.impl.InternalKdcServer

import collection.JavaConversions._

object SimpleKdcServer {

  var kerbyServer: Option[SimpleKdcServer] = _

  @throws(classOf[Exception])
  def main(args: Array[String]): Unit = {
    val config: Config = ConfigFactory.load
    val kdcHost = config.getString("kdc-server.host")
    if (kdcHost.isEmpty) throw new RuntimeException("KDC Host required")
    val kdcRealm = config.getString("kdc-server.realm")
    if (kdcRealm.isEmpty) throw new RuntimeException("KDC Realm required")
    val kdcTcpPort = config.getInt("kdc-server.port")
    if (!((1024 to 9999) contains kdcTcpPort)) throw new RuntimeException("KDC Port required")
    val workDir = config.getString("kdc-server.work-dir")
    if (workDir.isEmpty) throw new RuntimeException("KDC Work dir required")
    val principals = config.getStringList("kdc-server.principals").toList
    if (principals.isEmpty) throw new RuntimeException("KDC Principal list required")
    sys.addShutdownHook {
      stop()
    }
    start(kdcHost, kdcRealm, kdcTcpPort, workDir, principals)
  }

  @throws(classOf[Exception])
  def start(kdcHost: String, kdcRealm: String, kdcTcpPort: Int, workDir: String, principals: List[String]): Unit = {
    kerbyServer = Option(new SimpleKdcServer())
    if (kerbyServer.isDefined) {
      val server = kerbyServer.get
      setConfig(kdcHost, kdcRealm, kdcTcpPort, workDir, server)
      val nettyKdcServer: Option[InternalKdcServer] = Option(new NettyKdcServerImpl(server.getKdcSetting))
      if (nettyKdcServer.isEmpty) throw new Exception("Failed to create NettyKdcServer")
      else {
        server.setInnerKdcImpl(nettyKdcServer.get)
        server.init()
        createPrincipals(server, principals, workDir)
        println("Starting KDC Server...")
        server.start()
      }
    }
  }

  private def setConfig(kdcHost: String, kdcRealm: String, kdcTcpPort: Int, workDir: String,
                        server: SimpleKdcServer): Unit = {
    server.setKdcHost(kdcHost)
    server.setKdcRealm(kdcRealm)
    server.setWorkDir(new File(workDir))
    server.setKdcTcpPort(kdcTcpPort)
    server.setAllowUdp(false)
  }

  def stop(): Unit = {
    if (kerbyServer != null && kerbyServer.isDefined)
      println("Stopping KDC Server...")
      kerbyServer.get.stop()
  }

  private def createPrincipals(server: SimpleKdcServer, principals: List[String], workDir: String): Unit = {
    principals.map(p => p + "@" + server.getKdcSetting.getKdcRealm)
      .foreach(principal => {
        println("Creating keytab for: " + principal)
        val name: String = "([A-Za-z]+)".r.findFirstMatchIn(principal).mkString
        server.createPrincipal(principal, name)
        server.exportPrincipal(principal, new File(workDir + "/" + name + ".keytab"))
      })
  }

}
