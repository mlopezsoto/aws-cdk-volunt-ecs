package com.myorg;

import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.rds.*;
import software.constructs.Construct;

import java.util.Collections;
import java.util.List;

public class AwsCdkVoluntDatabaseStack extends Stack {

    private final DatabaseInstance databaseInstance;

    private final ISecurityGroup databaseSecurityGroup;

    public AwsCdkVoluntDatabaseStack(final Construct scope, final String idPrefix, Vpc vpc) {
        super(scope, idPrefix + "-database", null);

        databaseSecurityGroup = SecurityGroup.fromSecurityGroupId(this, idPrefix, vpc.getVpcDefaultSecurityGroup());
        databaseSecurityGroup.addEgressRule(Peer.anyIpv4(), Port.tcp(5432));
        databaseSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(5432), "Ingress MLS");

        final IInstanceEngine instanceEngine = DatabaseInstanceEngine.postgres(
                PostgresInstanceEngineProps.builder()
                        .version(PostgresEngineVersion.VER_13_6)
                        .build()
        );

        databaseInstance = DatabaseInstance.Builder.create(this, idPrefix + "-database-postgres")
                .vpc(vpc)
                .databaseName("volunt")
                .securityGroups((Collections.singletonList(databaseSecurityGroup)))
                .vpcSubnets(SubnetSelection.builder().subnets(vpc.getIsolatedSubnets()).build())
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.MICRO))
                .engine(instanceEngine)
                .credentials(Credentials.fromPassword("volunt", SecretValue.unsafePlainText("voluntpwd")))
                .instanceIdentifier(idPrefix + "-database-postgres")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        new CfnOutput(this, "Database Address", CfnOutputProps.builder().value(databaseInstance.getDbInstanceEndpointAddress()).build());
        new CfnOutput(this, "Instance Endpoint", CfnOutputProps.builder().value(databaseInstance.getInstanceEndpoint().toString()).build());
        new CfnOutput(this, "Database Port", CfnOutputProps.builder().value(databaseInstance.getDbInstanceEndpointPort()).build());

    }

    public DatabaseInstance getDatabaseInstance() {
        return databaseInstance;
    }

    public ISecurityGroup getDatabaseSecurityGroup() {
        return databaseSecurityGroup;
    }
}

