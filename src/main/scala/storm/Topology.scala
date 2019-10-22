package storm

import org.apache.storm.kafka.{KafkaSpout, SpoutConfig, ZkHosts}
import org.apache.storm.topology.TopologyBuilder
import org.apache.storm.{StormSubmitter, Config => StormConfig}
import config.Config
import storm.bolts.{CounterBolt, ItemItemBolt, StatsBolt, TrendingBolt}


object Topology {

  def createKafkaSpout(zkString: String, topic: String, zkSpoutId: String): KafkaSpout = {
    val zkConnString = zkString
    val zkHosts = new ZkHosts(zkConnString)
    val spoutConfig = new SpoutConfig(zkHosts, topic, "/" + topic, zkSpoutId)
    val kafkaSpout = new KafkaSpout(spoutConfig)
    kafkaSpout
  }

  def main(args: Array[String]): Unit = {

    val kafkaSpout = createKafkaSpout(Config.ZK_STRING, Config.TOPIC, Config.ZK_SPOUT_ID)

    val builder = new TopologyBuilder()
    builder.setSpout("kafka_spout", kafkaSpout, 2)
    builder.setBolt("item_item_bolt", new ItemItemBolt(), 2).shuffleGrouping("kafka_spout")
    builder.setBolt("trending_bolt", new TrendingBolt(), 2).shuffleGrouping("kafka_spout")
    builder.setBolt("counter_bolt", new CounterBolt(), 2).shuffleGrouping("kafka_spout")
    builder.setBolt("stats_bolt", new StatsBolt(), 2).shuffleGrouping("kafka_spout")

    val config = new StormConfig()
    config.setDebug(true)
    config.setMessageTimeoutSecs(30)
    config.setMaxSpoutPending(10000)
    config.setTopologyWorkerMaxHeapSize(4096)

    StormSubmitter.submitTopology("product-recommender", config, builder.createTopology())
  }

}
