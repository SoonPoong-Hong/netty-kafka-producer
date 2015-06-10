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

import static org.hamcrest.CoreMatchers.is;

import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;

import org.junit.Assert;
import org.junit.Test;

public class KafkaTopicSingleBrokerTest extends AbstractSingleBrokerTest {

    public static final String TEST_MESSAGE = "test message from netty - netty likes kafka";

    @Test
    public void test_producer() throws Exception {

        String topic = "test";
        ProducerProperties properties = new ProducerProperties();
        properties.override(ProducerProperties.NETTY_DEBUG_PIPELINE, true);
        createTopic(topic);

        KafkaProducer producer = new KafkaProducer("localhost", START_PORT, topic, properties);
        producer.connect().sync();
        KafkaTopic kafkaTopic = producer.topic();

        kafkaTopic.send(null, freeLaterBuffer((TEST_MESSAGE + "01").getBytes()));
        kafkaTopic.send(null, freeLaterBuffer((TEST_MESSAGE + "02").getBytes()));
        kafkaTopic.send(null, freeLaterBuffer((TEST_MESSAGE + "03").getBytes()));

        final KafkaStream<byte[], byte[]> stream = consume(topic).get(0);
        final ConsumerIterator<byte[], byte[]> messages = stream.iterator();

        Assert.assertThat(new String(messages.next().message()), is(TEST_MESSAGE + "01"));
        Assert.assertThat(new String(messages.next().message()), is(TEST_MESSAGE + "02"));
        Assert.assertThat(new String(messages.next().message()), is(TEST_MESSAGE + "03"));
        producer.disconnect().sync();
    }
}
