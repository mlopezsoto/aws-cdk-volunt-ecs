package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;

public class AwsCdkVoluntApp {
    public static void main(final String[] args) {
        App app = new App();

        String stackPrefix = "volunt";

        AwsCdkVoluntVpcStack awsCdkVoluntVpcStack = new AwsCdkVoluntVpcStack(app, stackPrefix);

        AwsCdkVoluntDatabaseStack awsCdkVoluntDatabaseStack = new AwsCdkVoluntDatabaseStack(app, stackPrefix, awsCdkVoluntVpcStack.getVpc());
        awsCdkVoluntDatabaseStack.addDependency(awsCdkVoluntVpcStack);

        AwsCdkVoluntEcsStack awsCdkVoluntEcsStack = new AwsCdkVoluntEcsStack(app, stackPrefix, awsCdkVoluntVpcStack.getVpc(),
                awsCdkVoluntDatabaseStack.getDatabaseInstance().getDbInstanceEndpointAddress(),
                awsCdkVoluntDatabaseStack.getDatabaseInstance().getDbInstanceEndpointPort(), awsCdkVoluntDatabaseStack.getDatabaseSecurityGroup());
        awsCdkVoluntEcsStack.addDependency(awsCdkVoluntDatabaseStack);

        app.synth();
    }
}

