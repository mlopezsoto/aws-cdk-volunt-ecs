package com.butterfly.aws;

import com.butterfly.util.Tuple;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.rds.*;
import software.constructs.Construct;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * CDK Stack that creates a Database and ECS Cluster. The ECS service task assumes that the
 * image being used is a Spring Boot App and sets environment variables for the application to be
 * able to connect to the database. ECS service is exposed to the internet using an ALB.
 */
public class AwsCdkSpringPostgresEcsFargateStack extends Stack {

    private final String stackId;

    private final String dbName;

    private final String dbUserName;

    private final String dbPassword;

    private final String springBootAppDockerImage;

    private final int vpcMaxAzs;

    private final int ecsTaskCpu;

    private final int ecsTaskDesiredCount;

    private final int ecsTaskMemoryLimitMiB;

    private final String targetGroupHealthCheckPath;

    private final int springContainerPort;




    private AwsCdkSpringPostgresEcsFargateStack(Builder builder) {
        super(builder.scope, builder.stackId, null);

        this.stackId = builder.stackId;
        this.dbName = builder.dbName;
        this.dbUserName = builder.dbUserName;
        this.dbPassword = builder.dbPassword;
        this.springBootAppDockerImage = builder.appDockerImage;
        this.vpcMaxAzs = builder.vpcMaxAzs;
        this.ecsTaskCpu = builder.ecsTaskCpu;
        this.ecsTaskDesiredCount = builder.ecsTaskDesiredCount;
        this.ecsTaskMemoryLimitMiB = builder.ecsTaskMemoryLimitMiB;
        this.targetGroupHealthCheckPath = builder.targetGroupHealthCheckPath;
        this.springContainerPort = builder.springContainerPort;

        Vpc vpc = getVpc(stackId);
        Tuple<DatabaseInstance, SecurityGroup> databaseAndEcsSecurityGroup = getDatabaseInstanceAndEcsSecurityGroup(vpc);
        ApplicationLoadBalancedFargateService applicationLoadBalancedFargateService = getEcs(vpc, databaseAndEcsSecurityGroup);

        new CfnOutput(this, "ALB DNS Name", CfnOutputProps.builder().value(applicationLoadBalancedFargateService.getLoadBalancer().getLoadBalancerDnsName()).build());

    }

    private Vpc getVpc(String stackId) {

        return Vpc.Builder.create(this, stackId + "-vpc")
                .maxAzs(vpcMaxAzs)  // Default is all AZs in region
                .subnetConfiguration(List.of(
                        SubnetConfiguration.builder()
                                .name("Public")
                                .subnetType(SubnetType.PUBLIC)
                                .cidrMask(24)
                                .build(),
                        SubnetConfiguration.builder()
                                .name("Isolated")
                                .subnetType(SubnetType.PRIVATE_ISOLATED)
                                .cidrMask(24)
                                .build()))
                .ipAddresses(IpAddresses.cidr("10.0.0.0/16"))
                .build();
    }

    private Tuple<DatabaseInstance, SecurityGroup> getDatabaseInstanceAndEcsSecurityGroup(Vpc vpc) {

        final SecurityGroup ecsSecurityGroup = new SecurityGroup(this,  stackId + "-ecs-fargate-security-group",  SecurityGroupProps.builder()
                .vpc(vpc)
                .description("ECS Security Group")
                .build());
        ecsSecurityGroup.addEgressRule(Peer.anyIpv4(), Port.tcp(5432), "Egress rule to DB port");

        final SecurityGroup databaseSecurityGroup =  new SecurityGroup(this, stackId + "-database-security-group", SecurityGroupProps.builder()
                .vpc(vpc)
                .description("Database Security Group")
                .build());

        databaseSecurityGroup.addIngressRule(Peer.securityGroupId(ecsSecurityGroup.getSecurityGroupId()), Port.tcp(5432), "Ingress rule to DB port");


        final IInstanceEngine instanceEngine = DatabaseInstanceEngine.postgres(
                PostgresInstanceEngineProps.builder()
                        .version(PostgresEngineVersion.VER_14_7)
                        .build()
        );

        DatabaseInstance databaseInstance = DatabaseInstance.Builder.create(this, stackId + "-database-postgres")
                .vpc(vpc)
                .databaseName(dbName)
                .securityGroups((Collections.singletonList(databaseSecurityGroup)))
                .vpcSubnets(SubnetSelection.builder().subnets(vpc.getIsolatedSubnets()).build())
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.MICRO))
                .engine(instanceEngine)
                .credentials(Credentials.fromPassword(dbUserName, SecretValue.unsafePlainText(dbPassword)))
                .instanceIdentifier(stackId + "-database-postgresql")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        new CfnOutput(this, "Database Address", CfnOutputProps.builder().value(databaseInstance.getDbInstanceEndpointAddress()).build());
        new CfnOutput(this, "Database Port", CfnOutputProps.builder().value(databaseInstance.getDbInstanceEndpointPort()).build());

        return new Tuple<>(databaseInstance, ecsSecurityGroup);
    }

    private ApplicationLoadBalancedFargateService getEcs(Vpc vpc, Tuple<DatabaseInstance, SecurityGroup> databaseAndEcsSecurityGroup) {
        Cluster cluster = Cluster.Builder.create(this, stackId + "-ecs-cluster")
                .vpc(vpc)
                .build();

        String datasourceUrl = "jdbc:postgresql://" + databaseAndEcsSecurityGroup.getElement1().getDbInstanceEndpointAddress() + ":" + databaseAndEcsSecurityGroup.getElement1().getDbInstanceEndpointPort() + "/" + dbName;
        new CfnOutput(this, "datasourceUrl", CfnOutputProps.builder().value(datasourceUrl).build());

        // Create a load-balanced Fargate service and make it public
        var albfs = ApplicationLoadBalancedFargateService.Builder.create(this, stackId + "-ecs-service")
                .cluster(cluster)           // Required
                .cpu(ecsTaskCpu)                   // Default is 256
                .desiredCount(ecsTaskDesiredCount)
                .taskImageOptions(
                        ApplicationLoadBalancedTaskImageOptions.builder()
                                .image(ContainerImage.fromRegistry(springBootAppDockerImage))
                                .containerPort(springContainerPort)
                                .environment(Map.of(
                                        "SPRING_DATASOURCE_URL", datasourceUrl,
                                        "SPRING_DATASOURCE_USERNAME", dbUserName,
                                        "SPRING_DATASOURCE_PASSWORD", dbPassword
                                ))
                                .build())

                .memoryLimitMiB(ecsTaskMemoryLimitMiB)       // Default is 512
                .publicLoadBalancer(true)   // Default is true
                .assignPublicIp(true)
                .securityGroups(Collections.singletonList(databaseAndEcsSecurityGroup.getElement2()))
                .build();

        albfs.getTargetGroup().configureHealthCheck(new HealthCheck.Builder()
                .path(targetGroupHealthCheckPath)
                .port(String.valueOf(springContainerPort))
                .healthyHttpCodes("200")
                .build());

        return albfs;
    }

    public static class Builder {

        private final Construct scope;

        private final String stackId;

        private final String dbName;

        private final String dbUserName;

        private final String dbPassword;

        private final String appDockerImage;

        private int vpcMaxAzs = 3;

        private int ecsTaskCpu = 512;

        private int ecsTaskDesiredCount = 2;

        private int ecsTaskMemoryLimitMiB = 1024;

        private String targetGroupHealthCheckPath = "/";

        private int springContainerPort = 8080;

        private Builder(Construct scope, String stackId, String dbName, String dbUserName, String dbPassword, String appDockerImage) {
            this.scope = scope;
            this.stackId = stackId;
            this.dbName = dbName;
            this.dbUserName = dbUserName;
            this.dbPassword = dbPassword;
            this.appDockerImage = appDockerImage;
        }

        public static Builder create(Construct scope, String stackId, String dbName, String dbUserName, String dbPassword, String appDockerImage) {
            return new Builder(scope, stackId, dbName, dbUserName, dbPassword, appDockerImage);
        }

        public Builder vpcMaxAzs(int vpcMaxAzs) {
            this.vpcMaxAzs = vpcMaxAzs;
            return this;
        }

        public Builder ecsTaskCpu(int ecsTaskCpu) {
            this.ecsTaskCpu = ecsTaskCpu;
            return this;
        }

        public Builder ecsTaskDesiredCount(int ecsTaskDesiredCount) {
            this.ecsTaskDesiredCount = ecsTaskDesiredCount;
            return this;
        }

        public Builder ecsTaskMemoryLimitMiB(int ecsTaskMemoryLimitMiB) {
            this.ecsTaskMemoryLimitMiB = ecsTaskMemoryLimitMiB;
            return this;
        }

        public Builder targetGroupHealthCheckPath(String targetGroupHealthCheckPath) {
            this.targetGroupHealthCheckPath = targetGroupHealthCheckPath;
            return this;
        }

        public Builder springContainerPort(int springContainerPort) {
            this.springContainerPort = springContainerPort;
            return this;
        }

        public AwsCdkSpringPostgresEcsFargateStack build() {
            return new AwsCdkSpringPostgresEcsFargateStack(this);
        }
    }
}
