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
    builder.setSpout("kafka_spout", kafkaSpout, 1)
    builder.setBolt("item_item_bolt", new ItemItemBolt(), 1).shuffleGrouping("kafka_spout")
    builder.setBolt("trending_bolt", new TrendingBolt(), 1).shuffleGrouping("kafka_spout")
    builder.setBolt("counter_bolt", new CounterBolt(), 1).shuffleGrouping("kafka_spout")
    builder.setBolt("stats_bolt", new StatsBolt(), 1).shuffleGrouping("kafka_spout")

    val config = new StormConfig()
    config.setDebug(false)
    config.setMessageTimeoutSecs(7200)
    config.setMaxSpoutPending(100000)
    config.setTopologyWorkerMaxHeapSize(8192)

    StormSubmitter.submitTopology("product-recommender", config, builder.createTopology())
  }

}
