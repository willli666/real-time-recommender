package recengines

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Try,Success,Failure}
import scala.concurrent.Future
import storage._
import cassandra._
import config.Config


class CounterRecommender(storage: CassandraStorage) {

  def trackEvent(event: Event): Unit = {

    val userId = event.userId
    val itemId = event.itemId
    val action = event.action
    val weight = Config.ACTION_WEIGHTS(action)
    val timestamp = event.timestamp

    if (action == "click") checkUserViews(userId, ViewItem(itemId, timestamp))
    if (action == "buy") checkBestSellers(itemId)

  }

  def checkUserViews(userId: String, item: ViewItem): Unit = {
    val userViews = storage.userViews.getById(userId)
    // userViews.onComplete {
    //   case Failure(msg) => println(msg)
    //   case Success(None) => saveNewUserViews(userId, item)
    //   case Success(Some(views)) => updateUserViews(views, item)
    // }

    Try(Await.result(userViews, 1.second)) match {
      case Failure(msg) => println(msg)
      case Success(None) => saveNewUserViews(userId, item)
      case Success(Some(views)) => updateUserViews(views, item)
    }
  }

  def saveNewUserViews(userId: String, item: ViewItem): Unit = {
    Await.result(storage.userViews.store(UserViews(userId, item :: Nil)), 1.second)
  }

  def updateUserViews(lastViews: UserViews, newItem: ViewItem): Unit = {
    if (lastViews.views.exists(_.itemId == newItem.itemId)) {
      val updatedViews = lastViews.views.filter(_.itemId != newItem.itemId)
      Await.result(storage.userViews.store(UserViews(lastViews.userId, newItem :: updatedViews)), 1.second)
    }
    else {
      Await.result(storage.userViews.store(UserViews(lastViews.userId, (newItem :: lastViews.views).take(Config.RECENT_VIEWS_LIST_SIZE))),1.second)
    }
  }

  def checkBestSellers(itemId: String): Unit = {
    val bestSeller = storage.bestSellersIndex.getById(itemId)
    Try(Await.result(bestSeller, 1.second)) match {
      case Failure(msg) => println(msg)
      case Success(None) => saveNewBestSeller(itemId)
      case Success(Some(b)) => updateBestSeller(b)
    }
    // bestSeller.onComplete {
    //   case Failure(msg) => println(msg)
    //   case Success(None) => saveNewBestSeller(itemId)
    //   case Success(Some(b)) => updateBestSeller(b)
    // }
  }

  def saveNewBestSeller(itemId: String): Unit = {
    Await.result(storage.bestSellers.store(BestSeller("bestseller", itemId, 1)),1.second)
    Await.result(storage.bestSellersIndex.incrementCount(itemId),1.second)
  }

  def updateBestSeller(bestSeller: BestSellerIndex): Unit = {
    Await.result(storage.bestSellers.deleteRow(BestSeller("bestseller", bestSeller.itemId, bestSeller.score)),1.second)
    Await.result(storage.bestSellers.store(BestSeller("bestseller", bestSeller.itemId, bestSeller.score + 1)),1.second)
    Await.result(storage.bestSellersIndex.incrementCount(bestSeller.itemId),1.second)
  }

  def getBestSellers(limit: Int = 100): Future[Seq[BestSeller]] = {
    storage.bestSellers.getMany(limit)
  }

  def getRecentViews(userId: String): Future[List[ViewItem]] = {
    storage.userViews.getById(userId).map {
      case Some(views) => views.views
      case None => Nil
    }
  }

}
