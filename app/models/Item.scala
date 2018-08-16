package models

import java.time.ZonedDateTime

import scalikejdbc._
import jsr310._
import skinny.orm._
import skinny.orm.feature.CRUDFeatureWithId
import skinny.orm.feature.associations.HasManyAssociation

case class Item(id: Option[Long],
                code: String,
                name: String,
                url: String,
                imageUrl: String,
                price: Int,
                createAt: ZonedDateTime,
                updateAt: ZonedDateTime,
                users: Seq[User] = Seq.empty, // 追加
                haveUsers: Seq[User] = Seq.empty, // 追加
                wantUsers: Seq[User] = Seq.empty) // 追加

object Item extends SkinnyCRUDMapper[Item] {

  // 追加: allAssociations, allUsersRef, wantUsersRefを追加する
  lazy val allAssociations: CRUDFeatureWithId[Long, Item] = joins(allUsersRef, wantUsersRef, haveUsersRef)

  val allUsersRef: HasManyAssociation[Item] = hasManyThrough[User](
    through = ItemUser,
    many = User,
    merge = (item, users) => item.copy(users = users)
  )

  val wantUsersRef: HasManyAssociation[Item] = hasManyThrough[ItemUser, User](
    through = ItemUser -> ItemUser.defaultAlias,
    throughOn = (item: Alias[Item], itemUser: Alias[ItemUser]) => sqls.eq(item.id, itemUser.itemId),
    many = User -> User.createAlias("i_in_u_want"),
    on = (itemUser: Alias[ItemUser], user: Alias[User]) =>
      sqls.eq(itemUser.userId, user.id).and.eq(itemUser.`type`, WantHaveType.Want.toString),
    merge = (item, wantUsers) => item.copy(wantUsers = wantUsers)
  )

  val haveUsersRef: HasManyAssociation[Item] = hasManyThrough[ItemUser, User](
    through = ItemUser -> ItemUser.defaultAlias,
    throughOn = (item: Alias[Item], itemUser: Alias[ItemUser]) => sqls.eq(item.id, itemUser.itemId),
    many = User -> User.createAlias("i_in_u_have"),
    on = (itemUser: Alias[ItemUser], user: Alias[User]) =>
      sqls.eq(itemUser.userId, user.id).and.eq(itemUser.`type`, WantHaveType.Have.toString),
    merge = (item, haveUsers) => item.copy(haveUsers = haveUsers)
  )

  override def tableName: String = "items"

  override def defaultAlias: Alias[Item] = createAlias("i")

  private def toNamedValues(record: Item): Seq[(Symbol, Any)] = Seq(
    'code     -> record.code,
    'name     -> record.name,
    'url      -> record.url,
    'imageUrl -> record.imageUrl,
    'price    -> record.price,
    'createAt -> record.createAt,
    'updateAt -> record.updateAt
  )

  // 変更
  override def extract(rs: WrappedResultSet, n: ResultName[Item]): Item =
    autoConstruct(rs, n, "users", "haveUsers", "wantUsers") // users, wantUsersを除外する


  def create(item: Item)(implicit session: DBSession = AutoSession): Long =
    createWithAttributes(toNamedValues(item): _*)

  def update(item: Item)(implicit session: DBSession = AutoSession): Int =
    updateById(item.id.get).withAttributes(toNamedValues(item): _*)

}