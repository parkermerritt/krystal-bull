package com.krystal.bull.core.storage

import com.krystal.bull.core.SigningVersion
import org.bitcoins.crypto.{FieldElement, SchnorrNonce}
import org.bitcoins.db.{AppConfig, CRUD, DbCommonsColumnMappers, SlickUtil}
import slick.lifted.{ForeignKeyQuery, ProvenShape}

import scala.concurrent.{ExecutionContext, Future}

case class EventDAO()(implicit
    val ec: ExecutionContext,
    override val appConfig: AppConfig)
    extends CRUD[EventDb, SchnorrNonce]
    with SlickUtil[EventDb, SchnorrNonce] {

  import profile.api._

  private val mappers = new DbCommonsColumnMappers(profile)

  import mappers._

  implicit val fieldElementMapper: BaseColumnType[FieldElement] =
    MappedColumnType.base[FieldElement, String](_.hex, FieldElement.fromHex)

  implicit val signingVersionMapper: BaseColumnType[SigningVersion] =
    MappedColumnType.base[SigningVersion, String](_.toString,
                                                  SigningVersion.fromString)

  override val table: TableQuery[EventTable] = TableQuery[EventTable]

  private lazy val rValueTable: TableQuery[RValueDAO#RValueTable] =
    RValueDAO().table

  override def createAll(ts: Vector[EventDb]): Future[Vector[EventDb]] =
    createAllNoAutoInc(ts, safeDatabase)

  override protected def findByPrimaryKeys(
      ids: Vector[SchnorrNonce]): Query[EventTable, EventDb, Seq] =
    table.filter(_.nonce.inSet(ids))

  override protected def findAll(
      ts: Vector[EventDb]): Query[EventTable, EventDb, Seq] =
    findByPrimaryKeys(ts.map(_.nonce))

  def getPendingEvents: Future[Vector[EventDb]] = {
    val query = table.filter(_.attestationOpt.inSet(None))

    safeDatabase.runVec(query.result.transactionally)
  }

  class EventTable(tag: Tag) extends Table[EventDb](tag, schemaName, "events") {

    def nonce: Rep[SchnorrNonce] = column("nonce", O.PrimaryKey)

    def label: Rep[String] = column("label")

    def numOutcomes: Rep[Long] = column("num_outcomes")

    def signingVersion: Rep[SigningVersion] = column("signing_version")

    def attestationOpt: Rep[Option[FieldElement]] = column("attestation")

    def * : ProvenShape[EventDb] =
      (nonce,
       label,
       numOutcomes,
       signingVersion,
       attestationOpt) <> (EventDb.tupled, EventDb.unapply)

    def fk: ForeignKeyQuery[_, RValueDb] = {
      foreignKey("fk_nonce",
                 sourceColumns = nonce,
                 targetTableQuery = rValueTable)(_.nonce)
    }
  }
}