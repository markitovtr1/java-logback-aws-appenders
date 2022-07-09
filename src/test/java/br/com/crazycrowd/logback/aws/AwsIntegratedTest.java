package br.com.crazycrowd.logback.aws;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.ListTopicsResponse;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.Topic;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class AwsIntegratedTest {

  private static final SqsClient sqsClient;
  private static final SnsClient snsClient;

  private static final String testTopicArn = "arn:aws:sns:us-east-1:000000000000:test-topic-appender";
  private static final String testQueueAppenderUrl = "http://localhost:4566/000000000000/test-queue-appender";
  private static final String testTopicAppenderQueueUrl = "http://localhost:4566/000000000000/test-topic-appender";
  private static final Logger testLog = LoggerFactory.getLogger("test-logger");

  static {
    try {
      URI localEndpointUri = new URI("http://localhost:4566");
      sqsClient = SqsClient.builder().endpointOverride(localEndpointUri).build();
      snsClient = SnsClient.builder().endpointOverride(localEndpointUri).build();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @BeforeAll
  static void beforeAll() {
    final ListQueuesResponse listQueuesResponse = sqsClient.listQueues();
    final ListTopicsResponse listTopicsResponse = snsClient.listTopics();
    final List<String> queueUrls = listQueuesResponse.queueUrls();
    final List<String> topicsArns =
        listTopicsResponse.topics().stream()
            .map(Topic::topicArn)
            .collect(Collectors.toList());

    checkAndCreateQueue(queueUrls, testQueueAppenderUrl);

    boolean topicCreated = checkAndCreateTestTopic(topicsArns);
    boolean testTopicQueueCreated = checkAndCreateQueue(queueUrls, testTopicAppenderQueueUrl);

    if (topicCreated || testTopicQueueCreated) {
      createTestSubscription();
    }
  }

  @AfterAll
  static void afterAll() {
    purgeQueue(testQueueAppenderUrl);
    purgeQueue(testTopicAppenderQueueUrl);
  }

  private static boolean checkAndCreateQueue(final List<String> queuesUrls, final String queueUrl) {
    if (!queuesUrls.contains(queueUrl)) {
      createQueue(queueUrl.substring(queueUrl.lastIndexOf("/") + 1));
      return true;
    }

    return false;
  }

  private static boolean checkAndCreateTestTopic(final List<String> topicsArns) {
    if (!topicsArns.contains(testTopicArn)) {
      createTopic(testTopicArn.substring(testTopicArn.lastIndexOf(":") + 1));
      return true;
    }

    return false;
  }

  private static void createQueue(final String queueName) {
    final CreateQueueRequest request = CreateQueueRequest.builder()
        .queueName(queueName)
        .build();

    sqsClient.createQueue(request);
  }

  private static void createTestSubscription() {
    final Map<String, String> attributes = new HashMap<>();
    attributes.put("RawMessageDelivery", "true");

    final SubscribeRequest request = SubscribeRequest.builder()
        .topicArn(testTopicArn)
        .endpoint(testTopicAppenderQueueUrl)
        .protocol("sqs")
        .attributes(attributes)
        .build();

    snsClient.subscribe(request);
  }

  private static void createTopic(final String topicName) {
    final CreateTopicRequest request = CreateTopicRequest.builder()
        .name(topicName)
        .build();

    snsClient.createTopic(request);
  }

  private static void purgeQueue(final String queueUrl) {
    final PurgeQueueRequest request = PurgeQueueRequest.builder()
        .queueUrl(queueUrl)
        .build();

    sqsClient.purgeQueue(request);
  }

  static class WhenMessageIsLogged {

    @BeforeAll
    static void beforeAll() {
      final String overMaxSizeMessage = "01234567890123456789012345678901234567890123456789";

      testLog.info(overMaxSizeMessage);
      testLog.info(overMaxSizeMessage);
      testLog.info(overMaxSizeMessage);
      testLog.debug("msg-debug");
      testLog.info("msg-info");
    }

  }

  @Nested
  class ThenSendsMessageToExpectedAwsServices extends WhenMessageIsLogged {

    @Test
    public void sqsAppenderWorks() {
      checkQueueHasExpectedContent(testQueueAppenderUrl);
    }

    @Test
    public void snsAppenderWorks() {
      checkQueueHasExpectedContent(testTopicAppenderQueueUrl);
    }

    private void checkQueueHasExpectedContent(final String queueUrl) {
      final ReceiveMessageRequest request = ReceiveMessageRequest.builder()
          .queueUrl(queueUrl)
          .maxNumberOfMessages(2)
          .waitTimeSeconds(10)
          .build();

      final ReceiveMessageResponse response = sqsClient.receiveMessage(request);
      final List<String> messages = response.messages().stream()
          .map(Message::body)
          .collect(Collectors.toList());

      assertThat(messages).containsExactlyInAnyOrder("DEBUG: msg-debug\n", "INFO : msg-info\n");
    }

  }

}
