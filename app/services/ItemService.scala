package services

import models.Item
import scalikejdbc.{AutoSession, DBSession}

import scala.concurrent.Future
import scala.util.Try

trait ItemService {

  def getItemAndCreateByCode(itemCode: String)(implicit dbSession: DBSession = AutoSession): Future[Item]

  def getItemByCode(itemCode: String)(implicit dbSession: DBSession = AutoSession): Future[Option[Item]]

  def getItemById(itemId: Long)(implicit dbSession: DBSession = AutoSession): Future[Option[Item]]

  def getItemsByUserId(userId: Long)(implicit dbSession: DBSession = AutoSession): Try[Seq[Item]]

  def getLatestItems(limit: Int = 20): Try[Seq[Item]]

  def searchItems(keywordOpt: Option[String]): Future[Seq[Item]]

}
