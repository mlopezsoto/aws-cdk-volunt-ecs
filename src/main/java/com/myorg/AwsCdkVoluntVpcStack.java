package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.ec2.IpAddresses;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.constructs.Construct;

import java.util.List;

public class AwsCdkVoluntVpcStack extends Stack {

    private final Vpc vpc;

    public AwsCdkVoluntVpcStack(final Construct scope, final String idPrefix) {
        super(scope, idPrefix + "-vpc-stack", null);

        vpc = Vpc.Builder.create(this, idPrefix + "-vpc-stack-vpc")
                .maxAzs(3)  // Default is all AZs in region
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

    public Vpc getVpc() {
        return vpc;
    }
}
