package com.butterfly.aws;

import software.amazon.awscdk.App;

/**
 * AWS CDK App that shows how to use AwsCdkSpringPostgresEcsFargateStack.
 * It creates a stack named 'butterfly'. Creates a database with name 'butterflydb',
 * username 'butter', password 'flyingbutter'. Creates a ECS Cluster that uses a Docker
 * image from 'docker.io/mlopezsoto/volunt:latest'
 */
public class AwsCdkSpringPostgresEcsFargateApp {
    public static void main(final String[] args) {
        App app = new App();

        AwsCdkSpringPostgresEcsFargateStack.Builder.create(app, "butterfly", "butterflydb", "butter", "flyingbutter", "docker.io/mlopezsoto/volunt:latest")
                .ecsTaskCpu(256)
                .ecsTaskMemoryLimitMiB(512)
                .ecsTaskDesiredCount(1)
                .springContainerPort(8080)
                .targetGroupHealthCheckPath("/actuator/health")
                .vpcMaxAzs(3)
                .build();

        app.synth();
    }
}

