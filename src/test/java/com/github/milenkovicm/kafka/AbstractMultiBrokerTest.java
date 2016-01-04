/*
 * Copyright 2015 Marko Milenkovic (http://github.com/milenkovicm)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.milenkovicm.kafka;

import java.io.File;
import java.util.*;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.KafkaStream;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServer;
import kafka.utils.MockTime;
import kafka.utils.TestUtils;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import kafka.zk.EmbeddedZookeeper;

import org.I0Itec.zkclient.IDefaultNameSpace;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkServer;
import org.apache.kafka.common.protocol.SecurityProtocol;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import scala.Option;

public abstract class AbstractMultiBrokerTest extends AbstractTest {

    protected static final int TIMEOUT = 5000;
    protected static final int START_BROKER_ID = 0;
    protected static final int START_PORT = TestUtils.RandomPort();
    protected static final int DEFAULT_BROKER_COUNT = 3;

    protected volatile static int BROKER_COUNT = DEFAULT_BROKER_COUNT;

    protected static ZkServer zkServer;
    protected static ZkClient zkClient;
    protected static ZkUtils zkUtils;

    protected static volatile String zkConnect;
    protected static volatile List<KafkaServer> kafkaServers;
    protected static final IDefaultNameSpace DEFAULT_NAME_SPACE = new IDefaultNameSpace() {
        @Override
        public void createDefaultNameSpace(ZkClient zkClient) {

        }
    };

    @BeforeClass
    public static void start() {
        kafkaServers = new ArrayList<>();

        zkServer = new ZkServer("/tmp/zk/data","/tmp/zk/log",DEFAULT_NAME_SPACE);
        zkServer.start();

        zkClient = zkServer.getZkClient();
        zkUtils =  ZkUtils.apply(zkClient,false);

        for (int i = 0; i < BROKER_COUNT; i++) {
            LOGGER.info("starting test broker id: [{}] at port: [{}]", START_BROKER_ID + i, START_PORT + i);

            Properties properties = TestUtils.createBrokerConfig(START_BROKER_ID + i,TestUtils.MockZkConnect(),
                    true, false,START_PORT + i,
                    Option.apply(SecurityProtocol.PLAINTEXT),Option.<File>empty(),true,false,0,false,0,false,0);
            KafkaConfig configuration = new KafkaConfig(properties);

            KafkaServer kafkaServer = TestUtils.createServer(configuration, new MockTime());
            kafkaServers.add(kafkaServer);
        }
    }

    @AfterClass
    public static void stop() {
        for (KafkaServer kafkaServer : kafkaServers) {
            kafkaServer.shutdown();
        }
        kafkaServers.clear();
        zkClient.close();
        zkServer.shutdown();
    }

    /**
     * Creates {@code topic} on currently running broker
     *
     * @param topic
     *        name to create
     */
    public static void createTopic(String topic) {
        createTopic(topic, 1, 1);
    }

    public static void createTopic(String topic, Integer partitionNum) {
        createTopic(topic, partitionNum, 1);
    }

    public static void createTopic(String topic, Integer partitionNum, Integer replicas) {
        TestUtils
                .createTopic(zkUtils, topic, partitionNum, replicas, scala.collection.JavaConversions.asScalaBuffer(kafkaServers), new Properties());
        TestUtils.waitUntilMetadataIsPropagated(scala.collection.JavaConversions.asScalaBuffer(kafkaServers), topic, 0, TIMEOUT);
    }

    public List<KafkaStream<byte[], byte[]>> consume(String topic) {
        Properties consumerProperties = TestUtils.createConsumerProperties(zkConnect, UUID.randomUUID().toString(), "client", TIMEOUT);
        //consumerProperties.setProperty("serializer.class", "kafka.serializer.StringEncoder");

        ConsumerConfig consumerConfig = new ConsumerConfig(consumerProperties);

        Map<String, Integer> topicCountMap = new HashMap<>();
        topicCountMap.put(topic, 1); // not sure why is this 1

        Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = Consumer.createJavaConsumerConnector(consumerConfig).createMessageStreams(
                topicCountMap);

        List<KafkaStream<byte[], byte[]>> streams = consumerMap.get(topic);

        return streams;
    }
}
