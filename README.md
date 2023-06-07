# Java AWS CDK Stack for a Spring Boot App that uses a Database
This AWS CDK Stack deploys a Spring Boot based app to ECS. It assumes the image is a Spring boot that connects to a database.

## Requirements
- AWS Account
- Java AWS CDK project.

To install AWS CDK follow the steps outlined here: https://cdkworkshop.com/


## Stack Steps
* VPN creation with two subnets, one public one isolated (software.amazon.awscdk.services.ec2.Vpc).
* Postgres Database creation (software.amazon.awscdk.services.rds.DatabaseInstance).
* ECS Fargate Cluster creation (software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService).


## Usage example
Once you have your AWS CDK project, you can copy the class AwsCdkSpringPostgresEcsFargateStack to your project and use it. For instance:

```java
import software.amazon.awscdk.App;

public class AwsCdkSpringPostgresEcsFargateApp {
    public static void main(final String[] args) {
        App app = new App();
        
        AwsCdkSpringPostgresEcsFargateStack.Builder.create(app, "butterfly", "butterflydb", "butter", "flyingbutter", "'docker.io/mlopezsoto/volunt:latest'")
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
```

THe code above creates:
- CDK/Cloudformation Stack named 'butterfly' (Parameter 2)
- Database named 'butterflydb' (Parameter 3)
- Database username 'butter' (Parameter 4)
- Database password 'flyingbutter' (Parameter 5)
- Pulls and runs image from 'docker.io/mlopezsoto/volunt:latest'

Using the values above as an example, you can deploy your stack using:
> cdk deploy butterfly

After it finishes, it will display some results and your application will be available online. Your application should be 
available from the Load Balancer DNS name, which you can get from the output 'ALBDNSName'. The prefix changes depending on the
name you use for your stack, in our case it is 'butterfly'.

```
Outputs:
butterfly.ALBDNSName = butte-butte-DZD07FFTQKI5-224749273.ap-southeast-2.elb.amazonaws.com
butterfly.DatabaseAddress = butterfly-database-postgresql.cmwkrmicwxfc.ap-southeast-2.rds.amazonaws.com
butterfly.DatabasePort = 5432
butterfly.butterflyecsserviceLoadBalancerDNS7FF68FBE = butte-butte-DZD07FFTQKI5-224749273.ap-southeast-2.elb.amazonaws.com
butterfly.butterflyecsserviceServiceURL1D691D62 = http://butte-butte-DZD07FFTQKI5-224749273.ap-southeast-2.elb.amazonaws.com
butterfly.datasourceUrl = jdbc:postgresql://butterfly-database-postgresql.cmwkrmicwxfc.ap-southeast-2.rds.amazonaws.com:5432/butterflydb
```

## Sample App
The image 'docker.io/mlopezsoto/volunt:latest' contains a basic Spring Boot app that creates a user table
and exposes a couple of endpoints:

- /user/listAll
- /user/validateCredentials?username=<username>&password=<password>

You can test this using the CDK output 'butterfly.ALBDNSName' (sample above) and then appending the above paths.

butterfly.ALBDNSName = butte-butte-DZD07FFTQKI5-224749273.ap-southeast-2.elb.amazonaws.com
then: http://butte-butte-DZD07FFTQKI5-224749273.ap-southeast-2.elb.amazonaws.com/user/listAll

App code: https://github.com/mlopezsoto/volunt-be

Note: You can use your own image. This stack will deploy your image and set environment vars for your database access.

## Basic CDK commands
### List all Stacks
> cdk ls
### Deploy a stack, for instance a stack named 'butterfly' as in the sample code above
> cdk deploy butterfly
### Destroy (undeploy) a stack, for instance a stack named 'butterfly' as in the sample code above
> cdk destroy butterfly
