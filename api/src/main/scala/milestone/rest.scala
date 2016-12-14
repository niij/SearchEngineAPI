package com.milestone

import org.apache.http.HttpResponse
import org.apache.http.util.EntityUtils
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.protocol.HttpClientContext
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


case class HttpResp(header: String, body: String, statusCode: Int)
object HttpResp {
  def apply(resp: HttpResponse): HttpResp = {
    val entity = resp.getEntity
    var header, body = ""
    var status = -1
    if (entity != null) {
      body = EntityUtils.toString(entity)
    }
    header = resp.getAllHeaders().mkString
    status = resp.getStatusLine().getStatusCode()
    new HttpResp(header, body, status)
  }
}

trait HttpWorker {
  def Post(url: String, body: Map[String,String]): HttpResp
  def Get(url: String): HttpResp
}

object HttpWorker {
  val poolingConnManager = new PoolingHttpClientConnectionManager()
  poolingConnManager.setMaxTotal(100);
  poolingConnManager.setDefaultMaxPerRoute(20);
  val httpClient = HttpClients.custom.setConnectionManager(poolingConnManager).build

  def Post(url: String, body: Map[String, String]): Future[HttpResp] = Future {
    val post = new HttpPost(url)
    body.foreach{
      case(k,v) => post.addHeader(k, v)
    }
    val context = HttpClientContext.create()
    val full_resp = httpClient.execute(post, context)
    val response = HttpResp(full_resp)
    EntityUtils.consume(full_resp.getEntity)
    full_resp.close
    response
  }

  def Get(url: String): Future[HttpResp] = Future {
    val get = new HttpGet(url)
    val context = HttpClientContext.create()
    val full_resp = httpClient.execute(get, context)
    val response = HttpResp(full_resp)
    EntityUtils.consume(full_resp.getEntity)
    full_resp.close
    response
  }
}
