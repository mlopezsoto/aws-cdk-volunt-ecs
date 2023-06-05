package com.myorg;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.constructs.Construct;

import java.util.Collections;
import java.util.Map;

public class AwsCdkVoluntEcsStack extends Stack {

    public AwsCdkVoluntEcsStack(final Construct scope, final String idPrefix, Vpc vpc, String dbAddress, String dbPort, ISecurityGroup databaseSecurityGroup) {
        super(scope, idPrefix + "-ecs", null);


        Cluster cluster = Cluster.Builder.create(this, idPrefix + "-ecs-cluster")
                .vpc(vpc)
                .build();

        // Fargate security group
        final SecurityGroup fargateSecurityGroup = new SecurityGroup(this, idPrefix + "-ecs-FargateSecurityGroup",  SecurityGroupProps.builder()
                .vpc(vpc)
                .build());

        fargateSecurityGroup.addEgressRule(Peer.anyIpv4(), Port.tcp(5432));

        databaseSecurityGroup.addEgressRule(Peer.securityGroupId(fargateSecurityGroup.getSecurityGroupId()), Port.tcp(5432));

        String datasourceUrl = "jdbc:postgresql://" + dbAddress + ":" + dbPort + "/volunt";
        new CfnOutput(this, "datasourceUrl", CfnOutputProps.builder().value(datasourceUrl).build());

        // Create a load-balanced Fargate service and make it public
        ApplicationLoadBalancedFargateService.Builder.create(this, idPrefix + "-ecs-service")
                .cluster(cluster)           // Required
                .cpu(512)                   // Default is 256
                .desiredCount(6)
                .taskImageOptions(
                        ApplicationLoadBalancedTaskImageOptions.builder()
                                .image(ContainerImage.fromRegistry("docker.io/mlopezsoto/volunt:latest"))
                                .containerPort(8080)
                                .environment(Map.of(
                                        "SPRING_DATASOURCE_URL", datasourceUrl,
                                        "SPRING_DATASOURCE_USERNAME", "volunt",
                                        "SPRING_DATASOURCE_PASSWORD", "voluntpwd"
                                ))
                                .build())

                .memoryLimitMiB(1024)       // Default is 512
                .publicLoadBalancer(true)   // Default is true
                .assignPublicIp(true)
                .securityGroups(Collections.singletonList(fargateSecurityGroup))
                .build();
    }
}
