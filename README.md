# AWS Logback Appenders

Basic AWS SDK v2 Logback appenders to SNS and SQS.

Appenders are implemented using synchronous AWS clients.

## Why use AWS synchronous API?

To avoid have another hidden thing in your application creating threads for you to manage.

Logback classic provides `ch.qos.logback.classic.AsyncAppender` with some basic configurations for you to handle a
logging queue. You can use this lib with `AsyncAppender` with something like:

```xml

<configuration>
    <appender name="sns" class="br.com.crazycrowd.logback.aws.SnsAppender">
        <maxMessageSize>30</maxMessageSize>
        <snsEndpoint>http://localhost:4566</snsEndpoint>
        <topicArn>arn:aws:sns:us-east-1:000000000000:test-topic-appender</topicArn>
        <encoder>
            <pattern>%-5level: %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="sns-async" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="sns"/>
    </appender>
</configuration>
```

## Using this library

### Configuring AWS Client

This library will allow you to override endpoint configuration for testing reasons with localstack. Besides that,
all other configuration (AWS credentials, region, etc) should be made using environment variables or via instance
profile.

### Careful: dependencies are not included in default JAR!

You can see that all dependencies are not included with jar. This is because if you are using this, you probably are
already including logging and AWS SDK and I did not want to have any version conflicts or vulnerabilities.

If you have in your class path AWS SDK v2 (SQS or SNS), SLF4j and Logback-classic, you should be good to go.

## Running tests

We use localstack via docker-compose. You'll have to have installed docker and docker-compose to run tests.
